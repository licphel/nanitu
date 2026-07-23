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

package net.fmhi.codec;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A high-performance, cursor-based byte buffer for binary serialization.
 *
 * <p>A {@code Buf} maintains independent read and write cursors over a backing
 * byte store. Data is written at {@link #writerIndex()} and read from {@link #readerIndex()}; the region between them
 * is <em>readable</em> (already written but not yet consumed).
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Create via a static factory ({@link #heap(int)}
 *   <li>Write data with {@code putXxx()} methods — the write cursor advances automatically</li>
 *   <li>Read data with {@code getXxx()} methods — the read cursor advances automatically</li>
 *   <li>Call {@link #clear()} to reset both cursors and reuse the buffer</li>
 *   <li>Call {@link #close()} to release resources</li>
 * </ol>
 *
 * <h3>Byte order</h3>
 * <p>Multibyte reads and writes respect the buffer's {@link ByteOrder}.
 * Subclasses may override individual read/write methods to use
 * native bulk operations for better performance.
 *
 * <p>This class is <strong>not</strong> thread-safe.
 *
 * @see BufImpl
 */
public abstract class Buf implements AutoCloseable {
  public static final int DEFAULT_CAPACITY = 128;

  protected long readerIndex;
  protected long writerIndex;

  /**
   * Creates a buffer with the given byte order. Both cursors start at zero.
   */
  protected Buf() {
    this.readerIndex = 0;
    this.writerIndex = 0;
  }

  /**
   * Creates a heap buffer with the given capacity.
   *
   * @param capacity   initial capacity in bytes
   * @return a new heap-backed buffer
   */
  public static Buf heap(int capacity) {
    return new BufImpl(capacity);
  }

  /**
   * Creates a heap buffer with {@link Buf#DEFAULT_CAPACITY}.
   *
   * @return a new heap-backed buffer
   */
  public static Buf heap() {
    return new BufImpl(DEFAULT_CAPACITY);
  }

  /**
   * Wraps a byte array into a Buf.
   *
   * @param data the byte array
   * @return a Buf with the given data written
   */
  public static Buf wrap(byte[] data) {
    return new BufImpl(data);
  }

  /**
   * Grows the backing store so its capacity is at least {@code minCapacity} bytes. Called automatically when an
   * expandable buffer overflows.
   *
   * @param minCapacity the required minimum capacity
   */
  protected abstract void expandTo(long minCapacity);

  /**
   * Returns the total capacity of this buffer in bytes.
   *
   * @return byte capacity
   */
  public abstract long capacity();

  /**
   * Returns the current write-cursor position.
   *
   * @return writer index
   */
  public long writerIndex() {
    return writerIndex;
  }

  /**
   * Sets the write cursor to the given index.
   *
   * @param index new write-cursor position; must be in {@code [readerIndex, capacity]}
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if the index is out of the valid range
   */
  public Buf writerIndex(long index) {
    if (index < readerIndex || index > capacity()) {
      throw new IndexOutOfBoundsException(String.format("writerIndex=%d out of range [readerIndex=%d, capacity=%d]",
          index, readerIndex, capacity()));
    }
    writerIndex = index;
    return this;
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
   * Sets the read cursor to the given index.
   *
   * @param index new read-cursor position; must be in {@code [0, writerIndex]}
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if the index is out of the valid range
   */
  public Buf readerIndex(long index) {
    if (index < 0 || index > writerIndex) {
      throw new IndexOutOfBoundsException(String.format("readerIndex=%d out of range [0, writerIndex=%d]", index,
          writerIndex));
    }
    readerIndex = index;
    return this;
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
   * Returns the byte order of the buffer.
   *
   * @return byte order of the buffer.
   */
  public abstract ByteOrder order();

  /**
   * Sets the byte order of the buffer.
   *
   * @param order byte order to set
   * @return {@code this} for chaining
   */
  public abstract Buf order(ByteOrder order);

  /**
   * Resets both cursors to zero, effectively clearing the buffer without zeroing memory.
   *
   * @return {@code this} for chaining
   */
  public Buf clear() {
    readerIndex = 0;
    writerIndex = 0;
    return this;
  }

  protected abstract byte get(long index);

  protected abstract void put(long index, byte value);

  /**
   * Writes a single byte at the current write-cursor position and advances the cursor by 1.
   *
   * @param value the byte to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if the buffer is full and not expandable
   */
  public Buf putByte(byte value) {
    ensureWritable(1);
    put(writerIndex++, value);
    return this;
  }

  /**
   * Writes a {@code boolean} as a single byte (1 for {@code true}, 0 for {@code false}).
   *
   * @param value the boolean to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if the buffer is full and not expandable
   */
  public Buf putBoolean(boolean value) {
    return putByte((byte) (value ? 1 : 0));
  }

  /**
   * Writes a {@code short} (2 bytes) at the current write-cursor position using the buffer's byte order, then advances
   * the cursor by 2.
   *
   * @param value the short to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if fewer than 2 bytes are writable
   */
  public abstract Buf putShort(short value);

  /**
   * Writes an {@code int} (4 bytes) at the current write-cursor position using the buffer's byte order, then advances
   * the cursor by 4.
   *
   * @param value the int to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if fewer than 4 bytes are writable
   */
  public abstract Buf putInt(int value);

  /**
   * Writes a {@code long} (8 bytes) at the current write-cursor position using the buffer's byte order, then advances
   * the cursor by 8.
   *
   * @param value the long to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if fewer than 8 bytes are writable
   */
  public abstract Buf putLong(long value);

  /**
   * Writes a {@code float} (4 bytes) at the current write-cursor position using the buffer's byte order, then advances
   * the cursor by 4.
   *
   * @param value the float to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if fewer than 4 bytes are writable
   */
  public Buf putFloat(float value) {
    return putInt(Float.floatToRawIntBits(value));
  }

  /**
   * Writes a {@code double} (8 bytes) at the current write-cursor position using the buffer's byte order, then advances
   * the cursor by 8.
   *
   * @param value the double to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if fewer than 8 bytes are writable
   */
  public Buf putDouble(double value) {
    return putLong(Double.doubleToRawLongBits(value));
  }

  /**
   * Writes a 32-bit integer in VarInt format (variable-length, little-endian base-128).
   *
   * <p>Each byte uses 7 bits for data, with the most significant bit set to 1 if more
   * bytes follow. Small values (0–127) use 1 byte, larger values use up to 5 bytes.
   *
   * <table>
   *   <caption>VarInt encoding examples</caption>
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
  public Buf putVarInt(int value) {
    ensureWritable(5);
    int v = value;
    while ((v & ~0x7F) != 0) {
      putByte((byte) ((v & 0x7F) | 0x80));
      v >>>= 7;
    }
    putByte((byte) v);
    return this;
  }

  /**
   * Writes a 32-bit signed integer in ZigZag + VarInt format, optimal for small signed values like position deltas.
   *
   * <table>
   *   <caption>ZigZag mapping</caption>
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
  public Buf putZigZagInt(int value) {
    return putVarInt((value << 1) ^ (value >> 31));
  }

  /**
   * Writes a 64-bit integer in VarLong format (variable-length, little-endian base-128). Small values (0–127) use 1
   * byte, up to 10 bytes for the full 64-bit range.
   *
   * @param value the 64-bit integer to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if insufficient writable space
   */
  public Buf putVarLong(long value) {
    ensureWritable(10);
    long v = value;
    while ((v & ~0x7FL) != 0) {
      putByte((byte) ((v & 0x7F) | 0x80));
      v >>>= 7;
    }
    putByte((byte) v);
    return this;
  }

  /**
   * Writes a 64-bit signed integer in ZigZag + VarLong format, optimal for timestamps and delta values.
   *
   * @param value the signed 64-bit integer
   * @return {@code this} for chaining
   */
  public Buf putZigZagLong(long value) {
    return putVarLong((value << 1) ^ (value >> 63));
  }

  /**
   * Writes all bytes from {@code src} at the current write-cursor position and advances the cursor by
   * {@code src.length}.
   *
   * @param src the bytes to write
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if the buffer does not have enough writable space
   */
  public Buf putBytes(byte[] src) {
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
  public abstract Buf putBytes(byte[] src, int srcOffset, int length);

  /**
   * Transfers {@code length} bytes from {@code src}'s readable region into this buffer, advancing both buffers'
   * respective cursors.
   *
   * @param src    source buffer to read from
   * @param length number of bytes to transfer
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if {@code src} has fewer than {@code length} readable bytes or this buffer has
   *                                   insufficient writable space
   */
  public Buf putBuffer(Buf src, long length) {
    src.ensureReadable(length);
    ensureWritable(length);
    for (long i = 0; i < length; i++) {
      put(writerIndex + i, src.get(src.readerIndex + i));
    }
    src.readerIndex += length;
    writerIndex += length;
    return this;
  }

  /**
   * Encodes {@code s} to bytes using the given {@code charset} and writes them at the current write-cursor position,
   * preceded by a 4-byte length prefix (int, in the buffer's byte order).
   *
   * @param s       the string to write
   * @param charset the encoding to use
   * @return {@code this} for chaining
   * @throws IndexOutOfBoundsException if the buffer does not have enough writable space
   */
  public Buf putString(String s, Charset charset) {
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
  public Buf putString(String s) {
    return putString(s, StandardCharsets.UTF_8);
  }

  /**
   * Reads a single byte at the current read-cursor position and advances the cursor by 1.
   *
   * @return the byte value
   * @throws IndexOutOfBoundsException if no bytes are available for reading
   */
  public byte getByte() {
    ensureReadable(1);
    return get(readerIndex++);
  }

  /**
   * Reads a {@code boolean} (1 byte) at the current read-cursor position, then advances the cursor by 1.
   *
   * @return the boolean value
   * @throws IndexOutOfBoundsException if fewer than 1 byte are available
   */
  public boolean getBoolean() {
    return getByte() == 1;
  }

  /**
   * Reads a {@code short} (2 bytes) at the current read-cursor position using the buffer's byte order, then advances
   * the cursor by 2.
   *
   * @return the short value
   * @throws IndexOutOfBoundsException if fewer than 2 bytes are available
   */
  public abstract short getShort();

  /**
   * Reads an {@code int} (4 bytes) at the current read-cursor position using the buffer's byte order, then advances the
   * cursor by 4.
   *
   * @return the int value
   * @throws IndexOutOfBoundsException if fewer than 4 bytes are available
   */
  public abstract int getInt();

  /**
   * Reads a {@code long} (8 bytes) at the current read-cursor position using the buffer's byte order, then advances the
   * cursor by 8.
   *
   * @return the long value
   * @throws IndexOutOfBoundsException if fewer than 8 bytes are available
   */
  public abstract long getLong();

  /**
   * Reads a {@code float} (4 bytes) at the current read-cursor position using the buffer's byte order, then advances
   * the cursor by 4.
   *
   * @return the float value
   * @throws IndexOutOfBoundsException if fewer than 4 bytes are available
   */
  public float getFloat() {
    return Float.intBitsToFloat(getInt());
  }

  /**
   * Reads a {@code double} (8 bytes) at the current read-cursor position using the buffer's byte order, then advances
   * the cursor by 8.
   *
   * @return the double value
   * @throws IndexOutOfBoundsException if fewer than 8 bytes are available
   */
  public double getDouble() {
    return Double.longBitsToDouble(getLong());
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
   * Reads a ZigZag + VarInt encoded 32-bit integer back to a signed int.
   *
   * @return the decoded signed value
   */
  public int getZigZagInt() {
    int encoded = getVarInt();
    return (encoded >>> 1) ^ -(encoded & 1);
  }

  /**
   * Reads a 64-bit integer in VarLong format.
   *
   * @return the decoded value
   * @throws IndexOutOfBoundsException if insufficient readable bytes
   * @throws RuntimeException          if VarLong exceeds 10 bytes (invalid encoding)
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
   * Reads a ZigZag + VarLong encoded 64-bit integer back to a signed long.
   *
   * @return the decoded signed value
   */
  public long getZigZagLong() {
    long encoded = getVarLong();
    return (encoded >>> 1) ^ -(encoded & 1);
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
    for (int i = 0; i < length; i++) {
      dst[i] = get(readerIndex + i);
    }
    readerIndex += length;
    return dst;
  }

  /**
   * Reads a length-prefixed string from the current read-cursor position. The 4-byte int prefix (in the buffer's byte
   * order) encodes the byte length of the encoded string.
   *
   * @param charset the charset to use for decoding
   * @return the decoded string
   * @throws IndexOutOfBoundsException if not enough bytes are available
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
   */
  public String getString() {
    return getString(StandardCharsets.UTF_8);
  }

  /**
   * Discards the already-read bytes by compacting the readable region to the front. After this call
   * {@code readerIndex == 0} and {@code writerIndex == readableBytes()}.
   *
   * @return {@code this} for chaining
   */
  public Buf compact() {
    if (readerIndex == 0) {
      return this;
    }
    long readable = readableBytes();
    if (readable > 0) {
      for (long i = 0; i < readable; i++) {
        put(i, get(readerIndex + i));
      }
    }
    writerIndex = readable;
    readerIndex = 0;
    return this;
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
   * Returns the raw backing array (probably longer than data size).
   *
   * @return raw backing array
   */
  public abstract byte[] backingArray();

  /**
   * Ensures at least {@code needed} bytes are writable, expanding the backing store if necessary.
   *
   * @param needed the number of bytes required
   * @throws IndexOutOfBoundsException if the buffer is not expandable and space is insufficient
   */
  protected void ensureWritable(long needed) {
    if (writableBytes() < needed) {
      expandTo(writerIndex + needed);
    }
  }

  /**
   * Ensures at least {@code needed} bytes are readable.
   *
   * @param needed the number of bytes required
   * @throws IndexOutOfBoundsException if fewer than {@code needed} bytes are available
   */
  protected void ensureReadable(long needed) {
    if (readableBytes() < needed) {
      throw new IndexOutOfBoundsException(needed + " bytes required");
    }
  }

  @Override
  public abstract void close();
}
