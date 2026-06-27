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

import net.nanitu.gfx.text.raster.Raster;
import net.nanitu.gfx.text.raster.Rasterizer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A mutable sequence of text components that share common layout parameters.
 *
 * <p>A {@code TextSequence} collects child {@link Text} components and rasterizes them
 * as a unit. Layout parameters such as maximum width, justification, line spacing, and line count are configured
 * through fluent setter methods before calling {@link #raster()}. The result is cached until the sequence or its layout
 * parameters change.
 */
public final class TextSequence implements Text {
  private final List<Text> children = new ArrayList<>();
  private final Rasterizer rasterizer = new Rasterizer();
  private volatile @Nullable Raster cached;
  private String text = "";

  private static void flatten(Text c, List<TextLiteral> out) {
    if (c instanceof TextLiteral lit) {
      out.add(lit);
    } else if (c instanceof TextSequence seq) {
      for (Text child : seq.children) {
        flatten(child, out);
      }
    }
  }

  /**
   * Sets the maximum width for line breaking, in pixels.
   *
   * @param v the maximum width in pixels
   * @return this sequence, for fluent chaining
   */
  public TextSequence maxWidth(float v) {
    rasterizer.maxWidth = v;
    cached = null;
    return this;
  }

  /**
   * Sets whether the Y-axis is flipped during rasterization.
   *
   * @param v {@code true} to flip the Y-axis
   * @return this sequence, for fluent chaining
   */
  public TextSequence flipY(boolean v) {
    rasterizer.flipY = v;
    cached = null;
    return this;
  }

  /**
   * Sets whether text should be justified (stretched to fill the full line width).
   *
   * @param v {@code true} to enable justification
   * @return this sequence, for fluent chaining
   */
  public TextSequence justify(boolean v) {
    rasterizer.justify = v;
    cached = null;
    return this;
  }

  /**
   * Sets the line spacing multiplier.
   *
   * <p>A value of {@code 1.0} uses the font's default line height. Values greater than
   * {@code 1.0} increase spacing; values less than {@code 1.0} reduce it.
   *
   * @param multiplier the line spacing multiplier
   * @return this sequence, for fluent chaining
   */
  public TextSequence lineSpacing(float multiplier) {
    rasterizer.lineSpacing = multiplier;
    cached = null;
    return this;
  }

  /**
   * Sets the maximum number of visible lines. Text beyond this limit is clipped.
   *
   * @param n the maximum number of lines
   * @return this sequence, for fluent chaining
   */
  public TextSequence maxLines(int n) {
    rasterizer.maxLines = n;
    cached = null;
    return this;
  }

  /**
   * Sets whether an ellipsis is appended when text is truncated.
   *
   * @param v {@code true} to append an ellipsis on overflow
   * @return this sequence, for fluent chaining
   */
  public TextSequence ellipsis(boolean v) {
    cached = null;
    // Not implemented yet.
    return this;
  }

  @Override
  public String text() {
    return text;
  }

  @Override
  public TextSequence append(Text component) {
    if (component instanceof TextSequence sequence) {
      // Flatten
      children.addAll(sequence.children);
    } else {
      children.add(component);
    }

    text += component.text();
    cached = null;
    return this;
  }

  @Override
  public Raster raster() {
    if (cached == null) {
      List<TextLiteral> literals = new ArrayList<>();
      flatten(this, literals);
      cached = rasterizer.render(literals);
    }
    return Objects.requireNonNull(cached);
  }

  @Override
  public @Nullable Style forwadingStyle() {
    return children.isEmpty() ? null : children.getLast().forwadingStyle();
  }

  /**
   * Appends a newline character to this sequence.
   *
   * <p>The newline inherits the style and font size of the last child component.
   * If the sequence is empty, this method has no effect.
   *
   * @return this sequence, for fluent chaining
   */
  public TextSequence newline() {
    Style style = forwadingStyle();

    if (style != null) {
      append(new TextLiteral("\n", style));
    }

    return this;
  }
}
