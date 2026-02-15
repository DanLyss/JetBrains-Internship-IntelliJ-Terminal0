package terminal.resize

import terminal.model.Cell
import terminal.model.TerminalLine

class TruncateResizeStrategy : ResizeStrategy {

    override fun resize(
        screen: Array<TerminalLine>,
        scrollback: List<TerminalLine>,
        newWidth: Int,
        newHeight: Int
    ): ResizeResult {
        val oldHeight = screen.size

        val newScrollback = scrollback.map { resizeLine(it, newWidth) }

        val newScreen = Array(newHeight) { row ->
            if (row < oldHeight) {
                resizeLine(screen[row], newWidth)
            } else {
                TerminalLine(newWidth)
            }
        }

        val extraScrollback = if (newHeight < oldHeight) {
            (newHeight until oldHeight).map { resizeLine(screen[it], newWidth) }
        } else {
            emptyList()
        }

        return ResizeResult(
            screen = newScreen,
            scrollback = newScrollback + extraScrollback
        )
    }

    private fun resizeLine(line: TerminalLine, newWidth: Int): TerminalLine {
        val newLine = TerminalLine(newWidth)
        val copyWidth = minOf(line.width, newWidth)
        for (col in 0 until copyWidth) {
            newLine[col] = line[col]
        }
        return newLine
    }
}
