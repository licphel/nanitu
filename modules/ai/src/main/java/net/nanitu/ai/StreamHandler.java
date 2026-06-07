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

/**
 * Callback interface for processing a streaming chat completion token by token.
 *
 * <p>An implementation is passed to
 * {@link Model#chatStream(ChatRequest, StreamHandler)}, where it receives each generated fragment as soon as it is
 * available instead of waiting for the full response.
 */
public interface StreamHandler {
  /**
   * Delivers a single token produced during a streaming completion.
   *
   * <p>May be invoked zero or more times per stream. Concatenating every token
   * in order yields the complete response text.
   *
   * @param token a text fragment from the model output
   */
  void onToken(String token);

  /**
   * Signals that the stream finished successfully and all tokens have been delivered.
   *
   * <p>The default implementation is a no-op.
   *
   * @param response the aggregated response containing the full text and token usage metadata
   */
  default void onComplete(ChatResponse response) {
  }

  /**
   * Signals that the stream terminated due to an error.
   *
   * <p>After this callback, no further calls to {@link #onToken(String)} or
   * {@link #onComplete(ChatResponse)} will occur. The default implementation is a no-op.
   *
   * @param error the error that caused the stream to fail
   */
  default void onError(Throwable error) {
  }
}
