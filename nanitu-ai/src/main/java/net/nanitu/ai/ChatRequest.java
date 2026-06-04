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
 * Parameters for a chat completion submitted to a {@link Model}.
 *
 * <p>Bundles the conversation messages, an optional model override, and
 * generation hyperparameters. Convenience constructors supply sensible defaults for temperature, maximum tokens, and
 * top-p when they are not given explicitly.
 *
 * @param messages    the ordered messages that compose the prompt
 * @param model       the model name, or {@code null} to let the implementation decide
 * @param temperature the sampling temperature, clamped to {@code [0.0, 2.0]}
 * @param maxTokens   the maximum number of tokens the model may generate
 * @param topP        the nucleus sampling cutoff, clamped to {@code [0.0, 1.0]}
 */
public record ChatRequest(List<ChatMessage> messages, @Nullable String model, double temperature, int maxTokens,
                          double topP) {
  /** The default temperature ({@code 0.7}). */
  public static final double DEFAULT_TEMPERATURE = 0.7;
  /** The default maximum number of tokens to generate ({@code 1024}). */
  public static final int DEFAULT_MAX_TOKENS = 1024;
  /** The default top-p value ({@code 1.0}), which disables nucleus truncation. */
  public static final double DEFAULT_TOP_P = 1.0;

  /**
   * Creates a {@code ChatRequest} instance.
   *
   * @param messages    the ordered messages that compose the prompt
   * @param model       the model name, or {@code null} to let the implementation decide
   * @param temperature the sampling temperature, must be in {@code [0.0, 2.0]}
   * @param maxTokens   the maximum number of tokens to generate, must be positive
   * @param topP        the nucleus sampling cutoff, must be in {@code [0.0, 1.0]}
   * @throws IllegalArgumentException if {@code messages} is empty, {@code temperature} is outside {@code [0.0, 2.0]},
   *                                  {@code maxTokens} is not positive, or {@code topP} is outside {@code [0.0, 1.0]}
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
   * Creates a request using all default generation parameters and no model override.
   *
   * @param messages the ordered messages that compose the prompt
   */
  public ChatRequest(List<ChatMessage> messages) {
    this(messages, null, DEFAULT_TEMPERATURE, DEFAULT_MAX_TOKENS, DEFAULT_TOP_P);
  }

  /**
   * Creates a request with default generation parameters and an explicit model selection.
   *
   * @param messages the ordered messages that compose the prompt
   * @param model    the model name, or {@code null} to let the implementation decide
   */
  public ChatRequest(List<ChatMessage> messages, @Nullable String model) {
    this(messages, model, DEFAULT_TEMPERATURE, DEFAULT_MAX_TOKENS, DEFAULT_TOP_P);
  }

  /**
   * Creates a request with explicit temperature and maximum tokens while using the default top-p value.
   *
   * @param messages    the ordered messages that compose the prompt
   * @param model       the model name, or {@code null} to let the implementation decide
   * @param temperature the sampling temperature, must be in {@code [0.0, 2.0]}
   * @param maxTokens   the maximum number of tokens to generate, must be positive
   */
  public ChatRequest(List<ChatMessage> messages, @Nullable String model, double temperature, int maxTokens) {
    this(messages, model, temperature, maxTokens, DEFAULT_TOP_P);
  }
}
