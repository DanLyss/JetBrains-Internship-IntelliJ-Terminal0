package terminal.buffer

import terminal.model.Cell
import terminal.model.CharWidth
import terminal.model.Color
import terminal.model.Style
import terminal.model.TerminalLine
import terminal.model.TextAttributes
import terminal.resize.ResizeStrategy

class TerminalBuffer(
    width: Int,
    height: Int,
    val maxScrollbackSize: Int = 1000
) {
    init {
        require(width > 0) { "Width must be positive" }
        require(height > 0) { "Height must be positive" }
        require(maxScrollbackSize >= 0) { "Max scrollback size must be non-negative" }
    }

    var width: Int = width
        private set
    var height: Int = height
        private set

    private var screen: Array<TerminalLine> = Array(height) { TerminalLine(width) }
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

    fun getChar(column: Int, row: Int): Char {
        require(column in 0 until width) { "Column $column out of bounds [0, $width)" }
        require(row in 0 until height) { "Row $row out of bounds [0, $height)" }
        return screen[row][column].character
    }

    fun getAttributes(column: Int, row: Int): TextAttributes {
        require(column in 0 until width) { "Column $column out of bounds [0, $width)" }
        require(row in 0 until height) { "Row $row out of bounds [0, $height)" }
        return screen[row][column].attributes
    }

    fun getScrollbackChar(column: Int, row: Int): Char {
        require(column in 0 until width) { "Column $column out of bounds [0, $width)" }
        require(row in 0 until scrollback.size) { "Scrollback row $row out of bounds [0, ${scrollback.size})" }
        return scrollback[row][column].character
    }

    fun getScrollbackAttributes(column: Int, row: Int): TextAttributes {
        require(column in 0 until width) { "Column $column out of bounds [0, $width)" }
        require(row in 0 until scrollback.size) { "Scrollback row $row out of bounds [0, ${scrollback.size})" }
        return scrollback[row][column].attributes
    }

    fun getLineText(row: Int): String {
        require(row in 0 until height) { "Row $row out of bounds [0, $height)" }
        return screen[row].getText()
    }

    fun getScrollbackLineText(row: Int): String {
        require(row in 0 until scrollback.size) { "Scrollback row $row out of bounds [0, ${scrollback.size})" }
        return scrollback[row].getText()
    }

    fun getScreenText(): String {
        return (0 until height).joinToString("\n") { screen[it].getText() }
    }

    fun getAllText(): String {
        val scrollbackText = (0 until scrollback.size).joinToString("\n") { scrollback[it].getText() }
        val screenText = getScreenText()
        return if (scrollback.isEmpty()) screenText else "$scrollbackText\n$screenText"
    }

    fun writeText(text: String) {
        for (char in text) {
            if (char == '\n') {
                cursorColumn = 0
                if (cursorRow == height - 1) {
                    scrollUp()
                } else {
                    cursorRow++
                }
                continue
            }

            val charWidth = CharWidth.of(char)

            if (charWidth == 2 && cursorColumn >= width - 1) {
                if (cursorColumn == width - 1) {
                    screen[cursorRow][cursorColumn] = Cell.EMPTY
                }
                cursorColumn = 0
                if (cursorRow == height - 1) {
                    scrollUp()
                } else {
                    cursorRow++
                }
            } else if (cursorColumn >= width) {
                cursorColumn = 0
                if (cursorRow == height - 1) {
                    scrollUp()
                } else {
                    cursorRow++
                }
            }

            clearWideCharAt(cursorRow, cursorColumn)
            if (charWidth == 2 && cursorColumn + 1 < width) {
                clearWideCharAt(cursorRow, cursorColumn + 1)
            }

            if (charWidth == 2) {
                screen[cursorRow][cursorColumn] = Cell.wide(char, currentAttributes)
                screen[cursorRow][cursorColumn + 1] = Cell.placeholder(currentAttributes)
                cursorColumn += 2
            } else {
                screen[cursorRow][cursorColumn] = Cell(char, currentAttributes)
                cursorColumn++
            }
        }
    }

    private fun clearWideCharAt(row: Int, column: Int) {
        val cell = screen[row][column]
        if (cell.isWide && column + 1 < width) {
            screen[row][column] = Cell.EMPTY
            screen[row][column + 1] = Cell.EMPTY
        } else if (cell.isPlaceholder && column - 1 >= 0 && screen[row][column - 1].isWide) {
            screen[row][column - 1] = Cell.EMPTY
            screen[row][column] = Cell.EMPTY
        }
    }

    // TODO: wide character support is not yet implemented for insertText
    fun insertText(text: String) {
        var index = 0
        while (index < text.length) {
            if (text[index] == '\n') {
                cursorColumn = 0
                if (cursorRow == height - 1) {
                    scrollUp()
                } else {
                    cursorRow++
                }
                index++
                continue
            }

            if (cursorColumn >= width) {
                cursorColumn = 0
                if (cursorRow == height - 1) {
                    scrollUp()
                } else {
                    cursorRow++
                }
            }

            val nextNewline = text.indexOf('\n', index)
            val charsUntilNewline = if (nextNewline == -1) text.length - index else nextNewline - index
            val spaceOnLine = width - cursorColumn
            val batchSize = minOf(charsUntilNewline, spaceOnLine)

            shiftRightBy(cursorRow, cursorColumn, batchSize)

            for (i in 0 until batchSize) {
                screen[cursorRow][cursorColumn] = Cell(text[index], currentAttributes)
                cursorColumn++
                index++
            }
        }
    }

    private fun shiftRightBy(startRow: Int, fromColumn: Int, count: Int) {
        if (count <= 0) return

        var previousOverflow: Array<Cell>? = null
        var row = startRow
        var shiftFrom = fromColumn

        while (row < height) {
            val currentOverflow = Array(count) { screen[row][width - count + it] }

            for (col in width - count - 1 downTo shiftFrom) {
                screen[row][col + count] = screen[row][col]
            }
            for (col in shiftFrom until minOf(shiftFrom + count, width)) {
                screen[row][col] = Cell.EMPTY
            }

            if (previousOverflow != null) {
                for (i in previousOverflow.indices) {
                    screen[row][i] = previousOverflow[i]
                }
            }

            if (currentOverflow.all { it == Cell.EMPTY }) break

            previousOverflow = currentOverflow
            row++
            shiftFrom = 0
        }
    }

    fun clearScreen() {
        for (row in 0 until height) {
            screen[row] = TerminalLine(width)
        }
        cursorColumn = 0
        cursorRow = 0
    }

    fun clearAll() {
        clearScreen()
        scrollback.clear()
    }

    fun fillLine(row: Int, character: Char) {
        require(row in 0 until height) { "Row $row out of bounds [0, $height)" }
        screen[row].fill(character, currentAttributes)
    }

    fun clearLine(row: Int) {
        require(row in 0 until height) { "Row $row out of bounds [0, $height)" }
        screen[row].clear()
    }

    fun insertNewLine() {
        scrollUp()
    }

    fun resize(newWidth: Int, newHeight: Int, strategy: ResizeStrategy) {
        require(newWidth > 0) { "Width must be positive" }
        require(newHeight > 0) { "Height must be positive" }

        val result = strategy.resize(screen, scrollback.toList(), newWidth, newHeight)

        screen = result.screen
        scrollback.clear()
        val trimmedScrollback = if (result.scrollback.size > maxScrollbackSize) {
            result.scrollback.drop(result.scrollback.size - maxScrollbackSize)
        } else {
            result.scrollback
        }
        for (line in trimmedScrollback) {
            scrollback.addLast(line)
        }

        width = newWidth
        height = newHeight
        cursorColumn = cursorColumn.coerceIn(0, newWidth - 1)
        cursorRow = cursorRow.coerceIn(0, newHeight - 1)
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
