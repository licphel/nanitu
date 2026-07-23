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

import net.fmhi.util.InternalApi;

import java.nio.ByteOrder;

@InternalApi
public final class BufImpl extends Buf {
  private byte[] data;
  private boolean bigEndian;

  public BufImpl(int capacity) {
    data = new byte[capacity];
    order(ByteOrder.nativeOrder());
  }

  public BufImpl(byte[] initial) {
    data = initial;
  }

  @Override
  protected byte get(long index) {
    return data[(int) index];
  }

  @Override
  protected void put(long index, byte value) {
    data[(int) index] = value;
  }

  @Override
  public long capacity() {
    return data.length;
  }

  @Override
  protected void expandTo(long minCapacity) {
    int newLen = (int) Math.max(data.length * 2L, minCapacity);
    byte[] newData = new byte[newLen];
    System.arraycopy(data, 0, newData, 0, (int) writerIndex);
    data = newData;
  }

  @Override
  public ByteOrder order() {
    return bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
  }

  @Override
  public Buf order(ByteOrder order) {
    bigEndian = order == ByteOrder.BIG_ENDIAN;
    return this;
  }

  @Override
  public Buf putShort(short value) {
    ensureWritable(2);
    int wi = (int) writerIndex;
    if (bigEndian) {
      data[wi] = (byte) (value >> 8);
      data[wi + 1] = (byte) value;
    } else {
      data[wi] = (byte) value;
      data[wi + 1] = (byte) (value >> 8);
    }
    writerIndex = wi + 2;
    return this;
  }

  @Override
  public Buf putInt(int value) {
    ensureWritable(4);
    int wi = (int) writerIndex;
    if (bigEndian) {
      data[wi] = (byte) (value >> 24);
      data[wi + 1] = (byte) (value >> 16);
      data[wi + 2] = (byte) (value >> 8);
      data[wi + 3] = (byte) value;
    } else {
      data[wi] = (byte) value;
      data[wi + 1] = (byte) (value >> 8);
      data[wi + 2] = (byte) (value >> 16);
      data[wi + 3] = (byte) (value >> 24);
    }
    writerIndex = wi + 4;
    return this;
  }

  @Override
  public Buf putLong(long value) {
    ensureWritable(8);
    int wi = (int) writerIndex;
    if (bigEndian) {
      data[wi] = (byte) (value >> 56);
      data[wi + 1] = (byte) (value >> 48);
      data[wi + 2] = (byte) (value >> 40);
      data[wi + 3] = (byte) (value >> 32);
      data[wi + 4] = (byte) (value >> 24);
      data[wi + 5] = (byte) (value >> 16);
      data[wi + 6] = (byte) (value >> 8);
      data[wi + 7] = (byte) value;
    } else {
      data[wi] = (byte) value;
      data[wi + 1] = (byte) (value >> 8);
      data[wi + 2] = (byte) (value >> 16);
      data[wi + 3] = (byte) (value >> 24);
      data[wi + 4] = (byte) (value >> 32);
      data[wi + 5] = (byte) (value >> 40);
      data[wi + 6] = (byte) (value >> 48);
      data[wi + 7] = (byte) (value >> 56);
    }
    writerIndex = wi + 8;
    return this;
  }

  @Override
  public Buf putBytes(byte[] src, int srcOffset, int length) {
    ensureWritable(length);
    System.arraycopy(src, srcOffset, data, (int) writerIndex, length);
    writerIndex += length;
    return this;
  }

  @Override
  public short getShort() {
    ensureReadable(2);
    int ri = (int) readerIndex;
    short v;
    if (bigEndian) {
      v = (short) ((data[ri] << 8)
          | (data[ri + 1] & 0xFF));
    } else {
      v = (short) ((data[ri] & 0xFF)
          | (data[ri + 1] << 8));
    }
    readerIndex = ri + 2;
    return v;
  }

  @Override
  public int getInt() {
    ensureReadable(4);
    int ri = (int) readerIndex;
    int v;
    if (bigEndian) {
      v = (data[ri] << 24)
          | ((data[ri + 1] & 0xFF) << 16)
          | ((data[ri + 2] & 0xFF) << 8)
          | (data[ri + 3] & 0xFF);
    } else {
      v = (data[ri] & 0xFF)
          | ((data[ri + 1] & 0xFF) << 8)
          | ((data[ri + 2] & 0xFF) << 16)
          | (data[ri + 3] << 24);
    }
    readerIndex = ri + 4;
    return v;
  }

  @Override
  public long getLong() {
    ensureReadable(8);
    int ri = (int) readerIndex;
    long v;
    if (bigEndian) {
      v = ((long) data[ri] << 56)
          | ((data[ri + 1] & 0xFFL) << 48)
          | ((data[ri + 2] & 0xFFL) << 40)
          | ((data[ri + 3] & 0xFFL) << 32)
          | ((data[ri + 4] & 0xFFL) << 24)
          | ((data[ri + 5] & 0xFFL) << 16)
          | ((data[ri + 6] & 0xFFL) << 8)
          | (data[ri + 7] & 0xFFL);
    } else {
      v = (data[ri] & 0xFFL)
          | ((data[ri + 1] & 0xFFL) << 8)
          | ((data[ri + 2] & 0xFFL) << 16)
          | ((data[ri + 3] & 0xFFL) << 24)
          | ((data[ri + 4] & 0xFFL) << 32)
          | ((data[ri + 5] & 0xFFL) << 40)
          | ((data[ri + 6] & 0xFFL) << 48)
          | ((long) data[ri + 7] << 56);
    }
    readerIndex = ri + 8;
    return v;
  }

  @Override
  public byte[] getBytes(int length) {
    ensureReadable(length);
    byte[] dst = new byte[length];
    System.arraycopy(data, (int) readerIndex, dst, 0, length);
    readerIndex += length;
    return dst;
  }

  @Override
  public byte[] backingArray() {
    return data;
  }

  @Override
  public void close() {
    data = new byte[0];
  }
}
