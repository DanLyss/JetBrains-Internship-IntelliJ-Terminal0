package terminal.buffer

import terminal.model.Cell
import terminal.model.Color
import terminal.model.Style
import terminal.model.TextAttributes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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

    @Test
    fun `writeText places characters at cursor`() {
        val buf = TerminalBuffer(10, 3)
        buf.writeText("Hello")
        assertEquals("Hello", buf.getScreenLine(0).getText())
        assertEquals(CursorPosition(5, 0), buf.getCursorPosition())
    }

    @Test
    fun `writeText uses current attributes`() {
        val buf = TerminalBuffer(10, 3)
        val attrs = TextAttributes.of(Color.RED, Color.BLACK, setOf(Style.BOLD))
        buf.setAttributes(attrs)
        buf.writeText("AB")

        assertEquals(attrs, buf.getScreenLine(0)[0].attributes)
        assertEquals(attrs, buf.getScreenLine(0)[1].attributes)
    }

    @Test
    fun `writeText wraps to next line at edge`() {
        val buf = TerminalBuffer(5, 3)
        buf.writeText("HelloWorld")
        assertEquals("Hello", buf.getScreenLine(0).getText())
        assertEquals("World", buf.getScreenLine(1).getText())
        assertEquals(CursorPosition(5, 1), buf.getCursorPosition())
    }

    @Test
    fun `writeText handles newline character`() {
        val buf = TerminalBuffer(10, 3)
        buf.writeText("Hi\nBye")
        assertEquals("Hi", buf.getScreenLine(0).getText())
        assertEquals("Bye", buf.getScreenLine(1).getText())
        assertEquals(CursorPosition(3, 1), buf.getCursorPosition())
    }

    @Test
    fun `writeText scrolls when reaching bottom`() {
        val buf = TerminalBuffer(5, 2)
        buf.writeText("AAAAA")
        buf.writeText("BBBBB")
        buf.writeText("CCCCC")

        assertEquals(1, buf.getScrollbackSize())
        assertEquals("AAAAA", buf.getScrollbackLine(0).getText())
        assertEquals("BBBBB", buf.getScreenLine(0).getText())
        assertEquals("CCCCC", buf.getScreenLine(1).getText())
    }

    @Test
    fun `writeText scrolls on newline at bottom`() {
        val buf = TerminalBuffer(10, 2)
        buf.writeText("Line1\nLine2\nLine3")

        assertEquals(1, buf.getScrollbackSize())
        assertEquals("Line1", buf.getScrollbackLine(0).getText())
        assertEquals("Line2", buf.getScreenLine(0).getText())
        assertEquals("Line3", buf.getScreenLine(1).getText())
    }

    @Test
    fun `writeText overwrites existing content`() {
        val buf = TerminalBuffer(10, 3)
        buf.writeText("XXXXXXXXXX")
        buf.setCursorPosition(0, 0)
        buf.writeText("Hello")
        assertEquals("HelloXXXXX", buf.getScreenLine(0).getText())
    }

    @Test
    fun `writeText from middle of line`() {
        val buf = TerminalBuffer(10, 3)
        buf.setCursorPosition(3, 0)
        buf.writeText("Hi")
        assertEquals("Hi", buf.getScreenLine(0).getText().trim())
        assertEquals(CursorPosition(5, 0), buf.getCursorPosition())
    }

    @Test
    fun `writeText empty string does nothing`() {
        val buf = TerminalBuffer(10, 3)
        buf.writeText("")
        assertEquals(CursorPosition(0, 0), buf.getCursorPosition())
        assertEquals("", buf.getScreenLine(0).getText())
    }

    @Test
    fun `writeText scrollback respects max size during long write`() {
        val buf = TerminalBuffer(3, 2, maxScrollbackSize = 2)
        buf.writeText("AAABBBCCCDDDEEEFFF")

        assertEquals(2, buf.getScrollbackSize())
    }

    @Test
    fun `insertText shifts existing content right`() {
        val buf = TerminalBuffer(10, 3)
        buf.writeText("World")
        buf.setCursorPosition(0, 0)
        buf.insertText("Hello")
        assertEquals("HelloWorld", buf.getScreenLine(0).getText())
    }

    @Test
    fun `insertText uses current attributes`() {
        val buf = TerminalBuffer(10, 3)
        val attrs = TextAttributes.of(Color.GREEN, Color.DEFAULT, emptySet())
        buf.setAttributes(attrs)
        buf.insertText("AB")
        assertEquals(attrs, buf.getScreenLine(0)[0].attributes)
        assertEquals(attrs, buf.getScreenLine(0)[1].attributes)
    }

    @Test
    fun `insertText overflow wraps to next line`() {
        val buf = TerminalBuffer(5, 3)
        buf.writeText("ABCDE")
        buf.setCursorPosition(0, 0)
        buf.insertText("X")
        assertEquals("XABCD", buf.getScreenLine(0).getText())
        assertEquals("E", buf.getScreenLine(1).getText())
    }

    @Test
    fun `insertText into empty line`() {
        val buf = TerminalBuffer(10, 3)
        buf.insertText("Hello")
        assertEquals("Hello", buf.getScreenLine(0).getText())
        assertEquals(CursorPosition(5, 0), buf.getCursorPosition())
    }

    @Test
    fun `insertText in middle of line`() {
        val buf = TerminalBuffer(10, 3)
        buf.writeText("AE")
        buf.setCursorPosition(1, 0)
        buf.insertText("BCD")
        assertEquals("ABCDE", buf.getScreenLine(0).getText())
    }

    @Test
    fun `insertText handles newline`() {
        val buf = TerminalBuffer(10, 3)
        buf.insertText("Hi\nBye")
        assertEquals("Hi", buf.getScreenLine(0).getText())
        assertEquals("Bye", buf.getScreenLine(1).getText())
    }

    @Test
    fun `insertText empty string does nothing`() {
        val buf = TerminalBuffer(10, 3)
        buf.writeText("Hello")
        buf.setCursorPosition(2, 0)
        buf.insertText("")
        assertEquals("Hello", buf.getScreenLine(0).getText())
    }

    @Test
    fun `fillLine fills with character and current attributes`() {
        val buf = TerminalBuffer(5, 3)
        val attrs = TextAttributes.of(Color.RED, Color.BLACK, setOf(Style.BOLD))
        buf.setAttributes(attrs)
        buf.fillLine(0, '#')

        assertEquals("#####", buf.getScreenLine(0).getText())
        assertEquals(attrs, buf.getScreenLine(0)[0].attributes)
        assertEquals(attrs, buf.getScreenLine(0)[4].attributes)
    }

    @Test
    fun `fillLine does not affect other lines`() {
        val buf = TerminalBuffer(5, 3)
        buf.fillLine(1, 'X')
        assertEquals("", buf.getScreenLine(0).getText())
        assertEquals("XXXXX", buf.getScreenLine(1).getText())
        assertEquals("", buf.getScreenLine(2).getText())
    }

    @Test
    fun `fillLine with invalid row throws`() {
        val buf = TerminalBuffer(5, 3)
        assertFailsWith<IllegalArgumentException> { buf.fillLine(-1, 'X') }
        assertFailsWith<IllegalArgumentException> { buf.fillLine(3, 'X') }
    }

    @Test
    fun `clearLine resets line to empty`() {
        val buf = TerminalBuffer(5, 3)
        buf.writeText("Hello")
        buf.clearLine(0)
        assertEquals("", buf.getScreenLine(0).getText())
        for (col in 0 until 5) {
            assertEquals(Cell.EMPTY, buf.getScreenLine(0)[col])
        }
    }

    @Test
    fun `clearLine with invalid row throws`() {
        val buf = TerminalBuffer(5, 3)
        assertFailsWith<IllegalArgumentException> { buf.clearLine(-1) }
        assertFailsWith<IllegalArgumentException> { buf.clearLine(3) }
    }

    @Test
    fun `insertNewLine scrolls screen up`() {
        val buf = TerminalBuffer(5, 3)
        buf.writeText("AAAAA")
        buf.setCursorPosition(0, 1)
        buf.writeText("BBBBB")
        buf.setCursorPosition(0, 2)
        buf.writeText("CCCCC")

        buf.insertNewLine()

        assertEquals(1, buf.getScrollbackSize())
        assertEquals("AAAAA", buf.getScrollbackLine(0).getText())
        assertEquals("BBBBB", buf.getScreenLine(0).getText())
        assertEquals("CCCCC", buf.getScreenLine(1).getText())
        assertEquals("", buf.getScreenLine(2).getText())
    }

    @Test
    fun `insertNewLine respects max scrollback`() {
        val buf = TerminalBuffer(5, 2, maxScrollbackSize = 1)
        buf.writeText("AAAAA")
        buf.setCursorPosition(0, 1)
        buf.writeText("BBBBB")

        buf.insertNewLine()
        buf.clearLine(0)
        buf.getScreenLine(0)[0] = Cell('C', TextAttributes.DEFAULT)
        buf.insertNewLine()

        assertEquals(1, buf.getScrollbackSize())
        assertEquals("C", buf.getScrollbackLine(0).getText())
    }

    @Test
    fun `insertNewLine on empty screen`() {
        val buf = TerminalBuffer(5, 3)
        buf.insertNewLine()
        assertEquals(1, buf.getScrollbackSize())
        assertEquals("", buf.getScrollbackLine(0).getText())
        for (row in 0 until 3) {
            assertEquals("", buf.getScreenLine(row).getText())
        }
    }

    @Test
    fun `clearScreen empties all lines and resets cursor`() {
        val buf = TerminalBuffer(5, 3)
        buf.writeText("Hello")
        buf.setCursorPosition(0, 1)
        buf.writeText("World")

        buf.clearScreen()

        for (row in 0 until 3) {
            assertEquals("", buf.getScreenLine(row).getText())
        }
        assertEquals(CursorPosition(0, 0), buf.getCursorPosition())
    }

    @Test
    fun `clearScreen does not affect scrollback`() {
        val buf = TerminalBuffer(5, 2)
        buf.writeText("AAAAABBBBB")
        buf.writeText("CCCCC")

        val scrollbackBefore = buf.getScrollbackSize()
        buf.clearScreen()

        assertEquals(scrollbackBefore, buf.getScrollbackSize())
    }

    @Test
    fun `clearAll empties screen and scrollback`() {
        val buf = TerminalBuffer(5, 2)
        buf.writeText("AAAAABBBBB")
        buf.writeText("CCCCC")

        buf.clearAll()

        for (row in 0 until 2) {
            assertEquals("", buf.getScreenLine(row).getText())
        }
        assertEquals(0, buf.getScrollbackSize())
        assertEquals(CursorPosition(0, 0), buf.getCursorPosition())
    }

    @Test
    fun `clearScreen on already empty screen`() {
        val buf = TerminalBuffer(5, 3)
        buf.clearScreen()
        for (row in 0 until 3) {
            assertEquals("", buf.getScreenLine(row).getText())
        }
        assertEquals(CursorPosition(0, 0), buf.getCursorPosition())
    }

    @Test
    fun `getChar returns character at position`() {
        val buf = TerminalBuffer(10, 3)
        buf.writeText("Hello")
        assertEquals('H', buf.getChar(0, 0))
        assertEquals('o', buf.getChar(4, 0))
        assertEquals(' ', buf.getChar(5, 0))
    }

    @Test
    fun `getChar with invalid position throws`() {
        val buf = TerminalBuffer(10, 3)
        assertFailsWith<IllegalArgumentException> { buf.getChar(-1, 0) }
        assertFailsWith<IllegalArgumentException> { buf.getChar(10, 0) }
        assertFailsWith<IllegalArgumentException> { buf.getChar(0, -1) }
        assertFailsWith<IllegalArgumentException> { buf.getChar(0, 3) }
    }

    @Test
    fun `getAttributes returns attributes at position`() {
        val buf = TerminalBuffer(10, 3)
        val attrs = TextAttributes.of(Color.RED, Color.BLACK, setOf(Style.BOLD))
        buf.setAttributes(attrs)
        buf.writeText("Hi")

        assertEquals(attrs, buf.getAttributes(0, 0))
        assertEquals(attrs, buf.getAttributes(1, 0))
        assertEquals(TextAttributes.DEFAULT, buf.getAttributes(2, 0))
    }

    @Test
    fun `getScrollbackChar and getScrollbackAttributes`() {
        val buf = TerminalBuffer(5, 2)
        val attrs = TextAttributes.of(Color.GREEN, Color.DEFAULT, emptySet())
        buf.setAttributes(attrs)
        buf.writeText("AAAAA")
        buf.setAttributes(TextAttributes.DEFAULT)
        buf.writeText("BBBBB")
        buf.writeText("CCCCC")

        assertEquals('A', buf.getScrollbackChar(0, 0))
        assertEquals(attrs, buf.getScrollbackAttributes(0, 0))
    }

    @Test
    fun `getLineText returns trimmed line content`() {
        val buf = TerminalBuffer(10, 3)
        buf.writeText("Hello")
        assertEquals("Hello", buf.getLineText(0))
        assertEquals("", buf.getLineText(1))
    }

    @Test
    fun `getScrollbackLineText returns scrollback line`() {
        val buf = TerminalBuffer(5, 2)
        buf.writeText("FIRST")
        buf.writeText("SECND")
        buf.writeText("THIRD")

        assertEquals("FIRST", buf.getScrollbackLineText(0))
    }

    @Test
    fun `getScreenText returns all screen lines joined`() {
        val buf = TerminalBuffer(5, 3)
        buf.writeText("Hello")
        buf.setCursorPosition(0, 1)
        buf.writeText("World")

        assertEquals("Hello\nWorld\n", buf.getScreenText())
    }

    @Test
    fun `getScreenText on empty buffer`() {
        val buf = TerminalBuffer(5, 3)
        assertEquals("\n\n", buf.getScreenText())
    }

    @Test
    fun `getAllText returns scrollback and screen`() {
        val buf = TerminalBuffer(5, 2)
        buf.writeText("AAAAA")
        buf.writeText("BBBBB")
        buf.writeText("CCCCC")

        assertEquals("AAAAA\nBBBBB\nCCCCC", buf.getAllText())
    }

    @Test
    fun `getAllText with empty scrollback`() {
        val buf = TerminalBuffer(5, 2)
        buf.writeText("Hello")

        assertEquals("Hello\n", buf.getAllText())
    }

    @Test
    fun `writeText wide character occupies two cells`() {
        val buf = TerminalBuffer(10, 3)
        buf.writeText("A中B")

        assertEquals('A', buf.getChar(0, 0))
        assertEquals('中', buf.getChar(1, 0))
        assertTrue(buf.getScreenLine(0)[1].isWide)
        assertTrue(buf.getScreenLine(0)[2].isPlaceholder)
        assertEquals('B', buf.getChar(3, 0))
        assertEquals(CursorPosition(4, 0), buf.getCursorPosition())
    }

    @Test
    fun `writeText wide character getText skips placeholders`() {
        val buf = TerminalBuffer(10, 3)
        buf.writeText("中日本")
        assertEquals("中日本", buf.getLineText(0))
    }

    @Test
    fun `writeText wide character wraps when at last column`() {
        val buf = TerminalBuffer(5, 3)
        buf.writeText("ABCD中")

        assertEquals("ABCD", buf.getLineText(0))
        assertEquals("中", buf.getLineText(1))
    }

    @Test
    fun `writeText overwriting half of wide character clears both cells`() {
        val buf = TerminalBuffer(10, 3)
        buf.writeText("中")
        buf.setCursorPosition(1, 0)
        buf.writeText("X")

        assertEquals(' ', buf.getChar(0, 0))
        assertEquals('X', buf.getChar(1, 0))
    }

    @Test
    fun `writeText overwriting first cell of wide character clears placeholder`() {
        val buf = TerminalBuffer(10, 3)
        buf.writeText("中")
        buf.setCursorPosition(0, 0)
        buf.writeText("A")

        assertEquals('A', buf.getChar(0, 0))
        assertEquals(' ', buf.getChar(1, 0))
    }

    @Test
    fun `writeText multiple wide characters fill line`() {
        val buf = TerminalBuffer(6, 3)
        buf.writeText("中日本")

        assertEquals("中日本", buf.getLineText(0))
        assertEquals(CursorPosition(6, 0), buf.getCursorPosition())
    }

    @Test
    fun `writeText wide character with attributes`() {
        val buf = TerminalBuffer(10, 3)
        val attrs = TextAttributes.of(Color.RED, Color.DEFAULT, emptySet())
        buf.setAttributes(attrs)
        buf.writeText("中")

        assertEquals(attrs, buf.getAttributes(0, 0))
        assertEquals(attrs, buf.getAttributes(1, 0))
    }

    @Test
    fun `buffer 1x1 write and scroll`() {
        val buf = TerminalBuffer(1, 1)
        buf.writeText("A")
        assertEquals("A", buf.getLineText(0))
        buf.writeText("B")
        assertEquals("B", buf.getLineText(0))
        assertEquals(1, buf.getScrollbackSize())
        assertEquals("A", buf.getScrollbackLineText(0))
    }

    @Test
    fun `zero scrollback discards all`() {
        val buf = TerminalBuffer(5, 2, maxScrollbackSize = 0)
        buf.writeText("AAAAA")
        buf.writeText("BBBBB")
        buf.writeText("CCCCC")

        assertEquals(0, buf.getScrollbackSize())
        assertEquals("BBBBB", buf.getLineText(0))
        assertEquals("CCCCC", buf.getLineText(1))
    }

    @Test
    fun `write empty string preserves state`() {
        val buf = TerminalBuffer(5, 2)
        buf.writeText("Hi")
        val pos = buf.getCursorPosition()
        buf.writeText("")
        assertEquals(pos, buf.getCursorPosition())
        assertEquals("Hi", buf.getLineText(0))
    }

    @Test
    fun `long text causes multiple scrolls`() {
        val buf = TerminalBuffer(3, 2, maxScrollbackSize = 100)
        buf.writeText("ABCDEFGHIJKL")

        assertEquals("GHI", buf.getLineText(0))
        assertEquals("JKL", buf.getLineText(1))
        assertEquals(2, buf.getScrollbackSize())
        assertEquals("ABC", buf.getScrollbackLineText(0))
        assertEquals("DEF", buf.getScrollbackLineText(1))
    }

    @Test
    fun `all styles combined`() {
        val buf = TerminalBuffer(10, 3)
        val attrs = TextAttributes.Builder()
            .foreground(Color.BRIGHT_RED)
            .background(Color.BLUE)
            .bold()
            .italic()
            .underline()
            .build()
        buf.setAttributes(attrs)
        buf.writeText("X")

        val result = buf.getAttributes(0, 0)
        assertEquals(Color.BRIGHT_RED, result.foreground)
        assertEquals(Color.BLUE, result.background)
        assertEquals(setOf(Style.BOLD, Style.ITALIC, Style.UNDERLINE), result.styles)
    }

    @Test
    fun `sequential operations write insert clear write`() {
        val buf = TerminalBuffer(10, 3)
        buf.writeText("Hello")
        buf.setCursorPosition(5, 0)
        buf.insertText(" World")
        assertEquals("Hello Worl", buf.getLineText(0))

        buf.clearLine(0)
        assertEquals("", buf.getLineText(0))

        buf.setCursorPosition(0, 0)
        buf.writeText("New")
        assertEquals("New", buf.getLineText(0))
    }

    @Test
    fun `scrollback full then oldest removed`() {
        val buf = TerminalBuffer(3, 1, maxScrollbackSize = 3)
        buf.writeText("AAA")
        buf.writeText("BBB")
        buf.writeText("CCC")
        buf.writeText("DDD")
        buf.writeText("EEE")

        assertEquals(3, buf.getScrollbackSize())
        assertEquals("BBB", buf.getScrollbackLineText(0))
        assertEquals("CCC", buf.getScrollbackLineText(1))
        assertEquals("DDD", buf.getScrollbackLineText(2))
    }

    @Test
    fun `cursor movement does not affect content`() {
        val buf = TerminalBuffer(10, 3)
        buf.writeText("Hello")
        buf.moveCursorUp(5)
        buf.moveCursorLeft(100)
        buf.moveCursorDown(1)
        buf.moveCursorRight(3)

        assertEquals("Hello", buf.getLineText(0))
        assertEquals(CursorPosition(3, 1), buf.getCursorPosition())
    }
}
