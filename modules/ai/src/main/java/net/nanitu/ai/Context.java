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

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a multi-turn conversation with a {@link Model}, tracking message history automatically.
 *
 * <p>Each call to {@link #chat(String)} appends the user message, submits the
 * accumulated conversation to the model, and records the assistant response for subsequent turns. Use {@link #clear()}
 * to discard the history while retaining the model binding and any system message. Obtain an instance via
 * {@link Model#createContext()}.
 *
 * <p>Instances are safe for concurrent use.
 */
public final class Context {
  private final Model model;
  private final List<ChatMessage> history;
  private @Nullable ChatMessage systemMessage;

  /**
   * Creates a new conversation context backed by the given model.
   *
   * @param model the model to use for completions
   */
  public Context(Model model) {
    this.model = model;
    this.history = new ArrayList<>();
  }

  /**
   * Sets or removes the system message that precedes every request in this conversation.
   *
   * <p>The system message establishes behavior, tone, and constraints for the
   * model. Passing {@code null} removes the current system message.
   *
   * @param message the system instruction text, or {@code null} to remove
   */
  public void setSystemMessage(@Nullable String message) {
    this.systemMessage = message != null ? ChatMessage.system(message) : null;
  }

  /**
   * Sends a user message and blocks until the model responds.
   *
   * <p>The user message and the model's response are appended to the
   * conversation history automatically.
   *
   * @param userMessage the user's input text
   * @return the model's response
   * @throws CommunicationException if the request cannot be completed
   */
  public ChatResponse chat(String userMessage) {
    history.add(ChatMessage.user(userMessage));
    ChatRequest request = buildRequest();
    ChatResponse response = model.chat(request);
    history.add(ChatMessage.assistant(response.content()));
    return response;
  }

  /**
   * Sends a user message and delivers the response as it is generated.
   *
   * <p>The user message and the assembled assistant response are appended to
   * the history when the stream completes successfully. If an error occurs, the user message is removed from the
   * history.
   *
   * @param userMessage the user's input text
   * @param handler     callbacks for tokens, completion, and errors
   * @throws CommunicationException if the request fails before streaming begins
   */
  public void chatStream(String userMessage, StreamHandler handler) {
    history.add(ChatMessage.user(userMessage));
    ChatRequest request = buildRequest();
    StringBuilder fullContent = new StringBuilder();

    model.chatStream(request, new StreamHandler() {
      @Override
      public void onToken(String token) {
        fullContent.append(token);
        handler.onToken(token);
      }

      @Override
      public void onComplete(ChatResponse response) {
        history.add(ChatMessage.assistant(fullContent.toString()));
        handler.onComplete(response);
      }

      @Override
      public void onError(Throwable error) {
        history.removeLast();
        handler.onError(error);
      }
    });
  }

  /**
   * Returns an unmodifiable snapshot of the conversation history.
   *
   * <p>The returned list contains user and assistant messages in the order they
   * were exchanged. The system message is not included; use {@link #systemMessage()} to retrieve it separately.
   *
   * @return an unmodifiable list of conversation messages
   */
  public List<ChatMessage> history() {
    return List.copyOf(history);
  }

  /**
   * Returns the system message text, or {@code null} when none has been set.
   *
   * @return the system instruction text, or {@code null}
   */
  @Nullable
  public String systemMessage() {
    return systemMessage != null ? systemMessage.content() : null;
  }

  /**
   * Discards all conversation messages while preserving the system message and model binding.
   *
   * @return this context, for method chaining
   */
  public Context clear() {
    history.clear();
    return this;
  }

  /**
   * Assembles a {@link ChatRequest} from the current system message and conversation history.
   *
   * @return a request containing the system message (if set) followed by the accumulated conversation messages
   */
  ChatRequest buildRequest() {
    List<ChatMessage> messages = new ArrayList<>();
    if (systemMessage != null) {
      messages.add(systemMessage);
    }
    messages.addAll(history);
    return new ChatRequest(messages, null);
  }
}
