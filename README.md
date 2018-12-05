# Remote Method Delegation

## Introduction

RMD (remote method delegation) is a secure lightweight cluster computing
platform with load balancing and code migration. RMD is focused on
improving the usability issues that plague related
industry solutions.

## Project Layout

- [rmd-client](/rmd-client/src/main/java/net/uoit/rmd) - the client side delegate implementation
- [rmd-comm](/rmd-comm/src/main/java/net/uoit/rmd) - the communication layer
- [rmd-examples](/rmd-examples) - examples in both java and Kotlin
- [rmd-jobserver](/rmd-jobserver/src/main/java/net/uoit/rmd) - the server responsible for executing jobs
- [rmd-kotlin](/rmd-kotlin/src/main/kotlin/net/uoit/rmd) - kotlin client module with dsl
- [rmd-load-balancer](/rmd-load-balancer/src/main/java/net/uoit/rmd) - load balancing across several job servers

## Build Instructions

This project is built using maven build tool. The maven wrapper is included
to simplify the build process. You may need install JDK-8 and set you
JAVA_HOME path in your environment variables. Simply execute the following
command to build the project.

```
mvnw install
```

or

```
./mvnw install
```

### Test an example

Firstly, start a job server by executing the following
command in the root directory of the project

```
java -jar rmd-jobserver/target/rmd-jobserver-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Finally, to test the Kotlin example execute the example
with the following command. This example will generate
a random list of 10 numbers and delegate the sorting job
to the job server.

```
java -jar rmd-examples/kotlin-example/target/kotlin-example-1.0-SNAPSHOT-jar-with-dependencies.jar
```

The Java example will use the job server to calculate
a factorial and perform some basic arithmetic.
To test the Java example:

```
java -jar rmd-examples/java-example/target/java-example-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Java Support

Currently, to delegate execution to a remote server you must use
either a method reference or a lambda expression to define the
code that you wish to execute remotely. Consumers, Producers, and Functions
with up to 4 parameters can be delegated with type safety. Code can
easily be executed asynchronously by providing a callback.

### Synchronous delegates in java

```java
System.out.println("6! " + delegate(Function::factorial, 6));
```

Example with a lambda expression

```java
System.out.println("6 * 2 = " + delegate(n -> n * 2, 6));
```

### Asynchronous delegates in java

```java
delegate(Functions::factorial, 6, System.out::println);
```

Example with lambda expressions

```java
delegate((a, b) -> a * b, 5, 9, n -> {
    System.out.println("5 * 9 = " + n);
});
```

### Kotlin Support

A very simple kotlin dsl has been implemented to simplify your
programs and provide syntactic sugar. For example, code inside
a 'delegate' block will run on a remote job server in a blocking manor.

To achieve asynchronous code execution, code is played inside an
'async' block. Immediately following the 'async' block, you can
optionally place a 'callback' block which will run locally when
execution is complete. The callback returns the result from
the async block.

#### Synchronous delegates in kotlin
```kotlin
import net.uoit.rmd.delegate

fun main(args: Array<String>) {
    val a = 100
    val b = 200

    val result = delegate {
        a * b
    }

    println("$a * $b = $result")
}
```

#### Asynchronous delegates in kotlin

```kotlin
import net.uoit.rmd.async
import net.uoit.rmd.callback
import java.lang.Thread.sleep

fun main(args: Array<String>) {
    val a = 100
    val b = 200

    async {
        a * b
    } callback {
        println("Result = $it")
    }

    sleep(1000)
}
```
