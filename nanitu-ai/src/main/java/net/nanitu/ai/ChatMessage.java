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
 * A single message in a chat conversation, associating a speaker role with text content.
 *
 * <p>Messages are the fundamental unit of a chat completion prompt. The role identifies
 * the speaker: {@code "system"} for instructions that shape model behavior, {@code "user"} for human input, and
 * {@code "assistant"} for model output from earlier turns. Use the static factory methods to create properly-typed
 * instances.
 *
 * @param role    the speaker role, conventionally {@code "system"}, {@code "user"}, or {@code "assistant"}
 * @param content the message text
 */
public record ChatMessage(String role, String content) {
  /**
   * Creates a {@code ChatMessage} instance.
   *
   * @param role    the speaker role, must not be blank
   * @param content the message text
   * @throws IllegalArgumentException if {@code role} is blank
   */
  public ChatMessage {
    if (role.isBlank()) {
      throw new IllegalArgumentException("role must not be blank");
    }
  }

  /**
   * Creates a message with the {@code "system"} role.
   *
   * <p>System messages convey instructions about the desired behavior, tone,
   * and constraints the model should follow throughout the conversation.
   *
   * @param content the system instruction text
   * @return a new system message
   */
  public static ChatMessage system(String content) {
    return new ChatMessage("system", content);
  }

  /**
   * Creates a message with the {@code "user"} role.
   *
   * <p>User messages carry the human participant's input for the current turn.
   *
   * @param content the user's input text
   * @return a new user message
   */
  public static ChatMessage user(String content) {
    return new ChatMessage("user", content);
  }

  /**
   * Creates a message with the {@code "assistant"} role.
   *
   * <p>Assistant messages record previous model output, allowing multi-turn
   * exchanges to include conversational context from earlier interactions.
   *
   * @param content the assistant's response text
   * @return a new assistant message
   */
  public static ChatMessage assistant(String content) {
    return new ChatMessage("assistant", content);
  }
}
