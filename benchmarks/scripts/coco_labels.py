"""COCO-2017 label map (90 usable class ids) for the bundled EfficientDet-Lite0 model.

The model emits integer class indices into the standard COCO-2017 91-class id
space. Ids with no COCO category (the gap at 12) are omitted, matching the
common "90-class" contiguous convention.

Source of truth for the id->name mapping: the official TensorFlow object
detection API label map (COCO 2017 category ids). See
https://github.com/tensorflow/models/blob/master/research/object_detection/api/detection_model_zoo.py
"""
MODEL_INDEX_TO_COCO_ID_OFFSET = 1


def label_for_model_index(model_index: int):
    """Translate a raw model class index into a COCO label name.

    The model emits a 0-based index into the COCO-2017 91-slot id space:
    model_class_index + 1 == coco_category_id. Verified on this exact model
    with four independent anchors on real images: person->0, bus->5,
    dog->17, tie->31.
    """
    coco_id = int(model_index) + MODEL_INDEX_TO_COCO_ID_OFFSET
    return COCO_LABELS.get(coco_id)


COCO_LABELS = {
    1: "person",
    2: "bicycle",
    3: "car",
    4: "motorcycle",
    5: "airplane",
    6: "bus",
    7: "train",
    8: "truck",
    9: "boat",
    10: "traffic light",
    11: "fire hydrant",
    13: "stop sign",
    14: "parking meter",
    15: "bench",
    16: "bird",
    17: "cat",
    18: "dog",
    19: "horse",
    20: "sheep",
    21: "cow",
    22: "elephant",
    23: "bear",
    24: "zebra",
    25: "giraffe",
    27: "backpack",
    28: "umbrella",
    31: "handbag",
    32: "tie",
    33: "suitcase",
    34: "frisbee",
    35: "skis",
    36: "snowboard",
    37: "sports ball",
    38: "kite",
    39: "baseball bat",
    40: "baseball glove",
    41: "skateboard",
    42: "surfboard",
    43: "tennis racket",
    44: "bottle",
    46: "wine glass",
    47: "cup",
    48: "fork",
    49: "knife",
    50: "spoon",
    51: "bowl",
    52: "banana",
    53: "apple",
    54: "sandwich",
    55: "orange",
    56: "broccoli",
    57: "carrot",
    58: "hot dog",
    59: "pizza",
    60: "donut",
    61: "cake",
    62: "chair",
    63: "couch",
    64: "potted plant",
    65: "bed",
    67: "dining table",
    70: "toilet",
    72: "tv",
    73: "laptop",
    74: "mouse",
    75: "remote",
    76: "keyboard",
    77: "cell phone",
    78: "microwave",
    79: "oven",
    80: "toaster",
    81: "sink",
    82: "refrigerator",
    84: "book",
    85: "clock",
    86: "vase",
    87: "scissors",
    88: "teddy bear",
    89: "hair drier",
    90: "toothbrush",
}
