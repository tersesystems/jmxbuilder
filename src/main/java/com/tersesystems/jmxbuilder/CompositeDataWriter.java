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

import javax.management.openmbean.*;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * This class converts from a type I to a CompositeData.
 *
 * @param <I> The input type.
 */
public class CompositeDataWriter<I> implements Function<I, CompositeData> {
    private final String[] attributeNames;
    private final List<Function<I, ?>> attributeMappers;
    private final CompositeType compositeType;

    public CompositeDataWriter(String typeName,
                               String typeDescription,
                               List<String> attributeNames,
                               List<String> attributeDescriptions,
                               List<OpenType<?>> attributeTypes,
                               List<Function<I, ?>> attributeMappers) {
        List<String> names = requireNonNull(attributeNames, "Null attributeNames");
        String name = requireNonNull(typeName, "Null typeName");
        String description = requireNonNull(typeDescription, "Null typeDescription");
        List<String> descriptions = requireNonNull(attributeDescriptions, "Null attributeDescriptions");
        List<OpenType<?>> types = requireNonNull(attributeTypes, "Null attributeTypes");

        this.attributeNames = names.toArray(new String[0]);
        this.attributeMappers = requireNonNull(attributeMappers);
        try {
            this.compositeType = new CompositeType(name,
                    description,
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

    public static <I> Builder<I> builder(Class<I> clazz) {
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

        /**
         * Sets a name for the composite type.
         *
         * @param typeName the name
         * @return the builder
         */
        public Builder<I> withTypeName(String typeName) {
            this.typeName = typeName;
            return this;
        }

        /**
         * Sets a description on the composite type.
         *
         * @param typeDescription the description.
         * @return the builder
         */
        public Builder<I> withTypeDescription(String typeDescription) {
            this.typeDescription = typeDescription;
            return this;
        }

        /**
         * Infers the attribute type from {@code <T>}.
         *
         * @param name the name of the attribute
         * @param mapper the mapper to the attribute type.
         * @param <T> the attribute type.
         * @return the builder with the attribute
         */
        public <T> Builder<I> withSimpleAttribute(String name, Function<I, T> mapper) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Function.class, mapper.getClass());
            Class<T> attributeType = (Class<T>) types[1];
            if (TypeResolver.Unknown.class.equals(attributeType)) {
                throw new IllegalStateException("Cannot infer type from class " + Arrays.toString(types));
            }
            return withSimpleAttribute(attributeType, name, mapper);
        }

        /**
         * Infers the attribute type from {@code <T>}.
         *
         * @param name the name of the attribute
         * @param description the description of the attribute
         * @param mapper the mapper to the attribute type.
         * @param <T> the attribute type.
         * @return the builder with the attribute
         */
        public <T> Builder<I> withSimpleAttribute(String name, String description, Function<I, T> mapper) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Function.class, mapper.getClass());
            Class<T> attributeType = (Class<T>) types[1];
            if (TypeResolver.Unknown.class.equals(attributeType)) {
                throw new IllegalStateException("Cannot infer type from class " + Arrays.toString(types));
            }
            return withSimpleAttribute(attributeType, name, description, mapper);
        }

        /**
         * Creates an attribute of type T.
         *
         * @param attributeType the attribute type.
         * @param name the name of the attribute
         * @param mapper the mapper to the attribute type.
         * @param <T> the attribute type.
         * @return the builder with the attribute
         */
        public <T> Builder<I> withSimpleAttribute(Class<? extends T> attributeType, String name, Function<I, T> mapper) {
           return withSimpleAttribute(attributeType, name, name, mapper);
        }

        /**
         * Creates an attribute of type T.
         *
         * @param attributeType the attribute type.
         * @param name the name of the attribute
         * @param description   the description of the attribute.
         * @param mapper the mapper to the attribute type.
         * @param <T> the attribute type.
         * @return the builder with the attribute
         */
        public <T> Builder<I> withSimpleAttribute(Class<? extends T> attributeType, String name, String description, Function<I, T> mapper) {
            attributeNames.add(name);
            attributeDescriptions.add(description);
            attributeTypes.add(openTypeMapper.fromClass(attributeType));
            attributeMappers.add(mapper);
            return this;
        }

