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

import com.ibm.icu.text.BreakIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Breaks shaped text into lines according to a width limit, producing a {@link TextSketch}.
 *
 * <p>Lines are broken at word boundaries when possible. If a single word exceeds the
 * width limit, it is broken at character boundaries.
 */
public final class LineBreaker {
  private final ShapedText shaped;
  private final float widthLimit;
  private final boolean justify;
  private final List<BreakPoint> breakPoints = new ArrayList<>();
  private float lineWidthLimit, lineWidth;
  private int lineNum, prevBreakChar = -1;
  private float lineWidthAtPrevBreak;

  /**
   * Creates a new line breaker for the given shaped text.
   *
   * @param shaped     the shaped text to break into lines
   * @param widthLimit the maximum line width in pixels before wrapping
   * @param justify    whether to justify lines to fill the width limit
   */
  public LineBreaker(ShapedText shaped, float widthLimit, boolean justify) {
    this.shaped = shaped;
    this.widthLimit = widthLimit;
    this.justify = justify;
    this.lineWidthLimit = widthLimit;
  }

  /**
   * Creates a single-line layout from the given shaped text without any line breaking.
   *
   * @param shaped the shaped text
   * @return a single-line text layout
   */
  public static TextSketch singleLine(ShapedText shaped) {
    int n = shaped.glyphCount();
    int[] glyphs = new int[n];
    float[] positions = new float[n * 2];
    float[] advances = new float[n];
    float w = 0;
    for (int i = 0; i < n; i++) {
      glyphs[i] = shaped.glyphs[i];
      positions[i * 2] = w + shaped.xOffset(i);
      positions[i * 2 + 1] = shaped.yOffset(i);
      advances[i] = shaped.advance(i);
      w += advances[i];
    }
    return new TextSketch(shaped.font(), glyphs, positions, advances, shaped.fontSize(), w, shaped.fontSize(),
        shaped.flipY(), shaped.fontStyle(), w);
  }

  /**
   * Performs line breaking and returns the resulting text layout.
   *
   * @return the line-broken text layout; never {@code null}
   */
  public TextSketch breakText() {
    String text = shaped.text();
    int textLen = text.length();
    if (textLen == 0 || shaped.glyphCount() == 0) {
      return new TextSketch(shaped.font(), new int[0], new float[0], new float[0], shaped.fontSize(), 0, 0,
          shaped.flipY(), shaped.fontStyle(), 0);
    }

    BreakIterator breaker = BreakIterator.getLineInstance(Locale.US);
    breaker.setText(text);
    int nextBoundary = breaker.following(0);
    if (nextBoundary == BreakIterator.DONE) {
      nextBoundary = textLen;
    }

    for (int ci = 0; ci < textLen; ci++) {
      lineWidth += shaped.charAdvance(ci);
      if (ci + 1 == nextBoundary) {
        processBreak(ci + 1);
        nextBoundary = breaker.next();
        if (nextBoundary == BreakIterator.DONE) {
          nextBoundary = textLen;
        }
      }
    }
    if (prevBreakChar >= 0 && prevBreakChar < textLen) {
      addBreakPoint(textLen, lineWidth);
    } else if (breakPoints.isEmpty() || breakPoints.getLast().charOffset < textLen) {
      addBreakPoint(textLen, lineWidth);
    }

    return buildTextBlob();
  }

  private void processBreak(int charOffset) {
    while (lineWidth > lineWidthLimit) {
      if (!tryBreak()) {
        forceBreak(charOffset);
        return;
      }
    }
    prevBreakChar = charOffset;
    lineWidthAtPrevBreak = lineWidth;
  }

  private boolean tryBreak() {
    if (prevBreakChar < 0) {
      return false;
    }
    addBreakPoint(prevBreakChar, lineWidthAtPrevBreak);
    return true;
  }

  private void forceBreak(int end) {
    int lastChar = Math.max(prevBreakChar, breakPoints.isEmpty() ? 0 : breakPoints.getLast().charOffset);
    float w = 0;
    for (int ci = lastChar; ci < end; ci++) {
      float adv = shaped.charAdvance(ci);
      if (adv == 0) {
        continue;
      }
      if (w + adv > lineWidthLimit) {
        addBreakPoint(ci, w);
        return;
      }
      w += adv;
    }
    addBreakPoint(Math.max(lastChar + 1, end), lineWidth);
  }

  private void addBreakPoint(int charOffset, float lw) {
    breakPoints.add(new BreakPoint(charOffset, lw));
    lineNum++;
    lineWidthLimit = widthLimit;
    lineWidth -= lw;
    prevBreakChar = -1;
    lineWidthAtPrevBreak = 0;
  }

  private TextSketch buildTextBlob() {
    int textLen = shaped.text().length();
    int totalGlyphs = shaped.glyphCount();
    int[] glyphs = new int[totalGlyphs];
    float[] outPositions = new float[totalGlyphs * 2];
    float[] outAdvances = new float[totalGlyphs];
    float lineHeight = shaped.fontSize() * 1.2f;
    int outIdx = 0;
    float maxWidth = 0, lastLineW = 0;

    int glyphLineStart = 0;
    for (int li = 0; li < breakPoints.size(); li++) {
      BreakPoint bp = breakPoints.get(li);
      int breakChar = Math.min(bp.charOffset, textLen);
      int glyphLineEnd = shaped.charToFirstGlyph(breakChar);
      float lineY = li * lineHeight;
      float lineCursor = 0;

      int spaces = 0;
      float lineWidthSum = 0;
      for (int g = glyphLineStart; g < glyphLineEnd; g++) {
        lineWidthSum += shaped.advance(g);
        int ci = shaped.glyphToChar(g);
        if (ci < textLen && shaped.text().charAt(ci) == ' ') {
          spaces++;
        }
      }

      float extra = 0;
      if (justify && li < breakPoints.size() - 1 && spaces > 0) {
        extra = (widthLimit - lineWidthSum) / spaces;
      }

      for (int g = glyphLineStart; g < glyphLineEnd; g++) {
        int ci = shaped.glyphToChar(g);
        float e = (ci < textLen && shaped.text().charAt(ci) == ' ') ? extra : 0;
        glyphs[outIdx] = shaped.glyphs[g];
        outPositions[outIdx * 2] = lineCursor + shaped.xOffset(g);
        outPositions[outIdx * 2 + 1] = lineY + shaped.yOffset(g);
        outAdvances[outIdx] = shaped.advance(g) + e;
        outIdx++;
        lineCursor += shaped.advance(g) + e;
      }
      maxWidth = Math.max(maxWidth, lineCursor);
      lastLineW = lineCursor;
      glyphLineStart = glyphLineEnd;
    }

    float totalHeight = Math.max(1, breakPoints.size()) * lineHeight;
    return new TextSketch(shaped.font(), glyphs, outPositions, outAdvances, shaped.fontSize(), maxWidth, totalHeight,
        shaped.flipY(), shaped.fontStyle(), lastLineW);
  }

  private record BreakPoint(int charOffset, float lineWidth) {
  }
}
