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
import javax.management.ObjectName;
import javax.management.openmbean.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Parameter info.  Used to make method parameter names a bit more intelligible.
 * <p>
 * Intentionally open without a builder pattern so you can pass in your own.
 *
 * @param <T>
 */
public final class ParameterInfo<T> {
    private static final OpenTypeMapper openTypeMapper = OpenTypeMapper.getInstance();

    private final OpenType<T> type;
    private final String name;
    private final String description;
    private final Descriptor descriptor;

    public ParameterInfo(OpenType<T> type, String name) {
        this(type, name, name, null);
    }

    public ParameterInfo(OpenType<T> type, String name, String description, Descriptor descriptor) {
        this.type = requireNonNull(type, "null type");
        this.name = requireNonNull(name, "null name");
        this.description = requireNonNull(description, "null description");
        this.descriptor = descriptor;
    }

    public OpenType<T> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Optional<Descriptor> getDescriptor() {
        return Optional.ofNullable(descriptor);
    }

    public String getDescription() {
        return description;
    }

    public OpenMBeanParameterInfo getMBeanParameterInfo() {
        return new OpenMBeanParameterInfoSupport(
                getName(),
                getDescription(),
                getType(),
                getDescriptor().orElse(null)
        );
    }

    public static <F> Builder<F> builder(Class<F> clazz) {
        return new Builder<F>(openTypeMapper.fromClass(clazz));
    }

    // Type, name and description are required.
    public static class Builder<F> {
        private OpenType<F> type;
        private String name;
        private String description;
        private Descriptor descriptor = DescriptorSupport.builder().build();

        Builder(OpenType<F> type) {
            this.type = type;
        }

        public Builder<F> withName(String name) {
            String s =requireNonNull(name);
            if (s.isEmpty()) {
                throw new IllegalStateException("Empty description!");
            }
            this.name = s;
            return this;
        }

        public Builder<F> withDescription(String description) {
            String s = requireNonNull(description);
            if (s.isEmpty()) {
                throw new IllegalStateException("Empty description!");
            }
            this.description = s;
            return this;
        }

        public Builder<F> withDescriptor(Descriptor descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        public ParameterInfo<F> build() {
            if (description == null || description.isEmpty()) {
                description = name;
            }

            // if this is a boolean parameter, then we want the legal values to be true and false only.
            if (type.getClassName().equals("java.lang.Boolean")) {
                HashSet<Boolean> booleans = new HashSet<>();
                booleans.add(Boolean.TRUE);
                booleans.add(Boolean.FALSE);
                descriptor.setField("legalValues", booleans);
            }
            return new ParameterInfo<F>(type, name, description, descriptor);
        }
    }

}
