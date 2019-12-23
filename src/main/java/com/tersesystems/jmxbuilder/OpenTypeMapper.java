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

import com.sun.jmx.mbeanserver.DefaultMXBeanMappingFactory;
import com.sun.jmx.mbeanserver.MXBeanMapping;
import com.sun.jmx.mbeanserver.MXBeanMappingFactory;
import net.jodah.typetools.TypeResolver;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularData;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * Maps between a type and an OpenType.
 */
public class OpenTypeMapper {
    private final MXBeanMappingFactory mappingFactory;
    private OpenTypeMapper(MXBeanMappingFactory mappingFactory) {
        this.mappingFactory = mappingFactory;
    }
    private OpenTypeMapper() {
        this(DefaultMXBeanMappingFactory.DEFAULT);
    }

    public static OpenTypeMapper getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public <T> OpenType<T> fromClass(Class<T> clazz) {
        return fromType(TypeResolver.reify(clazz));
    }

    @SuppressWarnings("unchecked")
    public <T> OpenType<T> fromType(Type type) {
        try {
            final MXBeanMapping mapping = mappingFactory.mappingForType(type, mappingFactory);
            return (OpenType<T>) mapping.getOpenType();
        } catch (OpenDataException e) {
            throw new IllegalStateException(e);
        }
    }

    public <T> Class<T> getTypeAsClass(String className) {
        if (className.equals("java.lang.Void")) {
            return (Class<T>) Void.TYPE;
        }
        if (className.equals("java.lang.Boolean")) {
            return (Class<T>) Boolean.TYPE;
        }
        if (className.equals("java.lang.Character")) {
            return (Class<T>) Character.TYPE;
        }
        if (className.equals("java.lang.Byte")) {
            return (Class<T>) Byte.TYPE;
        }
        if (className.equals("java.lang.Short")) {
            return (Class<T>) Short.TYPE;
        }
        if (className.equals("java.lang.Integer")) {
            return (Class<T>) Integer.TYPE;
        }
        if (className.equals("java.lang.Long")) {
            return (Class<T>) Long.TYPE;
        }
        if (className.equals("java.lang.Float")) {
            return (Class<T>) Float.TYPE;
        }
        if (className.equals("java.lang.Double")) {
            return (Class<T>) Double.TYPE;
        }
        if (className.equals("java.lang.String")) {
            return (Class<T>) String.class;
        }
        if (className.equals("java.math.BigDecimal")) {
            return (Class<T>) BigDecimal.class;
        }
        if (className.equals("java.math.BigInteger")) {
            return (Class<T>) BigInteger.class;
        }
        if (className.equals("java.util.Date")) {
            return (Class<T>) Date.class;
        }
        if (className.equals("javax.management.ObjectName")) {
            return (Class<T>) ObjectName.class;
        }
        if (className.equals(CompositeData.class.getName())) {
            return (Class<T>) CompositeData.class;
        }
        if (className.equals(TabularData.class.getName())) {
            return (Class<T>) TabularData.class;
        }
        throw new IllegalStateException("Not a legal opentype: " + className);
    }


    private static class SingletonHolder {
        static final OpenTypeMapper INSTANCE = new OpenTypeMapper();
    }

}
