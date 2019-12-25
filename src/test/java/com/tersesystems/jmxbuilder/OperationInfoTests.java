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
package com.tersesystems.jmxbuilder;

import com.tersesystems.jmxbuilder.model.ExampleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import javax.management.*;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.openmbean.SimpleType;

import static org.assertj.core.api.Assertions.assertThat;

public class OperationInfoTests {

    //private final Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());
    static final MBeanParameterInfo[] NO_PARAMS = new MBeanParameterInfo[0];

    @Test
    @DisplayName("With Operation From Function")
    public void testOperationFromFunction() {
        ExampleService service = new ExampleService();
        OperationInfo info = OperationInfo.builder()
                .withName("dump")
                .withDescription("dumps the internal state")
                .withSupplier(service::dump)
                .build();

        DescriptorSupport descriptorSupport = new DescriptorSupport();
        descriptorSupport.setField("enabled", true);
        descriptorSupport.setField("openType", SimpleType.STRING);
        descriptorSupport.setField("originalType", "java.lang.String");

        MBeanOperationInfo operationInfo = new MBeanOperationInfo("dump",
                "dumps the internal state",
                NO_PARAMS,
                "java.lang.String",
                MBeanOperationInfo.INFO,
                descriptorSupport);
        assertThat(info.getMBeanOperationInfo()).isEqualTo(operationInfo);
    }


    @Test
    @DisplayName("With Operation From Reflection")
    public void testOperationFromReflectionWithNoParams() {
        DescriptorSupport descriptorSupport = new DescriptorSupport();
        descriptorSupport.setField("enabled", true);
        descriptorSupport.setField("openType", SimpleType.BOOLEAN);
        descriptorSupport.setField("originalType", "boolean");

        ExampleService service = new ExampleService();
        OperationInfo info = OperationInfo.builder()
                .withName("isDebugEnabled")
                .withReflection(service, "isDebugEnabled")
                .withDescription("returns true if is debugging")
                .build();
        MBeanOperationInfo operationInfo = new MBeanOperationInfo("isDebugEnabled",
                "returns true if is debugging",
                NO_PARAMS,
                "boolean",
                MBeanOperationInfo.INFO,
                descriptorSupport);
        assertThat(info.getMBeanOperationInfo()).isEqualTo(operationInfo);
    }

    @Test
    @DisplayName("With Operation From Parameter")
    public void testOperationFromReflectionWithParameter() {
        ExampleService service = new ExampleService();

        ParameterInfo debugParam = ParameterInfo.builder().withClassType(Boolean.TYPE).withName("debug").build();
        OperationInfo info = OperationInfo.builder()
                .withName("setDebugEnabled")
                .withReflection(service, "setDebugEnabled", debugParam)
                .withDescription("sets debugging")
                .build();

        DescriptorSupport descriptorSupport = new DescriptorSupport();
        descriptorSupport.setField("enabled", true);
        descriptorSupport.setField("openType", SimpleType.VOID);
        descriptorSupport.setField("originalType", "void");

        DescriptorSupport parameterDescriptor = new DescriptorSupport();
        parameterDescriptor.setField("enabled", true);
        parameterDescriptor.setField("openType", SimpleType.BOOLEAN);
        parameterDescriptor.setField("originalType", "boolean");

        MBeanOperationInfo expected = new MBeanOperationInfo("setDebugEnabled",
                "sets debugging",
                new MBeanParameterInfo[] {
                    new MBeanParameterInfo("debug", "boolean", null, parameterDescriptor)
                },
                "void",
                MBeanOperationInfo.INFO,
                descriptorSupport);

        assertThat(info.getMBeanOperationInfo()).isEqualTo(expected);
    }
}
