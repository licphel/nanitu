/*
 * MIT License
 *
 * Copyright (c) 2026 Mellowhue
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

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A thread-safe pool of {@link Buffer} instances that reduces allocation overhead.
 *
 * <p>Instead of allocating and freeing buffers repeatedly, acquire one from the
 * pool with {@link #acquire()}, use it, and return it with {@link #release(Buffer)}. If no buffers are available in the
 * pool, a new one is allocated transparently.
 *
 * <p>All buffers created by this pool are expandable — they grow automatically
 * when more space is needed — and share the same {@link Endianness} and capacity.
 *
 * <p>This class is thread-safe. All public methods are synchronized.
 */
public final class BufferPool {
  private final MemoryAllocator allocator;
  private final Endianness endianness;
  private final long capacity;
  private final Queue<Buffer> pool;

  /**
   * Creates a new buffer pool.
   *
   * @param allocator  the memory allocator used for new buffers
   * @param endianness the byte order for all buffers from this pool
   * @param capacity   the fixed capacity of each buffer in bytes, must be &gt; 0
   */
  public BufferPool(MemoryAllocator allocator, Endianness endianness, long capacity) {
    this.allocator = allocator;
    this.endianness = endianness;
    this.capacity = capacity;
    this.pool = new ArrayDeque<>();
  }

  /**
   * Acquires a buffer from the pool, or creates a new one if no buffers are available.
   *
   * <p>The returned buffer has its cursors reset ({@code readerIndex = 0},
   * {@code writerIndex = 0}) and is ready for immediate use. Its capacity is the fixed size specified at pool
   * creation.
   *
   * @return a cleared buffer of exactly {@link #capacity} bytes
   */
  public synchronized Buffer acquire() {
    Buffer buf = pool.poll();
    if (buf == null) {
      buf = new Buffer(allocator.allocate(capacity), endianness);
    } else {
      buf.clear();
    }
    return buf;
  }

  /**
   * Returns a buffer to the pool for future reuse.
   *
   * <p>The buffer must not be used after this call. There is no guarantee
   * that the buffer's contents are cleared before the next {@link #acquire()}.
   *
   * @param buffer the buffer to return to the pool
   */
  public synchronized void release(Buffer buffer) {
    pool.offer(buffer);
  }

  /**
   * Returns the number of buffers currently available in the pool.
   *
   * <p>A return value of zero means the next {@link #acquire()} will
   * allocate a new buffer rather than reusing an existing one.
   *
   * @return the count of idle buffers ready for acquisition
   */
  public synchronized int available() {
    return pool.size();
  }
}
