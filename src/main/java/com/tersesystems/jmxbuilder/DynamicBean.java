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

import net.jodah.typetools.TypeResolver;

import javax.management.*;
import javax.management.openmbean.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A dynamic bean that extends DynamicMBean and can be constructed through a fluent builder pattern.
 * <p>
 * {@code <pre>
 * public class Service {
 *     public void createServiceBean() {
 *         final DynamicBean serviceBean = DynamicBean.builder()
 *                 .withOperation("dump", this::dump)
 *                 .build();
 *
 *         MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
 *         ObjectName objectName = new ObjectName("com.tersesystems.jmxbuilder:type=ServiceBean,name=ThisServiceBean");
 *         mBeanServer.registerMBean(serviceBean, objectName);
 *     }
 *
 *     private void dump() {
 *        System.out.println("dump internal information");
 *     }
 * }
 * </pre>}
 * <p>
 * See https://github.com/tersesystems/jmxbuilder for details.
 */
public class DynamicBean implements DynamicMBean, NotificationEmitter {
    private final MBeanInfo info;
    private final Builder.AttributeInfos attributeInfos;
    private final Builder.OperationInfos operationInfos;
    private final Builder.NotificationInfos notificationInfos;
    private final NotificationBroadcasterSupport notifier;

    /**
     * You should start here.
     *
     * @return a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    public DynamicBean(MBeanInfo info, Builder.AttributeInfos attributeInfos, Builder.OperationInfos operationInfos, Builder.NotificationInfos notificationInfos) {
        this.info = requireNonNull(info);
        this.attributeInfos = requireNonNull(attributeInfos);
        this.operationInfos = requireNonNull(operationInfos);
        this.notificationInfos = requireNonNull(notificationInfos);
        this.notifier = new NotificationBroadcasterSupport(notificationInfos.getExecutor(), notificationInfos.getMBeanNotificationInfos());
    }

    @Override
    public Object getAttribute(String name) throws AttributeNotFoundException {
        Optional<Object> o = attributeInfos.find(name).flatMap(attributeInfo -> (Optional<Object>) attributeInfo.get());
        return o.orElseThrow(() -> new AttributeNotFoundException(String.format("Attribute %s not found", name)));
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException {
        // I really wish Attribute had a type.
        Optional<AttributeInfo<?>> optionalAttributeInfo = attributeInfos.find(attribute);
        if (optionalAttributeInfo.isPresent()) {
            @SuppressWarnings("unchecked")
            AttributeInfo<Object> attributeInfo = (AttributeInfo<Object>) optionalAttributeInfo.get();
            Consumer<Function<Object, Notification>> c = f -> notifier.sendNotification(f.apply(DynamicBean.this));
            attributeInfo.set(attribute.getValue(), c);
        } else {
            String msg = String.format("Cannot find %s", attribute.getName());
            throw new AttributeNotFoundException(msg);
        }
    }

    @Override
    public AttributeList getAttributes(String[] names) {
        AttributeList list = new AttributeList();
        for (String name : names) {
            try {
                Object value = getAttribute(name);
                if (value != null) {
                    list.add(new Attribute(name, value));
                }
            } catch (Exception e) {
                // OK: attribute is not included in returned list, per spec
                // XXX: log the exception
            }
        }
        return list;
    }

    @Override
    public AttributeList setAttributes(AttributeList list) {
        // XXX should use a fold here
        AttributeList retlist = new AttributeList();
        for (Attribute attr : list.asList()) {
            Optional<AttributeInfo<?>> o = attributeInfos.find(attr.getName());
            if (o.isPresent()) {
                @SuppressWarnings("unchecked")
                AttributeInfo<Object> attributeInfo = (AttributeInfo<Object>) o.get();
                Object value = attr.getValue();
                Consumer<Function<Object, Notification>> c = f -> notifier.sendNotification(f.apply(DynamicBean.this));
                attributeInfo.set(value, c);
                retlist.add(attr);
            } else {
                //String msg = String.format("Cannot find %s in attributeInfos", attr.getName());
                //throw new AttributeNotFoundException(msg);
            }
        }
        return retlist;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        return operationInfos.invoke(this, actionName, params, signature);
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return info;
    }

    @Override
    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
        notifier.removeNotificationListener(listener, filter, handback);
    }

    @Override
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws IllegalArgumentException {
        notifier.addNotificationListener(listener, filter, handback);
    }

    @Override
    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        notifier.removeNotificationListener(listener);
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return notificationInfos.getMBeanNotificationInfos();
    }

    public void sendNotification(Notification notification) {
        notifier.sendNotification(notification);
    }

    /**
     * The builder.  You should call {@code DynamicBean.builder()} to return a fresh instance of this class.
     */
    public static class Builder {
        private final AttributeInfos attributeInfos = new AttributeInfos();
        private final OperationInfos operationInfos = new OperationInfos();
        private final NotificationInfos notificationInfos = new NotificationInfos();

