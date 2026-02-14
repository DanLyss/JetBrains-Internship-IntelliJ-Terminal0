package terminal.model

class TerminalLine(val width: Int) {

    private val cells: Array<Cell> = Array(width) { Cell.EMPTY }

    operator fun get(column: Int): Cell {
        requireValidColumn(column)
        return cells[column]
    }

    operator fun set(column: Int, cell: Cell) {
        requireValidColumn(column)
        cells[column] = cell
    }

    fun clear() {
        for (i in cells.indices) {
            cells[i] = Cell.EMPTY
        }
    }

    fun fill(character: Char, attributes: TextAttributes) {
        val cell = Cell(character, attributes)
        for (i in cells.indices) {
            cells[i] = cell
        }
    }

    fun getText(): String {
        val builder = StringBuilder(width)
        for (cell in cells) {
            builder.append(cell.character)
        }
        return builder.toString().trimEnd()
    }

    fun copyOf(): TerminalLine {
        val copy = TerminalLine(width)
        cells.copyInto(copy.cells)
        return copy
    }

    private fun requireValidColumn(column: Int) {
        require(column in 0 until width) { "Column $column out of bounds [0, $width)" }
    }
}
