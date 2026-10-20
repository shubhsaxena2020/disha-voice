"""Inspect a TFLite/LiteRT model's real tensor contract and run a CPU inference.

This is the evidence step for Disha Voice's detector: it establishes the true
input/output names, types and shapes of the bundled model instead of assuming
the published EfficientDet layout.

Usage:
  python inspect_model.py <model.tflite> [--image path]
"""
import sys
import numpy as np
import ai_edge_litert.interpreter as litert


def describe(path):
    it = litert.Interpreter(model_path=path)
    it.allocate_tensors()
    print(f"MODEL: {path}")
    print("INPUTS:")
    for d in it.get_input_details():
        print(f"  name={d['name']!r} shape={d['shape']} dtype={d['dtype'].__name__} "
              f"quant={d['quantization']}")
    print("OUTPUTS:")
    for d in it.get_output_details():
        print(f"  name={d['name']!r} shape={d['shape']} dtype={d['dtype'].__name__} "
              f"quant={d['quantization']}")
    return it


def main():
    path = sys.argv[1]
    it = describe(path)

    img = None
    if "--image" in sys.argv:
        from PIL import Image
        img_path = sys.argv[sys.argv.index("--image") + 1]
        img = Image.open(img_path).convert("RGB")
        print(f"\nIMAGE: {img_path} size={img.size}")

    inp = it.get_input_details()[0]
    h, w = int(inp["shape"][1]), int(inp["shape"][2])
    if img is None:
        # Synthetic probe: mid-grey image. Purpose is to validate the tensor
        # plumbing, NOT to claim any detection.
        arr = np.full((1, h, w, 3), 128, dtype=np.float32)
        print(f"\nNo --image given: running a synthetic {h}x{w} probe "
              "(validates plumbing only, proves no detection)")
    else:
        arr = np.asarray(img.resize((w, h), Image.BILINEAR), dtype=np.float32)[None, ...]

    scale, zero = inp["quantization"]
    if inp["dtype"] == np.uint8:
        arr = arr.astype(np.uint8)
    elif inp["dtype"] == np.int8:
        if scale:
            arr = np.clip(np.round(arr / scale + zero), -128, 127).astype(np.int8)
    else:
        if scale:
            arr = (arr - zero) * scale

    it.set_tensor(inp["index"], arr)
    it.invoke()

    print("\nRAW OUTPUTS:")
    for d in it.get_output_details():
        out = it.get_tensor(d["index"])
        flat = out.reshape(-1)
        print(f"  {d['name']!r} shape={out.shape} dtype={out.dtype}")
        print(f"     first8={np.array2string(flat[:8], precision=4, threshold=20)}")
        if flat.size > 1:
            print(f"     min={flat.min():.4f} max={flat.max():.4f} mean={flat.mean():.4f}")


if __name__ == "__main__":
    main()