        // The Descriptor for the MBeanInfo will have a field `mxbean` whose value is the string "true".
        private final DescriptorSupport.Builder descriptorBuilder = DescriptorSupport.builder()
                .withField(" mxbean", "true");

        private String className = "";
        private String description = "";

        Builder() {
        }

        /**
         * Sets a description on the bean.
         * @param description self explanatory.
         * @return builder.
         */
        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the "class name" of the builder.  This is optional and you probably won't use this.
         * @param className any random string.
         * @return builder.
         */
        public Builder withClassName(String className) {
            this.className = className;
            return this;
        }

        /**
         * Uses javabean property to pull the attribute out, given a javabean property name and the class of the property.
         *
         * If this is a primitive use @{ClassName.TYPE} i.e. if {@code boolean} use Boolean.TYPE instead of Boolean.class.
         *
         * @param attributeType the attribute's class.  If this is a primitive use @{ClassName.TYPE}.
         * @param name the name of the attribute.
         * @param object the object to call the bean property getter/setter on.
         * @param propertyName the javabean property that will have getters/setters on the object.
         * @return the builder.
         * @param <T> the type of the class.
         */
        public <T> Builder withBeanAttribute(Class<T> attributeType, String name, Object object, String propertyName) {
            AttributeInfo<T> info = AttributeInfo.builder(attributeType).withName(name)
                    .withDescription(name)
                    .withBeanProperty(object, propertyName)
                    .build();
            return withAttribute(info);
        }

        /**
         * A simple attribute that will use a supplier, and will infer the type from the supplier.
         *
         * @param name the name of the attribute.
         * @param getter the supplier that returns a value.
         * @param <T> the type of the getter
         * @return the builder.
         */
        public <T> Builder withSimpleAttribute(String name, Supplier<T> getter) {
            Class<T> attributeType = (Class<T>) TypeResolver.resolveRawArgument(Supplier.class, getter.getClass());
            return withSimpleAttribute(attributeType, name, getter);
        }

        /**
         * A simple attribute that will use a supplier with an explicit type.
         *
         * @param attributeType the attribute's class.  If this is a primitive use @{ClassName.TYPE}.
         * @param name the name of the attribute.
         * @param getter the supplier that returns a value.
         * @param <T> the type of the getter
         * @return the builder.
         */
        public <T> Builder withSimpleAttribute(Class<T> attributeType, String name, Supplier<T> getter) {
            AttributeInfo<T> info = AttributeInfo.builder(attributeType).withName(name)
                    .withDescription(name)
                    .withSupplier(getter)
                    .build();
            return withAttribute(info);
        }

        /**
         * Create a write only attribute using inference.
         *
         * @param name the name of the attribute.
         * @param setter the consumer that accepts an attribute change.
         * @param <T> the type of the setter
         * @return the builder.
         */
        public <T> Builder withSimpleAttribute(String name, Consumer<T> setter) {
            Class<T> attributeType = (Class<T>) TypeResolver.resolveRawArgument(Consumer.class, setter.getClass());
            return withSimpleAttribute(attributeType, name, setter);
        }

        /**
         * Create a write only attribute with an explicit class.
         *
         * @param attributeType the attribute's class.  If this is a primitive use @{ClassName.TYPE}.
         * @param name the name of the attribute.
         * @param setter the consumer that accepts an attribute change.
         * @param <T> the type of the setter
         * @return the builder.
         */
        public <T> Builder withSimpleAttribute(Class<T> attributeType, String name, Consumer<T> setter) {
            AttributeInfo<T> info = AttributeInfo.builder(attributeType).withName(name)
                    .withDescription(name)
                    .withConsumer(setter)
                    .build();
            return withAttribute(info);
        }

