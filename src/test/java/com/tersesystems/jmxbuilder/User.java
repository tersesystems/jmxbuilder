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

import static java.lang.String.format;

class User {
    private String name;
    private Integer age;
    private final Address address;

    User(String name, Integer age, Address address) {
        this.name = name;
        this.age = age;
        this.address = address;
    }

     String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Address getAddress() {
        return address;
    }

    public void ping() {
        System.out.println("ping!");
    }

    @Override
    public String toString() {
        return format("User(%s, %n, %s)", name, age, address);
    }

    public String pong(String pong) {
        return pong;
    }

    public String concatenate(String s1, String s2) {
        return s1 + s2;
    }

    public String callMethod(String s1, String s2, String s3, String s4) {
        return "method called";
    }
}
