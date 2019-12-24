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

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * This class converts from an iterable of I to a TabularData instance.
 */
public class TabularDataWriter<I> implements Function<Iterable<I>, TabularData> {
    private  final CompositeDataWriter<I> compositeDataWriter;
    private final TabularType tabularType;

    public TabularDataWriter(String typeName, String typeDescription, String[] indexNames, CompositeDataWriter<I> compositeDataWriter) {
        Objects.requireNonNull(typeName, "Null typeName");
        Objects.requireNonNull(typeDescription, "Null typeDescription");
        Objects.requireNonNull(indexNames, "Null indexNames");

        this.compositeDataWriter = Objects.requireNonNull(compositeDataWriter, "Null compositeDataWriter");
        try {
            this.tabularType = new TabularType(typeName, typeDescription, compositeDataWriter.getCompositeType(), indexNames);
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }

    public TabularType getTabularType() {
       return tabularType;
    }

    @Override
    public TabularData apply(Iterable<I> items) {
        final TabularDataSupport tabularDataSupport = new TabularDataSupport(getTabularType());
        for (I item : items) {
            tabularDataSupport.put(compositeDataWriter.apply(item));
        }
        return tabularDataSupport;
    }

    public static <I> Builder<I> builder(Class<I> clazz) {
        return new Builder<I>();
    }

    public static class Builder<I> {
        private String typeDescription;
        private String typeName;
        private String[] indexNames;
        private CompositeDataWriter<I> compositeDataWriter;

        public Builder() {
            this(null, null, null, null);
        }

        public Builder(String typeName, String typeDescription) {
            this(typeName, typeDescription, null, null);
        }

        public Builder(String typeName, String typeDescription, String[] indexNames) {
            this(typeName, typeDescription, indexNames, null);
        }

        public Builder(String typeName, String typeDescription, List<String> indexNames) {
            this(typeName, typeDescription, indexNames.toArray(new String[0]), null);
        }

        public Builder(String typeName, String typeDescription, String[] indexNames, CompositeDataWriter<I> compositeDataWriter) {
            this.typeName = typeName;
            this.typeDescription = typeDescription;
            this.indexNames = indexNames;
            this.compositeDataWriter = compositeDataWriter;
        }

        public Builder<I> withTypeName(String name) {
            this.typeName = name;
            return this;
        }

        public Builder<I> withTypeDescription(String description) {
            this.typeDescription = description;
            return this;
        }

        public Builder<I> withIndexName(String indexName) {
            this.indexNames = new String[] { indexName };
            return this;
        }

        public Builder<I> withIndexNames(String[] indexNames) {
            this.indexNames = indexNames;
            return this;
        }

        public Builder<I> withIndexNames(List<String> indexNames) {
            this.indexNames = indexNames.toArray(new String[0]);
            return this;
        }

        public Builder<I> withCompositeDataWriter(CompositeDataWriter<I> compositeDataWriter) {
            this.compositeDataWriter = compositeDataWriter;
            return this;
        }

        public TabularDataWriter<I> build() {
            return new TabularDataWriter<>(typeName, typeDescription, indexNames, compositeDataWriter);
        }

    }

}