        /**
         * Create a write only attribute using inference and a descriptor.
         *
         * @param name the name of the attribute.
         * @param getter      the "getter"
         * @param setter the "setter" that accepts an attribute change.
         * @param <T> the type of the setter
         * @return the builder.
         */
        public <T> Builder withSimpleAttribute(String name, Supplier<T> getter, Consumer<T> setter) {
            Class<T> attributeType = (Class<T>) TypeResolver.resolveRawArgument(Supplier.class, getter.getClass());
            return withSimpleAttribute(attributeType, name, getter, setter);
        }

        /**
         * Create a write only attribute using inference and a descriptor.
         *
         * @param name the name of the attribute.
         * @param reader      the "getter"
         * @param setter the "setter" that accepts an attribute change.
         * @param <T> the type of the getter/setter
         * @return the builder.
         */
        public <T> Builder withSimpleAttribute(Class<T> attributeType, String name, Supplier<T> reader, Consumer<T> setter) {
            AttributeInfo<T> info = AttributeInfo.builder(attributeType).withName(name)
                    .withDescription(name)
                    .withSupplier(reader)
                    .withConsumer(setter)
                    .build();
            return withAttribute(info);
        }

        /**
         * Creates a composite attribute using a getter.
         *
         * @param name name of the attribute.
         * @param getter the name of the getter.
         * @param compositeWriter the composite data writer that turns the getter from T into a CompositeData.
         * @param <T> the type of the getter.
         * @return builder.
         */
        public <T> Builder withCompositeAttribute(String name, Supplier<T> getter, CompositeDataWriter<T> compositeWriter) {
            AttributeInfo<CompositeData> info = AttributeInfo.builder(CompositeData.class).withName(name)
                    .withDescription(name)
                    .withSupplier(() -> compositeWriter.apply(getter.get()))
                    .build();
            return withAttribute(info);
        }

        /**
         * Creates a tabular attribute using a iterableGetter that returns an iterable and a tabular data writer.
         *
         * @param name the attribute name.
         * @param iterableGetter returns a getter that has an iterable of T, typically a {@code java.util.List}
         * @param tabularDataWriter the tabular data writer
         * @param <T> the type of the element in the iterable.
         * @return builder.
         */
        public <T> Builder withTabularAttribute(String name, Supplier<Iterable<T>> iterableGetter, TabularDataWriter<T> tabularDataWriter) {
            AttributeInfo<TabularData> info = AttributeInfo.builder(TabularData.class)
                    .withName(name)
                    .withDescription(name)
                    .withSupplier(() -> tabularDataWriter.apply(iterableGetter.get()))
                    .build();
            return withAttribute(info);
        }

        /**
         * Create an attribute from an AttributeInfo.  You should use {@code AttributeInfo.builder()} to create the info.
         *
         * This is useful when you want to do more complex stuff like to set the description or the descriptor on the attribute.
         *
         * @param info an AttributeInfo instance.
         * @param <T> the type of attribute info.
         * @return builder.
         */
        public <T> Builder withAttribute(AttributeInfo<T> info) {
            attributeInfos.add(info);
            return this;
        }

        /**
         * Creates an operation from a runnable.
         *
         * @param name     the name of the operation.
         * @param runnable the runnable instance that will be run.
         * @return builder.
         */
        public Builder withOperation(String name, Runnable runnable) {
            OperationInfo info = OperationInfo.builder()
                    .withName(name)
                    .withDescription(name)
                    .withMethod(runnable)
                    .build();

            return withOperation(info);
        }

        /**
         * Creates an operation from a callable.
         *
         * @param name     the name of the operation.
         * @param callable the runnable instance that will be run.
         * @param paramName the parameter name.
         * @return builder.
         */
        public <V> Builder withOperation(String name, Callable<V> callable, String paramName) {
            OperationInfo info = OperationInfo.builder()
                    .withName(name)
                    .withDescription(name)
                    .withMethod(callable, paramName)
                    .build();
            return withOperation(info);
        }

        /**
         * Creates an operation from a callable.
         *
         * @param name     the name of the operation.
         * @param callable the runnable instance that will be run.
         * @param paramInfo the parameter info.
         * @return builder.
         */
        public <V> Builder withOperation(String name, Callable<V> callable, ParameterInfo<V> paramInfo) {
            OperationInfo info = OperationInfo.builder()
                    .withName(name)
                    .withDescription(name)
                    .withMethod(callable, paramInfo)
                    .build();
            return withOperation(info);
        }

