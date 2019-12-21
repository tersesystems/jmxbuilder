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

import javax.management.JMX;
import java.util.Set;

/**
 * Creates an instance of java.management.Descriptor.
 */
public class DescriptorSupport {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final javax.management.modelmbean.DescriptorSupport descriptor = new javax.management.modelmbean.DescriptorSupport();

        public Builder withDefaultValue(Object defaultValue) {
            descriptor.setField(JMX.DEFAULT_VALUE_FIELD, defaultValue);
            return this;
        }

        public Builder withDeprecated(String value) {
            descriptor.setField("deprecated", value);
            return this;
        }

        public Builder withImmutableInfo(boolean value) {
            descriptor.setField(JMX.IMMUTABLE_INFO_FIELD, value);
            return this;
        }

        public Builder withInterfaceClassName(String value) {
            descriptor.setField(JMX.INTERFACE_CLASS_NAME_FIELD, value);
            return this;
        }

        public Builder withLocale(String locale) {
            descriptor.setField(JMX.LEGAL_VALUES_FIELD, locale);
            return this;
        }

        public Builder withLegalValues(Set<?> values) {
            descriptor.setField(JMX.LEGAL_VALUES_FIELD, values);
            return this;
        }

        public Builder withMaxValue(Object value) {
            descriptor.setField(JMX.MAX_VALUE_FIELD, value);
            return this;
        }

        public Builder withMinValue(Object value) {
            descriptor.setField(JMX.MIN_VALUE_FIELD, value);
            return this;
        }

        public Builder withEnabled(boolean value) {
            descriptor.setField("enabled", value);
            return this;
        }

        public Builder withExceptions(String... exceptions) {
            descriptor.setField("exceptions", exceptions);
            return this;
        }

        public Builder withInfoTimeout(Long infoTimeout) {
            descriptor.setField("infoTimeout", infoTimeout.toString());
            return this;
        }

        // "counter" or "gauge"
        public Builder withMetricType(String metricType) {
            descriptor.setField("metricType", metricType);
            return this;
        }

        public Builder withSeverity(Integer severity) {
            descriptor.setField("severity", severity);
            return this;
        }

        public Builder withSince(String since) {
            descriptor.setField("since", since);
            return this;
        }

        public Builder withUnits(String units) {
            descriptor.setField("units", units);
            return this;
        }

        public Builder withDisplayName(String displayName) {
            descriptor.setField("displayName", displayName);
            return this;
        }

        public Builder withDescriptionResource(String descriptionResource) {
            descriptor.setField("descriptionResource", descriptionResource);
            return this;
        }

        public Builder withDescriptionResourceKey(String descriptionResourceKey) {
            descriptor.setField("descriptionResourceKey", descriptionResourceKey);
            return this;
        }

        public Builder withDescriptor(javax.management.Descriptor newDescriptor) {
            String[] fieldNames = newDescriptor.getFieldNames();
            for (String fieldName : fieldNames) {
                Object value = newDescriptor.getFieldValue(fieldName);
                descriptor.setField(fieldName, value);
            }
            return this;
        }

        public javax.management.modelmbean.DescriptorSupport build() {
            return descriptor;
        }
    }
}
