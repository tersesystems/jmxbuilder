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

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import java.lang.reflect.Type;

/**
 * Maps between a type and an OpenType.
 *
 * This uses internal Sun classes so compiler gets super judgy.
 */
public class OpenTypeMapper {

    public OpenType<?> toOpenType(Type type) throws OpenDataException {
        // XXX only available in 1.8, dunno what you'd use in JDK 11
        final MXBeanMappingFactory mappingFactory = DefaultMXBeanMappingFactory.DEFAULT;
        final MXBeanMapping mapping = mappingFactory.mappingForType(type, mappingFactory);
        return mapping.getOpenType();
    }

}
