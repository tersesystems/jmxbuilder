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
import com.tersesystems.jmxbuilder.model.User;
import org.junit.jupiter.api.Test;

import javax.management.Descriptor;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenType;

import static org.assertj.core.api.Assertions.assertThat;

public class AttributeInfoTests {

    private OpenTypeMapper openTypeMapper = OpenTypeMapper.getInstance();

    @Test
    public void testDynamicBeanWithReadOnlyAttribute() {
        Address address = new Address("street1", "city", "state");
        User user = new User("name", 12, address);

        final DynamicMBean userBean = new DynamicBean.Builder()
                .withSimpleAttribute("name", "User name", user::getName)
                .build();


        OpenType<String> stringOpenType = openTypeMapper.fromClass(String.class);
        MBeanInfo mBeanInfo = userBean.getMBeanInfo();
        MBeanAttributeInfo nameAttributeValue = new OpenMBeanAttributeInfoSupport("name", "user name", stringOpenType,
                true, false, false);
        assertThat(mBeanInfo.getAttributes()).contains(nameAttributeValue);
    }

    @Test
    public void testDynamicBeanWithWriteOnlyAttribute() {
        Address address = new Address("street1", "city", "state");
        User user = new User("name", 12, address);

        final DynamicMBean userBean = new DynamicBean.Builder()
                .withSimpleAttribute("name", "User name", user::setName)
                .build();

        OpenType<String> stringOpenType = openTypeMapper.fromClass(String.class);
        MBeanInfo mBeanInfo = userBean.getMBeanInfo();
        MBeanAttributeInfo nameAttributeValue = new OpenMBeanAttributeInfoSupport("name", "User name", stringOpenType, false, true, false);
        assertThat(mBeanInfo.getAttributes()).contains(nameAttributeValue);
    }

    @Test
    public void testDynamicBeanWithReadWriteAttribute() {
        Address address = new Address("street1", "city", "state");
        User user = new User("name", 12, address);

        final DynamicMBean userBean = new DynamicBean.Builder()
                .withSimpleAttribute("name", "User name", user::getName, user::setName)
                .build();

        OpenType<String> stringOpenType = openTypeMapper.fromClass(String.class);
        MBeanInfo mBeanInfo = userBean.getMBeanInfo();
        MBeanAttributeInfo nameAttributeValue = new OpenMBeanAttributeInfoSupport("name",  "User name", stringOpenType, true, true, false);
        assertThat(mBeanInfo.getAttributes()).contains(nameAttributeValue);
    }

    @Test
    public void testDynamicBeanWithReadWriteAttributeWithDescriptor() {
        Address address = new Address("street1", "city", "state");
        User user = new User("name", 12, address);

        Descriptor ageDescriptor = DescriptorSupport.builder().withMinValue(0).withMaxValue(120).withUnits("years").build();

        final DynamicMBean userBean = new DynamicBean.Builder()
                .withSimpleAttribute("age", "Age", user::getAge, user::setAge, ageDescriptor)
                .build();

        OpenType<Integer> integerOpenType = openTypeMapper.fromClass(Integer.class);
        MBeanInfo mBeanInfo = userBean.getMBeanInfo();
        MBeanAttributeInfo ageAttributeValue = new OpenMBeanAttributeInfoSupport("age", "Age", integerOpenType, true, true, false, ageDescriptor);
        assertThat(mBeanInfo.getAttributes()).contains(ageAttributeValue);
    }

}
