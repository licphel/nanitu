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

package net.nanitu.memory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A high-performance, cursor-based byte buffer for binary serialization.
 *
 * <p>A {@code Buffer} wraps a {@link Memory} region and maintains independent
 * read and write cursors. Data is written at {@link #writerIndex()} and read from {@link #readerIndex()}; the region
 * between them is <em>readable</em> (already written but not yet consumed).
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Create via {@link #Buffer(Memory, Endianness)} or acquire from a {@link BufferPool}</li>
 *   <li>Write data with {@code putXxx()} methods — the write cursor advances automatically</li>
 *   <li>Read data with {@code getXxx()} methods — the read cursor advances automatically</li>
 *   <li>Call {@link #clear()} to reset both cursors and reuse the buffer</li>
 *   <li>Call {@link #close()} to dispose both the buffer and its backing memory</li>
 * </ol>
 *
 * <p>Heap-backed buffers are expandable by default: writing past capacity
 * automatically reallocates with double the size. Native buffers are not
 * expandable by default; use {@link #setExpandable(boolean)} to change this.
 *
 * <p>This class is <strong>not</strong> thread-safe.
 *
 * @see Memory
 * @see Endianness
 */
public final class Buffer implements AutoCloseable {
  private final Endianness endianness;
  private Memory memory;
  private long writerIndex;
  private long readerIndex;
  private boolean isExpandable;

  /**
   * Creates a {@code Buffer} over an existing {@link Memory} region with the given byte order.
   *
   * <p>Both cursors start at zero. The caller retains ownership of {@code memory} and is
   * responsible for calling {@link Memory#close()} when the buffer is no longer needed.
   *
   * @param memory     the backing memory region; must not be disposed
   * @param endianness byte order for multibyte reads and writes
   */
  public Buffer(Memory memory, Endianness endianness) {
    this.memory = memory;
    this.endianness = endianness;
    writerIndex = 0;
    readerIndex = 0;
    isExpandable = true;
  }

  /**
   * Returns the total capacity of this buffer in bytes.
   *
   * @return byte capacity
   */
  public long capacity() {
    return memory.size();
  }

  /**
   * Returns the current write-cursor position.
   *
   * @return writer index
   */
  public long writerIndex() {
    return writerIndex;
  }

  /**
   * Returns the current read-cursor position.
   *
   * @return reader index
   */
  public long readerIndex() {
    return readerIndex;
  }

  /**
   * Returns the number of bytes available for reading ({@code writerIndex - readerIndex}).
   *
   * @return readable byte count
   */
  public long readableBytes() {
    return writerIndex - readerIndex;
  }

  /**
   * Returns the number of bytes available for writing ({@code capacity - writerIndex}).
   *
   * @return writable byte count
   */
  public long writableBytes() {
    return capacity() - writerIndex;
  }

  /**
   * Returns the {@link Endianness} used by this buffer.
   *
   * @return the byte order
   */
  public Endianness endianness() {
    return endianness;
  }

  /**
   * Returns the backing {@link Memory} region.
   *
   * @return the backing memory
   */
  public Memory memory() {
    return memory;
  }

  /**
   * Sets whether the buffer can expand.
   *
   * @param expandable the buffer expandability
   */
  public void setExpandable(boolean expandable) {
    isExpandable = expandable;
  }

  /**
   * Sets the write cursor to the given index.
   *
   * @param index new write-cursor position; must be in {@code [readerIndex, capacity]}
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if the index is out of the valid range
   */
  public Buffer writerIndex(long index) {
    if (index < readerIndex || index > capacity()) {
      throw new IndexOutOfBoundsException(String.format("writerIndex=%d out of range [readerIndex=%d, capacity=%d]",
          index, readerIndex, capacity()));
    }
    writerIndex = index;
    return this;
  }

  /**
   * Sets the read cursor to the given index.
   *
   * @param index new read-cursor position; must be in {@code [0, writerIndex]}
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if the index is out of the valid range
   */
  public Buffer readerIndex(long index) {
    if (index < 0 || index > writerIndex) {
      throw new IndexOutOfBoundsException(String.format("readerIndex=%d out of range [0, writerIndex=%d]", index,
          writerIndex));
    }
    readerIndex = index;
    return this;
  }

  /**
   * Resets both cursors to zero, effectively clearing the buffer without zeroing memory.
   *
   * @return {@code this} for chaining
   */
  public Buffer clear() {
    readerIndex = 0;
    writerIndex = 0;
    return this;
  }

  /**
   * Discards the already-read bytes by compacting the readable region to the front. After this call
   * {@code readerIndex == 0} and {@code writerIndex == readableBytes()}.
   *
   * @return {@code this} for chaining
   */
  public Buffer compact() {
    if (readerIndex == 0) {
      return this;
    }
    long readable = readableBytes();
    if (readable > 0) {
      memory.copyTo(readerIndex, memory, 0, readable);
    }
    writerIndex = readable;
    readerIndex = 0;
    return this;
  }

  /**
   * Writes a single byte at the current write-cursor position and advances the cursor by 1.
   *
   * @param value the byte to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if the buffer is full
   */
  public Buffer putByte(byte value) {
    ensureWritable(Byte.BYTES);
    memory.putByte(writerIndex, value);
    writerIndex += Byte.BYTES;
    return this;
  }

  /**
   * Writes a {@code boolean} (1 byte) at the current write-cursor position using the buffer's byte order, then advances
   * the cursor by 1.
   *
   * @param value the boolean to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if fewer than 1 byte are available for writing
   */
  public Buffer putBoolean(boolean value) {
    ensureWritable(Byte.BYTES);
    memory.putByte(writerIndex, (byte) (value ? 1 : 0));
    writerIndex += Byte.BYTES;
    return this;
  }

  /**
   * Writes a {@code short} (2 bytes) at the current write-cursor position using the buffer's byte order, then advances
   * the cursor by 2.
   *
   * @param value the short to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if fewer than 2 bytes are available for writing
   * @throws IllegalArgumentException  if endianness is {@link Endianness#UNSURE}
   */
  public Buffer putShort(short value) {
    ensureWritable(Short.BYTES);
    memory.putShort(writerIndex, value, endianness);
    writerIndex += Short.BYTES;
    return this;
  }

  /**
   * Writes an {@code int} (4 bytes) at the current write-cursor position using the buffer's byte order, then advances
   * the cursor by 4.
   *
   * @param value the int to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if fewer than 4 bytes are available for writing
   * @throws IllegalArgumentException  if endianness is {@link Endianness#UNSURE}
   */
  public Buffer putInt(int value) {
    ensureWritable(Integer.BYTES);
    memory.putInt(writerIndex, value, endianness);
    writerIndex += Integer.BYTES;
    return this;
  }

  /**
   * Writes a 32-bit integer in VarInt format (variable-length, little-endian base-128).
   *
   * <p>Each byte uses 7 bits for data, with the most significant bit set to 1 if more bytes follow.
   * Small values (0-127) use 1 byte, larger values use up to 5 bytes.
   *
   * <table summary="VarInt encoding examples">
   *   <caption>VarInt Encoding Examples</caption>
   *   <tr><th>Value</th><th>Hex bytes</th></tr>
   *   <tr><td>0</td><td>0x00</td></tr>
   *   <tr><td>1</td><td>0x01</td></tr>
   *   <tr><td>127</td><td>0x7F</td></tr>
   *   <tr><td>128</td><td>0x80 0x01</td></tr>
   *   <tr><td>300</td><td>0xAC 0x02</td></tr>
   * </table>
   *
   * @param value the 32-bit integer to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if insufficient writable space remains
   */
  public Buffer putVarInt(int value) {
    ensureWritable(5); // VarInt max 5 bytes
    int v = value;
    while ((v & ~0x7F) != 0) {
      putByte((byte) ((v & 0x7F) | 0x80));
      v >>>= 7;
    }
    putByte((byte) v);
    return this;
  }

  /**
   * Writes a 32-bit integer in ZigZag + VarInt format, optimal for small signed values.
   *
   * <p>ZigZag mapping: encodes signed integers as unsigned so small negative numbers become small
   * positive numbers, then writes via VarInt. Perfect for position deltas, entity IDs, etc.
   *
   * <table summary="ZigZag mapping examples">
   *   <caption>ZigZag Mapping</caption>
   *   <tr><th>Original</th><th>ZigZag encoded</th><th>VarInt bytes</th></tr>
   *   <tr><td>0</td><td>0</td><td>0x00 (1)</td></tr>
   *   <tr><td>-1</td><td>1</td><td>0x01 (1)</td></tr>
   *   <tr><td>1</td><td>2</td><td>0x02 (1)</td></tr>
   *   <tr><td>-2</td><td>3</td><td>0x03 (1)</td></tr>
   *   <tr><td>127</td><td>254</td><td>0xFE 0x01 (2)</td></tr>
   *   <tr><td>-128</td><td>255</td><td>0xFF 0x01 (2)</td></tr>
   * </table>
   *
   * @param value the signed 32-bit integer
   * @return {@code this} for chaining
   */
  public Buffer putZigZagInt(int value) {
    int encoded = (value << 1) ^ (value >> 31);
    return putVarInt(encoded);
  }

  /**
   * Writes a {@code long} (8 bytes) at the current write-cursor position using the buffer's byte order, then advances
   * the cursor by 8.
   *
   * @param value the long to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if fewer than 8 bytes are available for writing
   * @throws IllegalArgumentException  if endianness is {@link Endianness#UNSURE}
   */
  public Buffer putLong(long value) {
    ensureWritable(Long.BYTES);
    memory.putLong(writerIndex, value, endianness);
    writerIndex += Long.BYTES;
    return this;
  }

  /**
   * Writes a 64-bit integer in VarInt format (variable-length, little-endian base-128).
   *
   * <p>Small values (0-127) use 1 byte, up to 10 bytes for full 64-bit range.
   *
   * @param value the 64-bit integer to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if insufficient writable space
   */
  public Buffer putVarLong(long value) {
    ensureWritable(10); // VarLong max 10 bytes
    long v = value;
    while ((v & ~0x7FL) != 0) {
      putByte((byte) ((v & 0x7F) | 0x80));
      v >>>= 7;
    }
    putByte((byte) v);
    return this;
  }

  /**
   * Writes a 64-bit signed integer in ZigZag + VarLong format.
   *
   * <p>Ideal for timestamps, delta values, or any signed long where small magnitude expected.
   *
   * @param value the signed 64-bit integer
   * @return {@code this} for chaining
   */
  public Buffer putZigZagLong(long value) {
    long encoded = (value << 1) ^ (value >> 63);
    return putVarLong(encoded);
  }

  /**
   * Writes a {@code float} (4 bytes) at the current write-cursor position using the buffer's byte order, then advances
   * the cursor by 4.
   *
   * @param value the float to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if fewer than 4 bytes are available for writing
   * @throws IllegalArgumentException  if endianness is {@link Endianness#UNSURE}
   */
  public Buffer putFloat(float value) {
    ensureWritable(Float.BYTES);
    memory.putFloat(writerIndex, value, endianness);
    writerIndex += Float.BYTES;
    return this;
  }

  /**
   * Writes a {@code double} (8 bytes) at the current write-cursor position using the buffer's byte order, then advances
   * the cursor by 8.
   *
   * @param value the double to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if fewer than 8 bytes are available for writing
   * @throws IllegalArgumentException  if endianness is {@link Endianness#UNSURE}
   */
  public Buffer putDouble(double value) {
    ensureWritable(Double.BYTES);
    memory.putDouble(writerIndex, value, endianness);
    writerIndex += Double.BYTES;
    return this;
  }

  /**
   * Writes all bytes from {@code src} at the current write-cursor position and advances the cursor by
   * {@code src.length}.
   *
   * @param src the bytes to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if the buffer does not have enough writable space
   */
  public Buffer putBytes(byte[] src) {
    return putBytes(src, 0, src.length);
  }

  /**
   * Writes {@code length} bytes from {@code src} starting at {@code srcOffset}, advancing the write cursor by
   * {@code length}.
   *
   * @param src       the source byte array
   * @param srcOffset starting offset within {@code src}
   * @param length    number of bytes to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if source range or buffer space is insufficient
   */
  public Buffer putBytes(byte[] src, int srcOffset, int length) {
    ensureWritable(length);
    new Memory(src).copyTo(srcOffset, memory, writerIndex, length);
    writerIndex += length;
    return this;
  }

  /**
   * Writes {@code length} bytes from another {@code Buffer}'s readable region into this buffer, advancing both buffers'
   * respective cursors.
   *
   * @param src    source buffer to read from
   * @param length number of bytes to transfer
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if {@code src} has fewer than {@code length} readable bytes or this buffer has
   *                                   insufficient writable space
   */
  public Buffer putBuffer(Buffer src, long length) {
    src.ensureReadable(length);
    ensureWritable(length);

    src.memory.copyTo(src.readerIndex, memory, writerIndex, length);
    src.readerIndex += length;
    writerIndex += length;
    return this;
  }

  /**
   * Encodes {@code s} to bytes using the given {@code charset} and writes them at the current write-cursor position,
   * preceded by a 4-byte length prefix (int, buffer byte order).
   *
   * @param s       the string to write
   * @param charset the encoding to use
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if the buffer does not have enough writable space
   * @throws IllegalArgumentException  if endianness is {@link Endianness#UNSURE}
   */
  public Buffer putString(String s, Charset charset) {
    byte[] encoded = s.getBytes(charset);
    putInt(encoded.length);
    putBytes(encoded);
    return this;
  }

  /**
   * Encodes {@code s} to UTF-8 bytes and writes them with a 4-byte length prefix.
   *
   * @param s the string to write
   * @return {@code this} for chaining
   */
  public Buffer putString(String s) {
    return putString(s, StandardCharsets.UTF_8);
  }

  /**
   * Reads a single byte at the current read-cursor position and advances the cursor by 1.
   *
   * @return the byte value
   * @throws IndexOutOfBoundsException if no bytes are available for reading
   */
  public byte getByte() {
    ensureReadable(Byte.BYTES);
    byte v = memory.getByte(readerIndex);
    readerIndex += Byte.BYTES;
    return v;
  }

  /**
   * Reads a {@code boolean} (1 byte) at the current read-cursor position using the buffer's byte order, then advances
   * the cursor by 1.
   *
   * @return the boolean value
   * @throws IndexOutOfBoundsException if fewer than 1 byte are available for reading
   */
  public boolean getBoolean() {
    ensureReadable(Byte.BYTES);
    boolean v = memory.getByte(readerIndex) == 1;
    readerIndex += Byte.BYTES;
    return v;
  }

  /**
   * Reads a {@code short} (2 bytes) at the current read-cursor position using the buffer's byte order, then advances
   * the cursor by 2.
   *
   * @return the short value
   * @throws IndexOutOfBoundsException if fewer than 2 bytes are available for reading
   * @throws IllegalArgumentException  if endianness is {@link Endianness#UNSURE}
   */
  public short getShort() {
    ensureReadable(Short.BYTES);
    short v = memory.getShort(readerIndex, endianness);
    readerIndex += Short.BYTES;
    return v;
  }

  /**
   * Reads an {@code int} (4 bytes) at the current read-cursor position using the buffer's byte order, then advances the
   * cursor by 4.
   *
   * @return the int value
   * @throws IndexOutOfBoundsException if fewer than 4 bytes are available for reading
   * @throws IllegalArgumentException  if endianness is {@link Endianness#UNSURE}
   */
  public int getInt() {
    ensureReadable(Integer.BYTES);
    int v = memory.getInt(readerIndex, endianness);
    readerIndex += Integer.BYTES;
    return v;
  }

  /**
   * Reads a 32-bit integer in VarInt format.
   *
   * @return the decoded value
   * @throws IndexOutOfBoundsException if insufficient readable bytes
   * @throws RuntimeException          if the VarInt exceeds 5 bytes (invalid encoding)
   */
  public int getVarInt() {
    int result = 0;
    int shift = 0;
    byte b;
    do {
      ensureReadable(1);
      b = getByte();
      result |= (b & 0x7F) << shift;
      shift += 7;
      if (shift > 35) {
        throw new RuntimeException("VarInt too long, exceeds 5 bytes");
      }
    } while ((b & 0x80) != 0);
    return result;
  }

  /**
   * Reads a ZigZag + VarInt encoded 32-bit integer back to signed int.
   *
   * @return the decoded signed value
   */
  public int getZigZagInt() {
    int encoded = getVarInt();
    return (encoded >>> 1) ^ -(encoded & 1);
  }

  /**
   * Reads a {@code long} (8 bytes) at the current read-cursor position using the buffer's byte order, then advances the
   * cursor by 8.
   *
   * @return the long value
   * @throws IndexOutOfBoundsException if fewer than 8 bytes are available for reading
   * @throws IllegalArgumentException  if endianness is {@link Endianness#UNSURE}
   */
  public long getLong() {
    ensureReadable(Long.BYTES);
    long v = memory.getLong(readerIndex, endianness);
    readerIndex += Long.BYTES;
    return v;
  }

  /**
   * Reads a 64-bit integer in VarLong format.
   *
   * @return the decoded value
   * @throws IndexOutOfBoundsException if insufficient readable bytes
   * @throws RuntimeException          if VarLong exceeds 10 bytes
   */
  public long getVarLong() {
    long result = 0;
    int shift = 0;
    byte b;
    do {
      ensureReadable(1);
      b = getByte();
      result |= ((long) (b & 0x7F)) << shift;
      shift += 7;
      if (shift > 70) {
        throw new RuntimeException("VarLong too long, exceeds 10 bytes");
      }
    } while ((b & 0x80) != 0);
    return result;
  }

  /**
   * Reads a ZigZag + VarLong encoded 64-bit integer back to signed long.
   *
   * @return the decoded signed value
   */
  public long getZigZagLong() {
    long encoded = getVarLong();
    return (encoded >>> 1) ^ -(encoded & 1);
  }

  /**
   * Reads a {@code float} (4 bytes) at the current read-cursor position using the buffer's byte order, then advances
   * the cursor by 4.
   *
   * @return the float value
   * @throws IndexOutOfBoundsException if fewer than 4 bytes are available for reading
   * @throws IllegalArgumentException  if endianness is {@link Endianness#UNSURE}
   */
  public float getFloat() {
    ensureReadable(Float.BYTES);
    float v = memory.getFloat(readerIndex, endianness);
    readerIndex += Float.BYTES;
    return v;
  }

  /**
   * Reads a {@code double} (8 bytes) at the current read-cursor position using the buffer's byte order, then advances
   * the cursor by 8.
   *
   * @return the double value
   * @throws IndexOutOfBoundsException if fewer than 8 bytes are available for reading
   * @throws IllegalArgumentException  if endianness is {@link Endianness#UNSURE}
   */
  public double getDouble() {
    ensureReadable(Double.BYTES);
    double v = memory.getDouble(readerIndex, endianness);
    readerIndex += Double.BYTES;
    return v;
  }

  /**
   * Reads {@code length} bytes from the current read-cursor position into a new array and advances the cursor by
   * {@code length}.
   *
   * @param length number of bytes to read
   * @return a freshly allocated {@code byte[]} containing the read bytes
   * @throws IndexOutOfBoundsException if fewer than {@code length} bytes are available
   */
  public byte[] getBytes(int length) {
    ensureReadable(length);
    byte[] dst = new byte[length];
    memory.copyTo(readerIndex, new Memory(dst), 0, length);
    readerIndex += length;
    return dst;
  }

  /**
   * Reads a length-prefixed string from the current read-cursor position. The 4-byte int prefix (buffer byte order)
   * encodes the byte length of the encoded string.
   *
   * @param charset the charset to use for decoding
   * @return the decoded string
   * @throws IndexOutOfBoundsException if not enough bytes are available
   * @throws IllegalArgumentException  if endianness is {@link Endianness#UNSURE}
   */
  public String getString(Charset charset) {
    int byteLen = getInt();
    byte[] encoded = getBytes(byteLen);
    return new String(encoded, charset);
  }

  /**
   * Reads a length-prefixed UTF-8 string from the current read-cursor position.
   *
   * @return the decoded string
   * @throws IndexOutOfBoundsException if not enough bytes are available
   * @throws IllegalArgumentException  if endianness is {@link Endianness#UNSURE}
   */
  public String getString() {
    return getString(StandardCharsets.UTF_8);
  }

  /**
   * Returns a {@link Memory} slice of the current readable region {@code [readerIndex, writerIndex)}. The slice is a
   * lightweight view — no data is copied.
   *
   * @return a {@code Memory} slice of readable bytes
   */
  public Memory slice() {
    return memory.slice(readerIndex, readableBytes());
  }

  /**
   * Copies all readable bytes into a new {@code byte[]}.
   *
   * @return a snapshot of the readable bytes
   */
  public byte[] toByteArray() {
    return getBytes((int) readableBytes());
  }

  /**
   * Disposes the buffer and its memory region.
   */
  @Override
  public void close() {
    memory.close();
  }

  private void ensureWritable(long needed) {
    if (writableBytes() < needed) {
      if (!isExpandable) {
        throw new IndexOutOfBoundsException("Buffer is not expandable! " + needed + " bytes required");
      }

      long nextCapacity = capacity() == 0 ? 16 : capacity() * 2;
      MemoryAllocator allocator = memory.isNative() ? MemoryAllocator.NATIVE : MemoryAllocator.HEAP;
      memory = allocator.reallocate(memory, nextCapacity);
    }
  }

  private void ensureReadable(long needed) {
    if (readableBytes() < needed) {
      throw new IndexOutOfBoundsException(needed + " bytes required");
    }
  }
}
