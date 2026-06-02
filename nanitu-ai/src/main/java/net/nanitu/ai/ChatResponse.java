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
 * The result of a chat completion request.
 *
 * <p>Contains the assistant's response text along with metadata about
 * token usage and the reason generation stopped.
 *
 * @param content          the generated response text
 * @param finishReason     why generation stopped ({@code "stop"}, {@code "length"},
 *                         {@code "content_filter"}, etc.), or {@code null} if unknown
 * @param promptTokens     number of tokens consumed by the prompt
 * @param completionTokens number of tokens generated in the response
 */
public record ChatResponse(String content, @Nullable String finishReason, int promptTokens, int completionTokens) {
  /**
   * Creates a ChatResponse, validating response fields.
   *
   * @param content          the generated response text
   * @param finishReason     why generation stopped ({@code "stop"}, {@code "length"},
   *                         {@code "content_filter"}, etc.), or {@code null} if unknown
   * @param promptTokens     number of tokens consumed by the prompt
   * @param completionTokens number of tokens generated in the response
   * @throws IllegalArgumentException if {@code content} is {@code null}
   *                                  or token counts are negative
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
   * Returns the total number of tokens consumed by this request and response.
   *
   * @return the sum of prompt and completion tokens
   */
  public int totalTokens() {
    return promptTokens + completionTokens;
  }
}
