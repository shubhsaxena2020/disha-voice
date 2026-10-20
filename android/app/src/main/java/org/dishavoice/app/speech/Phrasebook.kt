package org.dishavoice.app.speech

import org.dishavoice.core.ConfidenceBand
import org.dishavoice.core.ImageZone

/**
 * Spoken phrases for every product state.
 *
 * Wording rules applied throughout:
 *  - never imply distance, a safe route, or that a path is clear;
 *  - "nothing detected" is phrased as a statement about the model's view, never
 *    as reassurance about the surroundings;
 *  - tentative detections are announced as tentative;
 *  - image-relative position is always described as being in the image.
 */
data class Phrasebook(
    val hindi: Boolean,
) {
    private fun pick(hi: String, en: String): String = if (hindi) hi else en

    fun started(): String = pick(
        "कैमरा शुरू। मैं बताऊँगा कि तस्वीर में क्या दिख रहा है।",
        "Camera started. I will say what I can see in the image.",
    )

    fun stopped(): String = pick(
        "कैमरा बंद।",
        "Camera stopped.",
    )

    fun objectFound(label: String, zone: ImageZone, band: ConfidenceBand): String {
        val thing = objectName(label, hindi)
        val position = when (zone) {
            ImageZone.LEFT -> pick("तस्वीर के बाईं ओर", "on the left of the image")
            ImageZone.CENTER -> pick("तस्वीर के बीच में", "in the middle of the image")
            ImageZone.RIGHT -> pick("तस्वीर के दाईं ओर", "on the right of the image")
        }
        val certainty = if (band == ConfidenceBand.TENTATIVE) {
            pick("लगता है", "it looks like")
        } else {
            pick("दिख रहा है", "there is")
        }
        return pick(
            "$position $certainty $thing दिख रहा है।",
            "There is $thing $position.",
        )
    }

    fun lowConfidence(): String = pick(
        "कुछ दिखा, लेकिन पक्का नहीं है।",
        "Something appeared, but I am not sure.",
    )

    /** Never says the path is clear; it only describes what the model did not find. */
    fun nothingDetected(): String = pick(
        "इस तस्वीर में मुझे कोई जानी-पहचानी चीज़ नहीं दिखी।",
        "I did not recognise anything in this image.",
    )

    fun permissionNeeded(): String = pick(
        "कैमरे की अनुमति चाहिए। बिना कैमरे के मैं कुछ नहीं देख सकता।",
        "Camera permission is needed. Without the camera I cannot see anything.",
    )

    fun cameraError(): String = pick(
        "कैमरा शुरू नहीं हो सका।",
        "The camera could not be started.",
    )

    fun modelUnavailable(): String = pick(
        "पहचानने वाला मॉडल लोड नहीं हो सका।",
        "The recognition model could not be loaded.",
    )

    fun speechUnavailable(): String = pick(
        "इस फ़ोन पर हिंदी आवाज़ उपलब्ध नहीं है, इसलिए मैं अंग्रेज़ी में बोलूँगा।",
        "A Hindi voice is not available on this phone, so I will speak English.",
    )

    fun repeat(): String = pick(
        "आख़िरी बात दोहराता हूँ।",
        "Repeating the last message.",
    )

    fun help(): String = pick(
        "मैं कैमरे की तस्वीर में आम चीज़ें पहचानता हूँ और बताता हूँ कि वे तस्वीर में कहाँ हैं।",
        "I recognise common objects in the camera image and say where they are in the image.",
    )

    /** Translates a COCO class into a Hindi word when one is known. */
    private fun objectName(label: String, useHindi: Boolean): String {
        if (!useHindi) return label
        return HINDI_OBJECT_NAMES[label] ?: label
    }

    companion object {
        /** Hindi names for the COCO classes most likely to appear in a home scene. */
        val HINDI_OBJECT_NAMES: Map<String, String> = mapOf(
            "person" to "एक व्यक्ति",
            "bicycle" to "एक साइकिल",
            "car" to "एक कार",
            "motorcycle" to "एक मोटरसाइकिल",
            "bus" to "एक बस",
            "train" to "एक ट्रेन",
            "truck" to "एक ट्रक",
            "boat" to "एक नाव",
            "bird" to "एक पक्षी",
            "cat" to "एक बिल्ली",
            "dog" to "एक कुत्ता",
            "horse" to "एक घोड़ा",
            "sheep" to "एक भेड़",
            "cow" to "एक गाय",
            "backpack" to "एक बैग",
            "umbrella" to "एक छाता",
            "handbag" to "एक हैंडबैग",
            "tie" to "एक टाई",
            "suitcase" to "एक सूटकेस",
            "bottle" to "एक बोतल",
            "wine glass" to "एक गिलास",
            "cup" to "एक कप",
            "fork" to "एक कांटा",
            "knife" to "एक चाकू",
            "spoon" to "एक चम्मच",
            "bowl" to "एक कटोरी",
            "banana" to "एक केला",
            "apple" to "एक सेब",
            "sandwich" to "एक सैंडविच",
            "orange" to "एक संतरा",
            "broccoli" to "एक ब्रोकली",
            "carrot" to "एक गाजर",
            "pizza" to "एक पिज़्ज़ा",
            "donut" to "एक डोनट",
            "cake" to "एक केक",
            "chair" to "एक कुर्सी",
            "couch" to "एक सोफ़ा",
            "potted plant" to "एक गमले का पौधा",
            "bed" to "एक बिस्तर",
            "dining table" to "एक मेज़",
            "toilet" to "एक शौचालय",
            "tv" to "एक टीवी",
            "laptop" to "एक लैपटॉप",
            "mouse" to "एक माउस",
            "remote" to "एक रिमोट",
            "keyboard" to "एक कीबोर्ड",
            "cell phone" to "एक मोबाइल फ़ोन",
            "microwave" to "एक माइक्रोवेव",
            "oven" to "एक ओवन",
            "toaster" to "एक टोस्टर",
            "sink" to "एक नल",
            "refrigerator" to "एक फ़्रिज",
            "book" to "एक किताब",
            "clock" to "एक घड़ी",
            "vase" to "एक फूलदान",
            "scissors" to "एक कैंची",
            "teddy bear" to "एक खिलौना भालू",
            "toothbrush" to "एक दाँत का ब्रश",
        )
    }
}
