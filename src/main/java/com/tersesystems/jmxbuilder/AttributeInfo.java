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

import net.jodah.typetools.TypeResolver;

import javax.management.AttributeChangeNotification;
import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.Notification;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Creates an attribute.
 *
 * Intentionally open so you can pass in your own if the builder pattern doesn't work.
 */
public class AttributeInfo<T> {
    // Keep the class instance around to protect against type erasure
    private final String typeString;
    private final String name;
    private final String description;
    private final Supplier<? extends T> supplier;
    private final Consumer<? super T> consumer;
    private final Descriptor descriptor;

    private final AtomicLong sequenceNumber = new AtomicLong(1L);

    public AttributeInfo(String name, String typeString, Supplier<? extends T> supplier) {
        this(name, typeString, null, supplier, null, null);
    }

    public AttributeInfo(String name, String typeString, Supplier<? extends T> supplier, Consumer<? super T> consumer) {
        this(name, typeString, null, supplier, consumer, null);
    }

    public AttributeInfo(String name, String typeString, String description, Supplier<? extends T> supplier, Consumer<? super T> consumer, Descriptor descriptor) {
        this.typeString = requireNonNull(typeString, "Null typeString");
        this.name = requireNonNull(name, "Null name");
        this.description = description;
        this.supplier = supplier;
        this.consumer = consumer;
        this.descriptor = descriptor;
    }

    public Optional<T> get() {
        return Optional.ofNullable(supplier).map(Supplier::get);
    }

    public Boolean set(T value, Consumer<Function<Object, Notification>> receiver) {
        if (consumer != null) {
            T oldValue = supplier.get();
            consumer.accept(value);
            if (receiver != null) {
                // Neither JConsole nor JMC have a built in way to display a custom AttributeChange notification
                receiver.accept(source -> new AttributeChangeNotification(source,
                        sequenceNumber.getAndIncrement(), System.currentTimeMillis(),
                        String.format("%s changed from %s to %s", name, oldValue, value), name, typeString,
                        oldValue, value));
            }
            return true;
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<Supplier<? extends T>> getSupplier() {
        return Optional.ofNullable(supplier);
    }

    public Optional<Consumer<? super T>> getConsumer() {
        return Optional.ofNullable(consumer);
    }

    public Optional<Descriptor> getDescriptor() {
        return Optional.ofNullable(descriptor);
    }

    public MBeanAttributeInfo getMBeanAttributeInfo() {
        return new MBeanAttributeInfo(name,
                typeString,
                description,
                supplier != null,
                consumer != null,
                false,
                descriptor);
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private String name;
        private String description;
        private Supplier<? extends T> supplier;
        private Consumer<? super T> consumer;
        private Descriptor descriptor;

        Builder() {
        }

        public Builder<T> withName(String name) {
            this.name = name;
            return this;
        }

        public Builder<T> withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder<T> withDescriptor(Descriptor descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        public Builder<T> withSupplier(Supplier<? extends T> supplier) {
            this.supplier = supplier;
            return this;
        }

        public Builder<T> withConsumer(Consumer<? super T> consumer) {
            this.consumer = consumer;
            return this;
        }

        public AttributeInfo<T> build() {
            if (supplier == null && consumer == null) {
                throw new IllegalStateException("No supplier or consumer found!");
            }

            // resolveRawArgument doesn't seem to work on the AttributeInfoBuilder itself, but WILL
            // work on the functions...
            Class<T> attributeType;
            if (supplier != null) {
                attributeType = (Class<T>) TypeResolver.resolveRawArgument(Supplier.class, supplier.getClass());
            } else {
                attributeType = (Class<T>) TypeResolver.resolveRawArgument(Consumer.class, consumer.getClass());
            }
            return new AttributeInfo<T>(name, attributeType.getName(), description, supplier, consumer, descriptor);
        }
    }

}
