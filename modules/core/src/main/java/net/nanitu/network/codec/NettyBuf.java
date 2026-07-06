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

package net.fmhi.network.codec;

import io.netty.buffer.ByteBuf;
import net.fmhi.memory.Buf;
import net.fmhi.memory.Endianness;

/**
 * A {@link Buf} that wraps a Netty {@link ByteBuf}.
 *
 * <p>Read and write cursors are mapped to the Netty buffer's reader and writer indices. This implementation delegates
 * all byte-level operations to the underlying Netty buffer.
 */
public final class NettyBuf extends Buf {
  private final ByteBuf delegate;

  /**
   * Wraps an existing Netty buffer with the given byte order.
   *
   * @param endianness byte order for multi-byte operations
   * @param delegate   the Netty buffer to wrap; its cursors are adopted as this buffer's cursors
   */
  public NettyBuf(Endianness endianness, ByteBuf delegate) {
    super(endianness);
    this.delegate = delegate;
    this.readerIndex = delegate.readerIndex();
    this.writerIndex = delegate.writerIndex();
  }

  /**
   * Returns the underlying Netty buffer.
   *
   * @return the underlying Netty buffer
   */
  public ByteBuf delegate() {
    return delegate;
  }

  @Override
  protected byte _getByte(long index) {
    return delegate.getByte((int) index);
  }

  @Override
  protected void _putByte(long index, byte value) {
    delegate.setByte((int) index, value);
  }

  @Override
  protected long _capacity() {
    return delegate.capacity();
  }

  @Override
  protected void _expand(long minCapacity) {
    delegate.capacity((int) minCapacity);
  }

  @Override
  public byte getByte() {
    ensureReadable(1);
    return delegate.getByte((int) readerIndex++);
  }

  @Override
  public Buf putByte(byte value) {
    ensureWritable(1);
    delegate.setByte((int) writerIndex++, value);
    return this;
  }

  @Override
  public short getShort() {
    ensureReadable(2);
    short v = delegate.getShort((int) readerIndex);
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
    delegate.setShort((int) writerIndex, v);
    writerIndex += 2;
    return this;
  }

  @Override
  public int getInt() {
    ensureReadable(4);
    int v = delegate.getInt((int) readerIndex);
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
    delegate.setInt((int) writerIndex, v);
    writerIndex += 4;
    return this;
  }

  @Override
  public long getLong() {
    ensureReadable(8);
    long v = delegate.getLong((int) readerIndex);
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
    delegate.setLong((int) writerIndex, v);
    writerIndex += 8;
    return this;
  }

  @Override
  public float getFloat() {
    ensureReadable(4);
    float v = delegate.getFloat((int) readerIndex);
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
      delegate.setFloat((int) writerIndex, Float.intBitsToFloat(bits));
    } else {
      delegate.setFloat((int) writerIndex, value);
    }
    writerIndex += 4;
    return this;
  }

  @Override
  public double getDouble() {
    ensureReadable(8);
    double v = delegate.getDouble((int) readerIndex);
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
      delegate.setDouble((int) writerIndex, Double.longBitsToDouble(bits));
    } else {
      delegate.setDouble((int) writerIndex, value);
    }
    writerIndex += 8;
    return this;
  }

  @Override
  public byte[] getBytes(int length) {
    ensureReadable(length);
    byte[] dst = new byte[length];
    delegate.getBytes((int) readerIndex, dst);
    readerIndex += length;
    return dst;
  }

  @Override
  public Buf putBytes(byte[] src, int srcOffset, int length) {
    ensureWritable(length);
    delegate.setBytes((int) writerIndex, src, srcOffset, length);
    writerIndex += length;
    return this;
  }

  @Override
  public void close() {
    delegate.release();
  }
}
