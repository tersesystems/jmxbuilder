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
package example;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.lang.management.ManagementFactory;
import java.util.*;

import com.tersesystems.jmxbuilder.*;

public class App {
    static final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    static final CompositeDataWriter<Address> addressWriter = CompositeDataWriter.builder(Address.class)
            .withTypeName("address")
            .withTypeDescription("Address")
            .withSimpleAttribute(String.class, "street1", "Street 1", Address::getStreet1)
            .withSimpleAttribute("city", Address::getCity)
            .withSimpleAttribute("state", Address::getState)
            .build();

    static final CompositeDataWriter<User> userWriter = CompositeDataWriter.builder(User.class)
            .withTypeName("user")
            .withTypeDescription("User")
            .withSimpleAttribute("name", User::getName)
            .withSimpleAttribute("age", User::getAge)
            .withCompositeAttribute("address", user -> user.getAddress(), addressWriter).build();

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
            //noinspection InfiniteLoopStatement
            while (true) {
                try {
                    //System.out.println("Waiting for input");
                    String line = scanner.nextLine();
                } catch (IllegalStateException | NoSuchElementException e) {
                    // System.in has been closed
                    //e.printStackTrace();
                    //System.out.println("System.in was closed; exiting");
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void dynamicBean() throws Exception {
        Address address = new Address("street1", "city", "state");
        final User user = new User("name", 12, address);

        final DynamicMBean userBean = DynamicBean.builder()
                .withSimpleAttribute("name", user::getName, user::setName)
                .withSimpleAttribute(Integer.TYPE, "age", user::getAge, user::setAge)
                .withCompositeAttribute("address", user::getAddress, addressWriter)
                .withOperation("ping", user::ping)
                .withOperation("pong", user::pong, "arg1")
                .withOperation("concatenate", user::concatenate, "arg1", "arg2")
                .withOperation("callMethod", user, "callMethod",
                        ParameterInfo.builder().withClassType(String.class).withName("arg1").build(),
                        ParameterInfo.builder().withClassType(String.class).withName("arg2").build(),
                        ParameterInfo.builder().withClassType(String.class).withName("arg3").build(),
                        ParameterInfo.builder().withClassType(String.class).withName("arg4").build()
                )
                .withOperation(OperationInfo.builder()
                        .withName("notificationCallback")
                        .withSupplier(() -> "this is also used as a callback")
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
        final DynamicMBean userBean = DynamicBean.builder()
                .withCompositeAttribute("User", () -> user, userWriter)
                .build();

        ObjectName objectName = new ObjectName("com.tersesystems:type=DynamicBean,name=CompositeBean");
        mBeanServer.registerMBean(userBean, objectName);

        final CompositeData userItem = (CompositeData) mBeanServer.getAttribute(objectName, "User");
        printUser(userItem);
    }

    public static void tabularBean() throws Exception {
        final Address address = new Address("street1", "city", "state");
        final List<User> usersList = Collections.singletonList(new User("name", 12, address));

        final DynamicMBean usersBean = DynamicBean.builder()
                .withTabularAttribute("Users", () -> usersList, usersWriter)
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
        final DynamicMBean serviceBean = DynamicBean.builder()
                .withSimpleAttribute(Boolean.TYPE,
                        "debugEnabled",
                        service::isDebugEnabled,
                        service::setDebugEnabled)
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

}