        /**
         * Creates an operation from a function.
         *
         * @param name the name of the operation
         * @param f the function.
         * @param paramName parameter name to function.
         * @param <T> input into function
         * @param <R> output from function.
         * @return builder.
         */
        public <T, R> Builder withOperation(String name, Function<T, R> f, String paramName) {
            OperationInfo info = OperationInfo.builder()
                    .withName(name)
                    .withDescription(name)
                    .withMethod(f, paramName)
                    .build();

            return withOperation(info);
        }

        /**
         * Creates an operation from a function.
         *
         * @param name the name of the operation
         * @param f the function.
         * @param parameterInfo parameter info to function.
         * @param <T> input into function
         * @param <R> output from function.
         * @return builder.
         */
        public <T, R> Builder withOperation(String name, Function<T, R> f, ParameterInfo<T> parameterInfo) {
            OperationInfo info = OperationInfo.builder()
                    .withName(name)
                    .withDescription(name)
                    .withMethod(f, parameterInfo)
                    .build();

            return withOperation(info);
        }

        /**
         * Creates an operation from a bifunction.
         *
         * @param name the name of the operation
         * @param f the function.
         * @param paramName1 parameter info to function.
         * @param paramName2 parameter info to function.
         * @param <T> input into function
         * @param <R> output from function.
         * @return builder.
         */
        public <T, U, R> Builder withOperation(String name, BiFunction<T, U, R> f, String paramName1, String paramName2) {
            OperationInfo info = OperationInfo.builder()
                    .withName(name)
                    .withDescription(name)
                    .withMethod(f, paramName1, paramName2)
                    .build();

            return withOperation(info);
        }

        /**
         * Creates an operation from a bifunction.
         *
         * @param name the name of the operation
         * @param f the bifunction.
         * @param param1 parameter info to bifunction.
         * @param param2 parameter info to bifunction.
         * @param <T> input1 into function
         * @param <U> input2 into function
         * @param <R> output from function.
         * @return builder.
         */
        public <T, U, R> Builder withOperation(String name, BiFunction<T, U, R> f, ParameterInfo<T> param1, ParameterInfo<U> param2) {
            OperationInfo info = OperationInfo.builder()
                    .withName(name)
                    .withDescription(name)
                    .withMethod(f, new ParameterInfo[]{param1, param2})
                    .build();

            return withOperation(info);
        }

        /**
         * Calls a method on the object with the given parameters through reflection.
         *
         * @param name           the name of the operation
         * @param object         the object, i.e. the user
         * @param methodName     the method name itself, for reflection purposes.
         * @param parameterInfos the method signature, if any (i.e. getLogger(String loggerName) would have a single String parameter info)
         * @param <T>            the type of object
         * @return the dynamic bean builder.
         */
        public <T> Builder withOperation(String name, T object, String methodName, ParameterInfo... parameterInfos) {
            OperationInfo info = OperationInfo.builder()
                    .withName(name)
                    .withDescription(name)
                    .withReflection(object, methodName, parameterInfos)
                    .build();

            return withOperation(info);
        }

        /**
         * Creates an operation from an operation info.  Use {@code OperationInfo.builder()} to build the info.
         *
         * Useful when you want to do things like setting the description or the descriptor.
         *
         * @param info the operation info.
         * @return builder.
         */
        public Builder withOperation(OperationInfo info) {
            operationInfos.add(info);
            return this;
        }

        /**
         * Creates a notification of type.
         *
         * @param name  the name of the notification
         * @param types the types of notifications.
         * @return builder.
         */
        public Builder withNotification(String name, String... types) {
            NotificationInfo info = NotificationInfo.builder()
                    .withName(name)
                    .withTypes(types)
                    .withDescription(name)
                    .build();
            return withNotification(info);
        }

        /**
         * Creates a notification from a notification info.
         *
         * Create the info from {@code NotificationInfo.builder()}.
         *
         * @param notificationInfo info
         * @return builder.
         */
        public Builder withNotification(NotificationInfo notificationInfo) {
            notificationInfos.add(notificationInfo);
            return this;
        }

        /**
         * Sets attribute change notifications on this dynamic bean.
         *
         * @return builder.
         */
        public Builder withAttributeChangeNotifications() {
            withNotification(AttributeChangeNotification.class.getName(), "An attribute of this MBean has changed", AttributeChangeNotification.ATTRIBUTE_CHANGE);
            return this;
        }

