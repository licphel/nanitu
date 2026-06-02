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

package net.nanitu.util;

import org.jspecify.annotations.Nullable;

import java.util.ServiceLoader;

/**
 * A service provider interface for discovering and loading pluggable implementations.
 *
 * <p>This is the central extension mechanism for the nanitu framework. Modules
 * expose implementations by registering them in
 * {@code META-INF/services/<interface-name>}, and consumers obtain them via
 * {@link #get(Class)} or {@link #collect(Class)} without any compile-time
 * dependency on the implementing module.
 *
 * <p>Each {@code Service} implementation must provide:
 * <ul>
 *   <li>{@link #isAvailable()} — whether the service can run in the current environment</li>
 *   <li>{@link #create()} — a factory method that returns a new instance of {@code T}</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Get the first available implementation
 * ModelProvider provider = Service.get(ModelProvider.class);
 * if (provider != null) {
 *     Model model = provider.create();
 * }
 *
 * // Enumerate all available implementations
 * for (ModelProvider p : Service.collect(ModelProvider.class)) {
 *     System.out.println(p.create().name());
 * }
 * }</pre>
 *
 * @param <T> the type this service provides
 */
public interface Service<T> {
  /**
   * Collects all available implementations of the given service interface.
   *
   * <p>Uses {@link ServiceLoader} to discover providers registered via
   * {@code META-INF/services}. Only providers whose {@link #isAvailable()}
   * returns {@code true} are included in the result.
   *
   * @param serviceClass the service interface to discover
   * @param <R>          the service type
   * @return an array of available service instances, may be empty
   */
  @SuppressWarnings("unchecked")
  static <R> Service<R>[] collect(Class<? extends Service<R>> serviceClass) {
    return (Service<R>[]) ServiceLoader.load(serviceClass).stream().map(ServiceLoader.Provider::get).filter(Service::isAvailable).toArray(Service<?>[]::new);
  }

  /**
   * Returns the first available implementation of the given service interface.
   *
   * <p>This is a convenience method equivalent to taking the first element of
   * {@link #collect(Class)}. Returns {@code null} if no provider is available.
   *
   * @param serviceClass the service interface to discover
   * @param <R>          the service type
   * @return the first available service, or {@code null} if none found
   */
  static <R> @Nullable Service<R> get(Class<? extends Service<R>> serviceClass) {
    Service<R>[] providers = collect(serviceClass);
    return providers.length == 0 ? null : providers[0];
  }

  /**
   * Returns whether this service can operate in the current runtime environment.
   *
   * <p>Implementations should check for required native libraries, environment
   * variables, system properties, or hardware capabilities. A provider that
   * returns {@code false} is excluded from {@link #collect} results and will
   * not be returned by {@link #get}.
   *
   * @return {@code true} if the service is ready to use
   */
  boolean isAvailable();

  /**
   * Creates a new instance of the service.
   *
   * <p>The returned object is typically a fresh instance; callers are responsible
   * for its lifecycle (e.g. calling {@link AutoCloseable#close()} if applicable).
   *
   * @param args args passed in to create a service instance
   * @return a new service instance
   */
  T create(String args);

  /**
   * Creates a new instance of the service with no arguments.
   *
   * <p>The returned object is typically a fresh instance; callers are responsible
   * for its lifecycle (e.g. calling {@link AutoCloseable#close()} if applicable).
   *
   * @return a new service instance
   */
  default T create() {
    return create("");
  }

  /**
   * Returns the name of the service.
   *
   * @return the name of the service
   */
  String name();
}
