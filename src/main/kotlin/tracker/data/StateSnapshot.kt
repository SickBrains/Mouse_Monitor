package tracker.data

data class StateSnapshot(
    val timestamp: Long,
    val x: Int,
    val y: Int,
    val left: Boolean,
    val right: Boolean,
    val middle: Boolean,
    val windowTitle: String
) {
    fun equivalentTo(other: StateSnapshot): Boolean {
        return x == other.x &&
                y == other.y &&
                left == other.left &&
                right == other.right &&
                middle == other.middle &&
                windowTitle == other.windowTitle
    }
}
