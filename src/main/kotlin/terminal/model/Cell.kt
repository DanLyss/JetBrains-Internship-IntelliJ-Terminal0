package terminal.model

data class Cell(
    val character: Char,
    val attributes: TextAttributes,
    val isWide: Boolean = false,
    val isPlaceholder: Boolean = false
) {
    companion object {
        val EMPTY = Cell(' ', TextAttributes.DEFAULT)

        fun wide(character: Char, attributes: TextAttributes): Cell =
            Cell(character, attributes, isWide = true)

        fun placeholder(attributes: TextAttributes): Cell =
            Cell(' ', attributes, isPlaceholder = true)
    }
}
