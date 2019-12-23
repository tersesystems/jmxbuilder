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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.openmbean.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A dynamic bean that extends DynamicMBean and can be constructed through a fluent builder pattern.
 *
 * {@code <pre>
 * public class Service {
 *     public void createServiceBean() {
 *         final DynamicBean serviceBean = DynamicBean.builder()
 *                 .withOperation("dump", "dumps the internal state", this::dump)
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
 *
 * See https://github.com/tersesystems/jmxbuilder for details.
 */
public class DynamicBean implements DynamicMBean, NotificationEmitter {
    private final Logger logger = LoggerFactory.getLogger(getClass());
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
        Optional<Object> o = attributeInfos.find(name).flatMap(this::apply);
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
                logger.warn("Exception when getting attribute {}", name, e);
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
                String msg = String.format("Cannot find %s in attributeInfos", attr.getName());
                logger.warn(msg);
                //throw new AttributeNotFoundException(msg);
            }
        }
        return retlist;
    }

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

    @SuppressWarnings("unchecked")
    private <T> Optional<T> apply(AttributeInfo<?> attributeInfo) {
        return (Optional<T>) attributeInfo.get();
    }

    /**
     * The builder.  You should call {@code DynamicBean.builder()} to return a fresh instance of this class.
     */
    public static class Builder {
        private final AttributeInfos attributeInfos = new AttributeInfos();
        private final OperationInfos operationInfos = new OperationInfos();
        private final NotificationInfos notificationInfos = new NotificationInfos();
        private final DescriptorSupport.Builder descriptorBuilder = DescriptorSupport.builder();

        private String className = "";
        private String description = "";

        Builder() {
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withClassName(String className) {
            this.className = className;
            return this;
        }

        public <T> Builder withSimpleAttribute(String name, String description, Supplier<T> reader) {
            AttributeInfo<T> info = AttributeInfo.<T>builder().withName(name)
                    .withDescription(description)
                    .withSupplier(reader)
                    .build();
            return withAttribute(info);
        }

        public <T> Builder withSimpleAttribute(String name, String description, Consumer<T> writer) {
            AttributeInfo<T> info = AttributeInfo.<T>builder().withName(name)
                    .withDescription(description)
                    .withConsumer(writer)
                    .build();
            return withAttribute(info);
        }

        public <T> Builder withSimpleAttribute(String name, String description, Supplier<T> reader, Consumer<T> writer) {
            AttributeInfo<T> info = AttributeInfo.<T>builder().withName(name)
                    .withDescription(description)
                    .withSupplier(reader)
                    .withConsumer(writer)
                    .build();
            return withAttribute(info);
        }

        public <T> Builder withSimpleAttribute(String name, String description, Supplier<T> reader, Consumer<T> writer, Descriptor descriptor) {
            AttributeInfo<T> info = AttributeInfo.<T>builder().withName(name)
                    .withDescription(description)
                    .withSupplier(reader)
                    .withConsumer(writer)
                    .withDescriptor(descriptor)
                    .build();
            return withAttribute(info);
        }

        public <T> Builder withCompositeAttribute(String name, String description, Supplier<T> reader, CompositeDataWriter<T> writes) {
            AttributeInfo<CompositeData> info = AttributeInfo.<CompositeData>builder().withName(name)
                    .withDescription(description)
                    .withSupplier(() -> writes.apply(reader.get()))
                    .build();
            return withAttribute(info);
        }

        public <T> Builder withCompositeAttribute(String name, String description, Supplier<T> reader, CompositeDataWriter<T> writes, Descriptor descriptor) {
            AttributeInfo<CompositeData> info = AttributeInfo.<CompositeData>builder()
                    .withName(name)
                    .withDescription(description)
                    .withSupplier(() -> writes.apply(reader.get()))
                    .withDescriptor(descriptor)
                    .build();
            return withAttribute(info);
        }

        public <T> Builder withTabularAttribute(String name, String description, Supplier<Iterable<T>> reader, TabularDataWriter<T> writes) {
            AttributeInfo<TabularData> info = AttributeInfo.<TabularData>builder()
                    .withName(name)
                    .withDescription(description)
                    .withSupplier(() -> writes.apply(reader.get()))
                    .build();
            return withAttribute(info);
        }

        public <T> Builder withTabularAttribute(String name, String description, Supplier<Iterable<T>> reader, TabularDataWriter<T> writes, Descriptor descriptor) {
            AttributeInfo<TabularData> info = AttributeInfo.<TabularData>builder()
                    .withName(name)
                    .withDescription(description)
                    .withSupplier(() -> writes.apply(reader.get()))
                    .withDescriptor(descriptor)
                    .build();
            return withAttribute(info);
        }

        public <T> Builder withAttribute(AttributeInfo<T> info) {
            attributeInfos.add(info);
            return this;
        }

        public Builder withOperation(String name, String description, Runnable runnable) {
            OperationInfo info = OperationInfo.builder()
                    .withName(name)
                    .withDescription(description)
                    .withMethod(runnable)
                    .build();

            return withOperation(info);
        }

        public <T, R> Builder withOperation(String name, String description, Function<T, R> f, String paramName) {
            OperationInfo info = OperationInfo.builder()
                    .withName(name)
                    .withDescription(description)
                    .withMethod(f, paramName)
                    .build();

            return withOperation(info);
        }

        public <T, R> Builder withOperation(String name, String description, Function<T, R> f, ParameterInfo parameterInfo) {
            OperationInfo info =OperationInfo.builder()
                    .withName(name)
                    .withDescription(description)
                    .withMethod(f, parameterInfo)
                    .build();

            return withOperation(info);
        }

        public <T, U, R> Builder withOperation(String name, String description, BiFunction<T, U, R> f, String paramName1, String paramName2) {
            OperationInfo info =OperationInfo.builder()
                    .withName(name)
                    .withDescription(description)
                    .withMethod(f, paramName1, paramName2)
                    .build();

            return withOperation(info);
        }

        public <T, U, R> Builder withOperation(String name, String description, BiFunction<T, U, R> f, ParameterInfo... parameterInfos) {
            OperationInfo info = OperationInfo.builder()
                    .withName(name)
                    .withDescription(description)
                    .withMethod(f, parameterInfos)
                    .build();

            return withOperation(info);
        }

        /**
         * Calls a method on the object with the given parameters through reflection.
         *
         * @param name the name of the method to call (i.e. on user.getName, this is "getName")
         * @param description the description, i.e. "Gets the user's name"
         * @param object the object, i.e. the user
         * @param parameterInfos the method signature, if any (i.e. getLogger(String loggerName) would have a single String parameter info)
         * @param <T> the type of object
         * @return the dynamic bean builder.
         */
        public <T> Builder withOperation(String name, String description, T object, ParameterInfo... parameterInfos) {
            OperationInfo info = OperationInfo.builder()
                    .withName(name)
                    .withDescription(description)
                    .withMethod(object, parameterInfos)
                    .build();

            return withOperation(info);
        }

        public Builder withOperation(OperationInfo info) {
            operationInfos.add(info);
            return this;
        }

        public Builder withNotification(String name, String description, String... types) {
            NotificationInfo info = NotificationInfo.builder()
                    .withName(name)
                    .withTypes(types)
                    .withDescription(description)
                    .build();
            return withNotification(info);
        }

        public Builder withNotification(NotificationInfo notificationInfo) {
            notificationInfos.add(notificationInfo);
            return this;
        }

        public Builder withAttributeChangeNotifications() {
            withNotification(AttributeChangeNotification.class.getName(), "An attribute of this MBean has changed", AttributeChangeNotification.ATTRIBUTE_CHANGE);
            return this;
        }


        public Builder withImmutableInfo(boolean isImmmutable) {
            descriptorBuilder.withImmutableInfo(isImmmutable);
            return this;
        }


        public Builder withDescriptor(Descriptor descriptor) {
            descriptorBuilder.withDescriptor(descriptor);
            return this;
        }

        public DynamicBean build() {
            OpenMBeanInfoSupport info = new OpenMBeanInfoSupport(className, description,
                    attributeInfos.getMBeanAttributeInfos(),
                    new OpenMBeanConstructorInfo[0],
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

            public OpenMBeanAttributeInfo[] getMBeanAttributeInfos() {
                return infos.keySet().stream().map(k -> infos.get(k).getMBeanAttributeInfo()).toArray(OpenMBeanAttributeInfo[]::new);
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

            public OpenMBeanOperationInfo[] getMBeanOperationInfos() {
                return operationMap.keySet().stream().map(k -> operationMap.get(k).getMBeanOperationInfo()).toArray(OpenMBeanOperationInfo[]::new);
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
