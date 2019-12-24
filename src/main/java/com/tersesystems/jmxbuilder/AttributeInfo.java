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

import javax.management.AttributeChangeNotification;
import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.Notification;
import javax.management.openmbean.OpenType;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
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
    private final String type;
    private final String name;
    private final String description;
    private final Supplier<? extends T> supplier;
    private final Consumer<? super T> consumer;
    private final Descriptor descriptor;

    private final AtomicLong sequenceNumber = new AtomicLong(1L);

    public AttributeInfo(String name, String type, String description, Supplier<? extends T> supplier, Consumer<? super T> consumer, Descriptor descriptor) {
        this.name = requireNonNull(name, "Null name");
        this.type = requireNonNull(type, "Null type");
        this.description = requireNonNull(description, "Null description");
        this.supplier = supplier;
        this.consumer = consumer;
        this.descriptor = descriptor;
    }

    public Optional<T> get() {
        try {
            return Optional.ofNullable(supplier).map(Supplier::get);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Boolean set(T value, Consumer<Function<T, Notification>> receiver) {
            if (consumer != null) {
               consumer.accept(value);
                if (supplier != null && receiver != null) {
                    T oldValue = supplier.get();
                    // Neither JConsole nor JMC have a built in way to display a custom AttributeChange notification
                    receiver.accept(source -> new AttributeChangeNotification(source,
                            sequenceNumber.getAndIncrement(), System.currentTimeMillis(),
                            String.format("%s changed from %s to %s", name, oldValue, value), name, type,
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
        return new MBeanAttributeInfo(name, type, description, supplier != null, consumer != null, false, descriptor);
        //        return new OpenMBeanAttributeInfoSupport(name,
        //                description,
        //                openType,
        //                supplier != null,
        //                consumer != null,
        //                false,
        //                descriptor);
    }

    public static <T> Builder<T> builder(Class<T> attributeType) {
        return new Builder<>(attributeType);
    }

    public static class Builder<T> {
        private final OpenTypeMapper openTypeMapper = OpenTypeMapper.getInstance();

        private String name;
        private String description;
        private Supplier<? extends T> supplier;
        private Consumer<? super T> consumer;
        private DescriptorSupport.Builder descriptorBuilder = DescriptorSupport.builder();
        private final Class<T> attributeType;

        Builder(Class<T> attributeType) {
            this.attributeType = attributeType;
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
            this.descriptorBuilder.withDescriptor(descriptor);
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

        public Builder<T> withBeanProperty(Object obj, String propertyName) {
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
                Optional<PropertyDescriptor> maybeProperty = Arrays.stream(beanInfo.getPropertyDescriptors())
                        .filter(pd -> pd.getName().equals(propertyName))
                        .findFirst();

                maybeProperty.ifPresent(pd -> {
                    if (pd.getReadMethod() != null) {
                        this.supplier = () -> {
                            try {
                                return (T) pd.getReadMethod().invoke(obj);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                            return null;
                        };
                    }
                    if (pd.getWriteMethod() != null) {
                        this.consumer = newValue -> {
                            try {
                                pd.getWriteMethod().invoke(obj, newValue);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        };
                    }
                });
                return this;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public AttributeInfo<T> build() {
            if (description == null || description.isEmpty()) {
                description = name;
            }

            // The Descriptor for all of the MBeanAttributeInfo, MBeanParameterInfo, and MBeanOperationInfo objects
            // contained in the MBeanInfo will have a field openType whose value is the OpenType specified by the
            // mapping rules above. So even when getType() is "int", getDescriptor().getField("openType") will be
            // SimpleType.INTEGER.
            //
            // The Descriptor for each of these objects will also have a field originalType that is a string
            // representing the Java type that appeared in the MXBean interface.
            OpenType<?> openType = openTypeMapper.fromClass(attributeType);
            String originalType = attributeType.getName();
            Descriptor descriptor = descriptorBuilder
                    .withField("openType", openType)
                    .withField("originalType", originalType)
                    .build();
            return new AttributeInfo<T>(this.name, originalType, description, supplier, consumer, descriptor);
        }
    }

}
