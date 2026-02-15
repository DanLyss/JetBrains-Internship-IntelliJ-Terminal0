package terminal.resize

import terminal.model.TerminalLine

interface ResizeStrategy {
    fun resize(
        screen: Array<TerminalLine>,
        scrollback: List<TerminalLine>,
        newWidth: Int,
        newHeight: Int
    ): ResizeResult
}

data class ResizeResult(
    val screen: Array<TerminalLine>,
    val scrollback: List<TerminalLine>
)
