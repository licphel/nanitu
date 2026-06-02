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

package net.nanitu.ai;

import org.jspecify.annotations.Nullable;

/**
 * A callable tool that a model may invoke during a response.
 *
 * <p>Tools extend the model's capabilities by providing access to external
 * functions, APIs, or computations. When a model decides to use a tool,
 * the tool's {@link #invoke} method is called with the arguments supplied
 * by the model.
 *
 * <p>Implementations must be thread-safe if they may be called concurrently.
 */
@FunctionalInterface
public interface Tool {
  /**
   * Invokes this tool with the given arguments.
   *
   * @param args the arguments supplied by the model, or {@code null}
   * @return the result of the invocation, or {@code null}
   */
  @Nullable Object invoke(@Nullable Object[] args);
}
