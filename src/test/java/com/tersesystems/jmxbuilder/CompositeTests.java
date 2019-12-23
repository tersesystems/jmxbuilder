/**
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

import org.junit.jupiter.api.Test;

import javax.management.Descriptor;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularData;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CompositeTests {

    private final OpenTypeMapper openTypeMapper = OpenTypeMapper.getInstance();
    OpenType<CompositeData> openType = openTypeMapper.fromClass(CompositeData.class);

    @Test
    public void testDynamicBeanWithCompositeAttribute() {
        Address address = new Address("street1", "city", "state");
        User user = new User("name", 12, address);

        final CompositeDataWriter<Address> addressBuilder = CompositeDataWriter.<Address>builder()
                .withTypeName("address")
                .withTypeDescription("Address")
                .withSimpleAttribute("street1", "Street 1", Address::getStreet1)
                .withSimpleAttribute("city", "City", Address::getCity)
                .withSimpleAttribute("state", "State", Address::getState)
                .build();

        final DynamicMBean userBean = new DynamicBean.Builder()
                .withCompositeAttribute("address", "Address", user::getAddress, addressBuilder)
                .build();


        MBeanInfo mBeanInfo = userBean.getMBeanInfo();
        MBeanAttributeInfo addressAttributeValue = new OpenMBeanAttributeInfoSupport("address",  "Address", openType, true, false, false);
        assertThat(mBeanInfo.getAttributes()).contains(addressAttributeValue);
    }

    @Test
    public void testDynamicBeanWithCompositeAttributeDescriptor() {
        Address address = new Address("street1", "city", "state");
        User user = new User("name", 12, address);

        final CompositeDataWriter<Address> addressComposite = CompositeDataWriter.<Address>builder()
                .withTypeName("address")
                .withTypeDescription("Address")
                .withSimpleAttribute("street1", "Street 1", Address::getStreet1)
                .withSimpleAttribute("city", "City", Address::getCity)
                .withSimpleAttribute("state", "State", Address::getState)
                .build();

        Descriptor addressDescriptor = DescriptorSupport.builder().withSince("1.0").withLocale("en-US").build();

        final DynamicMBean userBean = new DynamicBean.Builder()
                .withCompositeAttribute("address", "Address", user::getAddress, addressComposite, addressDescriptor)
                .build();

        MBeanInfo mBeanInfo = userBean.getMBeanInfo();
        MBeanAttributeInfo addressAttributeValue = new OpenMBeanAttributeInfoSupport("address", "Address", openType, true, false, false, addressDescriptor);
        assertThat(mBeanInfo.getAttributes()).contains(addressAttributeValue);
    }

    @Test
    public void testDynamicBeanWithTabularAttribute() {
        CompositeDataWriter<Address> addressWriter = CompositeDataWriter.<Address>builder()
                .withTypeName("address")
                .withTypeDescription( "Address")
                .withSimpleAttribute("street1", "Street 1", Address::getStreet1)
                .withSimpleAttribute("city", "City", Address::getCity)
                .withSimpleAttribute("state", "State", Address::getState)
                .build();

        final TabularDataWriter<Address> addressesWriter = TabularDataWriter.<Address>builder()
                .withTypeName("addresses")
                .withTypeDescription( "Addresses")
                .withIndexName("street1")
                .withCompositeDataWriter(addressWriter)
                .build();

        Address address1 = new Address("street1", "city", "state");
        Address address2 = new Address("street2", "city", "state");
        Address address3 = new Address("street3", "city", "state");
        List<Address> addresses = Arrays.asList(address1, address2, address3);

        final DynamicMBean userBean = new DynamicBean.Builder()
                .withTabularAttribute("addresses", "Addresses", () -> addresses, addressesWriter)
                .build();
        MBeanInfo mBeanInfo = userBean.getMBeanInfo();
        OpenType<TabularData> tabularDataOpenType = openTypeMapper.fromClass(TabularData.class);
        MBeanAttributeInfo addressAttributeValue = new OpenMBeanAttributeInfoSupport("addresses", "Addresses", tabularDataOpenType, true, false, false);
        assertThat(mBeanInfo.getAttributes()).contains(addressAttributeValue);
    }
}
