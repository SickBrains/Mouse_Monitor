package tracker.data

data class StateSnapshot(
    val timestamp: Long,
    val x: Int,
    val y: Int,
    val left: Boolean,
    val right: Boolean,
    val middle: Boolean,
    val x1: Boolean,
    val x2: Boolean,
    val ctrl: Boolean,
    val shift: Boolean,
    val alt: Boolean,
    val win: Boolean,
    val windowTitle: String
) {
    fun equivalentTo(other: StateSnapshot): Boolean {
        return x == other.x &&
                y == other.y &&
                left == other.left &&
                right == other.right &&
                middle == other.middle &&
                x1 == other.x1 &&
                x2 == other.x2 &&
                ctrl == other.ctrl &&
                shift == other.shift &&
                alt == other.alt &&
                win == other.win &&
                windowTitle == other.windowTitle
    }
}
