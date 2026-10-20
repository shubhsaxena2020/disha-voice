"""Reproducible benchmark for the bundled Disha Voice detector.

Runs the real model on the real test images, N repeats each, and writes a
machine-readable JSON result plus a human-readable summary. Every number in
the output comes from an actual run; nothing is estimated.

IMPORTANT: this measures the model on the machine running this script (a
desktop CPU here). It is NOT a phone measurement. Phone numbers must be
collected on a real handset with the Android app before any user-facing
performance claim is made.

Usage:
  python run_benchmark.py --model ../model.tflite --images ../testdata --repeats 5 --out ../results/detector_benchmark.json
"""
import argparse
import hashlib
import json
import platform
import statistics
import time
from pathlib import Path

import numpy as np
from PIL import Image

import ai_edge_litert.interpreter as litert
from coco_labels import label_for_model_index

LEFT_END = 0.38
RIGHT_START = 0.62
SCORE_FLOOR = 0.45


def zone_of(cx: float) -> str:
    if cx < LEFT_END:
        return "LEFT"
    if cx > RIGHT_START:
        return "RIGHT"
    return "CENTER"


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", required=True, type=Path)
    ap.add_argument("--images", required=True, type=Path)
    ap.add_argument("--repeats", type=int, default=5)
    ap.add_argument("--out", required=True, type=Path)
    args = ap.parse_args()

    model_path = args.model.resolve()
    interpreter = litert.Interpreter(model_path=str(model_path))
    interpreter.allocate_tensors()
    inp = interpreter.get_input_details()[0]
    h, w = int(inp["shape"][1]), int(inp["shape"][2])

    images = sorted(p for p in args.images.iterdir() if p.suffix.lower() in {".jpg", ".jpeg", ".png"})
    if not images:
        raise SystemExit(f"No images found in {args.images}")

    results = []
    for img_path in images:
        img = Image.open(img_path).convert("RGB").resize((w, h), Image.BILINEAR)
        arr = np.asarray(img, dtype=np.float32)[None, ...]
        if inp["dtype"] == np.uint8:
            arr = arr.astype(np.uint8)

        timings = []
        detections = []
        for _ in range(args.repeats):
            interpreter.set_tensor(inp["index"], arr)
            t0 = time.perf_counter()
            interpreter.invoke()
            timings.append((time.perf_counter() - t0) * 1000.0)

            outs = interpreter.get_output_details()
            boxes_d = next(d for d in outs if len(d["shape"]) == 3)
            flat = [d for d in outs if len(d["shape"]) == 2]
            count_d = next(d for d in outs if len(d["shape"]) == 1)
            boxes = interpreter.get_tensor(boxes_d["index"])[0]
            count = int(interpreter.get_tensor(count_d["index"])[0])
            cand = [interpreter.get_tensor(d["index"])[0] for d in flat]
            classes = next(c for c in cand if c.max() > 1.5)
            scores = next(c for c in cand if c.max() <= 1.5)
            for i in range(min(count, len(scores))):
                if float(scores[i]) < SCORE_FLOOR:
                    continue
                ymin, xmin, ymax, xmax = [float(v) for v in boxes[i]]
                cx = (xmin + xmax) / 2.0
                detections.append({
                    "label": label_for_model_index(int(classes[i])) or f"class_{int(classes[i])}",
                    "score": round(float(scores[i]), 4),
                    "box_normalized": [round(xmin, 4), round(ymin, 4), round(xmax, 4), round(ymax, 4)],
                    "center_x": round(cx, 4),
                    "zone": zone_of(cx),
                })

        results.append({
            "image": img_path.name,
            "image_sha256": sha256(img_path),
            "latency_ms": {
                "repeats": args.repeats,
                "median": round(statistics.median(timings), 2),
                "min": round(min(timings), 2),
                "max": round(max(timings), 2),
                "stdev": round(statistics.stdev(timings), 2) if len(timings) > 1 else 0.0,
                "samples": [round(t, 2) for t in timings],
            },
            "detections_above_floor": detections,
        })
        top = detections[0] if detections else None
        print(f"{img_path.name:28s} median={statistics.median(timings):6.1f} ms  "
              f"min={min(timings):6.1f}  max={max(timings):6.1f}  "
              f"top={top['label'] + ' ' + str(top['score']) + ' ' + top['zone'] if top else 'none'}")

    payload = {
        "benchmark": "disha-voice-detector-cpu",
        "measured_on": "development machine (NOT a phone)",
        "host": {
            "platform": platform.platform(),
            "processor": platform.processor() or "unknown",
            "python": platform.python_version(),
        },
        "model": {
            "path": model_path.name,
            "sha256": sha256(model_path),
            "input_shape": [int(x) for x in inp["shape"]],
            "input_dtype": str(inp["dtype"]),
        },
        "parameters": {
            "repeats_per_image": args.repeats,
            "score_floor": SCORE_FLOOR,
            "zone_boundaries": {"left_end": LEFT_END, "right_start": RIGHT_START},
        },
        "results": results,
        "limitations": [
            "Desktop CPU measurement. Not a phone; do not present as device latency.",
            "Deterministic test images only; not an accuracy evaluation.",
            "No calibration of scores against ground truth was performed.",
        ],
    }
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    print(f"\nWrote {args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
