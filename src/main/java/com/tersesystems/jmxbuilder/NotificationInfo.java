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
import javax.management.MBeanNotificationInfo;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Notification info.  This is used for JMX notification support.
 *
 * Bear in mind nothing actually uses JMX notifications, but it's nice to know you could.
 *
 * Intentionally open without a builder pattern so you can pass in your own.
 */
public class NotificationInfo {
    private final String[] notifTypes;
    private final String name;
    private final String description;
    private final Descriptor descriptor;

    public NotificationInfo(String[] notifTypes, String name, String description, Descriptor descriptor) {
        this.notifTypes = requireNonNull(notifTypes);
        this.name = requireNonNull(name);
        this.description = (description);
        this.descriptor = (descriptor);
    }

    public String[] getNotificationTypes() {
        return notifTypes;
    }

    public String getName() {
        return name;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<Descriptor> getDescriptor() {
        return Optional.ofNullable(descriptor);
    }

    public MBeanNotificationInfo getMBeanNotificationInfo() {
        return new MBeanNotificationInfo(notifTypes, name, description, descriptor);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private String[] notifTypes;
        private final DescriptorSupport.Builder descriptorBuilder = new DescriptorSupport.Builder();

        Builder() {
        }

        public Builder withTypes(String... notifTypes){
            this.notifTypes = notifTypes;
            return this;
        }

        // name field should be the fully qualified Java class name of
        // the notification objects described by this class
        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description){
            this.description = description;
            return this;
        }

        public Builder withDescriptor(Descriptor descriptor) {
            descriptorBuilder.withDescriptor(descriptor);
            return this;
        }

        public NotificationInfo build() {
            return new NotificationInfo(notifTypes, name, description, descriptorBuilder.build());
        }
    }

}
