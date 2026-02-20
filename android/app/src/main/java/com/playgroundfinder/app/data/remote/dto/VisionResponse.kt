package com.playgroundfinder.app.data.remote.dto

/** Cloud Vision API annotate válasz burkoló objektuma. */
data class VisionAnnotateResponse(
    val responses: List<VisionImageAnnotation> = emptyList()
)

data class VisionImageAnnotation(
    /** LABEL_DETECTION eredmények — általános képcímkék. */
    val labelAnnotations: List<EntityAnnotation>? = null,
    /** OBJECT_LOCALIZATION eredmények — befoglaló téglalappal. */
    val localizedObjectAnnotations: List<LocalizedObjectAnnotation>? = null
)

/** Egy képcímke (pl. "Playground", "Park") a hozzá tartozó megbízhatóság-értékkel. */
data class EntityAnnotation(
    val description: String = "",
    /** 0.0–1.0 közötti megbízhatóság. */
    val score: Float = 0f
)

/** Felismert objektum normalizált befoglaló téglalappal. */
data class LocalizedObjectAnnotation(
    val name: String = "",
    /** 0.0–1.0 közötti megbízhatóság. */
    val score: Float = 0f,
    val boundingPoly: NormalizedBoundingPoly? = null
)

data class NormalizedBoundingPoly(
    /** Normalizált csúcspontok: x, y ∈ [0, 1] a képen belül. */
    val normalizedVertices: List<NormalizedVertex> = emptyList()
)

data class NormalizedVertex(
    val x: Float = 0f,
    val y: Float = 0f
)
