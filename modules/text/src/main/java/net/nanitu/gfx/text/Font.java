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
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_Size;

import java.nio.ByteBuffer;

import static org.lwjgl.util.freetype.FreeType.*;

/**
 * Wraps a FreeType face for glyph lookup and rasterization.
 *
 * <p>A font is opened from a file via {@link #open(String)}. Once opened, it provides
 * glyph indices, font metrics, and the backing data needed for text shaping.
 *
 * <p>Must be {@linkplain #close() closed} to release native resources.
 */
public final class Font implements AutoCloseable {
  /** Default pixel height used for metrics calculation. */
  public static final float BASIC_LINE_HEIGHT = 16.0F;

  private final FT_Face ftFace;
  private final long ftLib;
  private final FontMetrics metrics;
  private final ByteBuffer fileData; // keep alive for HarfBuzz blob
  private boolean disposed;

  /**
   * Creates a font instance.
   *
   * @param ftLib    the FreeType library handle
   * @param ftFace   the FreeType face handle
   * @param fileData the font file data in memory
   */
  private Font(long ftLib, FT_Face ftFace, ByteBuffer fileData) {
    this.ftLib = ftLib;
    this.ftFace = ftFace;
    this.fileData = fileData;
    FT_Set_Pixel_Sizes(ftFace, 0, (int) BASIC_LINE_HEIGHT);

    float scale = 1.0F / (BASIC_LINE_HEIGHT * 64.0F);
    FT_Size size = ftFace.size();

    if (size == null) {
      throw new GraphicsException("FT_Size is null");
    }

    this.metrics = new FontMetrics(ftFace.ascender() * scale, ftFace.descender() * scale,
        size.metrics().height() * scale, ftFace.underline_position() * scale, ftFace.underline_thickness() * scale);
  }

  /**
   * Opens a font from the given file path, using the first face.
   *
   * @param path the file path to the font file
   * @return the opened font
   * @throws RuntimeException if FreeType initialization or font loading fails
   */
  public static Font open(String path) {
    return open(path, 0);
  }

  /**
   * Opens a font from the given file path with the specified face index.
   *
   * @param path      the file path to the font file
   * @param faceIndex the face index within the font file (0 for the first face)
   * @return the opened font
   * @throws RuntimeException if FreeType initialization, font loading, or file reading fails
   */
  public static Font open(String path, int faceIndex) {
    try (var stack = MemoryStack.stackPush()) {
      var libOut = stack.callocPointer(1);
      check(FT_Init_FreeType(libOut), "FT_Init_FreeType");
      long lib = libOut.get(0);

      var faceOut = stack.callocPointer(1);
      ByteBuffer pathBuf = stack.UTF8(path);
      check(FT_New_Face(lib, pathBuf, faceIndex, faceOut), "FT_New_Face " + path);
      FT_Face face = FT_Face.create(faceOut.get(0));
      FT_Select_Charmap(face, FT_ENCODING_UNICODE);

      // Read file data into memory for HarfBuzz
      byte[] bytes;
      try {
        bytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path));
      } catch (java.io.IOException e) {
        throw new RuntimeException("Failed to read font file: " + path, e);
      }
      ByteBuffer fileBuf = ByteBuffer.allocateDirect(bytes.length).put(bytes).flip();

      return new Font(lib, face, fileBuf);
    }
  }

  private static void check(int err, String msg) {
    if (err != 0) {
      throw new RuntimeException("FreeType: " + msg + " (code " + err + ")");
    }
  }

  /**
   * Returns the font metrics at the default resolution.
   *
   * @return the font metrics
   */
  public FontMetrics metrics() {
    return metrics;
  }

  /** Returns the raw FreeType face for glyph rasterization. */
  FT_Face ftFace() {
    return ftFace;
  }

  /** Returns the font file data for HarfBuzz shaping. */
  ByteBuffer fileData() {
    return fileData;
  }

  /**
   * Maps a Unicode codepoint to a glyph index.
   *
   * @param codepoint the Unicode codepoint
   * @return the glyph index, or 0 if the codepoint has no glyph
   */
  public int getGlyphIndex(int codepoint) {
    return FT_Get_Char_Index(ftFace, codepoint);
  }

  /**
   * Returns whether the font contains a glyph for the given codepoint.
   *
   * @param codepoint the Unicode codepoint
   * @return {@code true} if a glyph exists for the codepoint
   */
  public boolean hasGlyph(int codepoint) {
    return FT_Get_Char_Index(ftFace, codepoint) != 0;
  }

  @Override
  public void close() {
    if (disposed) {
      return;
    }
    disposed = true;
    FT_Done_Face(ftFace);
    FT_Done_Library(ftLib);
  }
}
