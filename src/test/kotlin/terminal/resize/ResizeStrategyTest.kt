package terminal.resize

import terminal.buffer.CursorPosition
import terminal.buffer.TerminalBuffer
import terminal.model.TextAttributes
import kotlin.test.Test
import kotlin.test.assertEquals

class ResizeStrategyTest {

    @Test
    fun `truncate - increase width pads with empty cells`() {
        val buf = TerminalBuffer(5, 2)
        buf.writeText("Hello")

        buf.resize(10, 2, TruncateResizeStrategy())

        assertEquals(10, buf.width)
        assertEquals("Hello", buf.getLineText(0))
    }

    @Test
    fun `truncate - decrease width truncates content`() {
        val buf = TerminalBuffer(10, 2)
        buf.writeText("HelloWorld")

        buf.resize(5, 2, TruncateResizeStrategy())

        assertEquals(5, buf.width)
        assertEquals("Hello", buf.getLineText(0))
    }

    @Test
    fun `truncate - increase height adds empty lines`() {
        val buf = TerminalBuffer(5, 2)
        buf.writeText("Hello")

        buf.resize(5, 4, TruncateResizeStrategy())

        assertEquals(4, buf.height)
        assertEquals("Hello", buf.getLineText(0))
        assertEquals("", buf.getLineText(2))
        assertEquals("", buf.getLineText(3))
    }

    @Test
    fun `truncate - decrease height moves extra lines to scrollback`() {
        val buf = TerminalBuffer(5, 3)
        buf.writeText("AAAAA")
        buf.setCursorPosition(0, 1)
        buf.writeText("BBBBB")
        buf.setCursorPosition(0, 2)
        buf.writeText("CCCCC")

        buf.resize(5, 2, TruncateResizeStrategy())

        assertEquals(2, buf.height)
        assertEquals("AAAAA", buf.getLineText(0))
        assertEquals("BBBBB", buf.getLineText(1))
        assertEquals(1, buf.getScrollbackSize())
        assertEquals("CCCCC", buf.getScrollbackLineText(0))
    }

    @Test
    fun `truncate - cursor clamped after resize`() {
        val buf = TerminalBuffer(10, 10)
        buf.setCursorPosition(8, 7)

        buf.resize(5, 3, TruncateResizeStrategy())

        assertEquals(CursorPosition(4, 2), buf.getCursorPosition())
    }

    @Test
    fun `truncate - scrollback is resized too`() {
        val buf = TerminalBuffer(10, 2)
        buf.writeText("AAAAAAAAAA")
        buf.writeText("BBBBBBBBBB")
        buf.writeText("CCCCCCCCCC")

        buf.resize(5, 2, TruncateResizeStrategy())

        assertEquals("AAAAA", buf.getScrollbackLineText(0))
    }

    @Test
    fun `reflow - narrowing wraps long lines`() {
        val buf = TerminalBuffer(10, 2)
        buf.writeText("ABCDEFGHIJ")

        buf.resize(5, 4, ReflowResizeStrategy())

        assertEquals("ABCDE", buf.getLineText(0))
        assertEquals("FGHIJ", buf.getLineText(1))
    }

    @Test
    fun `reflow - widening unwraps lines`() {
        val buf = TerminalBuffer(5, 2)
        buf.writeText("Hello")
        buf.writeText("World")

        buf.resize(10, 2, ReflowResizeStrategy())

        assertEquals("HelloWorld", buf.getLineText(0))
    }

    @Test
    fun `reflow - empty buffer`() {
        val buf = TerminalBuffer(10, 5)

        buf.resize(20, 3, ReflowResizeStrategy())

        assertEquals(20, buf.width)
        assertEquals(3, buf.height)
        assertEquals("", buf.getLineText(0))
    }

    @Test
    fun `reflow - cursor clamped after resize`() {
        val buf = TerminalBuffer(10, 10)
        buf.setCursorPosition(8, 7)

        buf.resize(5, 3, ReflowResizeStrategy())

        assertEquals(CursorPosition(4, 2), buf.getCursorPosition())
    }

    @Test
    fun `resize respects max scrollback size`() {
        val buf = TerminalBuffer(5, 5, maxScrollbackSize = 2)
        for (i in 0 until 5) {
            buf.setCursorPosition(0, i)
            buf.writeText("LN${i + 1}")
        }

        buf.resize(5, 2, TruncateResizeStrategy())

        assertEquals(2, buf.getScrollbackSize())
    }
}
