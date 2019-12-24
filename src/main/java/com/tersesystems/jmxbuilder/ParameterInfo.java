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
import javax.management.MBeanParameterInfo;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Parameter info.  Used to make method parameter names a bit more intelligible.
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

    public static Builder builder() {
        return new Builder();
    }

    public MBeanParameterInfo getMBeanParameterInfo() {
        String description = getDescription().orElse(null);
        Descriptor descriptor = getDescriptor().orElse(null);
        return new MBeanParameterInfo(getName(), getType().getTypeName(), description, descriptor);
    }

    // only type and name are required here
    public static class Builder {
        private Class<?> type;
        private String name;
        private String description;
        private Descriptor descriptor;

        Builder() {}

        public Builder withClassType(Class<?> type) {
            this.type = type;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withDescriptor(Descriptor descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        public ParameterInfo build() {
            // The Descriptor for all of the MBeanAttributeInfo, MBeanParameterInfo, and MBeanOperationInfo objects
            // contained in the MBeanInfo will have a field openType whose value is the OpenType specified by the
            // mapping rules above. So even when getType() is "int", getDescriptor().getField("openType") will be
            // SimpleType.INTEGER.
            //
            // The Descriptor for each of these objects will also have a field originalType that is a string
            // representing the Java type that appeared in the MXBean interface.
            return new ParameterInfo(type, name, description, descriptor);
        }
    }

}
