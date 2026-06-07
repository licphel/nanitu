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

package net.nanitu.ai;

import org.jspecify.annotations.Nullable;

/**
 * A callable function that a model may choose to invoke during a chat completion.
 *
 * <p>Tools give models access to external computation, data retrieval, or
 * side effects. When the model requests a tool call, the framework invokes {@link #invoke(Object[])} with the arguments
 * the model supplied.
 *
 * <p>Implementations must be safe for concurrent use if multiple invocations
 * may overlap.
 */
@FunctionalInterface
public interface Tool {
  /**
   * Executes this tool with the arguments provided by the model.
   *
   * @param args the arguments the model supplied, or {@code null}
   * @return the result of the invocation, or {@code null}
   */
  @Nullable Object invoke(@Nullable Object[] args);
}
