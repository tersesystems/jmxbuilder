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

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.function.Supplier;

public class App {
    static final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    static final CompositeDataWriter<Address> addressWriter = CompositeDataWriter.builder(Address.class)
            .withTypeName("address")
            .withTypeDescription("Address")
            .withSimpleAttribute("street1", "Street 1", Address::getStreet1)
            .withSimpleAttribute("city", "City", Address::getCity)
            .withSimpleAttribute("state", "State", Address::getState)
            .build();

    static final CompositeDataWriter<User> userWriter = CompositeDataWriter.builder(User.class)
            .withTypeName("user")
            .withTypeDescription("User")
            .withSimpleAttribute("name", "Name", User::getName)
            .withSimpleAttribute("age", "Age", User::getAge)
            .withCompositeAttribute("address", "Address", User::getAddress, addressWriter).build();

    static final TabularDataWriter<User> usersWriter = TabularDataWriter.builder(User.class)
            .withTypeName("users")
            .withTypeDescription("Users")
            .withIndexName("name")
            .withCompositeDataWriter(userWriter)
            .build();

    public static void main(String[] args) {
        try {
            compositeBean();
            tabularBean();
            dynamicBean();
            exampleBean();

            Scanner scanner = new Scanner(System.in);
            try {
                //noinspection InfiniteLoopStatement
                while (true) {
                    System.out.println("Waiting for input");
                    String line = scanner.nextLine();
                    System.out.println("Closing...");
                }
            } catch (IllegalStateException | NoSuchElementException e) {
                // System.in has been closed
                System.out.println("System.in was closed; exiting");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void dynamicBean() throws Exception {
        Address address = new Address("street1", "city", "state");
        final User user = new User("name", 12, address);

        final DynamicMBean userBean = new DynamicBean.Builder()
                .withSimpleAttribute("name", "User name", user::getName, user::setName)
                .withSimpleAttribute("age", "User Age", user::getAge, user::setAge, DescriptorSupport.builder().withUnits("years").build())
                .withCompositeAttribute("address", "User Address", user::getAddress, addressWriter)
                .withOperation("ping", "Ping the user", user::ping)
                .withOperation("pong", "Pong the user", user::pong, "arg1")
                .withOperation("concatenate", "Concatenate", user::concatenate, "arg1", "arg2")
                .withOperation("callMethod", "Call method", user, "callMethod",
                        ParameterInfo.builder().withClassType(String.class).withName("arg1").build(),
                        ParameterInfo.builder().withClassType(String.class).withName("arg2").build(),
                        ParameterInfo.builder().withClassType(String.class).withName("arg3").build(),
                        ParameterInfo.builder().withClassType(String.class).withName("arg4").build()
                )
                .withOperation(OperationInfo.builder()
                        .withName("notificationCallback")
                        .withMethod(() -> "this is also used as a callback")
                        .withCompletionNotification((source, returnValue) -> {
                            Notification notification = new Notification("jmx.operation.completion", source, System.currentTimeMillis());
                            notification.setUserData(returnValue);
                            return notification;
                        })
                        .build())
                .build();

        ObjectName objectName = new ObjectName("com.tersesystems:type=UserBean,name=User");
        mBeanServer.registerMBean(userBean, objectName);
    }

    public static void compositeBean() throws Exception {
        final Address address = new Address("street1", "city", "state");
        final User user = new User("name", 12, address);
        final CompositeItemBean<User> compositeItemBean = new CompositeItemBean<>(userWriter, () -> user);

        final DynamicMBean userBean = new DynamicBean.Builder()
                .withSimpleAttribute("User", "User", compositeItemBean::getItem)
                .build();

        ObjectName objectName = new ObjectName("com.tersesystems:type=DynamicBean,name=CompositeBean");
        mBeanServer.registerMBean(userBean, objectName);

        final CompositeData userItem = (CompositeData) mBeanServer.getAttribute(objectName, "User");
        printUser(userItem);
    }

    public static void tabularBean() throws Exception {
        final Address address = new Address("street1", "city", "state");
        final List<User> usersList = Collections.singletonList(new User("name", 12, address));

        TabularItemBean<User> tabularItemBean = new TabularItemBean<>(usersWriter, () -> usersList);
        final DynamicMBean usersBean = new DynamicBean.Builder()
                .withSimpleAttribute("Users", "Users Table", tabularItemBean::getItem)
                .build();

        ObjectName objectName = new ObjectName("com.tersesystems:type=DynamicBean,name=TabularBean");
        mBeanServer.registerMBean(usersBean, objectName);

        final TabularData users = (TabularData) mBeanServer.getAttribute(objectName, "Users");
        for (Object row : users.values()) {
            printUser((CompositeData) row);
        }
    }

    public static void exampleBean() throws Exception {
        ExampleService service = new ExampleService();
        final DynamicMBean serviceBean = new DynamicBean.Builder()
                .withSimpleAttribute(
                        "debugEnabled",
                        "",
                        service::isDebugEnabled,
                        service::setDebugEnabled,
                        DescriptorSupport.builder().withImmutableInfo(false).build())
                .build();

        ObjectName objectName = new ObjectName("com.tersesystems:type=ServiceBean,name=ServiceBean");
        mBeanServer.registerMBean(serviceBean, objectName);
    }

    public static void printUser(CompositeData user) {
        for (Object value : user.values()) {
            System.out.println(value);
        }
        final CompositeData address1 = (CompositeData) user.get("address");
        final Collection<?> values = address1.values();
        for (Object value : values) {
            System.out.println(value);
        }
    }

    static class TabularItemBean<T> {
        private final Supplier<Iterable<T>> itemsSupplier;
        private final TabularDataWriter<T> builder;

        public TabularItemBean(TabularDataWriter<T> builder, Supplier<Iterable<T>> items) {
            this.builder = builder;
            this.itemsSupplier = items;
        }

        public TabularData getItem() {
            return builder.apply(itemsSupplier.get());
        }
    }

    static class CompositeItemBean<T> {
        private final CompositeDataWriter<T> compositeBuilder;
        private final Supplier<T> itemSupplier;

        public CompositeItemBean(CompositeDataWriter<T> compositeBuilder, Supplier<T> itemSupplier) {
            this.itemSupplier = itemSupplier;
            this.compositeBuilder = compositeBuilder;
        }

        public CompositeData getItem() {
            return compositeBuilder.apply(itemSupplier.get());
        }
    }
}

