package tracker.convert

data class SystemMetadata(
    val screenWidth: Int,
    val screenHeight: Int,
    val dpi: Int,
    val mouseSpeed: Int,
    val parquetVersion: String,
    val conversionTimestamp: String
)
