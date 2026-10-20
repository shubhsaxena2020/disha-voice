"""End-to-end CPU check of the bundled Disha Voice detector.

Runs the real EfficientDet-Lite0 model on a real image, decodes the SSD
outputs (boxes, class indices, scores, count), maps class ids to COCO names,
and reports the image-relative zone using the SAME boundaries the Android app
uses (0.38 / 0.62). This is evidence that the shipped model + decoding logic
can detect a real object, measured on this machine (not a phone).

Usage: python run_detection.py <model.tflite> <image> [more images...]
"""
import sys
import time
import numpy as np
from PIL import Image
import ai_edge_litert.interpreter as litert
from coco_labels import label_for_model_index

LEFT_END = 0.38
RIGHT_START = 0.62
SCORE_FLOOR = 0.45


def zone_of(cx):
    if cx < LEFT_END:
        return "LEFT"
    if cx > RIGHT_START:
        return "RIGHT"
    return "CENTER"


def main():
    model_path, images = sys.argv[1], sys.argv[2:]
    it = litert.Interpreter(model_path=model_path)
    it.allocate_tensors()
    inp = it.get_input_details()[0]
    outs = it.get_output_details()
    h, w = int(inp["shape"][1]), int(inp["shape"][2])
    print(f"model={model_path} input={w}x{h} dtype={inp['dtype'].__name__}")

    # Output tensors: boxes [1,N,4], classes [1,N], scores [1,N], count [1]
    by_shape = {}
    for d in outs:
        by_shape[tuple(d["shape"])] = d
    boxes_d = next(d for d in outs if len(d["shape"]) == 3)
    flat1 = [d for d in outs if len(d["shape"]) == 2]
    count_d = next(d for d in outs if len(d["shape"]) == 1)

    for img_path in images:
        img = Image.open(img_path).convert("RGB").resize((w, h), Image.BILINEAR)
        arr = np.asarray(img, dtype=np.float32)[None, ...]
        scale, zero = inp["quantization"]
        if inp["dtype"] == np.uint8:
            arr = arr.astype(np.uint8)
        elif inp["dtype"] == np.int8 and scale:
            arr = np.clip(np.round(arr / scale + zero), -128, 127).astype(np.int8)
        elif scale:
            arr = (arr - zero) * scale

        it.set_tensor(inp["index"], arr)
        t0 = time.perf_counter()
        it.invoke()
        dt = (time.perf_counter() - t0) * 1000.0

        boxes = it.get_tensor(boxes_d["index"])[0]
        count = int(it.get_tensor(count_d["index"])[0])
        # classes/scores: the two [1,N] tensors, distinguished by value range
        cand = [it.get_tensor(d["index"])[0] for d in flat1]
        classes = next(c for c in cand if c.max() > 1.5) if any(c.max() > 1.5 for c in cand) else cand[0]
        scores = next(c for c in cand if c.max() <= 1.5) if any(c.max() <= 1.5 for c in cand) else cand[-1]

        print(f"\nIMAGE {img_path}  inference={dt:.1f} ms (CPU, desktop)  num_detections={count}")
        shown = 0
        for i in range(min(count, len(scores))):
            s = float(scores[i])
            if s < SCORE_FLOOR:
                continue
            ymin, xmin, ymax, xmax = [float(v) for v in boxes[i]]
            cx = (xmin + xmax) / 2.0
            label = label_for_model_index(int(classes[i])) or f"class_{int(classes[i])}"
            print(f"  - {label:14s} score={s:.3f} box=({xmin:.3f},{ymin:.3f})-({xmax:.3f},{ymax:.3f}) "
                  f"centre_x={cx:.3f} zone={zone_of(cx)}")
            shown += 1
        if shown == 0:
            print("  (no detection above the 0.45 confidence floor)")


if __name__ == "__main__":
    main()
