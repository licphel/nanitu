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

package net.fmhi.memory;

import org.jspecify.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * A contiguous region of memory with explicit lifetime management. It offers low-level control of a memory block with
 * no extra assertions.
 */
public final class Memory implements AutoCloseable {
  private final MemorySegment segment;
  private final @Nullable Arena owningArena;
  private boolean disposed;

  /**
   * Creates an owning {@code Memory} that will close {@code arena} on {@link #close()}.
   *
   * @param segment     the backing segment
   * @param owningArena the arena to close when disposed
   */
  Memory(MemorySegment segment, Arena owningArena) {
    this.segment = segment;
    this.owningArena = owningArena;
    disposed = false;
  }

  /**
   * Creates a non-owning slice view over an existing segment. Disposing this instance has no effect on the underlying
   * memory.
   *
   * @param segment the sub-segment to view
   */
  Memory(MemorySegment segment) {
    this.segment = segment;
    owningArena = null;
    disposed = false;
  }

  /**
   * Creates a non-owning slice view over a byte array.
   *
   * @param heapBytes the array to view
   */
  public Memory(byte[] heapBytes) {
    this(MemorySegment.ofArray(heapBytes));
  }

  /**
   * Returns the size of this memory region in bytes.
   *
   * @return byte length of this region
   */
  public long size() {
    return segment.byteSize();
  }

  /**
   * Returns whether the memory is native memory.
   *
   * @return if the memory is native
   */
  public boolean isNative() {
    return segment.isNative();
  }

  /**
   * Returns the raw backing {@link MemorySegment}. Prefer the typed accessor methods ({@link #getByte},
   * {@link #putByte}, etc.) over direct segment access when possible.
   *
   * @return the backing segment
   * @throws IllegalStateException if this memory has been disposed
   */
  public MemorySegment segment() {
    return segment;
  }

  /**
   * Returns a non-owning view of the sub-region {@code [offset, offset + length)}.
   *
   * <p>The returned {@code Memory} does not extend the lifetime of this region; the caller is
   * responsible for ensuring this root {@code Memory} is not disposed while any slice is in use.
   *
   * @param offset byte offset from the start of this region (inclusive)
   * @param length byte length of the sub-region
   * @return a view over the specified sub-region
   * @throws IllegalStateException     if this memory has been disposed
   * @throws IndexOutOfBoundsException if the requested range falls outside this region
   */
  public Memory slice(long offset, long length) {
    return new Memory(segment.asSlice(offset, length));
  }

  /**
   * Returns a non-owning view starting at {@code offset} and extending to the end of this region.
   *
   * @param offset byte offset from the start of this region
   * @return a view from {@code offset} to the end
   * @throws IllegalStateException     if this memory has been disposed
   * @throws IndexOutOfBoundsException if {@code offset} is out of range
   */
  public Memory slice(long offset) {
    return slice(offset, size() - offset);
  }

  /**
   * Reads a single byte at the given offset.
   *
   * @param offset byte offset within this region
   * @return the byte value at {@code offset}
   * @throws IllegalStateException     if this memory has been disposed
   * @throws IndexOutOfBoundsException if {@code offset} is out of range
   */
  public byte getByte(long offset) {
    return segment.get(ValueLayout.JAVA_BYTE, offset);
  }

  /**
   * Writes a single byte at the given offset.
   *
   * @param offset byte offset within this region
   * @param value  the byte value to write
   * @throws IllegalStateException     if this memory has been disposed
   * @throws IndexOutOfBoundsException if {@code offset} is out of range
   */
  public void putByte(long offset, byte value) {
    segment.set(ValueLayout.JAVA_BYTE, offset, value);
  }

