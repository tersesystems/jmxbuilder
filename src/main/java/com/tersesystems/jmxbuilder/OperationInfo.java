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
import javax.management.Notification;
import javax.management.openmbean.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * OperationInfo class.  This is an easier interface than MBeanOperationInfo.
 *
 * Intentionally open without a builder pattern so you can pass in your own.
 */
public class OperationInfo {
    private final String name;
    private final ParameterInfo<?>[] signature;
    private final String description;
    private final OpenType<?> returnType;
    private final int impact;
    private final Descriptor descriptor;
    private final MethodInvoker invoker;
    private final BiFunction<Object, Object, Notification> notifier;

    public OperationInfo(String name,
                         ParameterInfo<?>[] signature,
                         OpenType<?> returnType,
                         MethodInvoker invoker) {
        this(name, signature, returnType, invoker, null, MBeanOperationInfo.UNKNOWN, null, null);
    }

    public OperationInfo(String name,
                         ParameterInfo<?>[] signature,
                         OpenType<?> returnType,
                         MethodInvoker invoker,
                         String description,
                         int impact, // See MBeanOperationInfo.UNKNOWN
                 Descriptor descriptor,
                 BiFunction<Object, Object, Notification> notifier) {
        this.name = requireNonNull(name, "null name");
        this.signature = requireNonNull(signature, "null signature");
        this.returnType = requireNonNull(returnType, "null return type");
        this.invoker = requireNonNull(invoker, "null invoker");
        this.description = requireNonNull(description, "null description");
        this.impact = impact;
        this.descriptor = descriptor;
        this.notifier = notifier;
    }

    public String getName() {
        return name;
    }

    public ParameterInfo<?>[] getSignature() {
        return signature;
    }

    public String getDescription() {
        return description;
    }

    public OpenType<?> getReturnType() {
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

    public OpenMBeanOperationInfo getMBeanOperationInfo() {
        OpenMBeanParameterInfo[] mBeanParameterInfos = new OpenMBeanParameterInfo[signature.length];
        for (int i = 0; i < mBeanParameterInfos.length; i++) {
            final ParameterInfo<?> parameterInfo = signature[i];
            mBeanParameterInfos[i] = parameterInfo.getMBeanParameterInfo();
        }

        return new OpenMBeanOperationInfoSupport(name, description, mBeanParameterInfos, returnType, impact, descriptor);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private static final OpenTypeMapper openTypeMapper = OpenTypeMapper.getInstance();

        private String name;
        private ParameterInfo<?>[] signature;
        private String description;
        private OpenType<?> returnType;
        private int impact;
        private final DescriptorSupport.Builder descriptorBuilder = DescriptorSupport.builder();
        private MethodInvoker invoker;
        private BiFunction<Object, Object, Notification> notifier;

        public Builder() {
        }

        public Builder withName(String name) {
            this.name = requireNonNull(name, "null name");
            return this;
        }

        public Builder withDescription(String description) {
            this.description = requireNonNull(name, "null description");
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
        public Builder withMethod(Runnable runnable) {
            ParameterInfo<?>[] signature = new ParameterInfo[0];
            this.returnType = openTypeMapper.fromType(Void.TYPE);
            this.signature = signature;
            this.invoker = MethodInvoker.build(runnable);
            return this;
        }

        // no args, returns R
        public <T> Builder withMethod(Supplier<T> supplier) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Supplier.class, supplier.getClass());
            this.returnType = openTypeMapper.fromClass(types[0]);
            this.signature = new ParameterInfo[0];
            this.invoker = MethodInvoker.build(supplier);
            return this;
        }

        public <T, R> Builder withMethod(Function<T, R> function, String paramName) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Function.class, function.getClass());
            this.signature = new ParameterInfo[]{
                    new ParameterInfo<T>(openTypeMapper.fromClass((Class<T>) types[0]), paramName)
            };
            this.returnType = openTypeMapper.fromClass(types[types.length - 1]);
            this.invoker = MethodInvoker.build(function);
            return this;
        }

        public <T, R> Builder withMethod(Function<T, R> function, ParameterInfo<T> signature) {
            Class<?>[] types = TypeResolver.resolveRawArguments(Function.class, function.getClass());
            this.signature = new ParameterInfo[]{signature};
            this.returnType = openTypeMapper.fromClass(types[types.length - 1]);
            this.invoker = MethodInvoker.build(function);
            return this;
        }

        public <T, U, R> Builder withMethod(BiFunction<T, U, R> biFunction, String arg1Name, String arg2Name) {
            Class<?>[] types = TypeResolver.resolveRawArguments(BiFunction.class, biFunction.getClass());
            this.signature = new ParameterInfo[]{
                    new ParameterInfo<T>(openTypeMapper.fromClass((Class<T>) types[0]), arg1Name),
                    new ParameterInfo<U>(openTypeMapper.fromClass((Class<U>) types[1]), arg2Name)
            };
            this.returnType = openTypeMapper.fromClass(types[types.length - 1]);
            this.invoker = MethodInvoker.build(biFunction);
            return this;
        }

        public <T, U, R> Builder withMethod(BiFunction<T, U, R> biFunction, ParameterInfo<T> sig1, ParameterInfo<U> sig2) {
            Class<?>[] types = TypeResolver.resolveRawArguments(BiFunction.class, biFunction.getClass());
            Class<?> returnClass = types[types.length - 1];
            this.signature = new ParameterInfo[]{ sig1, sig2 };
            this.returnType = openTypeMapper.fromClass(returnClass);
            this.invoker = MethodInvoker.build(biFunction);
            return this;
        }

        public <T> Builder withMethod(T obj, ParameterInfo<?>[] parameters) {
            try {
                // First, find all the methods with a matching name.
                Class<?> aClass = obj.getClass();
                List<Method> methods = Arrays.stream(aClass.getMethods())
                        .filter(m -> m.getName().equalsIgnoreCase(name))
                        .filter(m -> m.getParameterCount() == parameters.length)
                        .collect(Collectors.toList());

                if (methods.isEmpty()) {
                    throw new java.util.NoSuchElementException("No method found with name " +
                            name + " and parameter count " + parameters.length);
                }

                Class<?>[] parameterClasses = new Class[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    String className = parameters[i].getType().getClassName();
                    parameterClasses[i] = openTypeMapper.getTypeAsClass(className);
                }

                Method method = obj.getClass().getMethod(name, parameterClasses);
                this.signature = parameters;
                this.returnType = openTypeMapper.fromClass(method.getReturnType());
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
            if (description == null || description.isEmpty()) {
                description = name;
            }
            return new OperationInfo(name, signature, returnType, invoker, description, impact, descriptorBuilder.build(), notifier);
        }

    }

}
