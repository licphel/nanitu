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

package net.nanitu.natives.opengl;

import net.nanitu.graphics.BufferFrequency;
import net.nanitu.graphics.BufferObject;
import net.nanitu.graphics.BufferObjectDesc;
import net.nanitu.util.InternalApi;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * OpenGL buffer object implementation supporting vertex, index, and uniform
 * buffer usage.
 *
 * <p>Every buffer has a target ({@code GL_ARRAY_BUFFER}, {@code GL_ELEMENT_ARRAY_BUFFER},
 * or {@code GL_UNIFORM_BUFFER}) and a usage hint derived from its
 * {@link BufferFrequency}. All GL calls are enqueued via
 * {@link OpenGLDevice#submit(Runnable)} for execution on the render thread.
 *
 * <p><b>Buffer orphaning:</b> for {@link BufferFrequency#STREAM} buffers,
 * a {@link #submit(byte[], int)} call at offset 0 discards the previous
 * allocation (via an extra {@code glBufferData}) before writing. This
 * technique avoids GPU pipeline stalls by letting the driver rotate through
 * fresh backing storage each frame. The trade-off is an extra allocation
 * per frame.
 *
 * <p><b>Auto-expansion:</b> {@link #canExpand()} always returns {@code true}.
 * If a {@link #submit} would overflow the current capacity, the buffer is
 * transparently reallocated to at least double the previous size.
 *
 * <p><b>Thread safety:</b> creation, mutation ({@link #allocate}, {@link #submit}),
 * and {@link #close()} submit work to the render thread and are safe to call
 * from any thread. Reads ({@link #desc()}, {@link #capacity()}) return
 * values that may be stale if a pending submission has not yet executed.
 */
@InternalApi
final class OpenGLBufferObject implements BufferObject {
  private final OpenGLDevice ctx;
  private final BufferObjectDesc desc;
  private final int target;
  private final int hint;

  /**
   * The GL buffer handle (0 until the render thread creates it).
   */
  int handle = 0;
  private int capacity = 0;

  /**
   * Creates a new OpenGL buffer.
   *
   * <p>The GL handle is allocated asynchronously on the render thread.
   *
   * @param ctx  the GL context that owns this buffer
   * @param desc the buffer type, frequency, and initial-size hint
   */
  OpenGLBufferObject(OpenGLDevice ctx, BufferObjectDesc desc) {
    this.ctx = ctx;
    this.desc = desc;
    target = OpenGLUtils.bufferTarget(desc.type());
    hint = OpenGLUtils.bufferUsage(desc.frequency(), false);

    ctx.submit(() -> {
      handle = glGenBuffers();
    });
  }

  @Override
  public BufferObjectDesc desc() {
    return desc;
  }

  @Override
  public int capacity() {
    return capacity;
  }

  @Override
  public boolean canExpand() {
    return true;
  }

  @Override
  public void allocate(int cap, byte @Nullable [] data) {
    ctx.submit(() -> {
      OpenGLCache cache = ctx.cache;
      cache.bindBuffer(target, handle);
      if (data != null && data.length == cap) {
        ByteBuffer bb = memAlloc(cap);
        try {
          bb.put(data).flip();
          glBufferData(target, bb, hint);
        } finally {
          memFree(bb);
        }
      } else {
        glBufferData(target, cap, hint);
      }
      capacity = cap;
    });
  }

  @Override
  public void submit(byte[] data, int offset) {
    ctx.submit(() -> {
      OpenGLCache cache = ctx.cache;
      int needed = offset + data.length;
      cache.bindBuffer(target, handle);

      if (needed > capacity) {
        int newCap = Math.max(needed, capacity * 2);
        glBufferData(target, newCap, hint);
        capacity = newCap;
      }

      if (desc.frequency() == BufferFrequency.STREAM && offset == 0) {
        // Buffer orphaning: discard the old allocation before writing.
        glBufferData(target, capacity, hint);
      }

      ByteBuffer bb = memAlloc(data.length);
      try {
        bb.put(data).flip();
        glBufferSubData(target, offset, bb);
      } finally {
        memFree(bb);
      }
    });
  }

  @Override
  public void close() {
    ctx.submit(() -> {
      if (handle != 0) {
        glDeleteBuffers(handle);
        handle = 0;
      }
    });
  }
}
