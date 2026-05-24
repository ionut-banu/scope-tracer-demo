# scope-tracer-demo

Runnable examples for [**scope-tracer**](https://github.com/ionut-banu/scope-tracer) —
a library and Java agent that visualises
`java.util.concurrent.StructuredTaskScope` task trees, lifetimes, and
cancellation propagation on JDK 26+.

This is a **standalone project**. It depends on the published
`com.ionutbanu:scope-tracer-*` artifacts from Maven Central — there is no need
to clone or build scope-tracer itself.

> Companion article:
> *"Tracing Java Structured Concurrency Without Touching a Line of Code"*
> — https://medium.com/@ionutbanu/PLACEHOLDER-SLUG

---

## Requirements

| Tool  | Version                          |
|-------|----------------------------------|
| Java  | 26+ (the demos use `--enable-preview`) |
| Maven | 3.9+                             |

`StructuredTaskScope` is a JDK **preview** API, so every command below runs
under `--enable-preview`. The demo `pom.xml` already wires that into the
compiler and the `exec:exec` runner.

---

## The two ways to trace

scope-tracer can record your structured-concurrency activity two ways. Both
emit the **same six JFR events**, so the analyzer and the HTML report are
identical regardless of which you choose.

| | What you change | Demo |
|---|---|---|
| **Library** (`TracedScope`) | Swap `StructuredTaskScope` for `TracedScope` | `ParallelFetchDemo`, `FailFastDemo`, `NestedScopesDemo`, `OrderProcessingDemo` |
| **Agent** (`-javaagent`) | **Nothing** — plain `StructuredTaskScope`, instrumented at the bytecode level | `AgentDemo` |

Do **not** combine the two on the same scope — you would get duplicate events.

---

## Run the library demos

Each command records a `.jfr`, renders a self-contained `.html` report into
`target/`, and prints the absolute paths. Open the HTML in any browser — no
server required.

```bash
mvn -Pparallel-fetch  package exec:exec   # 3 parallel tasks, all succeed
mvn -Pfail-fast        package exec:exec   # 1 task fails, sibling cancelled
mvn -Pnested-scopes    package exec:exec   # inner scope nested under a task
mvn -Porder-processing package exec:exec   # multi-level e-commerce pipeline
```

What to look for in the report:

- **green** bar — task succeeded
- **amber** bar — the critical-path task (it determined the scope duration)
- **red** bar — task failed (hover for the exception type/message)
- **orange** bar — task cancelled by the fail-fast joiner before it finished
- nested scopes are indented beneath the task that opened them

---

## Run the agent demo (zero code changes)

`AgentDemo` contains **no scope-tracer code at all** — just plain
`StructuredTaskScope`. The agent rewrites `StructuredTaskScope`'s bytecode as
the JDK loads it, so every scope and fork is traced anyway.

```bash
mvn -Pagent package exec:exec
```

The `agent` profile copies the published agent fat-jar
(`com.ionutbanu:scope-tracer-agent:0.2.0:jar:agent`) to
`target/agent/scope-tracer-agent.jar` and attaches it with
`-javaagent`.

### Production-style: let the agent write the report

In a real service you don't wrap a recording in code — you start JFR with a
destination file (via `-XX:StartFlightRecording` or `jcmd`), and the agent
writes the HTML report **automatically** the moment the recording stops.
`scripts/run-agent-production.sh` demonstrates exactly that:

```bash
./scripts/run-agent-production.sh
```

It runs `AgentDemo` with:

```
-javaagent:target/agent/scope-tracer-agent.jar
-XX:StartFlightRecording=filename=target/agent-prod.jfr,dumponexit=true
```

No in-process recording, no analyzer call in code. When the JVM exits and JFR
dumps, the agent's `FlightRecorderListener` parses the `.jfr` and drops
`target/agent-prod.html` next to it. That is the whole point of the agent: an
operator can trace a running production service with `jcmd` and get an HTML
report for free.

Useful agent arguments (`-javaagent:...jar=key=value,...`):

| Argument | Effect |
|---|---|
| `html=false` | disable auto-HTML generation |
| `output.dir=<path>` | write the HTML somewhere other than next to the `.jfr` |
| `output.suffix=<ext>` | filename suffix replacing `.jfr` (default `.html`) |
| `min.scopes=<N>` | skip HTML if the recording has fewer than N scopes |
| `verbose` | print every instrumented class to stderr |

---

## Project layout

```
scope-tracer-demo/
├── pom.xml                         # standalone; depends on Maven Central artifacts
├── scripts/
│   └── run-agent-production.sh     # agent + -XX:StartFlightRecording, auto-HTML
└── src/main/java/com/ionutbanu/scopetracer/demo/
    ├── DemoSupport.java            # records JFR + renders HTML in-process
    ├── ParallelFetchDemo.java
    ├── FailFastDemo.java
    ├── NestedScopesDemo.java
    ├── OrderProcessingDemo.java
    └── AgentDemo.java              # plain StructuredTaskScope, traced by the agent
```

---

## License

[Apache License 2.0](LICENSE). scope-tracer itself is also Apache 2.0.
