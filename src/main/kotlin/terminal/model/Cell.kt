package terminal.model

data class Cell(
    val character: Char,
    val attributes: TextAttributes
) {
    companion object {
        val EMPTY = Cell(' ', TextAttributes.DEFAULT)
    }
}
