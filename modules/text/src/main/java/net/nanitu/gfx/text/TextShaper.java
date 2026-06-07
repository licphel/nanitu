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

import net.nanitu.gfx.GraphicsException;

import java.nio.ByteBuffer;

import static org.lwjgl.util.harfbuzz.HarfBuzz.*;

/**
 * Shapes text into positioned glyph IDs using HarfBuzz.
 *
 * <p>This is a static utility class that cannot be instantiated.
 * Shaped output is returned as a {@link TextBlob}.
 */
public final class TextShaper {
  private TextShaper() {
  }

  /**
   * Shapes text with Y-down screen coordinates, suitable for 2D rendering.
   *
   * @param font     the font to shape with
   * @param text     the text to shape
   * @param fontSize the pixel size
   * @return the shaped text blob, or {@code null} if the text is {@code null} or empty
   */
  public static TextBlob shape(Font font, String text, float fontSize) {
    return shape(font, text, fontSize, true);
  }

  /**
   * Shapes text at the given pixel size with configurable Y-axis direction.
   *
   * @param font     the font to shape with
   * @param text     the text to shape
   * @param fontSize the pixel size
   * @param flipY    if {@code true}, negate Y values for screen Y-down coordinates; set to {@code false} for Y-up
   *                 coordinate systems
   * @return the shaped text blob, or {@code null} if the text is {@code null} or empty, or if shaping produces no
   * glyphs
   */
  public static TextBlob shape(Font font, String text, float fontSize, boolean flipY) {
    ByteBuffer fileData = font.fileData();
    long blob = hb_blob_create(fileData, HB_MEMORY_MODE_READONLY, 0L, null);
    long face = hb_face_create(blob, 0);
    long hbFont = hb_font_create(face);

    hb_font_set_scale(hbFont, (int) (fontSize * 64), (int) (fontSize * 64));

    long buf = hb_buffer_create();
    try {
      hb_buffer_set_direction(buf, HB_DIRECTION_LTR);
      hb_buffer_set_script(buf, HB_SCRIPT_COMMON);
      hb_buffer_add_utf8(buf, text, 0, text.length());

      hb_shape(hbFont, buf, null);

      var glyphInfos = hb_buffer_get_glyph_infos(buf);
      var glyphPositions = hb_buffer_get_glyph_positions(buf);

      if (glyphInfos == null || glyphPositions == null) {
        throw new GraphicsException("Failed to load glyph infos or positions");
      }

      int count = glyphInfos.limit();
      if (count == 0) {
        return new TextBlob(font, new int[0], new float[0], fontSize, 0, 0);
      }

      int[] glyphs = new int[count];
      float[] positions = new float[count * 2];

      float ySign = flipY ? -1.0F : 1.0F;
      float cursorX = 0, cursorY = 0;
      float maxLineWidth = 0, minY = 0, maxY = 0;

      for (int i = 0; i < count; i++) {
        var info = glyphInfos.get(i);
        var pos = glyphPositions.get(i);

        int gid = info.codepoint();

        float xAdv = pos.x_advance() / 64.0F;
        float yAdv = ySign * pos.y_advance() / 64.0F;
        float xOff = pos.x_offset() / 64.0F;
        float yOff = ySign * pos.y_offset() / 64.0F;

        float gx = cursorX + xOff;
        float gy = cursorY + yOff;

        glyphs[i] = gid;
        positions[i * 2] = gx;
        positions[i * 2 + 1] = gy;

        cursorX += xAdv;
        cursorY += yAdv;
        maxLineWidth = Math.max(maxLineWidth, cursorX);
        minY = Math.min(minY, gy);
        maxY = Math.max(maxY, gy);
      }

      float totalHeight = maxY - minY + fontSize;
      return new TextBlob(font, glyphs, positions, fontSize, maxLineWidth, totalHeight);
    } finally {
      hb_buffer_destroy(buf);
      hb_font_destroy(hbFont);
      hb_face_destroy(face);
      hb_blob_destroy(blob);
    }
  }
}
