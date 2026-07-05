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

package net.nanitu.memory.buf;

import net.nanitu.memory.Buf;
import net.nanitu.memory.Endianness;
import org.jspecify.annotations.NullMarked;

import java.nio.ByteBuffer;

/**
 * A {@link Buf} implementation backed by a {@link java.nio.ByteBuffer}.
 *
 * <p>The buffer is expandable: writes that exceed the current capacity trigger automatic reallocation with double the
 * previous size.
 */
@NullMarked
public final class JavaBuf extends Buf {
  private ByteBuffer buffer;

  /**
   * Creates a new buffer with the given byte order and initial capacity.
   *
   * @param endianness byte order for multi-byte operations
   * @param capacity   initial capacity in bytes
   */
  public JavaBuf(Endianness endianness, int capacity) {
    super(endianness);
    this.buffer = ByteBuffer.allocate(capacity);
  }

  /**
   * Creates a new buffer wrapping an existing {@link ByteBuffer} with the given byte order.
   *
   * @param endianness byte order for multi-byte operations
   * @param buffer     the backing byte buffer; writes go directly into it
   */
  public JavaBuf(Endianness endianness, ByteBuffer buffer) {
    super(endianness);
    this.buffer = buffer;
    this.writerIndex = buffer.position();
    this.readerIndex = 0;
    buffer.position(0);
  }

  /**
   * Returns the underlying {@link ByteBuffer}.
   *
   * @return the underlying {@link ByteBuffer}
   */
  public ByteBuffer buffer() {
    return buffer;
  }

  @Override
  protected byte _getByte(long index) {
    return buffer.get((int) index);
  }

  @Override
  protected void _putByte(long index, byte value) {
    buffer.put((int) index, value);
  }

  @Override
  protected long _capacity() {
    return buffer.capacity();
  }

  @Override
  protected void _expand(long minCapacity) {
    long newCap = Math.max(_capacity() * 2, minCapacity);
    if (newCap > Integer.MAX_VALUE) {
      throw new IllegalStateException("Buffer capacity overflow: " + newCap);
    }
    ByteBuffer newBuf = ByteBuffer.allocate((int) newCap);
    buffer.position(0);
    newBuf.put(buffer);
    buffer = newBuf;
  }

  @Override
  public byte getByte() {
    ensureReadable(1);
    return buffer.get((int) readerIndex++);
  }

  @Override
  public Buf putByte(byte value) {
    ensureWritable(1);
    buffer.put((int) writerIndex++, value);
    return this;
  }

  @Override
  public short getShort() {
    ensureReadable(2);
    short v = buffer.getShort((int) readerIndex);
    if (endianness == Endianness.LITTLE) {
      v = Short.reverseBytes(v);
    }
    readerIndex += 2;
    return v;
  }

  @Override
  public Buf putShort(short value) {
    ensureWritable(2);
    short v = endianness == Endianness.LITTLE ? Short.reverseBytes(value) : value;
    buffer.putShort((int) writerIndex, v);
    writerIndex += 2;
    return this;
  }

  @Override
  public int getInt() {
    ensureReadable(4);
    int v = buffer.getInt((int) readerIndex);
    if (endianness == Endianness.LITTLE) {
      v = Integer.reverseBytes(v);
    }
    readerIndex += 4;
    return v;
  }

  @Override
  public Buf putInt(int value) {
    ensureWritable(4);
    int v = endianness == Endianness.LITTLE ? Integer.reverseBytes(value) : value;
    buffer.putInt((int) writerIndex, v);
    writerIndex += 4;
    return this;
  }

  @Override
  public long getLong() {
    ensureReadable(8);
    long v = buffer.getLong((int) readerIndex);
    if (endianness == Endianness.LITTLE) {
      v = Long.reverseBytes(v);
    }
    readerIndex += 8;
    return v;
  }

  @Override
  public Buf putLong(long value) {
    ensureWritable(8);
    long v = endianness == Endianness.LITTLE ? Long.reverseBytes(value) : value;
    buffer.putLong((int) writerIndex, v);
    writerIndex += 8;
    return this;
  }

  @Override
  public float getFloat() {
    ensureReadable(4);
    float v = buffer.getFloat((int) readerIndex);
    if (endianness == Endianness.LITTLE) {
      int bits = Integer.reverseBytes(Float.floatToRawIntBits(v));
      v = Float.intBitsToFloat(bits);
    }
    readerIndex += 4;
    return v;
  }

  @Override
  public Buf putFloat(float value) {
    ensureWritable(4);
    if (endianness == Endianness.LITTLE) {
      int bits = Integer.reverseBytes(Float.floatToRawIntBits(value));
      buffer.putFloat((int) writerIndex, Float.intBitsToFloat(bits));
    } else {
      buffer.putFloat((int) writerIndex, value);
    }
    writerIndex += 4;
    return this;
  }

  @Override
  public double getDouble() {
    ensureReadable(8);
    double v = buffer.getDouble((int) readerIndex);
    if (endianness == Endianness.LITTLE) {
      long bits = Long.reverseBytes(Double.doubleToRawLongBits(v));
      v = Double.longBitsToDouble(bits);
    }
    readerIndex += 8;
    return v;
  }

  @Override
  public Buf putDouble(double value) {
    ensureWritable(8);
    if (endianness == Endianness.LITTLE) {
      long bits = Long.reverseBytes(Double.doubleToRawLongBits(value));
      buffer.putDouble((int) writerIndex, Double.longBitsToDouble(bits));
    } else {
      buffer.putDouble((int) writerIndex, value);
    }
    writerIndex += 8;
    return this;
  }

  @Override
  public byte[] getBytes(int length) {
    ensureReadable(length);
    byte[] dst = new byte[length];
    for (int i = 0; i < length; i++) {
      dst[i] = buffer.get((int) readerIndex + i);
    }
    readerIndex += length;
    return dst;
  }

  @Override
  public Buf putBytes(byte[] src, int srcOffset, int length) {
    ensureWritable(length);
    for (int i = 0; i < length; i++) {
      buffer.put((int) writerIndex + i, src[srcOffset + i]);
    }
    writerIndex += length;
    return this;
  }

  @Override
  public void close() {
    buffer = ByteBuffer.allocate(0);
  }
}
