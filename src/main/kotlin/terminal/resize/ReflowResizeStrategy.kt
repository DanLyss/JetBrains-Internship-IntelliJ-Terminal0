package terminal.resize

import terminal.model.Cell
import terminal.model.TerminalLine

class ReflowResizeStrategy : ResizeStrategy {

    override fun resize(
        screen: Array<TerminalLine>,
        scrollback: List<TerminalLine>,
        newWidth: Int,
        newHeight: Int
    ): ResizeResult {
        val allLines = scrollback + screen.toList()
        val unwrapped = unwrapLines(allLines)
        val reflowed = reflowLines(unwrapped, newWidth)

        val screenStart = maxOf(0, reflowed.size - newHeight)
        val newScrollback = reflowed.subList(0, screenStart)
        val screenLines = reflowed.subList(screenStart, reflowed.size)

        val newScreen = Array(newHeight) { row ->
            if (row < screenLines.size) {
                screenLines[row]
            } else {
                TerminalLine(newWidth)
            }
        }

        return ResizeResult(
            screen = newScreen,
            scrollback = newScrollback
        )
    }

    private fun unwrapLines(lines: List<TerminalLine>): List<List<Cell>> {
        val result = mutableListOf<MutableList<Cell>>()
        var current = mutableListOf<Cell>()

        for (line in lines) {
            for (col in 0 until line.width) {
                current.add(line[col])
            }

            val isFull = line[line.width - 1] != Cell.EMPTY
            if (!isFull) {
                val trimmed = current.dropLastWhile { it == Cell.EMPTY }.toMutableList()
                result.add(trimmed)
                current = mutableListOf()
            }
        }

        if (current.isNotEmpty()) {
            val trimmed = current.dropLastWhile { it == Cell.EMPTY }.toMutableList()
            result.add(trimmed)
        }

        return result
    }

    private fun reflowLines(unwrapped: List<List<Cell>>, newWidth: Int): List<TerminalLine> {
        val result = mutableListOf<TerminalLine>()
        for (cells in unwrapped) {
            if (cells.isEmpty()) {
                result.add(TerminalLine(newWidth))
                continue
            }
            var offset = 0
            while (offset < cells.size) {
                val line = TerminalLine(newWidth)
                val count = minOf(newWidth, cells.size - offset)
                for (i in 0 until count) {
                    line[i] = cells[offset + i]
                }
                result.add(line)
                offset += count
            }
        }
        return result
    }
}
