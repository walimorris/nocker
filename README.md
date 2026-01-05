## Nocker Annotations

Nocker uses a small, composable annotation system to define CLI commands and their arguments.  
This system is designed to be **modular**, **scalable**, and **feature-agnostic**, allowing future Nocker features to reuse the same annotation model beyond port scanning.

At a high level:

- **Method-level annotations** define *commands*
- **Parameter-level annotations** define *arguments*
- All Nocker annotations are grouped under a common meta-annotation

---

### Core Meta-Annotations

#### `@Nocker`

`@Nocker` is a root marker annotation used to identify annotations that belong to the Nocker system.

All annotations that participate in command or argument resolution are ultimately annotated with `@Nocker`.  
This provides a single semantic and reflective boundary for Nocker features.

---

#### `@NockerMethod`

`@NockerMethod` marks an annotation as defining a **command method**.

- The `name` corresponds **1:1** with a user-facing CLI command
- For example, a command annotation named `"scan"` maps to:

```text
nocker scan
```

Concrete command annotations are built on top of `@NockerMethod`.

**Key design principle:**

> Multiple methods may represent the same command, as long as they conform to the semantics of that command.

This allows features to remain expressive without forcing rigid method signatures or naming conventions.

---

### `@NockerArg`

`@NockerArg` marks an annotation as defining a command argument.

Argument annotations are applied at the **parameter level** and describe how method parameters are bound from CLI input.

Each argument annotation:

- Defines a CLI argument name  
- Declares whether the argument is required  
- Can be reused across commands and features

---

### Command Annotations

Command annotations represent executable Nocker commands.  
They are applied to methods and are annotated with `@NockerMethod`.

Concrete examples include commands like `Scan` and `CIDRScan`.  
These command annotations map directly to CLI commands (e.g., `nocker scan`, `nocker cidr-scan`).

---

### Argument Annotations

Argument annotations describe CLI arguments and are applied to method parameters.

Examples include `Host`, `Hosts`, `Port`, and `Ports`.  
These define names, required/optional status, and argument types, allowing methods to remain expressive and flexible while still being discoverable via reflection.

---

### Example Usage

```java
@Scan
public void scan(@Host String host, @Ports String ports) {
    ...
}
```
This method maps directly to the CLI command:

```text
nocker scan --host=<value> --ports=<value>
```

Multiple methods can exist for the same command, each with different parameter types or numbers of arguments, as long as they conform to the semantics of the command annotation.

### Design Rational
#### The Nocker annotation system is intentionally decoupled from specific features such as port scanning
- Features define what a command means
- Annotations define how commands are discovered
- Reflection resolves commands without ambiguity

#### By mapping user commands directly to annotation metadata rather than method names, Nocker:
- Reduces reflective ambiguity 
- Allows multiple implementations of the same command
- Encourages extensibility without breaking existing behavior
- Future Nocker modules can reuse this system to introduce new commands and arguments without redefining the annotation model, making it modular, scalable, and expressive.

### Limitations
1. The purpose of Nocker annotations is to reduce ambiguity, improve meaning, and reduce incorrect results. In doing so, we annotate our meaning. Let's take the `@Host` annotation definition:
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@NockerArg
public @interface Host {
    String name() default "host";
    boolean required() default true;
}
```
When used, meaning is clear: this is an argument named host. Reflection will happily and easily read this as it's given because it's a `@NockerArg` and the system will resolve it's meaning because it knows it's a valid argument. 
However, you can change the name:

```java
import com.nocker.portscanner.annotation.arguments.Port;
import com.nocker.portscanner.annotation.arguments.Host;

public newMethod(@Host(name = "theHost") String theHost, @Port int port) {
    ...
}
```
There's no specific reason to do this and besides, does Nocker know what `theHost` resolves to? In any case, the command will fail and will not produce any results because `theHost` is not a legal argument.

2. Let's take `@Scan`, a method annotation. We know it means a Portscan, but what should we scan? That's what `@NockerArgs` and the method implementations are for. These methods are both legal:

```java
import com.nocker.portscanner.annotation.arguments.Host;
import com.nocker.portscanner.annotation.arguments.Port;
import com.nocker.portscanner.annotation.commands.Scan;

@Scan
public scanMethod1(@Host String host, @Port int port) {
    ...
}

@Scan
public scanMethod2(@Host String host, @Port int port) {
    ...
}
```

```text
nocker scan --host=127.0.0.1 --port=8080
```
Given that command, how should we know which method, `scanMethod1` or `scanMethod2` should be invoked? In Nocker, the first found method that resolves will be invoked. However, what if that's not what the user wanted?
This meets the design goal of being expressive and scalable, although it goes against Nocker's design philosophy to reduce ambiguity. In this case, producers of command methods should be aware of already provided actions. 
If one scan differs from another, there must be some variable(s) that make it different, i.e., should I investigate a port further and if so which port. Now, the definition of meaning becomes:

```java
import com.nocker.portscanner.annotation.arguments.Host;
import com.nocker.portscanner.annotation.arguments.Port;
import com.nocker.portscanner.annotation.commands.Scan;

@Scan
public scanMethod2(@Host String host, @Port int port, @SuspiciousPort int suspiciousPort) {
    ...
}
```
This meets the design philosophy. It removes ambiguity and introduces a new argument that can be reused.
Another way to solve this limitation is by introducing a `@Primary` annotation. 

## Chunking

In Nocker, chunking is the process of splitting large port ranges into easily digestible chunks. Another word for this is
"batching", but we're not going to call it that.

As an example, for a local scan on `127.0.0.1`, Nocker generates these chunks:

![Nocker Chunks](images/chunks.png)

There are 66 chunks of 1,000 ports (up to port 65,535) in total. In Nocker, this translates to 66 **Tasks** sent to a single **Scheduler**.
Why not just send all 65,535 ports as a single task, or one task per port (65k+ tasks)? Both approaches were tried, but they have several issues:

1. **Zero back-pressure** – once the scheduler (consumer) receives a task with 65k+ ports, there’s no way to stop it.  
   Adapting timeouts, aborting early, or dynamically submitting ports is impossible.

2. **Minimal observability** – a single 65k-port task starts and runs to completion. Long timeouts or the need for backoff cannot be handled.  
   Chunking allows dynamic monitoring, backoff, and the ability to abort scans if necessary.

3. **Poor use of parallelism & concurrency** – consider these scenarios:

| Scenario | Tasks | Behavior |
|----------|-------|---------|
| **1 Task** | 1 | Wastes 99% of threads, no back-pressure, no cancellation. Worst-case runtime: `50ms * 65,535 ≈ 3,276,750 ms`. |
| **65,535 Tasks** | 65,535 | 100 tasks run in parallel, threads never idle. Can cancel individual ports or implement backoff. Massive queue overhead (65k objects), more context switching, higher GC pressure. |
| **66 Tasks** | 66 | Threads fully utilized, maximum parallelism, low memory/scheduling overhead, easy cancellation, natural back-pressure. |

In these scenarios, **1 and 2 are similar in raw throughput**, but scenario 2 wastes resources. Nocker implements scenario 
3 because it balances **task count, thread utilization, memory efficiency, observability, and back-pressure**.