  /**
   * Reads a {@code short} (2 bytes) at the given offset using the specified byte order.
   *
   * @param offset     byte offset within this region
   * @param endianness byte order for the read
   * @return the short value at {@code offset}
   * @throws IllegalStateException     if this memory has been disposed
   * @throws IndexOutOfBoundsException if the range [{@code offset}, {@code offset+2}) is out of bounds
   * @throws IllegalArgumentException  if {@code endianness} is {@link Endianness#UNSURE}
   */
  public short getShort(long offset, Endianness endianness) {
    checkEndiannessUnsure(endianness);

    short value = segment.get(ValueLayout.JAVA_SHORT_UNALIGNED, offset);
    if (endianness != Endianness.NATIVE) {
      value = Short.reverseBytes(value);
    }
    return value;
  }

  /**
   * Writes a {@code short} (2 bytes) at the given offset using the specified byte order.
   *
   * @param offset     byte offset within this region
   * @param value      the short value to write
   * @param endianness byte order for writing
   * @throws IllegalStateException     if this memory has been disposed
   * @throws IndexOutOfBoundsException if the range is out of bounds
   * @throws IllegalArgumentException  if {@code endianness} is {@link Endianness#UNSURE}
   */
  public void putShort(long offset, short value, Endianness endianness) {
    checkEndiannessUnsure(endianness);

    if (endianness != Endianness.NATIVE) {
      value = Short.reverseBytes(value);
    }
    segment.set(ValueLayout.JAVA_SHORT_UNALIGNED, offset, value);
  }

  /**
   * Reads an {@code int} (4 bytes) at the given offset using the specified byte order.
   *
   * @param offset     byte offset within this region
   * @param endianness byte order for the read
   * @return the int value at {@code offset}
   * @throws IllegalStateException     if this memory has been disposed
   * @throws IndexOutOfBoundsException if the range is out of bounds
   * @throws IllegalArgumentException  if {@code endianness} is {@link Endianness#UNSURE}
   */
  public int getInt(long offset, Endianness endianness) {
    checkEndiannessUnsure(endianness);

    int value = segment.get(ValueLayout.JAVA_INT_UNALIGNED, offset);
    if (endianness != Endianness.NATIVE) {
      value = Integer.reverseBytes(value);
    }
    return value;
  }

  /**
   * Writes an {@code int} (4 bytes) at the given offset using the specified byte order.
   *
   * @param offset     byte offset within this region
   * @param value      the int value to write
   * @param endianness byte order for writing
   * @throws IllegalStateException     if this memory has been disposed
   * @throws IndexOutOfBoundsException if the range is out of bounds
   * @throws IllegalArgumentException  if {@code endianness} is {@link Endianness#UNSURE}
   */
  public void putInt(long offset, int value, Endianness endianness) {
    checkEndiannessUnsure(endianness);

    if (endianness != Endianness.NATIVE) {
      value = Integer.reverseBytes(value);
    }
    segment.set(ValueLayout.JAVA_INT_UNALIGNED, offset, value);
  }

  /**
   * Reads a {@code long} (8 bytes) at the given offset using the specified byte order.
   *
   * @param offset     byte offset within this region
   * @param endianness byte order for the read
   * @return the long value at {@code offset}
   * @throws IllegalStateException     if this memory has been disposed
   * @throws IndexOutOfBoundsException if the range is out of bounds
   * @throws IllegalArgumentException  if {@code endianness} is {@link Endianness#UNSURE}
   */
  public long getLong(long offset, Endianness endianness) {
    checkEndiannessUnsure(endianness);

    long value = segment.get(ValueLayout.JAVA_LONG_UNALIGNED, offset);
    if (endianness != Endianness.NATIVE) {
      value = Long.reverseBytes(value);
    }
    return value;
  }

  /**
   * Writes a {@code long} (8 bytes) at the given offset using the specified byte order.
   *
   * @param offset     byte offset within this region
   * @param value      the long value to write
   * @param endianness byte order for writing
   * @throws IllegalStateException     if this memory has been disposed
   * @throws IndexOutOfBoundsException if the range is out of bounds
   * @throws IllegalArgumentException  if {@code endianness} is {@link Endianness#UNSURE}
   */
  public void putLong(long offset, long value, Endianness endianness) {
    checkEndiannessUnsure(endianness);

    if (endianness != Endianness.NATIVE) {
      value = Long.reverseBytes(value);
    }
    segment.set(ValueLayout.JAVA_LONG_UNALIGNED, offset, value);
  }

