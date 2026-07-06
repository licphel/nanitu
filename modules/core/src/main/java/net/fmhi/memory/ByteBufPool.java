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

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A thread-safe pool of {@link Buf} instances that reduces allocation overhead.
 *
 * <p>Acquire a buffer with {@link #acquire()}, use it, and return it with
 * {@link #release(Buf)}. If no buffers are available, a new one is allocated transparently.
 *
 * <p>All buffers created by this pool share the same {@link Endianness} and capacity.
 *
 * <p>This class is thread-safe.
 */
public final class ByteBufPool {
  private final Endianness endianness;
  private final int capacity;
  private final Queue<Buf> pool;

  /**
   * Creates a new buffer pool.
   *
   * @param endianness the byte order for all buffers from this pool
   * @param capacity   the fixed capacity of each buffer in bytes
   */
  public ByteBufPool(Endianness endianness, int capacity) {
    this.endianness = endianness;
    this.capacity = capacity;
    this.pool = new ArrayDeque<>();
  }

  /**
   * Acquires a buffer from the pool, or creates a new heap buffer if none are available.
   *
   * @return a cleared buffer
   */
  public synchronized Buf acquire() {
    Buf buf = pool.poll();
    if (buf == null) {
      buf = Buf.heap(endianness, capacity);
    } else {
      buf.clear();
    }
    return buf;
  }

  /**
   * Returns a buffer to the pool for future reuse.
   *
   * @param buffer the buffer to return
   */
  public synchronized void release(Buf buffer) {
    pool.offer(buffer);
  }

  /**
   * Returns the number of idle buffers available in the pool.
   *
   * @return the remaining pool size
   */
  public synchronized int available() {
    return pool.size();
  }
}
