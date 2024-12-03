# Basic Example

This is a basic example of an application using jmxbuilder.

## Running

To run, install [JBang](https://www.jbang.dev/documentation/guide/latest/index.html) and then `jbang App.java` will start a running JVM instance.

## Connecting

Connect to this example with [JDK Mission Control](https://adoptium.net/jmc/) -- remember to use the `| tar xv -` option when uncompressing it on MacOS! -- and connect to the JVM.  Go to the MBean Browser tab, and you'll see the beans under `com.tersesystems` in the MBean Tree.