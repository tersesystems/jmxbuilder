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

    /**
     * Creates a tabular data from items.
     *
     * @param items the input.
     * @return the tabular data.
     */
    @Override
    public TabularData apply(Iterable<I> items) {
        final TabularDataSupport tabularDataSupport = new TabularDataSupport(getTabularType());
        for (I item : items) {
            tabularDataSupport.put(compositeDataWriter.apply(item));
        }
        return tabularDataSupport;
    }

    public static <I> Builder<I> builder(Class<I> clazz) {
        return new Builder<>();
    }

    public static class Builder<I> {
        private String typeDescription;
        private String typeName;
        private String[] indexNames;
        private CompositeDataWriter<I> compositeDataWriter;

        Builder() {
        }

        /**
         * Sets the name on the tabular type.
         *
         * @param name the name
         * @return the builder.
         */
        public Builder<I> withTypeName(String name) {
            this.typeName = name;
            return this;
        }

        /**
         * Sets the description on the tabular type..
         *
         * @param description the description
         * @return the builder.
         */
        public Builder<I> withTypeDescription(String description) {
            this.typeDescription = description;
            return this;
        }

        /**
         * Sets the index name on the tabular type.  This replaces any previous values.
         *
         * @param indexName the index name.
         * @return the builder.
         */
        public Builder<I> withIndexName(String indexName) {
            this.indexNames = new String[] { indexName };
            return this;
        }

        /**
         * Sets index names on the tabular type.  This replaces any previous values.
         *
         * @param indexNames the index names.
         * @return the builder.
         */
        public Builder<I> withIndexNames(String[] indexNames) {
            this.indexNames = indexNames;
            return this;
        }

        /**
         * Sets index names on the tabular type.  This replaces any previous values.
         *
         * @param indexNames the index names.
         * @return the builder.
         */
        public Builder<I> withIndexNames(List<String> indexNames) {
            this.indexNames = indexNames.toArray(new String[0]);
            return this;
        }

        /**
         * Sets the composite writer for the tabular type.
         *
         * @param compositeDataWriter the writer.
         * @return the builder.
         */
        public Builder<I> withCompositeDataWriter(CompositeDataWriter<I> compositeDataWriter) {
            this.compositeDataWriter = compositeDataWriter;
            return this;
        }

        /**
         * Creates the writer.
         *
         * @return the tabular data writer.
         */
        public TabularDataWriter<I> build() {
            return new TabularDataWriter<I>(typeName, typeDescription, indexNames, compositeDataWriter);
        }

    }

}