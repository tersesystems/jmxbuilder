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
package com.tersesystems.jmxbuilder.model;

import static java.lang.String.format;

public class Address {
    private final String street1;
    private final String city;
    private final String state;

    public Address(String street1, String city, String state) {
        this.street1 = street1;
        this.city = city;
        this.state = state;
    }

    public String getStreet1() {
        return street1;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    @Override
    public String toString() {
        return format("Address(%s, %s, %s)", street1, city, state);
    }
}
