package terminal.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TerminalLineTest {

    @Test
    fun `new line has correct width`() {
        val line = TerminalLine(80)
        assertEquals(80, line.width)
    }

    @Test
    fun `new line is filled with empty cells`() {
        val line = TerminalLine(10)
        for (i in 0 until 10) {
            assertEquals(Cell.EMPTY, line[i])
        }
    }

    @Test
    fun `get and set cell`() {
        val line = TerminalLine(10)
        val cell = Cell('A', TextAttributes.DEFAULT)
        line[3] = cell
        assertEquals(cell, line[3])
    }

    @Test
    fun `set does not affect other cells`() {
        val line = TerminalLine(5)
        line[2] = Cell('X', TextAttributes.DEFAULT)
        assertEquals(Cell.EMPTY, line[0])
        assertEquals(Cell.EMPTY, line[1])
        assertEquals(Cell.EMPTY, line[3])
        assertEquals(Cell.EMPTY, line[4])
    }

    @Test
    fun `get with invalid column throws`() {
        val line = TerminalLine(10)
        assertFailsWith<IllegalArgumentException> { line[-1] }
        assertFailsWith<IllegalArgumentException> { line[10] }
    }

    @Test
    fun `set with invalid column throws`() {
        val line = TerminalLine(10)
        assertFailsWith<IllegalArgumentException> { line[-1] = Cell.EMPTY }
        assertFailsWith<IllegalArgumentException> { line[10] = Cell.EMPTY }
    }

    @Test
    fun `getText returns trimmed text`() {
        val line = TerminalLine(10)
        line[0] = Cell('H', TextAttributes.DEFAULT)
        line[1] = Cell('i', TextAttributes.DEFAULT)
        assertEquals("Hi", line.getText())
    }

    @Test
    fun `getText on empty line returns empty string`() {
        val line = TerminalLine(10)
        assertEquals("", line.getText())
    }

    @Test
    fun `getText preserves inner spaces`() {
        val line = TerminalLine(10)
        line[0] = Cell('A', TextAttributes.DEFAULT)
        line[1] = Cell(' ', TextAttributes.DEFAULT)
        line[2] = Cell('B', TextAttributes.DEFAULT)
        assertEquals("A B", line.getText())
    }

    @Test
    fun `clear resets all cells to empty`() {
        val line = TerminalLine(5)
        line[0] = Cell('A', TextAttributes.DEFAULT)
        line[1] = Cell('B', TextAttributes.DEFAULT)
        line.clear()
        for (i in 0 until 5) {
            assertEquals(Cell.EMPTY, line[i])
        }
    }

    @Test
    fun `fill sets all cells to given character and attributes`() {
        val attrs = TextAttributes.of(Color.RED, Color.BLACK, setOf(Style.BOLD))
        val line = TerminalLine(5)
        line.fill('#', attrs)

        for (i in 0 until 5) {
            assertEquals('#', line[i].character)
            assertEquals(attrs, line[i].attributes)
        }
    }

    @Test
    fun `getText after fill`() {
        val line = TerminalLine(3)
        line.fill('-', TextAttributes.DEFAULT)
        assertEquals("---", line.getText())
    }

    @Test
    fun `copyOf creates independent copy`() {
        val line = TerminalLine(5)
        line[0] = Cell('A', TextAttributes.DEFAULT)

        val copy = line.copyOf()
        assertEquals('A', copy[0].character)

        copy[0] = Cell('B', TextAttributes.DEFAULT)
        assertEquals('A', line[0].character)
        assertEquals('B', copy[0].character)
    }

    @Test
    fun `line with width 1`() {
        val line = TerminalLine(1)
        line[0] = Cell('X', TextAttributes.DEFAULT)
        assertEquals("X", line.getText())
    }
}
