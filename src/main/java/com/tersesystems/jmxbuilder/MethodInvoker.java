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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Invokes methods on the given object, using whatever means available.
 */
public class MethodInvoker {
    private final Object obj;
    private final Method method;

    public MethodInvoker(Object obj, Method method) {
        this.obj = obj;
        this.method = method;
    }

    public Object invoke(Object[] args) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(obj, args);
    }


    public static MethodInvoker build(Runnable runnable) {
        try {
            return build(runnable, Runnable.class.getMethod("run"));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static MethodInvoker build(Callable callable) {
        try {
            return build(callable, Callable.class.getMethod("call"));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> MethodInvoker build(Supplier<T> supplier) {
        try {
            return build(supplier, Supplier.class.getMethod("get"));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T, R> MethodInvoker build(Function<T, R> function) {
        return build(Function.class, function);
    }

    public static <S, T, R> MethodInvoker build(BiFunction<S, T, R> bifunction) {
        return build(BiFunction.class, bifunction);
    }

    public static MethodInvoker build(Object obj, Method method) {
        return new MethodInvoker(obj, method);
    }

    public static MethodInvoker build(Class<?> functionClass, Object obj) {
        try {
            Class<?>[] types = TypeResolver.resolveRawArguments(functionClass, obj.getClass());
            final List<Class<?>> paramTypes = Arrays.asList(types).subList(0, types.length - 1);
            final Class[] objectClasses = paramTypes.stream().map(c -> Object.class).toArray(Class[]::new);
            return new MethodInvoker(obj, functionClass.getMethod("apply", objectClasses));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}

