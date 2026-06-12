package com.example.model

data class EditState(
    val brightness: Float = 0f,         // -150 to 150 (0 is neutral)
    val contrast: Float = 1f,           // 0.3 to 2.0 (1.0 is neutral)
    val saturation: Float = 1f,         // 0.0 (grayscale) to 2.0 (super-saturated)
    val rotation: Float = 0f,           // 0, 90, 180, 270 degrees
    val isFlippedHorizontally: Boolean = false,
    val isFlippedVertically: Boolean = false,
    val selectedFilter: FilterType = FilterType.NONE,
    val strokes: List<DrawStroke> = emptyList(),
    // Crop bounding box: margins as fraction (0.0f to 1.0f) of original bitmap dimensions
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f
)

enum class FilterType {
    NONE,
    VINTAGE,
    MONOCHROME,
    WARM_TONE,
    COOL_TONE,
    SHARPNESS_BOOST,
    GRAIN_DREAM
}
