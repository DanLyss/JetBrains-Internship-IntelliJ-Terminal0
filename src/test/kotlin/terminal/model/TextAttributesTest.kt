package terminal.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TextAttributesTest {

    @Test
    fun `DEFAULT has default colors and no styles`() {
        val attrs = TextAttributes.DEFAULT
        assertEquals(Color.DEFAULT, attrs.foreground)
        assertEquals(Color.DEFAULT, attrs.background)
        assertTrue(attrs.styles.isEmpty())
    }

    @Test
    fun `flyweight returns same instance for identical attributes`() {
        val first = TextAttributes.of(Color.RED, Color.BLACK, setOf(Style.BOLD))
        val second = TextAttributes.of(Color.RED, Color.BLACK, setOf(Style.BOLD))
        assertSame(first, second)
    }

    @Test
    fun `flyweight returns different instances for different attributes`() {
        val red = TextAttributes.of(Color.RED, Color.DEFAULT, emptySet())
        val green = TextAttributes.of(Color.GREEN, Color.DEFAULT, emptySet())
        assertTrue(red !== green)
    }

    @Test
    fun `builder creates attributes with specified values`() {
        val attrs = TextAttributes.Builder()
            .foreground(Color.CYAN)
            .background(Color.BLUE)
            .bold()
            .italic()
            .build()

        assertEquals(Color.CYAN, attrs.foreground)
        assertEquals(Color.BLUE, attrs.background)
        assertEquals(setOf(Style.BOLD, Style.ITALIC), attrs.styles)
    }

    @Test
    fun `builder defaults match DEFAULT`() {
        val attrs = TextAttributes.Builder().build()
        assertEquals(TextAttributes.DEFAULT, attrs)
    }

    @Test
    fun `builder with single style`() {
        val attrs = TextAttributes.Builder()
            .underline()
            .build()

        assertEquals(Color.DEFAULT, attrs.foreground)
        assertEquals(Color.DEFAULT, attrs.background)
        assertEquals(setOf(Style.UNDERLINE), attrs.styles)
    }

    @Test
    fun `builder result is cached by flyweight`() {
        val fromBuilder = TextAttributes.Builder()
            .foreground(Color.WHITE)
            .background(Color.BLACK)
            .build()
        val fromFactory = TextAttributes.of(Color.WHITE, Color.BLACK, emptySet())
        assertSame(fromBuilder, fromFactory)
    }

    @Test
    fun `of with all styles`() {
        val attrs = TextAttributes.of(
            Color.RED,
            Color.GREEN,
            setOf(Style.BOLD, Style.ITALIC, Style.UNDERLINE)
        )

        assertEquals(Color.RED, attrs.foreground)
        assertEquals(Color.GREEN, attrs.background)
        assertEquals(3, attrs.styles.size)
    }
}
