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

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a multi-turn conversation with a {@link Model}.
 *
 * <p>A context automatically tracks the conversation history — each call to
 * {@link #chat(String)} appends the user message, sends the full history
 * to the model, and records the assistant's response. The next call includes
 * all previous messages, maintaining conversational continuity.
 *
 * <pre>{@code
 * Context ctx = model.createContext();
 * ctx.setSystemMessage("You are a helpful assistant.");
 * ctx.chat("What is the capital of France?");
 * ctx.chat("What about Germany?"); // model remembers the previous Q&A
 * }</pre>
 *
 * <p>Instances are created via {@link Model#createContext()} and are bound
 * to that model for their lifetime. Use {@link #clear()} to reset the
 * conversation while keeping the same model binding.
 *
 * <p>This class is thread-safe.
 */
public final class Context {
  private final Model model;
  private final List<ChatMessage> history;
  private @Nullable ChatMessage systemMessage;

  /**
   * Creates a new context backed by the given model.
   *
   * @param model the model to use for completions
   */
  public Context(Model model) {
    this.model = model;
    this.history = new ArrayList<>();
  }

  /**
   * Sets the system message for this conversation.
   *
   * <p>The system message is prepended to the message history on every
   * request and establishes the assistant's behavior and constraints.
   * Pass {@code null} to remove the system message.
   *
   * @param message the system instruction, or {@code null} to remove
   */
  public void setSystemMessage(@Nullable String message) {
    this.systemMessage = message != null ? ChatMessage.system(message) : null;
  }

  /**
   * Sends a user message and returns the model's response.
   *
   * <p>The user message and the assistant's response are appended to the
   * conversation history automatically.
   *
   * @param userMessage the user's input
   * @return the model's response
   * @throws CommunicationException if the request fails
   */
  public ChatResponse chat(String userMessage) {
    history.add(ChatMessage.user(userMessage));
    ChatRequest request = buildRequest();
    ChatResponse response = model.chat(request);
    history.add(ChatMessage.assistant(response.content()));
    return response;
  }

  /**
   * Sends a user message and streams the model's response.
   *
   * <p>The user message and the assistant's full response are appended
   * to the conversation history after the stream completes.
   *
   * @param userMessage the user's input
   * @param handler     callbacks for streaming tokens
   * @throws CommunicationException if the request fails
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
   * Returns an unmodifiable view of the conversation history.
   *
   * <p>The returned list includes user and assistant messages in
   * chronological order. The system message is not included — use
   * {@link #systemMessage()} to retrieve it separately.
   *
   * @return the conversation history
   */
  public List<ChatMessage> history() {
    return List.copyOf(history);
  }

  /**
   * Returns the system message, or {@code null} if none is set.
   *
   * @return the system message content, or {@code null}
   */
  @Nullable
  public String systemMessage() {
    return systemMessage != null ? systemMessage.content() : null;
  }

  /**
   * Clears the conversation history while preserving the model binding.
   *
   * <p>The system message is not affected.
   *
   * @return this context, for method chaining
   */
  public Context clear() {
    history.clear();
    return this;
  }

  /**
   * Builds a request from the current history, optionally overriding the model.
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
