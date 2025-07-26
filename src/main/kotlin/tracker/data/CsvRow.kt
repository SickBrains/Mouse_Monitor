package tracker.data

data class CsvRow(
    val start: Long,
    val end: Long,
    val x: Int,
    val y: Int,
    val left: Int,
    val right: Int,
    val middle: Int,
    val window: String,
    val repeats: Int
)
