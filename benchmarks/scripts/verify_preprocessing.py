#!/usr/bin/env python3
"""Verify which input convention the bundled EfficientDet-Lite0 actually expects.

Why this exists
---------------
The Android app (`android/app/src/main/java/org/dishavoice/app/detection/
EfficientDetDetector.kt`) feeds **raw uint8 RGB (0-255)** into the model's uint8
input tensor. The desktop benchmark (`benchmarks/scripts/run_detection.py`)
does the same.

Those two sharing a convention is reassuring but not proof: if the model had
been trained for a different convention, both would be wrong together and the
benchmark would never reveal it. This script runs the real model on the real
test images under three conventions and reports what each produces. The
convention that names the object in the picture is the correct one.

The conventions tested
----------------------
``raw``
    Raw uint8 pixels, 0-255. **What the app does.**
``norm11``
    Normalise to [-1, 1], then requantise with the input tensor's
    scale/zero_point. The textbook convention for a quantised EfficientDet.
``imagenet``
    (x/255 - mean) / std with the ImageNet statistics, then requantise.

Measured result (see RESEARCH/model-and-data-audit.md):

  - ``raw`` and ``norm11`` agree closely, because this model's input
    quantisation (scale 0.0078125, zero_point 127) maps the uint8 range onto
    the [-1, 1] range, so requantising a [-1, 1] value lands back on
    approximately the same uint8 value.
  - ``imagenet`` is clearly **worse** (bus 0.801 -> 0.668, dog 0.840 -> 0.801),
    which confirms the model does not expect mean/std normalisation.

Usage:
    python verify_preprocessing.py <model.tflite> <image...>

Requires ``ai_edge_litert`` and ``Pillow``.
"""

from __future__ import annotations

import sys
import time

import numpy as np
from PIL import Image

try:
    from ai_edge_litert.interpreter import Interpreter
except ImportError:  # pragma: no cover
    from ai_edge_litert.python.interpreter import Interpreter  # type: ignore

from coco_labels import label_for_model_index


def build(rgb_uint8: np.ndarray, h: int, w: int, scale: float, zero: int, mode: str) -> np.ndarray:
    """Build the input tensor under the named convention."""
    if mode == "raw":
        # exactly what the Android app does
        return rgb_uint8.reshape(1, h, w, 3)
    if mode == "norm11":
        real = rgb_uint8.astype(np.float32) / 127.5 - 1.0
        return np.clip(np.round(real / scale) + zero, 0, 255).astype(np.uint8).reshape(1, h, w, 3)
    if mode == "imagenet":
        mean = np.array([0.485, 0.456, 0.406], dtype=np.float32)
        std = np.array([0.229, 0.224, 0.225], dtype=np.float32)
        real = (rgb_uint8.astype(np.float32) / 255.0 - mean) / std
        return np.clip(np.round(real / scale) + zero, 0, 255).astype(np.uint8).reshape(1, h, w, 3)
    raise ValueError(f"unknown mode: {mode}")


def label_for(model_index: int) -> str:
    """Map a model class index to a COCO label.

    The verified contract for this model is ``model_index + 1 == coco_id``,
    and the offset is applied by ``label_for_model_index`` in ``coco_labels``,
    which is the single source of truth shared with the Kotlin detector.
    """
    name = label_for_model_index(model_index)
    return name if name else f"id{model_index}"


def main() -> int:
    if len(sys.argv) < 3:
        print(__doc__)
        return 2

    model = sys.argv[1]
    images = sys.argv[2:]

    it = Interpreter(model_path=model)
    it.allocate_tensors()
    inp = it.get_input_details()[0]
    outs = it.get_output_details()
    h, w = int(inp["shape"][1]), int(inp["shape"][2])
    scale, zero = inp["quantization"]
    print(f"model input: {w}x{h} dtype={inp['dtype'].__name__} scale={scale} zero_point={zero}")
    print()

    boxes_d = next(d for d in outs if len(d["shape"]) == 3)
    flat1 = [d for d in outs if len(d["shape"]) == 2]
    count_d = next(d for d in outs if len(d["shape"]) == 1)

    def run(arr):
        it.set_tensor(inp["index"], arr)
        t0 = time.perf_counter()
        it.invoke()
        dt = (time.perf_counter() - t0) * 1000.0
        boxes = it.get_tensor(boxes_d["index"])[0]
        count = int(it.get_tensor(count_d["index"])[0])
        cand = [it.get_tensor(d["index"])[0] for d in flat1]
        classes = next(c for c in cand if c.max() > 1.5) if any(c.max() > 1.5 for c in cand) else cand[0]
        scores = next(c for c in cand if c.max() <= 1.5) if any(c.max() <= 1.5 for c in cand) else cand[-1]
        return boxes, classes, scores, count, dt

    modes = ["raw", "norm11", "imagenet"]
    for img_path in images:
        img = Image.open(img_path).convert("RGB").resize((w, h), Image.BILINEAR)
        arr = np.asarray(img, dtype=np.uint8)
        print(f"--- {img_path.replace(chr(92), '/').split('/')[-1]} ---")
        for mode in modes:
            data = build(arr, h, w, scale, zero, mode)
            _boxes, classes, scores, count, dt = run(data)
            n = min(int(count), 5)
            parts = []
            for i in range(n):
                parts.append(f"{label_for(int(classes[i]))} {scores[i]:.3f}")
            top = ", ".join(parts) if parts else "(no detections)"
            print(f"  {mode:9s} count={int(count):2d}  {top}")
        print()

    print("The convention that names the object in the image is the correct one.")
    print("The Android app uses 'raw'; the desktop benchmark uses 'raw'.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
