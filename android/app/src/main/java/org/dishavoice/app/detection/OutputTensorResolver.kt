package org.dishavoice.app.detection

/**
 * Resolves which of the bundled EfficientDet-Lite0 model's output tensors is
 * the box tensor, the class-id tensor, the score tensor, and the count tensor.
 *
 * Kept free of TensorFlow types so it can be unit tested on the JVM.
 *
 * ## Why this is not done by looking at the values
 *
 * The class-id tensor `[1, N]` and the score tensor `[1, N]` have the **same
 * shape**, so shape alone cannot separate them. The obvious workaround -
 * "scores are bounded to 0..1, class ids are integral" - does not work, and
 * the failure is silent and total.
 *
 * On a freshly allocated interpreter **every output tensor reads as zero**,
 * which is inside 0..1. A value-range test therefore classifies the class-id
 * tensor as the score tensor, leaves the class-id tensor unassigned, and
 * throws. Because the throw happens *before* `run()`, the buffers are never
 * populated, so every later call fails the same way. The detector would
 * return an empty list forever: the camera preview would run, and the app
 * would never announce a single object, with no error surfaced anywhere.
 *
 * This was measured against the shipped model, not assumed - see
 * `RESEARCH/platform-api-audit.md` section 2.
 *
 * ## The rule used instead
 *
 * Position and shape, never values:
 *
 *  - the one 3-D output whose last dimension is 4 is the **boxes** tensor;
 *  - the one 1-D output is the **count** tensor;
 *  - of the two remaining 2-D outputs, the earlier is the **class ids** and
 *    the later is the **scores**.
 *
 * The last rule is the model's documented output order, and it is pinned by
 * `OutputTensorResolverTest` against the contract measured from the shipped
 * asset, so replacing the model breaks the test rather than silently
 * mis-decoding detections.
 */
internal object OutputTensorResolver {

    /** The model emits exactly four output tensors. */
    const val EXPECTED_OUTPUT_COUNT = 4

    /** Resolved interpreter ordinals. */
    data class Resolved(
        val boxes: Int,
        val classes: Int,
        val scores: Int,
        val count: Int,
    )

    /**
     * @param shapes the output tensor shapes, in interpreter ordinal order.
     * @return the resolved ordinals.
     * @throws IllegalArgumentException if the layout is not the expected one.
     */
    fun resolve(shapes: List<IntArray>): Resolved {
        require(shapes.size == EXPECTED_OUTPUT_COUNT) {
            "expected $EXPECTED_OUTPUT_COUNT output tensors, got ${shapes.size}"
        }

        val boxes = shapes.indices.singleOrNull { shapes[it].size == 3 && shapes[it][2] == 4 }
            ?: throw IllegalArgumentException(
                "no output tensor of shape [1, N, 4]; got ${shapes.map { it.toList() }}"
            )

        val count = shapes.indices.singleOrNull { shapes[it].size == 1 }
            ?: throw IllegalArgumentException(
                "expected exactly one 1-D output tensor; got ${shapes.map { it.toList() }}"
            )

        val twoDimensional = shapes.indices.filter { shapes[it].size == 2 }
        require(twoDimensional.size == 2) {
            "expected exactly two 2-D output tensors, got ${twoDimensional.size}"
        }

        // Shape cannot separate these two, so position is used. See the class
        // comment for why the value-range alternative is unsafe.
        val classes = twoDimensional[0]
        val scores = twoDimensional[1]

        require(shapes[boxes][0] == 1 && shapes[boxes][1] > 0) {
            "boxes tensor must be [1, N, 4]; got ${shapes[boxes].toList()}"
        }
        require(shapes[classes].contentEquals(shapes[scores])) {
            "class-id and score tensors must share a shape; got " +
                "${shapes[classes].toList()} and ${shapes[scores].toList()}"
        }
        require(shapes[count][0] == 1) {
            "count tensor must be [1]; got ${shapes[count].toList()}"
        }

        return Resolved(boxes = boxes, classes = classes, scores = scores, count = count)
    }
}