        /**
         * Sets up the dynamic bean as explicitly immutable.
         *
         * @return builder.
         */
        public Builder withImmutableInfo() {
            descriptorBuilder.withImmutableInfo(true);
            return this;
        }

        /**
         * Sets a descriptor on this dynamic bean.  Note that this adds to rather than replaces any existing descriptor information.
         *
         * @param descriptor the descriptor
         * @return builder.
         */
        public Builder withDescriptor(Descriptor descriptor) {
            descriptorBuilder.withDescriptor(descriptor);
            return this;
        }

        /**
         * Builds the DynamicBean.
         *
         * @return the dynamic bean.
         */
        public DynamicBean build() {
            // https://docs.oracle.com/javase/8/docs/api/javax/management/MBeanInfo.html
            MBeanInfo info = new MBeanInfo(className, description,
                    attributeInfos.getMBeanAttributeInfos(),
                    new MBeanConstructorInfo[0],
                    operationInfos.getMBeanOperationInfos(),
                    notificationInfos.getMBeanNotificationInfos(),
                    descriptorBuilder.build());

            return new DynamicBean(info, attributeInfos, operationInfos, notificationInfos);
        }

        static class AttributeInfos {
            private final Map<String, AttributeInfo<?>> infos = new HashMap<>();

            public static AttributeInfos of(AttributeInfo<?>... attributes) {
                AttributeInfos attrs = new AttributeInfos();
                for (AttributeInfo<?> attribute : attributes) {
                    attrs.add(attribute);
                }
                return attrs;
            }

            public Optional<AttributeInfo<?>> find(Attribute attribute) {
                return find(attribute.getName());
            }

            public Optional<AttributeInfo<?>> find(String name) {
                return Optional.ofNullable(infos.get(name));
            }

            public AttributeInfo<?> add(AttributeInfo<?> info) {
                return infos.put(info.getName(), info);
            }

            public AttributeInfo<?> add(String key, AttributeInfo<?> info) {
                return infos.put(key, info);
            }

            public Optional<AttributeInfo<?>> remove(String key) {
                return Optional.ofNullable(infos.remove(key));
            }

            public MBeanAttributeInfo[] getMBeanAttributeInfos() {
                return infos.keySet().stream().map(k -> infos.get(k).getMBeanAttributeInfo()).toArray(MBeanAttributeInfo[]::new);
            }
        }

        static class OperationInfos {
            private final Map<String, OperationInfo> operationMap = new HashMap<>();

            static String key(String name, String[] types) {
                final ArrayList<String> list = new ArrayList<>();
                list.add(0, name);
                list.addAll(Arrays.asList(types));
                return String.join(",", list);
            }

            public Object invoke(DynamicBean source, String actionName, Object[] params, String[] signature) throws ReflectionException {
                final String key = key(actionName, signature);

                if (operationMap.containsKey(key)) {
                    OperationInfo info = operationMap.get(key);
                    MethodInvoker invoker = info.getMethodInvoker();
                    try {
                        Object returnValue = invoker.invoke(params);
                        info.getNotifier().ifPresent(nf -> source.sendNotification(nf.apply(source, returnValue)));
                        return returnValue;
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        return new ReflectionException(e);
                    }
                } else {
                    String msg = String.format("No method found with key %s, actual signature (%s)", key, String.join(",", signature));
                    throw new ReflectionException(new NoSuchMethodException(msg));
                }
            }

            public void add(OperationInfo info) {
                String[] signature = Arrays.stream(info.getSignature()).map(pi -> pi.getType().getTypeName()).toArray(String[]::new);
                operationMap.put(key(info.getName(), signature), info);
            }

            public MBeanOperationInfo[] getMBeanOperationInfos() {
                return operationMap.keySet().stream().map(k -> operationMap.get(k).getMBeanOperationInfo()).toArray(MBeanOperationInfo[]::new);
            }
        }

        public static class NotificationInfos {
            private final List<NotificationInfo> infos = new ArrayList<>();
            private Executor executor = ForkJoinPool.commonPool();

            public Executor getExecutor() {
                return executor;
            }

            public void setExecutor(Executor executor) {
                this.executor = executor;
            }

            public void add(NotificationInfo info) {
                infos.add(info);
            }

            public MBeanNotificationInfo[] getMBeanNotificationInfos() {
                return infos.stream().map(NotificationInfo::getMBeanNotificationInfo).toArray(MBeanNotificationInfo[]::new);
            }

        }
    }
}
