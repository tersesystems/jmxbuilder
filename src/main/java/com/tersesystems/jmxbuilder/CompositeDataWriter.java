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

import javax.management.openmbean.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

/**
 * This class converts from a type I to a CompositeData.
 *
 * @param <I>
 */
public class CompositeDataWriter<I> implements Function<I, CompositeData> {
    private final String[] attributeNames;
    private final List<Function<I, ?>> attributeMappers;
    private final CompositeType compositeType;

    public CompositeDataWriter(String typeDescription, String typeName, List<String> attributeNames, List<String> attributeDescriptions, List<OpenType<?>> attributeTypes, List<Function<I, ?>> attributeMappers) {
        List<String> names = requireNonNull(attributeNames, "Null attributeNames");
        String name = requireNonNull(typeName, "Null typeName");
        String description = requireNonNull(typeDescription, "Null typeDescription");
        List<String> descriptions = requireNonNull(attributeDescriptions, "Null attributeDescriptions");
        List<OpenType<?>> types = requireNonNull(attributeTypes, "Null attributeTypes");

        this.attributeNames = names.toArray(new String[0]);
        this.attributeMappers = requireNonNull(attributeMappers);
        try {
            this.compositeType = new CompositeType(name, description,
                    this.attributeNames,
                    descriptions.toArray(new String[0]),
                    types.toArray(new OpenType<?>[0]));
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }

    public CompositeType getCompositeType() {
        return compositeType;
    }

    @Override
    public CompositeData apply(I item) {
        try {
            final Object[] values = attributeMappers.stream().map(f -> f.apply(item)).toArray();
            return new CompositeDataSupport(compositeType, attributeNames, values);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <I> Builder<I> builder() {
        return new Builder<>();
    }

    public static class Builder<I> {
        private final OpenTypeMapper openTypeMapper = OpenTypeMapper.getInstance();

        private String typeDescription;
        private String typeName;
        private final List<String> attributeNames = new ArrayList<>();
        private final List<String> attributeDescriptions = new ArrayList<>();
        private final List<OpenType<?>> attributeTypes = new ArrayList<>();
        private final List<Function<I, ?>> attributeMappers = new ArrayList<>();

        Builder() {
        }

        public Builder<I> withTypeName(String typeName) {
            this.typeName = typeName;
            return this;
        }

        public Builder<I> withTypeDescription(String typeDescription) {
            this.typeDescription = typeDescription;
            return this;
        }

        // Ideally we want to say that for every T there is a SimpleType<T>
        public <T> Builder<I> withSimpleAttribute(String name, String description, Function<I, T> mapper) {
            try {
                attributeNames.add(name);
                attributeDescriptions.add(description);

                // Resolves T
                Class<?>[] types = TypeResolver.resolveRawArguments(Function.class, mapper.getClass());
                Class<?> attributeType = types[1];

                attributeTypes.add(toOpenType(attributeType));
                attributeMappers.add(mapper);
                return this;
            } catch (OpenDataException e) {
                throw new RuntimeException(e);
            }
        }

        public Builder<I> withCompositeAttribute(String name, String description, Function<I, CompositeData> mapper, CompositeType attributeType) {
            attributeNames.add(name);
            attributeDescriptions.add(description);
            attributeTypes.add(attributeType);
            attributeMappers.add(mapper);
            return this;
        }

        public <T> Builder<I> withCompositeAttribute(String name, String description, Function<I, T> mapper, CompositeDataWriter<T> attributeCompositeBuilder) {
            return withCompositeAttribute(name, description, mapper.andThen(attributeCompositeBuilder), attributeCompositeBuilder.getCompositeType());
        }

        public Builder<I> withTabularAttribute(String name, String description, Function<I, TabularData> mapper, TabularType attributeType) {
            attributeNames.add(name);
            attributeDescriptions.add(description);
            attributeTypes.add(attributeType);
            attributeMappers.add(mapper);
            return this;
        }

        public <R> Builder<I> withTabularAttribute(String name, String description, Function<I, Iterable<R>> mapper, TabularDataWriter<R> attributeTabularBuilder) {
            return withTabularAttribute(name, description, mapper.andThen(attributeTabularBuilder), attributeTabularBuilder.getTabularType());
        }

        private OpenType<?> toOpenType(Type type) throws OpenDataException {
            return openTypeMapper.fromType(type);
        }

        public CompositeDataWriter<I> build() {
            return new CompositeDataWriter<>(typeDescription, typeName, attributeNames, attributeDescriptions, attributeTypes, attributeMappers);
        }

    }

}