package ml.data

data class MouseEvent(
    val rtime: Double,
    val ctime: Double,
    val button: String,
    val state: String,
    val x: Int,
    val y: Int
)