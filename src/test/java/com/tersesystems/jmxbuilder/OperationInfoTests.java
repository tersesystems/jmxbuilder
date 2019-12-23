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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import javax.management.*;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.OpenType;

import static org.assertj.core.api.Assertions.assertThat;

public class OperationInfoTests {
    private final OpenTypeMapper openTypeMapper = OpenTypeMapper.getInstance();
    static final OpenMBeanParameterInfo[] NO_PARAMS = new OpenMBeanParameterInfo[0];

    static class ExampleService {
        private final Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());
        private boolean debug;

        String dump() {
            return ("Dumping contents");
        }

        public boolean isDebugEnabled() {
            return debug;
        }

        public void setDebugEnabled(boolean debug) {
            this.debug = debug;
        }
    }

    @Test
    @DisplayName("With Operation From Function")
    public void testOperationFromFunction() {
        ExampleService service = new ExampleService();
        final DynamicMBean serviceBean = DynamicBean.builder()
                .withOperation("dump", "dumps the internal state", service::dump)
                .build();

        MBeanInfo mBeanInfo = serviceBean.getMBeanInfo();
        MBeanOperationInfo operationInfo = new OpenMBeanOperationInfoSupport("dump",
                "dumps the internal state",
                NO_PARAMS,
                openTypeMapper.fromClass(Void.TYPE),
                MBeanOperationInfo.INFO,
                new DescriptorSupport());
        assertThat(mBeanInfo.getOperations()).contains(operationInfo);
    }

    @Test
    @DisplayName("With Operation From Runnable")
    public void testOperationFromRunnable() {
        final DynamicMBean serviceBean = new DynamicBean.Builder()
                .withOperation("dump", "dumps the internal state", new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("hello");
                    }
                })
                .build();

        MBeanInfo mBeanInfo = serviceBean.getMBeanInfo();
        MBeanOperationInfo operationInfo = new OpenMBeanOperationInfoSupport("dump",
                "dumps the internal state",
                NO_PARAMS,
                openTypeMapper.fromClass(Void.TYPE),
                MBeanOperationInfo.INFO,
                new DescriptorSupport());
        assertThat(mBeanInfo.getOperations()).contains(operationInfo);
    }

    @Test
    @DisplayName("With Operation From Parameter")
    public void testOperationFromParameter() {
        ExampleService service = new ExampleService();
        final DynamicMBean loggerBean = new DynamicBean.Builder()
                .withOperation("isDebugEnabled", "returns true if is debugging", service)
                .withOperation("setDebugEnabled", "sets debugging", service,
                        ParameterInfo.builder().withClassType(Boolean.TYPE).withName("debug").withDescription("debug").build())
                .build();

        MBeanInfo mBeanInfo = loggerBean.getMBeanInfo();
        MBeanOperationInfo operationInfo = new OpenMBeanOperationInfoSupport("isDebugEnabled",
                "returns true if is debugging",
                NO_PARAMS,
                openTypeMapper.fromClass(Boolean.TYPE),
                MBeanOperationInfo.INFO,
                new DescriptorSupport());
        assertThat(mBeanInfo.getOperations()).contains(operationInfo);

        MBeanOperationInfo setInfo = new OpenMBeanOperationInfoSupport("setDebugEnabled",
                "sets debugging",
                new OpenMBeanParameterInfoSupport[] {
                    new OpenMBeanParameterInfoSupport("debug", "debug description", openTypeMapper.fromClass(Boolean.TYPE))
                },
                openTypeMapper.fromClass(Void.TYPE),
                MBeanOperationInfo.INFO,
                new DescriptorSupport());
        assertThat(mBeanInfo.getOperations()).contains(setInfo);

    }
}
