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

import javax.management.Descriptor;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.Notification;
import javax.management.openmbean.OpenType;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * OperationInfo class.  This is an easier interface than MBeanOperationInfo.
 */
public class OperationInfo {
    private final String name;
    private final ParameterInfo<?>[] signature;
    private final String description;
    private final String returnType;
    private final int impact;
    private final Descriptor descriptor;
    private final MethodInvoker invoker;
    private final BiFunction<Object, Object, Notification> notifier;

    public OperationInfo(String name,
                         ParameterInfo<?>[] signature,
                         String returnType,
                         MethodInvoker invoker) {
        this(name, signature, returnType, invoker, null, MBeanOperationInfo.UNKNOWN, null, null);
    }

    public OperationInfo(String name,
                         ParameterInfo<?>[] signature,
                         String returnType,
                         MethodInvoker invoker,
                         String description,
                         int impact, // See MBeanOperationInfo.UNKNOWN
                 Descriptor descriptor,
                 BiFunction<Object, Object, Notification> notifier) {
        this.name = requireNonNull(name);
        this.signature = requireNonNull(signature);
        this.returnType = requireNonNull(returnType);
        this.invoker = requireNonNull(invoker);
        this.description = (description);
        this.impact = impact;
        this.descriptor = (descriptor);
        this.notifier = (notifier);
    }

    public String getName() {
        return name;
    }

    public ParameterInfo<?>[] getSignature() {
        return signature;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public String getReturnType() {
        return returnType;
    }

    public int getImpact() {
        return impact;
    }

    public Optional<Descriptor> getDescriptor() {
        return Optional.ofNullable(descriptor);
    }

    public MethodInvoker getMethodInvoker() {
        return invoker;
    }

    public Optional<BiFunction<Object, Object, Notification>> getNotifier() {
        return Optional.ofNullable(notifier);
    }

    public MBeanOperationInfo getMBeanOperationInfo() {
        MBeanParameterInfo[] mBeanParameterInfos = new MBeanParameterInfo[signature.length];
        for (int i = 0; i < mBeanParameterInfos.length; i++) {
            mBeanParameterInfos[i] = signature[i].getMBeanParameterInfo();
        }
        return new MBeanOperationInfo(name, description, mBeanParameterInfos, returnType, impact, descriptor);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final OpenTypeMapper openTypeMapper = OpenTypeMapper.getInstance();

        private String name;
        private ParameterInfo<?>[] signature;
        private String description;
        private String returnType;
        private int impact;
        private final DescriptorSupport.Builder descriptorBuilder = DescriptorSupport.builder();
        private MethodInvoker invoker;
        private BiFunction<Object, Object, Notification> notifier;

        public Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withImpact(int impact) {
            this.impact = impact;
            return this;
        }

        public Builder withDescriptor(Descriptor descriptor) {
            this.descriptorBuilder.withDescriptor(descriptor);
            return this;
        }

        // no args, returns void
        public Builder withRunnable(Runnable runnable) {
            ParameterInfo<?>[] signature = new ParameterInfo[0];
            this.returnType = "void";
            this.signature = signature;
            this.invoker = MethodInvoker.build(runnable);
            return this;
        }

        public <R> Builder withCallable(Callable<R> callable) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Callable.class, callable.getClass());
            Class<R> returnClass = (Class<R>) types[types.length - 1];
            return withCallable(returnClass, callable);
        }

        public <R> Builder withCallable(Class<R> returnClass, Callable<R> callable) {
            this.signature = new ParameterInfo[]{};
            this.returnType = returnClass.getName();
            this.invoker = MethodInvoker.build(callable);
            return this;
        }

