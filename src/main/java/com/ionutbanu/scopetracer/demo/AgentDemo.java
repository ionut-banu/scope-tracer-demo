package com.ionutbanu.scopetracer.demo;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;

/**
 * Zero-code-change tracing. Note what is <em>not</em> here: no {@code TracedScope}, no {@code
 * scope-tracer} import, no event-emitting wrapper. This is plain JDK 26 {@link
 * StructuredTaskScope}. Every scope, fork, success, failure and cancellation in this file is
 * recorded because the {@code scope-tracer-agent} rewrote {@code StructuredTaskScope}'s bytecode at
 * class-load time.
 *
 * <p>Run it with the agent attached:
 *
 * <pre>{@code
 * mvn -Pagent package exec:exec
 * }</pre>
 *
 * <p>The {@link DemoSupport} wrapper starts an in-process JFR recording so this stays a single
 * command; the agent emits its events into whatever recording is active. For the production-style
 * workflow — where the agent itself writes the HTML report the moment {@code JFR.stop} fires — see
 * {@code scripts/run-agent-production.sh}.
 */
public final class AgentDemo {

  private AgentDemo() {}

  public static void main(String[] args) throws Exception {
    System.out.println("=== AgentDemo (zero-code-change tracing) ===");

    DemoSupport.record(
        "agent-demo",
        () -> {
          // (1) Named scope: the agent picks up the name from Config.withName(...).
          try (var scope =
              StructuredTaskScope.open(
                  Joiner.awaitAllSuccessfulOrThrow(),
                  c -> c.withName("checkout-flow").withThreadFactory(Thread.ofVirtual().factory()))) {

            var price = scope.fork(AgentDemo::fetchPrice);
            scope.fork((Runnable) AgentDemo::validateCart); // fork(Runnable) is traced too
            var shipping = scope.fork(AgentDemo::fetchShipping);

            try {
              scope.join();
              System.out.println("price    : " + price.get());
              System.out.println("shipping : " + shipping.get());
            } catch (StructuredTaskScope.FailedException e) {
              System.out.println("scope failed: " + e.getCause().getMessage());
            }
          }

          // (2) Anonymous scope: no withName(...), so the agent derives the
          //     name from the call-site stack frame -> "AgentDemo#main".
          try (var scope =
              StructuredTaskScope.open(
                  Joiner.awaitAllSuccessfulOrThrow(),
                  c -> c.withThreadFactory(Thread.ofVirtual().factory()))) {

            scope.fork(AgentDemo::fetchInventory);
            scope.join();
          }
        });
  }

  private static String fetchPrice() throws InterruptedException {
    Thread.sleep(80);
    return "$19.99";
  }

  private static String fetchShipping() throws InterruptedException {
    Thread.sleep(120);
    return "arrives in 3 days";
  }

  private static void validateCart() {
    try {
      Thread.sleep(60);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static String fetchInventory() throws InterruptedException {
    Thread.sleep(50);
    return "42 in stock";
  }
}
