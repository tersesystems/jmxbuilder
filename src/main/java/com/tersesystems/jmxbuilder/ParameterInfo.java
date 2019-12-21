/*
  SPDX-License-Identifier: Apache-2.0

  Copyright 2019 Terse Systems <will@tersesystems.com>

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package com.tersesystems.jmxbuilder;

import javax.management.Descriptor;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Parameter info.  Used to make method parameter names a bit more intelligible.
 *
 * Intentionally open without a builder pattern so you can pass in your own.
 *
 * @param <T>
 */
public final class ParameterInfo<T> {
    private final Class<T> type;
    private final String name;
    private final String description;
    private final Descriptor descriptor;

    public ParameterInfo(Class<T> type, String name) {
        this(type, name, null, null);
    }

    public ParameterInfo(Class<T> type, String name, String description, Descriptor descriptor) {
        this.type = requireNonNull(type);
        this.name = name;
        this.description = description;
        this.descriptor = descriptor;
    }

    public Class<T> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Optional<Descriptor> getDescriptor() {
        return Optional.ofNullable(descriptor);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public static <F> Builder<F> builder(Class<F> type) {
        return new Builder<>(type);
    }

    // only type and name are required here
    public static class Builder<F> {
        private Class<F> type;
        private String name;
        private String description;
        private Descriptor descriptor;

        Builder(Class<F> type) {
            this.type = type;
        }

        public Builder<F> withName(String name) {
            this.name = name;
            return this;
        }

        public Builder<F> withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder<F> withDescriptor(Descriptor descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        public ParameterInfo<F> build() {
            return new ParameterInfo<F>(type, name, description, descriptor);
        }
    }

}