        public <R> Builder withSupplier(Supplier<R> supplier) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Supplier.class, supplier.getClass());
            Class<R> returnClass = (Class<R>) types[0];
            return withSupplier(returnClass, supplier);
        }

        public <R> Builder withSupplier(Class<R> returnClass, Supplier<R> supplier) {
            this.returnType = returnClass.getName();
            this.signature = new ParameterInfo[0];
            this.invoker = MethodInvoker.build(supplier);
            return this;
        }
        
        public <V> Builder withConsumer(Consumer<V> consumer, String paramName) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Consumer.class, consumer.getClass());
            ParameterInfo<V> parameterInfo = (ParameterInfo<V>) ParameterInfo.builder().withClassType(types[types.length - 1]).withName(paramName).build();
            return withConsumer(consumer, parameterInfo);
        }

        public <V> Builder withConsumer(Consumer<V> consumer, ParameterInfo<V> paramInfo) {
            this.returnType = "void";
            this.signature = new ParameterInfo[] {paramInfo};
            this.invoker = MethodInvoker.build(consumer);
            return this;
        }

        public <T, R> Builder withFunction(Function<T, R> function, String paramName) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Function.class, function.getClass());
            Class<R> returnClass = (Class<R>) types[types.length - 1];
            ParameterInfo<T> parameterInfo = (ParameterInfo<T>) ParameterInfo.builder().withClassType(types[0]).withName(paramName).build();
            return withFunction(returnClass, function, parameterInfo);
        }

        public <T, R> Builder withFunction(Function<T, R> function, ParameterInfo<T> signature) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Function.class, function.getClass());
            Class<R> returnClass = (Class<R>) types[types.length - 1];
            return withFunction(returnClass, function, signature);
        }

        public <T, R> Builder withFunction(Class<R> returnClass, Function<T, R> function, ParameterInfo<T> signature) {
            this.signature = new ParameterInfo[]{signature};
            this.returnType = returnClass.getName();
            this.invoker = MethodInvoker.build(function);
            return this;
        }

        public <T, U, R> Builder withBiFunction(BiFunction<T, U, R> biFunction, String arg1Name, String arg2Name) {
            Class<?>[] types = TypeResolver.resolveRawArguments(BiFunction.class, biFunction.getClass());
            ParameterInfo<T> param1 = (ParameterInfo<T>) ParameterInfo.builder().withClassType(types[0]).withName(arg1Name).build();
            ParameterInfo<U> param2 = (ParameterInfo<U>) ParameterInfo.builder().withClassType(types[1]).withName(arg2Name).build();
            Class<R> returnClass = (Class<R>) types[types.length - 1];
            return withBiFunction(returnClass, biFunction, param1, param2);
        }

        public <T, U, R> Builder withBiFunction(BiFunction<T, U, R> biFunction, ParameterInfo<T> param1, ParameterInfo<U> param2) {
            Class<?>[] types = TypeResolver.resolveRawArguments(BiFunction.class, biFunction.getClass());
            Class<R> returnClass = (Class<R>) types[types.length - 1];
            return withBiFunction(returnClass, biFunction, param1, param2);
        }

        public <T, U, R> Builder withBiFunction(Class<R> returnClass, BiFunction<T, U, R> biFunction, ParameterInfo<T> param1, ParameterInfo<U> param2) {
            String returnType = returnClass.getName();
            this.signature = new ParameterInfo[]{ param1, param2 };
            this.returnType = returnType;
            this.invoker = MethodInvoker.build(biFunction);
            return this;
        }

        /**
         * Sets up an operation to invoke a method using reflection.
         *
         * @param obj the object to call the reflected method on.
         * @param methodName the method name on the class to call.
         * @param parameters the parameter infos (this will go into the signature.
         * @param <T> the type of the object to call the reflected method on.
         * @return the modified builder.
         *
         * @throws RuntimeException if anything goes wrong.
         */
        public <T> Builder withReflection(T obj, String methodName, ParameterInfo<?>... parameters) {
            try {
                requireNonNull(obj, "Null object");
                requireNonNull(methodName, "Null methodName");

                Class<?>[] parameterClasses = new Class[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    parameterClasses[i] = parameters[i].getType();
                }

                Method method = obj.getClass().getMethod(methodName, parameterClasses);
                this.signature = parameters;
                this.returnType = method.getReturnType().getName();
                this.invoker = MethodInvoker.build(obj, method);
                return this;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        public Builder withCompletionNotification(BiFunction<Object, Object, Notification> f) {
            this.notifier = f;
            return this;
        }

        public OperationInfo build() {
            // The Descriptor for all of the MBeanAttributeInfo, MBeanParameterInfo, and MBeanOperationInfo objects
            // contained in the MBeanInfo will have a field openType whose value is the OpenType specified by the
            // mapping rules above. So even when getType() is "int", getDescriptor().getField("openType") will be
            // SimpleType.INTEGER.
            //
            // The Descriptor for each of these objects will also have a field originalType that is a string
            // representing the Java type that appeared in the MXBean interface.
            Class<?> typeAsClass = openTypeMapper.getTypeAsClass(returnType);
            OpenType<?> openType = openTypeMapper.fromClass(typeAsClass);
            Descriptor descriptor = descriptorBuilder
                    .withField("openType", openType)
                    .withField("originalType", returnType)
                    .build();

            return new OperationInfo(name, signature, returnType, invoker, description, impact, descriptor, notifier);
        }
    }

}
