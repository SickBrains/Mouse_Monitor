package ml.data

enum class MouseActionType { MM, PC, DD }

data class MouseAction(
    val type: MouseActionType,
    val events: List<MouseEvent>
)