        /**
         * Creates a composite attribute of type T.
         *
         * @param name the name of the attribute
         * @param mapper the mapper to the attribute.
         * @param writer the composite data writer.
         * @param <T> the attribute type.
         * @return the builder with the attribute
         */
        public <T> Builder<I> withCompositeAttribute(String name, Function<I, T> mapper, CompositeDataWriter<T> writer) {
            return withCompositeAttribute(
                    writer.getCompositeType(),
                    name,
                    mapper.andThen(writer)
            );
        }

        /**
         * Creates a composite attribute.
         *
         * @param name the name of the attribute
         * @param description the description of the attribute.
         * @param mapper the mapper to the attribute.
         * @param writer the composite data writer from T to CompositeData.
         * @param <T> the attribute type.
         * @return the builder with the attribute
         */
        public <T> Builder<I> withCompositeAttribute(String name, String description, Function<I, T> mapper, CompositeDataWriter<T> writer) {
            return withCompositeAttribute(
                    writer.getCompositeType(),
                    name,
                    description,
                    mapper.andThen(writer)
            );
        }

        /**
         * Creates a composite attribute.
         *
         * @param attributeType the composite type.
         * @param name the name of the attribute
         * @param mapper the mapper to the attribute.
         * @return the builder with the attribute
         */
        public Builder<I> withCompositeAttribute(CompositeType attributeType, String name, Function<I, CompositeData> mapper) {
            return withCompositeAttribute(attributeType, name, name, mapper);
        }

        /**
         * Creates a composite attribute.
         *
         * @param attributeType the composite type.
         * @param name the name of the attribute
         * @param description   the attribute description.
         * @param mapper the mapper to the attribute.
         * @return the builder with the attribute
         */
        public Builder<I> withCompositeAttribute(CompositeType attributeType, String name, String description, Function<I, CompositeData> mapper) {
            attributeNames.add(name);
            attributeDescriptions.add(description);
            attributeTypes.add(attributeType);
            attributeMappers.add(mapper);
            return this;
        }

        /**
         * Creates a tabular attribute, using a function that returns an Iterable (typically a list) of R.
         *
         * @param name the name of the attribute
         * @param mapper the mapper to the attribute.
         * @param tabularWriter the tabular data writer from R to TabularData
         * @return the builder with the attribute
         * @param <R> the element type.
         */
        public <R> Builder<I> withTabularAttribute(String name, Function<I, Iterable<R>> mapper, TabularDataWriter<R> tabularWriter) {
            return withTabularAttribute(tabularWriter.getTabularType(),
                    name,
                    mapper.andThen(tabularWriter));
        }

        /**
         * Creates a tabular attribute, using a function from I to TabularData.
         *
         * @param attributeType the tabular type.
         * @param name the name of the attribute
         * @param mapper the mapper to the attribute.
         * @return the builder with the attribute
         */
        public Builder<I> withTabularAttribute(TabularType attributeType, String name, Function<I, TabularData> mapper) {
            return withTabularAttribute(attributeType, name, name, mapper);
        }

        /**
         * Creates a tabular attribute, using a function from I to TabularData.
         *
         * @param attributeType the tabular type.
         * @param name the name of the attribute
         * @param description   the description of the attribute.
         * @param mapper the mapper to the attribute.
         * @return the builder with the attribute
         */
        public Builder<I> withTabularAttribute(TabularType attributeType, String name, String description, Function<I, TabularData> mapper) {
            attributeNames.add(name);
            attributeDescriptions.add(description);
            attributeTypes.add(attributeType);
            attributeMappers.add(mapper);
            return this;
        }

        /**
         * Builds the writer from input.
         *
         * @return a fully composed writer.
         */
        public CompositeDataWriter<I> build() {
            return new CompositeDataWriter<>(typeName, typeDescription, attributeNames, attributeDescriptions, attributeTypes, attributeMappers);
        }

    }

}