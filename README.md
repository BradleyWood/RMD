# Remote Method Delegation

## Introduction

Remote Method Delegation provides an implementation of RPC that performs
code migration at runtime.


## Project Layout

- []()

## Java Support



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
