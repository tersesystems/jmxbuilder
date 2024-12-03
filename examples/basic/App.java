///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.tersesystems.jmxbuilder:jmxbuilder:0.0.5
//DEPS net.jodah:typetools:0.6.3
//RUNTIME_OPTIONS --add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED --add-exports=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED

package example;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.lang.management.ManagementFactory;
import java.util.*;

import static java.lang.String.format;

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

class Address {
    private final String street1;
    private final String city;
    private final String state;

    public Address(String street1, String city, String state) {
        this.street1 = street1;
        this.city = city;
        this.state = state;
    }

    public String getStreet1() {
        return street1;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    @Override
    public String toString() {
        return format("Address(%s, %s, %s)", street1, city, state);
    }
}

class ExampleService {
    private boolean debug;

    public String dump() {
        return ("Dumping contents");
    }

    public boolean isDebugEnabled() {
        return debug;
    }

    public void setDebugEnabled(boolean debug) {
        this.debug = debug;
    }
}


class User {
    private String name;
    private Integer age;
    private final Address address;

    public User(String name, int age, Address address) {
        this.name = name;
        this.age = age;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Address getAddress() {
        return address;
    }

    public void ping() {
        System.out.println("ping!");
    }

    @Override
    public String toString() {
        return format("User(%s, %n, %s)", name, age, address);
    }

    public String pong(String pong) {
        return pong;
    }

    public String concatenate(String s1, String s2) {
        return s1 + s2;
    }

    public String callMethod(String s1, String s2, String s3, String s4) {
        return "method called";
    }
}
