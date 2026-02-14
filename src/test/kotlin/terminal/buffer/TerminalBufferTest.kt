package terminal.buffer

import terminal.model.Cell
import terminal.model.Color
import terminal.model.Style
import terminal.model.TextAttributes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TerminalBufferTest {

    @Test
    fun `buffer has correct dimensions`() {
        val buf = TerminalBuffer(80, 24)
        assertEquals(80, buf.width)
        assertEquals(24, buf.height)
    }

    @Test
    fun `invalid dimensions throw`() {
        assertFailsWith<IllegalArgumentException> { TerminalBuffer(0, 24) }
        assertFailsWith<IllegalArgumentException> { TerminalBuffer(80, 0) }
        assertFailsWith<IllegalArgumentException> { TerminalBuffer(80, 24, -1) }
    }

    @Test
    fun `initial cursor is at 0 0`() {
        val buf = TerminalBuffer(80, 24)
        assertEquals(CursorPosition(0, 0), buf.getCursorPosition())
    }

    @Test
    fun `set cursor position`() {
        val buf = TerminalBuffer(80, 24)
        buf.setCursorPosition(10, 5)
        assertEquals(CursorPosition(10, 5), buf.getCursorPosition())
    }

    @Test
    fun `set cursor clamps to bounds`() {
        val buf = TerminalBuffer(10, 5)
        buf.setCursorPosition(100, 100)
        assertEquals(CursorPosition(9, 4), buf.getCursorPosition())

        buf.setCursorPosition(-5, -5)
        assertEquals(CursorPosition(0, 0), buf.getCursorPosition())
    }

    @Test
    fun `move cursor up`() {
        val buf = TerminalBuffer(10, 5)
        buf.setCursorPosition(0, 3)
        buf.moveCursorUp(2)
        assertEquals(CursorPosition(0, 1), buf.getCursorPosition())
    }

    @Test
    fun `move cursor up clamps at top`() {
        val buf = TerminalBuffer(10, 5)
        buf.setCursorPosition(0, 1)
        buf.moveCursorUp(5)
        assertEquals(CursorPosition(0, 0), buf.getCursorPosition())
    }

    @Test
    fun `move cursor down`() {
        val buf = TerminalBuffer(10, 5)
        buf.moveCursorDown(3)
        assertEquals(CursorPosition(0, 3), buf.getCursorPosition())
    }

    @Test
    fun `move cursor down clamps at bottom`() {
        val buf = TerminalBuffer(10, 5)
        buf.moveCursorDown(100)
        assertEquals(CursorPosition(0, 4), buf.getCursorPosition())
    }

    @Test
    fun `move cursor left`() {
        val buf = TerminalBuffer(10, 5)
        buf.setCursorPosition(5, 0)
        buf.moveCursorLeft(3)
        assertEquals(CursorPosition(2, 0), buf.getCursorPosition())
    }

    @Test
    fun `move cursor left clamps at zero`() {
        val buf = TerminalBuffer(10, 5)
        buf.setCursorPosition(2, 0)
        buf.moveCursorLeft(10)
        assertEquals(CursorPosition(0, 0), buf.getCursorPosition())
    }

    @Test
    fun `move cursor right`() {
        val buf = TerminalBuffer(10, 5)
        buf.moveCursorRight(5)
        assertEquals(CursorPosition(5, 0), buf.getCursorPosition())
    }

    @Test
    fun `move cursor right clamps at edge`() {
        val buf = TerminalBuffer(10, 5)
        buf.moveCursorRight(100)
        assertEquals(CursorPosition(9, 0), buf.getCursorPosition())
    }

    @Test
    fun `initial attributes are DEFAULT`() {
        val buf = TerminalBuffer(10, 5)
        assertEquals(TextAttributes.DEFAULT, buf.currentAttributes)
    }

    @Test
    fun `set attributes with colors and styles`() {
        val buf = TerminalBuffer(10, 5)
        buf.setAttributes(Color.RED, Color.BLACK, setOf(Style.BOLD))
        assertEquals(Color.RED, buf.currentAttributes.foreground)
        assertEquals(Color.BLACK, buf.currentAttributes.background)
        assertEquals(setOf(Style.BOLD), buf.currentAttributes.styles)
    }

    @Test
    fun `set attributes with TextAttributes object`() {
        val buf = TerminalBuffer(10, 5)
        val attrs = TextAttributes.Builder()
            .foreground(Color.GREEN)
            .italic()
            .build()
        buf.setAttributes(attrs)
        assertEquals(attrs, buf.currentAttributes)
    }

    @Test
    fun `screen lines are initially empty`() {
        val buf = TerminalBuffer(10, 5)
        for (row in 0 until 5) {
            assertEquals("", buf.getScreenLine(row).getText())
        }
    }

    @Test
    fun `get screen line with invalid row throws`() {
        val buf = TerminalBuffer(10, 5)
        assertFailsWith<IllegalArgumentException> { buf.getScreenLine(-1) }
        assertFailsWith<IllegalArgumentException> { buf.getScreenLine(5) }
    }

    @Test
    fun `initial scrollback is empty`() {
        val buf = TerminalBuffer(10, 5)
        assertEquals(0, buf.getScrollbackSize())
    }

    @Test
    fun `get scrollback line with invalid row throws`() {
        val buf = TerminalBuffer(10, 5)
        assertFailsWith<IllegalArgumentException> { buf.getScrollbackLine(0) }
    }

    @Test
    fun `scrollUp moves top line to scrollback`() {
        val buf = TerminalBuffer(10, 3)
        buf.getScreenLine(0)[0] = Cell('A', TextAttributes.DEFAULT)
        buf.getScreenLine(1)[0] = Cell('B', TextAttributes.DEFAULT)
        buf.getScreenLine(2)[0] = Cell('C', TextAttributes.DEFAULT)

        buf.scrollUp()

        assertEquals(1, buf.getScrollbackSize())
        assertEquals("A", buf.getScrollbackLine(0).getText())
        assertEquals("B", buf.getScreenLine(0).getText())
        assertEquals("C", buf.getScreenLine(1).getText())
        assertEquals("", buf.getScreenLine(2).getText())
    }

    @Test
    fun `scrollUp respects max scrollback size`() {
        val buf = TerminalBuffer(10, 2, maxScrollbackSize = 2)
        buf.getScreenLine(0)[0] = Cell('A', TextAttributes.DEFAULT)
        buf.scrollUp()
        buf.getScreenLine(0)[0] = Cell('B', TextAttributes.DEFAULT)
        buf.scrollUp()
        buf.getScreenLine(0)[0] = Cell('C', TextAttributes.DEFAULT)
        buf.scrollUp()

        assertEquals(2, buf.getScrollbackSize())
        assertEquals("B", buf.getScrollbackLine(0).getText())
        assertEquals("C", buf.getScrollbackLine(1).getText())
    }

    @Test
    fun `scrollUp with zero scrollback discards top line`() {
        val buf = TerminalBuffer(10, 2, maxScrollbackSize = 0)
        buf.getScreenLine(0)[0] = Cell('A', TextAttributes.DEFAULT)
        buf.scrollUp()

        assertEquals(0, buf.getScrollbackSize())
        assertEquals("", buf.getScreenLine(0).getText())
    }
}
