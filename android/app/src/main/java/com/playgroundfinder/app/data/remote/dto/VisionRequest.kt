package com.playgroundfinder.app.data.remote.dto

/** Cloud Vision API annotate kérés burkoló objektuma. */
data class VisionAnnotateRequest(
    val requests: List<AnnotateImageRequest>
)

data class AnnotateImageRequest(
    val image: VisionImage,
    val features: List<VisionFeature>
)

/** Base64-kódolt képadat. */
data class VisionImage(
    val content: String
)

/**
 * @param type  pl. "LABEL_DETECTION", "OBJECT_LOCALIZATION"
 * @param maxResults  max visszaadott találat száma
 */
data class VisionFeature(
    val type: String,
    val maxResults: Int
)
