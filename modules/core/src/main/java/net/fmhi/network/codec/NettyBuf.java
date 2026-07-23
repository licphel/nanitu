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
import net.fmhi.codec.Buf;

import java.nio.ByteOrder;

/**
 * A {@link Buf} that wraps a Netty {@link ByteBuf}.
 *
 * <p>Read and write cursors are mapped to the Netty buffer's reader and writer indices. This implementation delegates
 * all byte-level operations to the underlying Netty buffer.
 */
public final class NettyBuf extends Buf {
  private final ByteBuf buffer;

  /**
   * Wraps an existing Netty buffer with the given byte order.
   *
   * @param buffer   the Netty buffer to wrap; its cursors are adopted as this buffer's cursors
   */
  public NettyBuf(ByteBuf buffer) {
    this.buffer = buffer;
    this.readerIndex = buffer.readerIndex();
    this.writerIndex = buffer.writerIndex();
  }

  /**
   * Returns the underlying Netty buffer.
   *
   * @return the underlying Netty buffer
   */
  public ByteBuf delegate() {
    return buffer;
  }

  @Override
  protected byte get(long index) {
    return buffer.getByte((int) index);
  }

  @Override
  protected void put(long index, byte value) {
    buffer.setByte((int) index, value);
  }

  @Override
  public long capacity() {
    return buffer.capacity();
  }

  @Override
  protected void expandTo(long minCapacity) {
    buffer.capacity((int) minCapacity);
  }

  @Override
  public ByteOrder order() {
    return ByteOrder.BIG_ENDIAN;
  }

  @Override
  public Buf order(ByteOrder order) {
    throw new UnsupportedOperationException("Net buffer doesn't support byte order");
  }

  @Override
  public Buf putShort(short value) {
    ensureWritable(2);
    buffer.setShort((int) writerIndex, value);
    writerIndex += 2;
    return this;
  }

  @Override
  public Buf putInt(int value) {
    ensureWritable(4);
    buffer.setInt((int) writerIndex, value);
    writerIndex += 4;
    return this;
  }

  @Override
  public Buf putLong(long value) {
    ensureWritable(8);
    buffer.setLong((int) writerIndex, value);
    writerIndex += 8;
    return this;
  }

  @Override
  public Buf putFloat(float value) {
    ensureWritable(4);
    buffer.setFloat((int) writerIndex, value);
    writerIndex += 4;
    return this;
  }

  @Override
  public Buf putDouble(double value) {
    ensureWritable(8);
    buffer.setDouble((int) writerIndex, value);
    writerIndex += 8;
    return this;
  }

  @Override
  public Buf putBytes(byte[] src, int srcOffset, int length) {
    ensureWritable(length);
    buffer.setBytes((int) writerIndex, src, srcOffset, length);
    writerIndex += length;
    return this;
  }

  @Override
  public short getShort() {
    ensureReadable(2);
    short v = buffer.getShort((int) readerIndex);
    readerIndex += 2;
    return v;
  }

  @Override
  public int getInt() {
    ensureReadable(4);
    int v = buffer.getInt((int) readerIndex);
    readerIndex += 4;
    return v;
  }

  @Override
  public long getLong() {
    ensureReadable(8);
    long v = buffer.getLong((int) readerIndex);
    readerIndex += 8;
    return v;
  }

  @Override
  public float getFloat() {
    ensureReadable(4);
    float v = buffer.getFloat((int) readerIndex);
    readerIndex += 4;
    return v;
  }

  @Override
  public double getDouble() {
    ensureReadable(8);
    double v = buffer.getDouble((int) readerIndex);
    readerIndex += 8;
    return v;
  }

  @Override
  public byte[] getBytes(int length) {
    ensureReadable(length);
    byte[] dst = new byte[length];
    buffer.getBytes((int) readerIndex, dst);
    readerIndex += length;
    return dst;
  }

  @Override
  public byte[] backingArray() {
    return buffer.array();
  }

  @Override
  public void close() {
    buffer.release();
  }
}
