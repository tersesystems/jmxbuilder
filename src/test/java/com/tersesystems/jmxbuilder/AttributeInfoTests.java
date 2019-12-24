/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Terse Systems <will@tersesystems.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tersesystems.jmxbuilder;

import com.tersesystems.jmxbuilder.model.Address;
import com.tersesystems.jmxbuilder.model.ExampleService;
import com.tersesystems.jmxbuilder.model.User;
import org.junit.jupiter.api.Test;

import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import static org.assertj.core.api.Assertions.assertThat;

public class AttributeInfoTests {

    private OpenTypeMapper openTypeMapper = OpenTypeMapper.getInstance();

    @Test
    public void testReadOnlyAttribute() {
        Address address = new Address("street1", "city", "state");
        User user = new User("name", 12, address);

        AttributeInfo info = AttributeInfo.builder(String.class).withName("name")
                .withDescription("User name")
                .withSupplier(user::getName)
                .build();

        MBeanAttributeInfo nameAttributeValue = new MBeanAttributeInfo("name", "java.lang.String", "User name", true, false, false, stringDescriptor());
        assertThat(info.getMBeanAttributeInfo()).isEqualTo(nameAttributeValue);
    }

    @Test
    public void testWriteOnlyAttribute() {
        Address address = new Address("street1", "city", "state");
        User user = new User("name", 12, address);

        AttributeInfo info = AttributeInfo.builder(String.class).withName("name")
                .withDescription("User name")
                .withConsumer(user::setName)
                .build();

        MBeanAttributeInfo nameAttributeValue = new MBeanAttributeInfo("name", "java.lang.String", "User name", false, true, false, stringDescriptor());
        assertThat(info.getMBeanAttributeInfo()).isEqualTo(nameAttributeValue);
    }

    @Test
    public void testReadWriteAttribute() {
        Address address = new Address("street1", "city", "state");
        User user = new User("name", 12, address);

        AttributeInfo info = AttributeInfo.builder(String.class).withName("name")
                .withDescription("User name")
                .withSupplier(user::getName)
                .withConsumer(user::setName)
                .build();

        OpenType<String> stringOpenType = openTypeMapper.fromClass(String.class);
        MBeanAttributeInfo nameAttributeValue = new MBeanAttributeInfo("name", "java.lang.String", "User name", true, true, false, stringDescriptor());
        assertThat(info.getMBeanAttributeInfo()).isEqualTo(nameAttributeValue);
    }

    @Test
    public void testReadWriteAttributeWithDescriptor() {
        Address address = new Address("street1", "city", "state");
        User user = new User("name", 12, address);

        Descriptor ageDescriptor = DescriptorSupport.builder().withMinValue(0).withMaxValue(120).withUnits("years").build();

        AttributeInfo<Integer> info = AttributeInfo.builder(Integer.TYPE).withName("age")
                .withDescription("Age")
                .withSupplier(user::getAge)
                .withConsumer(user::setAge)
                .withDescriptor(ageDescriptor)
                .build();

        OpenType<Integer> integerOpenType = openTypeMapper.fromClass(Integer.class);
        MBeanAttributeInfo ageAttributeValue = new MBeanAttributeInfo("age", "int", "Age", true, true, false,
                DescriptorSupport.builder()
                .withDescriptor(ageDescriptor)
                .withDescriptor(intDescriptor()).build());
        assertThat(info.getMBeanAttributeInfo()).isEqualTo(ageAttributeValue);
    }

    @Test
    public void testReadWriteWithBoolean() {
        ExampleService service = new ExampleService();
        AttributeInfo<Boolean> info = AttributeInfo.builder(Boolean.TYPE)
                .withName("debugEnabled")
                .withDescription("Debug Description")
                .withBeanProperty(service, "debugEnabled")
                .build();

        MBeanAttributeInfo actual = new MBeanAttributeInfo(
                "debugEnabled",
                "boolean",
                "Debug Description",
                true, true, false, booleanDescriptor());
        assertThat(info.getMBeanAttributeInfo()).isEqualTo(actual);
    }

    private Descriptor stringDescriptor() {
        Descriptor nameDescriptor = new javax.management.modelmbean.DescriptorSupport();
        nameDescriptor.setField("openType", SimpleType.STRING);
        nameDescriptor.setField("originalType", "java.lang.String");
        nameDescriptor.setField("enabled", true);
        return nameDescriptor;
    }

    private Descriptor booleanDescriptor() {
        Descriptor nameDescriptor = new javax.management.modelmbean.DescriptorSupport();
        nameDescriptor.setField("openType", SimpleType.BOOLEAN);
        nameDescriptor.setField("originalType", "boolean");
        nameDescriptor.setField("enabled", true);
        return nameDescriptor;
    }


    private Descriptor intDescriptor() {
        Descriptor nameDescriptor = new javax.management.modelmbean.DescriptorSupport();
        nameDescriptor.setField("openType", SimpleType.INTEGER);
        nameDescriptor.setField("originalType", "int");
        nameDescriptor.setField("enabled", true);
        return nameDescriptor;
    }
}
