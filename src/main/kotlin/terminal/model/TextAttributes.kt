package terminal.model

data class TextAttributes(
    val foreground: Color,
    val background: Color,
    val styles: Set<Style>
) {
    companion object {
        private val cache = HashMap<TextAttributes, TextAttributes>()

        val DEFAULT = of(Color.DEFAULT, Color.DEFAULT, emptySet())

        fun of(foreground: Color, background: Color, styles: Set<Style>): TextAttributes {
            val key = TextAttributes(foreground, background, styles)
            return cache.getOrPut(key) { key }
        }
    }

    class Builder {
        private var foreground: Color = Color.DEFAULT
        private var background: Color = Color.DEFAULT
        private val styles: MutableSet<Style> = mutableSetOf()

        fun foreground(color: Color) = apply { this.foreground = color }

        fun background(color: Color) = apply { this.background = color }

        fun bold() = apply { styles.add(Style.BOLD) }

        fun italic() = apply { styles.add(Style.ITALIC) }

        fun underline() = apply { styles.add(Style.UNDERLINE) }

        fun style(style: Style) = apply { styles.add(style) }

        fun build(): TextAttributes = of(foreground, background, styles)
    }
}
