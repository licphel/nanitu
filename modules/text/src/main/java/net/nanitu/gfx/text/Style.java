/*
 * MIT License
 *
 * Copyright (c) 2026 Licphel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.nanitu.gfx.text;

import net.nanitu.math.Color;

/**
 * Immutable text styling attributes for a span of text.
 *
 * <p>A {@code Style} combines a {@link Font}, color, style flags, and font size.
 * Use {@link Builder} to construct instances fluently.
 *
 * @param font      the font used to render text
 * @param color     the text color
 * @param fontStyle the style bitmask, combining flags from {@link Font}
 * @param fontSize  the font size, in pixels
 */
public record Style(Font font, Color color, int fontStyle, float fontSize) {
  /**
   * Fluent builder for {@link Style} instances.
   *
   * <p>Defaults: {@code color = WHITE}, {@code fontStyle = REGULAR}, {@code fontSize = 16}.
   */
  public static final class Builder {
    private final Font font;
    private Color color = Color.WHITE;
    private int fontStyle = Font.REGULAR;
    private float fontSize = Font.DEFAULT_SIZE;

    /**
     * Creates a new builder for the given font.
     *
     * @param font the font for the style being built
     */
    public Builder(Font font) {
      this.font = font;
    }

    /**
     * Sets the text color.
     *
     * @param color the text color
     * @return this builder, for fluent chaining
     */
    public Builder color(Color color) {
      this.color = color;
      return this;
    }

    /**
     * Adds style flags to the bitmask.
     *
     * <p>Multiple calls OR the flags together, so {@code fontStyle(BOLD).fontStyle(ITALIC)}
     * produces bold-italic.
     *
     * @param fontStyle the style flags to add, from {@link Font}
     * @return this builder, for fluent chaining
     */
    public Builder fontStyle(int fontStyle) {
      this.fontStyle |= fontStyle;
      return this;
    }

    /**
     * Sets the font size.
     *
     * @param fontSize the font size, in pixels
     * @return this builder, for fluent chaining
     */
    public Builder fontSize(float fontSize) {
      this.fontSize = fontSize;
      return this;
    }

    /**
     * Builds the {@link Style} instance.
     *
     * @return a new style with the configured attributes
     */
    public Style build() {
      return new Style(this.font, this.color, this.fontStyle, this.fontSize);
    }
  }
}
