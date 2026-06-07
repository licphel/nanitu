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

import org.jspecify.annotations.Nullable;

import java.lang.foreign.Arena;

/**
 * Strategy for allocating {@link Memory} blocks from a particular source.
 *
 * <p>Implementations decide where memory lives (JVM heap vs. native off-heap)
 * and how it is reclaimed. Two built-in allocators are provided as constants:
 *
 * <ul>
 *   <li>{@link #HEAP} — backed by {@code byte[]} arrays, garbage-collected</li>
 *   <li>{@link #NATIVE} — backed by off-heap memory via the
 *       {@link java.lang.foreign Foreign Memory API}, explicitly freed</li>
 * </ul>
 *
 * <p>Use a custom implementation to integrate with arena-based allocation,
 * memory-mapped files, or pooled native buffers.
 *
 * @see Memory
 * @see Buffer
 */
public interface MemoryAllocator {
  /**
   * Allocates memory on the JVM heap, backed by a {@code byte[]} array.
   *
   * <p>Heap memory is reclaimed automatically by the garbage collector.
   * This allocator is suitable for most use cases and is the safest default. The maximum size is limited to
   * {@link Integer#MAX_VALUE} bytes.
   */
  MemoryAllocator HEAP = size -> {
    if (size < 0 || size > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Heap size must be in [0, Integer.MAX_VALUE], got: " + size);
    }

    return new Memory(new byte[(int) size]);
  };

  /**
   * Allocates memory off-heap (native) via the Foreign Memory Access API.
   *
   * <p>Native memory is not managed by the garbage collector and must be
   * explicitly freed by calling {@link Memory#close()}. Each allocated block owns the global {@link Arena}.
   *
   * <p>Use this allocator for large buffers, memory-mapped file interop,
   * or when you need precise control over memory layout and lifetime.
   */
  MemoryAllocator NATIVE = size -> {
    if (size < 0 || size > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Native size must be in [0, Integer.MAX_VALUE], got: " + size);
    }

    Arena arena = Arena.global();

    try {
      return new Memory(arena.allocate(size), arena);
    } catch (OutOfMemoryError e) {
      arena.close();
      throw e;
    }
  };

  /**
   * Allocates a new memory block of the given size.
   *
   * <p>The returned block is uninitialized — its contents are whatever the
   * underlying allocation mechanism provides (typically zero for native memory, zero for fresh heap arrays).
   *
   * @param size the number of bytes to allocate
   * @return a new memory block of exactly {@code size} bytes
   */
  Memory allocate(long size);

  /**
   * Grows or shrinks an existing memory block, preserving as much data as possible.
   *
   * <p>The default implementation allocates a new block of {@code newSize} bytes,
   * copies {@code min(oldSize, newSize)} bytes from the old block to the new one, closes the old block, and returns the
   * new block. If {@code oldMemory} is {@code null}, a fresh allocation is performed.
   *
   * @param oldMemory the existing memory block, or {@code null} for fresh allocation
   * @param newSize   the desired size in bytes
   * @return a memory block of exactly {@code newSize} bytes, with old data preserved
   * @throws IllegalArgumentException if {@code newSize} is negative
   */
  default Memory reallocate(@Nullable Memory oldMemory, long newSize) {
    if (newSize < 0) {
      throw new IllegalArgumentException("Size must be >= 0, got: " + newSize);
    }

    Memory newMemory = allocate(newSize);

    if (oldMemory != null) {
      long copySize = Math.min(oldMemory.size(), newSize);
      if (copySize > 0) {
        oldMemory.copyTo(0, newMemory, 0, copySize);
      }
      oldMemory.close();
    }

    return newMemory;
  }
}
