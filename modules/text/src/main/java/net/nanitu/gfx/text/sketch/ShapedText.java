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

package net.nanitu.gfx.text.sketch;

import net.nanitu.gfx.text.Font;

/**
 * The raw result of text shaping — glyph identifiers, per-glyph advances and offsets, and character-to-glyph mapping.
 *
 * <p>This is an intermediate representation produced by a shaping backend.
 * It is consumed by line-breaking to produce the final {@link Text} layout.
 *
 * <p>All position and advance values are in pixels.
 */
public final class ShapedText {
  final Font font;
  final int[] glyphs;
  final float[] advances;
  final float[] offsets;
  final float fontSize;
  final boolean flipY;
  final int fontStyle;
  final int[] glyphToChar;
  final float[] charAdvances;
  final int[] charFirstGlyph;
  private final String text;

  /**
   * Creates a new shaped text result.
   *
   * @param font      the font used for shaping
   * @param text      the original text string
   * @param glyphs    the array of glyph identifiers
   * @param clusters  the array mapping each glyph to its originating character index
   * @param advances  the advance width for each glyph, in pixels
   * @param offsets   the X/Y offset pairs for each glyph, in pixels
   * @param fontSize  the font size used for shaping, in pixels
   * @param flipY     whether the Y axis is flipped
   * @param fontStyle the style bitmask, combining flags from {@link net.nanitu.gfx.text.FontStyle}
   */
  public ShapedText(Font font, String text, int[] glyphs, int[] clusters, float[] advances, float[] offsets,
                    float fontSize, boolean flipY, int fontStyle) {
    this.font = font;
    this.text = text;
    this.glyphs = glyphs;
    this.advances = advances;
    this.offsets = offsets;
    this.fontSize = fontSize;
    this.flipY = flipY;
    this.fontStyle = fontStyle;

    // clusters are already character indices (from JDK GlyphVector)
    this.glyphToChar = clusters.clone();

    // Build charFirstGlyph
    int textLen = text.length();
    this.charFirstGlyph = new int[textLen + 1];
    for (int i = 0; i <= textLen; i++) {
      charFirstGlyph[i] = -1;
    }
    for (int gi = 0; gi < glyphs.length; gi++) {
      int ci = glyphToChar[gi];
      if (ci >= 0 && ci < textLen && charFirstGlyph[ci] == -1) {
        charFirstGlyph[ci] = gi;
      }
    }
    int pg = 0;
    for (int ci = 0; ci < textLen; ci++) {
      if (charFirstGlyph[ci] == -1) {
        charFirstGlyph[ci] = pg;
      } else {
        pg = charFirstGlyph[ci];
      }
    }
    charFirstGlyph[textLen] = glyphs.length;

    // Per-character advances
    this.charAdvances = new float[textLen];
    for (int ci = 0; ci < textLen; ci++) {
      float s = 0;
      for (int g = charFirstGlyph[ci]; g < charFirstGlyph[ci + 1]; g++) {
        s += advances[g];
      }
      charAdvances[ci] = s;
    }
  }

  /**
   * Returns the font used for shaping.
   *
   * @return the font
   */
  public Font font() {
    return font;
  }

  /**
   * Returns the original text string that was shaped.
   *
   * @return the text string
   */
  public String text() {
    return text;
  }

  /**
   * Returns the total number of glyphs in the shaping result.
   *
   * @return the glyph count
   */
  public int glyphCount() {
    return glyphs.length;
  }

  /**
   * Returns the font size used for shaping.
   *
   * @return the font size in pixels
   */
  public float fontSize() {
    return fontSize;
  }

  /**
   * Returns whether the Y axis is flipped.
   *
   * @return {@code true} if Y is flipped (positive downward)
   */
  public boolean flipY() {
    return flipY;
  }

  /**
   * Returns the font style bitmask used for shaping.
   *
   * @return the style bitmask, combining flags from {@link net.nanitu.gfx.text.FontStyle}
   */
  public int fontStyle() {
    return fontStyle;
  }

  /**
   * Returns the advance width of the glyph at the given index.
   *
   * @param gi the glyph index
   * @return the advance width in pixels
   */
  public float advance(int gi) {
    return advances[gi];
  }

  /**
   * Returns the horizontal offset of the glyph at the given index.
   *
   * @param gi the glyph index
   * @return the X offset in pixels
   */
  public float xOffset(int gi) {
    return offsets[gi * 2];
  }

  /**
   * Returns the vertical offset of the glyph at the given index.
   *
   * @param gi the glyph index
   * @return the Y offset in pixels
   */
  public float yOffset(int gi) {
    return offsets[gi * 2 + 1];
  }

  /**
   * Returns the total advance width for the character at the given index.
   *
   * @param ci the character index in the original text
   * @return the character advance width in pixels
   */
  public float charAdvance(int ci) {
    return charAdvances[ci];
  }

  /**
   * Maps a glyph index back to the character index that produced it.
   *
   * @param gi the glyph index
   * @return the originating character index
   */
  public int glyphToChar(int gi) {
    return glyphToChar[gi];
  }

  /**
   * Returns the index of the first glyph for the given character.
   *
   * @param ci the character index in the original text
   * @return the first glyph index for that character
   */
  public int charToFirstGlyph(int ci) {
    return charFirstGlyph[ci];
  }
}
