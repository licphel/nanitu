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
 * An AI language model that produces chat completions from conversational input.
 *
 * <p>{@code Model} is the main entry point for interacting with an AI backend.
 * It abstracts over cloud APIs, local inference servers, and any compatible provider. Implementations are discovered
 * via {@link java.util.ServiceLoader} through the {@link net.nanitu.ai.spi.ModelProvider} service provider interface.
 *
 * <p>For a single completion use {@link #chat(ChatRequest)}. For delivery of
 * tokens as they are generated use {@link #chatStream(ChatRequest, StreamHandler)}. For multi-turn conversations call
 * {@link #createContext()} to obtain a {@link Context} that manages message history automatically.
 *
 * <p>Implementations must be safe for concurrent use.
 */
public interface Model extends AutoCloseable {
  /**
   * Returns the identifier of the underlying model as recognized by the backend.
   *
   * @return the model name, e.g. {@code "gpt-4"} or {@code "claude-3-opus-20240229"}
   */
  String name();

  /**
   * Sends a chat completion request and blocks until the full response is available.
   *
   * @param request the parameters for the completion, including messages and generation settings
   * @return the complete response with generated text and token usage
   * @throws CommunicationException if the backend cannot fulfill the request
   */
  ChatResponse chat(ChatRequest request);

  /**
   * Sends a streaming chat completion request.
   *
   * <p>Each generated token is delivered to
   * {@link StreamHandler#onToken(String)}. When the stream ends, {@link StreamHandler#onComplete(ChatResponse)} is
   * called; if it fails, {@link StreamHandler#onError(Throwable)} is called instead. This method blocks until the
   * stream finishes or an error occurs.
   *
   * @param request the parameters for the completion, including messages and generation settings
   * @param handler callbacks that receive the generated output
   * @throws CommunicationException if the request fails before streaming begins
   */
  void chatStream(ChatRequest request, StreamHandler handler);

  /**
   * Creates a new {@link Context} backed by this model.
   *
   * <p>The returned context tracks conversation history automatically,
   * appending user messages and assistant responses as the exchange progresses.
   *
   * @return a new, empty conversation context
   */
  Context createContext();

  /**
   * Releases resources held by this model.
   *
   * <p>After closing, subsequent calls to {@link #chat(ChatRequest)} or
   * {@link #chatStream(ChatRequest, StreamHandler)} may throw {@link IllegalStateException}.
   */
  @Override
  void close();
}
