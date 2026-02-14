package terminal.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CellTest {

    @Test
    fun `EMPTY cell has space character and default attributes`() {
        val cell = Cell.EMPTY
        assertEquals(' ', cell.character)
        assertEquals(TextAttributes.DEFAULT, cell.attributes)
    }

    @Test
    fun `cell stores character and attributes`() {
        val attrs = TextAttributes.of(Color.RED, Color.BLACK, setOf(Style.BOLD))
        val cell = Cell('A', attrs)

        assertEquals('A', cell.character)
        assertEquals(attrs, cell.attributes)
    }

    @Test
    fun `cells with same values are equal`() {
        val attrs = TextAttributes.of(Color.GREEN, Color.DEFAULT, emptySet())
        val cell1 = Cell('X', attrs)
        val cell2 = Cell('X', attrs)

        assertEquals(cell1, cell2)
    }
}
