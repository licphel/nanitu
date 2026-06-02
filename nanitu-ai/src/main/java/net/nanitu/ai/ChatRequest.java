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

import java.util.List;

/**
 * Parameters for a chat completion request.
 *
 * <p>Encapsulates all configurable options for a call to
 * {@link Model#chat(ChatRequest)} or {@link Model#chatStream(ChatRequest, StreamHandler)}.
 *
 * <p>Use the convenience constructors for common cases:
 * <pre>{@code
 * ChatRequest req = new ChatRequest(List.of(ChatMessage.user("Hello")));
 * ChatRequest req = new ChatRequest(List.of(ChatMessage.user("Hello")), "gpt-4");
 * }</pre>
 *
 * @param messages    the conversation history and current prompt
 * @param model       model name to use, or {@code null} to use the implementation default
 * @param temperature sampling temperature (0.0–2.0), defaults to 0.7
 * @param maxTokens   maximum number of tokens to generate, defaults to 1024
 * @param topP        nucleus sampling parameter (0.0–1.0), defaults to 1.0
 */
public record ChatRequest(List<ChatMessage> messages, @Nullable String model, double temperature, int maxTokens,
                          double topP) {
  /**
   * Default temperature value.
   */
  public static final double DEFAULT_TEMPERATURE = 0.7;

  /**
   * Default maximum tokens to generate.
   */
  public static final int DEFAULT_MAX_TOKENS = 1024;

  /**
   * Default top-p value (no truncation).
   */
  public static final double DEFAULT_TOP_P = 1.0;

  /**
   * Creates a ChatRequest, validating request parameters.
   *
   * @param messages    the conversation history and current prompt
   * @param model       model name to use, or {@code null} to use the implementation default
   * @param temperature sampling temperature (0.0–2.0), defaults to 0.7
   * @param maxTokens   maximum number of tokens to generate, defaults to 1024
   * @param topP        nucleus sampling parameter (0.0–1.0), defaults to 1.0
   * @throws IllegalArgumentException if {@code messages} is empty,
   *                                  {@code temperature} is out of range,
   *                                  {@code maxTokens} is not positive,
   *                                  or {@code topP} is out of range
   */
  public ChatRequest {
    if (messages.isEmpty()) {
      throw new IllegalArgumentException("messages must not be empty");
    }
    if (temperature < 0.0 || temperature > 2.0) {
      throw new IllegalArgumentException("temperature must be between 0.0 and 2.0");
    }
    if (maxTokens <= 0) {
      throw new IllegalArgumentException("maxTokens must be positive");
    }
    if (topP < 0.0 || topP > 1.0) {
      throw new IllegalArgumentException("topP must be between 0.0 and 1.0");
    }
  }

  /**
   * Creates a request with default parameters and no model override.
   *
   * @param messages the conversation messages
   */
  public ChatRequest(List<ChatMessage> messages) {
    this(messages, null, DEFAULT_TEMPERATURE, DEFAULT_MAX_TOKENS, DEFAULT_TOP_P);
  }

  /**
   * Creates a request with default parameters.
   *
   * @param messages the conversation messages
   * @param model    the model name, or {@code null} for implementation default
   */
  public ChatRequest(List<ChatMessage> messages, @Nullable String model) {
    this(messages, model, DEFAULT_TEMPERATURE, DEFAULT_MAX_TOKENS, DEFAULT_TOP_P);
  }

  /**
   * Creates a request with explicit parameters and default {@code topP}.
   *
   * @param messages    the conversation messages
   * @param model       the model name, or {@code null} for implementation default
   * @param temperature sampling temperature
   * @param maxTokens   maximum tokens to generate
   */
  public ChatRequest(List<ChatMessage> messages, @Nullable String model, double temperature, int maxTokens) {
    this(messages, model, temperature, maxTokens, DEFAULT_TOP_P);
  }
}
