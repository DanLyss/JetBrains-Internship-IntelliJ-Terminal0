package terminal.model

object CharWidth {
    fun of(char: Char): Int {
        val code = char.code
        return when {
            code in 0x1100..0x115F -> 2
            code in 0x2E80..0x303E -> 2
            code in 0x3041..0x33BF -> 2
            code in 0x3400..0x4DBF -> 2
            code in 0x4E00..0x9FFF -> 2
            code in 0xA000..0xA4CF -> 2
            code in 0xAC00..0xD7AF -> 2
            code in 0xF900..0xFAFF -> 2
            code in 0xFE10..0xFE6F -> 2
            code in 0xFF01..0xFF60 -> 2
            code in 0xFFE0..0xFFE6 -> 2
            else -> 1
        }
    }

    fun isWide(char: Char): Boolean = of(char) == 2
}
