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

import com.tersesystems.jmxbuilder.patched.MappedMXBeanType;
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

    public static OpenTypeMapper getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public <T> OpenType<T> fromClass(Class<T> clazz) {
        Type reifiedType = TypeResolver.reify(clazz);
        return fromType(reifiedType);
    }

    @SuppressWarnings("unchecked")
    public <T> OpenType<T> fromType(Type type) {
        try {
            return (OpenType<T>) MappedMXBeanType.toOpenType(type);
        } catch (OpenDataException e) {
            throw new IllegalStateException(e);
        }
    }

    public Class<?> getTypeAsClass(String typeName) {

        if (typeName.equals("void")) {
            return Void.TYPE;
        } else if (typeName.equals("java.lang.Void")) {
            return Void.class;
        }

        if (typeName.equals("boolean")) {
            return Boolean.TYPE;
        } else if (typeName.equals("java.lang.Boolean")) {
            return Boolean.class;
        }

        if (typeName.equals("char")) {
            return Character.TYPE;
        } else if (typeName.equals("java.lang.Character")) {
            return Character.class;
        }

        if (typeName.equals("byte")) {
            return Byte.TYPE;
        } else if (typeName.equals("java.lang.Byte")) {
            return Byte.class;
        }

        if (typeName.equals("short")) {
            return Short.TYPE;
        } else if (typeName.equals("java.lang.Short")) {
            return Short.class;
        }

        if (typeName.equals("int")) {
            return Integer.TYPE;
        } else if (typeName.equals("java.lang.Integer")) {
            return Integer.class;
        }

        if (typeName.equals("long")) {
            return Long.TYPE;
        } else if (typeName.equals("java.lang.Long")) {
            return Long.class;
        }

        if (typeName.equals("float")) {
            return Float.TYPE;
        } else if (typeName.equals("java.lang.Float")) {
            return Float.class;
        }

        if (typeName.equals("double")) {
            return Double.TYPE;
        } else if (typeName.equals("java.lang.Double")) {
            return Double.class;
        }

        if (typeName.equals("java.lang.String")) {
            return String.class;
        }
        if (typeName.equals("java.math.BigDecimal")) {
            return BigDecimal.class;
        }
        if (typeName.equals("java.math.BigInteger")) {
            return BigInteger.class;
        }
        if (typeName.equals("java.util.Date")) {
            return Date.class;
        }
        if (typeName.equals("javax.management.ObjectName")) {
            return ObjectName.class;
        }
        if (typeName.equals(CompositeData.class.getName())) {
            return CompositeData.class;
        }
        if (typeName.equals(TabularData.class.getName())) {
            return TabularData.class;
        }
        throw new IllegalStateException("Not a legal opentype: " + typeName);
    }


    private static class SingletonHolder {
        static final OpenTypeMapper INSTANCE = new OpenTypeMapper();
    }

}
