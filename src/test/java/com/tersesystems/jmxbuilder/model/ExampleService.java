package com.tersesystems.jmxbuilder.model;

public class ExampleService {
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