  /**
   * Reads a {@code float} (4 bytes) at the given offset using the specified byte order.
   *
   * @param offset     byte offset within this region
   * @param endianness byte order for the read
   * @return the float value at {@code offset}
   * @throws IllegalStateException     if this memory has been disposed
   * @throws IndexOutOfBoundsException if the range is out of bounds
   * @throws IllegalArgumentException  if {@code endianness} is {@link Endianness#UNSURE}
   */
  public float getFloat(long offset, Endianness endianness) {
    return Float.intBitsToFloat(getInt(offset, endianness));
  }

  /**
   * Writes a {@code float} (4 bytes) at the given offset using the specified byte order.
   *
   * @param offset     byte offset within this region
   * @param value      the float value to write
   * @param endianness byte order for writing
   * @throws IllegalStateException     if this memory has been disposed
   * @throws IndexOutOfBoundsException if the range is out of bounds
   * @throws IllegalArgumentException  if {@code endianness} is {@link Endianness#UNSURE}
   */
  public void putFloat(long offset, float value, Endianness endianness) {
    putInt(offset, Float.floatToIntBits(value), endianness);
  }

  /**
   * Reads a {@code double} (8 bytes) at the given offset using the specified byte order.
   *
   * @param offset     byte offset within this region
   * @param endianness byte order for the read
   * @return the double value at {@code offset}
   * @throws IllegalStateException     if this memory has been disposed
   * @throws IndexOutOfBoundsException if the range is out of bounds
   * @throws IllegalArgumentException  if {@code endianness} is {@link Endianness#UNSURE}
   */
  public double getDouble(long offset, Endianness endianness) {
    return Double.longBitsToDouble(getLong(offset, endianness));
  }

  /**
   * Writes a {@code double} (8 bytes) at the given offset using the specified byte order.
   *
   * @param offset     byte offset within this region
   * @param value      the double value to write
   * @param endianness byte order for writing
   * @throws IllegalStateException     if this memory has been disposed
   * @throws IndexOutOfBoundsException if the range is out of bounds
   * @throws IllegalArgumentException  if {@code endianness} is {@link Endianness#UNSURE}
   */
  public void putDouble(long offset, double value, Endianness endianness) {
    putLong(offset, Double.doubleToLongBits(value), endianness);
  }

  /**
   * Copies {@code length} bytes from this region starting at {@code srcOffset} into {@code dst} starting at
   * {@code dstOffset}.
   *
   * @param srcOffset byte offset in this region to start copying from
   * @param dst       destination memory region
   * @param dstOffset byte offset in {@code dst} to start writing to
   * @param length    number of bytes to copy
   * @throws IllegalStateException     if either region has been disposed
   * @throws IndexOutOfBoundsException if ranges are out of bounds
   */
  public void copyTo(long srcOffset, Memory dst, long dstOffset, long length) {
    MemorySegment.copy(segment, srcOffset, dst.segment, dstOffset, length);
  }

  /**
   * Fills every byte in this region with the given value.
   *
   * @param value the byte value to fill with
   * @throws IllegalStateException if this memory has been disposed
   */
  public void fill(byte value) {
    segment.fill(value);
  }

  /**
   * Zeroes out every byte in this region.
   *
   * @throws IllegalStateException if this memory has been disposed
   */
  public void zero() {
    fill((byte) 0);
  }

  @Override
  public void close() {
    if (disposed) {
      return;
    }
    disposed = true;

    try {
      if (owningArena != null) {
        owningArena.close();
      }

      segment.unload();
    } catch (Exception e) {
      // Ignore: we do not care whether we failed to free.
      // That actually means we needn't free it.
    }
  }

  private void checkEndiannessUnsure(Endianness endianness) {
    if (endianness == Endianness.UNSURE) {
      throw new IllegalArgumentException("Endianness must not be UNSURE");
    }
  }
}