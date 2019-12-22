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
import org.slf4j.Logger;

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
    private final Logger logger;

    // Keep the class instance around to protect against type erasure
    private final Class<T> attributeClass;
    private final String name;
    private final String description;
    private final Supplier<? extends T> supplier;
    private final Consumer<? super T> consumer;
    private final Descriptor descriptor;

    private final AtomicLong sequenceNumber = new AtomicLong(1L);

    public AttributeInfo(String name, Class<T> attributeClass, Supplier<? extends T> supplier) {
        this(name, attributeClass, null, supplier, null, null);
    }

    public AttributeInfo(String name, Class<T> attributeClass, Supplier<? extends T> supplier, Consumer<? super T> consumer) {
        this(name, attributeClass, null, supplier, consumer, null);
    }

    public AttributeInfo(String name, Class<T> attributeClass, String description, Supplier<? extends T> supplier, Consumer<? super T> consumer, Descriptor descriptor) {
        this.name = requireNonNull(name, "Null name");
        this.attributeClass = requireNonNull(attributeClass, "Null attribute class");
        this.description = description;
        this.supplier = supplier;
        this.consumer = consumer;
        this.descriptor = descriptor;
        this.logger = org.slf4j.LoggerFactory.getLogger(getClass());
    }

    public Optional<T> get() {
        try {
            return Optional.ofNullable(supplier).map(Supplier::get);
        } catch (Exception e) {
            logger.error("Cannot get from supplier", e);
            throw e;
        }
    }

    public Boolean set(T value, Consumer<Function<T, Notification>> receiver) {
        try {
            if (consumer != null) {
                // if the consumer type is a primitive, then passing in a null will result in unboxing to a raw value.
                // so we can't do that.  Instead, map null to false, 0, "" depending on type.
                if (value == null) {
                    // The function doesn't know if this is primitive or not.
                    if (Boolean.class.equals(attributeClass)) {
                        consumer.accept((T) Boolean.FALSE);
                    } else if (String.class.equals(attributeClass)) {
                        consumer.accept((T) "");
                    } else {
                        throw new IllegalStateException("Cannot map type " + attributeClass + " to null");
                    }
                } else {
                    consumer.accept(value);
                }
                if (supplier != null && receiver != null) {
                    T oldValue = supplier.get();
                    // Neither JConsole nor JMC have a built in way to display a custom AttributeChange notification
                    receiver.accept(source -> new AttributeChangeNotification(source,
                            sequenceNumber.getAndIncrement(), System.currentTimeMillis(),
                            String.format("%s changed from %s to %s", name, oldValue, value), name, attributeClass.toString(),
                            oldValue, value));
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Cannot set value {} on attribute {} with attributeClass {}", value, name, attributeClass.toString(), e);
            throw e;
        }
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
                attributeClass.toString(),
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
            return new AttributeInfo<T>(name, attributeType, description, supplier, consumer, descriptor);
        }
    }

}
