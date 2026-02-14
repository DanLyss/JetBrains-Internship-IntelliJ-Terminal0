package terminal.buffer

import terminal.model.Cell
import terminal.model.Color
import terminal.model.Style
import terminal.model.TerminalLine
import terminal.model.TextAttributes

class TerminalBuffer(
    val width: Int,
    val height: Int,
    val maxScrollbackSize: Int = 1000
) {
    init {
        require(width > 0) { "Width must be positive" }
        require(height > 0) { "Height must be positive" }
        require(maxScrollbackSize >= 0) { "Max scrollback size must be non-negative" }
    }

    private val screen: Array<TerminalLine> = Array(height) { TerminalLine(width) }
    private val scrollback: ArrayDeque<TerminalLine> = ArrayDeque()

    private var cursorColumn: Int = 0
    private var cursorRow: Int = 0

    var currentAttributes: TextAttributes = TextAttributes.DEFAULT
        private set

    fun setAttributes(foreground: Color, background: Color, styles: Set<Style>) {
        currentAttributes = TextAttributes.of(foreground, background, styles)
    }

    fun setAttributes(attributes: TextAttributes) {
        currentAttributes = attributes
    }

    fun getCursorPosition(): CursorPosition = CursorPosition(cursorColumn, cursorRow)

    fun setCursorPosition(column: Int, row: Int) {
        cursorColumn = column.coerceIn(0, width - 1)
        cursorRow = row.coerceIn(0, height - 1)
    }

    fun moveCursorUp(n: Int = 1) {
        cursorRow = (cursorRow - n).coerceIn(0, height - 1)
    }

    fun moveCursorDown(n: Int = 1) {
        cursorRow = (cursorRow + n).coerceIn(0, height - 1)
    }

    fun moveCursorLeft(n: Int = 1) {
        cursorColumn = (cursorColumn - n).coerceIn(0, width - 1)
    }

    fun moveCursorRight(n: Int = 1) {
        cursorColumn = (cursorColumn + n).coerceIn(0, width - 1)
    }

    fun getScreenLine(row: Int): TerminalLine {
        require(row in 0 until height) { "Row $row out of bounds [0, $height)" }
        return screen[row]
    }

    fun getScrollbackSize(): Int = scrollback.size

    fun getScrollbackLine(row: Int): TerminalLine {
        require(row in 0 until scrollback.size) { "Scrollback row $row out of bounds [0, ${scrollback.size})" }
        return scrollback[row]
    }

    internal fun scrollUp() {
        val topLine = screen[0].copyOf()
        if (maxScrollbackSize > 0) {
            scrollback.addLast(topLine)
            if (scrollback.size > maxScrollbackSize) {
                scrollback.removeFirst()
            }
        }
        for (i in 1 until height) {
            screen[i - 1] = screen[i]
        }
        screen[height - 1] = TerminalLine(width)
    }
}
