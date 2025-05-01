package ru.ptrff.photopano.ui.parameters

data class ParametersState(
    val shootingDuration: Float = 1f,
    val prepareDuration: Int = 3,
    val interpolate: Boolean = false,
    val reverse: Boolean = true,
    val upload: Boolean = true,
    val gifType: GifType = GifType.DEFAULT_REVERSE,
)

enum class GifType(val path: String) {
    DEFAULT("sample.gif"),
    DEFAULT_REVERSE("sample_reverse.gif"),
    INTERPOLATED("sample_interpolated.gif"),
    INTERPOLATED_REVERSE("sample_interpolated_reverse.gif")
}
