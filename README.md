# Terminal Buffer

A terminal text buffer implementation in Kotlin — the core data structure that terminal emulators use to store and manage displayed text.

## Building and Testing

```bash
./gradlew build
./gradlew test
```

Requires JDK 21+. No external dependencies (only JUnit 5 for testing).

## Features

### Core Model
- **Cell** — a single grid cell storing a character and its display attributes
- **TextAttributes** — foreground/background color (default + 16 standard terminal colors) and style flags (bold, italic, underline)
- **TerminalLine** — a row of cells with get/set, fill, clear, and text extraction

### Terminal Buffer
The main `TerminalBuffer` class manages a screen grid and a scrollback history.

**Setup:**
- Configurable width, height, and maximum scrollback size

**Attributes:**
- Set current foreground color, background color, and styles
- All subsequent editing operations use the current attributes

**Cursor:**
- Get/set cursor position (column, row)
- Move cursor up, down, left, right by N cells
- Cursor is always clamped to screen bounds

**Editing:**
- `writeText` — write text at cursor position, replacing existing content. Handles line wrapping and scrolling
- `insertText` — insert text at cursor position, shifting existing content right. Overflow cascades to subsequent lines using batch shifting for efficiency
- `fillLine` — fill a row with a given character using current attributes
- `clearLine` — reset a row to empty cells
- `insertNewLine` — push a blank line at the bottom, scrolling everything up
- `clearScreen` — clear all screen lines, reset cursor (scrollback preserved)
- `clearAll` — clear screen and scrollback

**Content Access:**
- `getChar` / `getAttributes` — access cell data by position (screen and scrollback)
- `getLineText` / `getScrollbackLineText` — get a single row as text
- `getScreenText` — all screen lines joined with newlines
- `getAllText` — scrollback + screen as text

**Wide Characters (Bonus):**
- CJK and other wide characters correctly occupy two cells (wide cell + placeholder)
- Automatic line wrapping when a wide character doesn't fit at the end of a row
- Overwriting half of a wide character clears both cells

**Resize (Bonus):**
- `resize(newWidth, newHeight, strategy)` — resize the terminal using a pluggable strategy
- Two built-in strategies: truncate and reflow (see Design Patterns below)

## Design Patterns

### Flyweight — TextAttributes
Many cells share identical attribute combinations (e.g., "white on black, no styles"). Instead of creating a new object for each cell, `TextAttributes.of(...)` returns a cached instance. This significantly reduces memory usage for large buffers with thousands of scrollback lines.

```kotlin
val a = TextAttributes.of(Color.RED, Color.BLACK, setOf(Style.BOLD))
val b = TextAttributes.of(Color.RED, Color.BLACK, setOf(Style.BOLD))
// a === b (same object in memory)
```

### Strategy — Resize
The resize behavior is encapsulated behind a `ResizeStrategy` interface, allowing different strategies to be used interchangeably:

- **TruncateResizeStrategy** — crops or pads lines to the new width; extra rows move to scrollback when height decreases
- **ReflowResizeStrategy** — rewraps text by detecting soft line breaks (lines that were full) and redistributing content across the new width

### Builder — TextAttributes
A fluent builder API for constructing attribute combinations:

```kotlin
val attrs = TextAttributes.Builder()
    .foreground(Color.CYAN)
    .background(Color.BLUE)
    .bold()
    .italic()
    .build()
```

## Tests

113 unit tests covering:

- **Model layer**: Cell equality, TextAttributes flyweight caching and builder, TerminalLine operations
- **Buffer operations**: cursor management and clamping, text writing with wrapping and scrolling, text insertion with batch shifting and overflow cascade, line and screen clearing, content access methods
- **Wide characters**: two-cell occupation, placeholder skipping in text output, wrapping at line end, overwriting half of a wide character
- **Resize**: both strategies with width/height increase and decrease, cursor clamping, scrollback limits
- **Edge cases**: 1x1 buffer, zero scrollback, long text with multiple scrolls, all styles combined, sequential operation mix, scrollback eviction

## Project Structure

```
src/main/kotlin/terminal/
├── model/
│   ├── Color.kt              — 17 terminal colors (DEFAULT + 16 standard)
│   ├── Style.kt              — BOLD, ITALIC, UNDERLINE
│   ├── TextAttributes.kt     — Flyweight cache + Builder
│   ├── Cell.kt               — Character + attributes + wide char support
│   ├── CharWidth.kt          — Unicode width detection
│   └── TerminalLine.kt       — Row of cells
├── buffer/
│   ├── CursorPosition.kt     — (column, row) data class
│   └── TerminalBuffer.kt     — Main buffer class
└── resize/
    ├── ResizeStrategy.kt      — Strategy interface
    ├── TruncateResizeStrategy.kt
    └── ReflowResizeStrategy.kt
```

## Trade-offs and Decisions

1. **Screen as Array, Scrollback as ArrayDeque** — screen needs O(1) random access by row index; scrollback needs efficient add/remove at both ends (FIFO with size limit).

2. **Overflow on insert is lost at screen bottom** — when inserting text causes content to cascade past the last screen row, that content is dropped. This avoids index corruption from scrolling mid-operation and matches how real terminals handle insert character mode.

3. **Soft wrap detection for reflow** — ReflowResizeStrategy considers a line "soft-wrapped" if its last cell is non-empty (the line was full). This is a heuristic that works well in practice but can misidentify intentionally full lines as wrapped.

4. **Wide character support in writeText only** — insertText does not yet handle wide characters. This is noted with a TODO in the code.

## Possible Improvements

- Wide character support in `insertText`
- Emoji support via surrogate pairs (characters outside the BMP)
- Tab character handling with configurable tab stops
- Scroll regions (partial screen scrolling)
- Alternate screen buffer (used by vim, htop, etc.)
