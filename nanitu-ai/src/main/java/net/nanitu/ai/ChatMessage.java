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

/**
 * A single message in a chat conversation.
 *
 * <p>Each message has a {@link #role} identifying the speaker and a
 * {@link #content} carrying the message body. Use the static factory
 * methods for the most common roles.
 *
 * <pre>{@code
 * ChatMessage sys = ChatMessage.system("You are a helpful assistant.");
 * ChatMessage usr = ChatMessage.user("What is AI?");
 * ChatMessage ast = ChatMessage.assistant("AI stands for...");
 * }</pre>
 *
 * @param role    the speaker role ({@code "system"}, {@code "user"}, or {@code "assistant"})
 * @param content the message body
 */
public record ChatMessage(String role, String content) {
  /**
   * Creates a ChatMessage, validating that {@code role} is not blank and {@code content} is not
   * {@code null}.
   *
   * @param role    the speaker role ({@code "system"}, {@code "user"}, or {@code "assistant"})
   * @param content the message body
   * @throws IllegalArgumentException if {@code role} is blank or {@code content} is {@code null}
   */
  public ChatMessage {
    if (role.isBlank()) {
      throw new IllegalArgumentException("role must not be blank");
    }
  }

  /**
   * Creates a system message.
   *
   * <p>System messages set the behavior and context for the assistant.
   *
   * @param content the system instruction
   * @return a message with role {@code "system"}
   */
  public static ChatMessage system(String content) {
    return new ChatMessage("system", content);
  }

  /**
   * Creates a user message.
   *
   * @param content the user's input
   * @return a message with role {@code "user"}
   */
  public static ChatMessage user(String content) {
    return new ChatMessage("user", content);
  }

  /**
   * Creates an assistant message.
   *
   * <p>Useful for priming the conversation with example responses or
   * for recording the assistant's previous output in a multi-turn context.
   *
   * @param content the assistant's response text
   * @return a message with role {@code "assistant"}
   */
  public static ChatMessage assistant(String content) {
    return new ChatMessage("assistant", content);
  }
}
