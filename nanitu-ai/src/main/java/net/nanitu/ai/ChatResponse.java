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
 * The outcome of a chat completion request.
 *
 * <p>Holds the text the model generated together with metadata about token usage
 * and the condition under which generation ended.
 *
 * @param content          the text produced by the model
 * @param finishReason     the reason generation halted (e.g. {@code "stop"}, {@code "length"},
 *                         {@code "content_filter"}), or {@code null} when not available
 * @param promptTokens     the number of tokens from the input prompt, must be non-negative
 * @param completionTokens the number of tokens generated, must be non-negative
 */
public record ChatResponse(String content, @Nullable String finishReason, int promptTokens, int completionTokens) {
  /**
   * Creates a {@code ChatResponse} instance.
   *
   * @param content          the text produced by the model
   * @param finishReason     the reason generation halted, or {@code null} when not available
   * @param promptTokens     the number of tokens from the input prompt
   * @param completionTokens the number of tokens generated
   * @throws IllegalArgumentException if {@code promptTokens} or {@code completionTokens} is negative
   */
  public ChatResponse {
    if (promptTokens < 0) {
      throw new IllegalArgumentException("promptTokens must not be negative");
    }
    if (completionTokens < 0) {
      throw new IllegalArgumentException("completionTokens must not be negative");
    }
  }

  /**
   * Returns the combined token count for the request and response.
   *
   * @return the sum of {@code promptTokens} and {@code completionTokens}
   */
  public int totalTokens() {
    return promptTokens + completionTokens;
  }
}
