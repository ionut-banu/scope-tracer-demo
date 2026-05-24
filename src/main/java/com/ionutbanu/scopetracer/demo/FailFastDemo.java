package com.ionutbanu.scopetracer.demo;

import com.ionutbanu.scopetracer.core.TracedScope;
import java.util.concurrent.StructuredTaskScope;

/**
 * Cancellation propagation: one task fails fast, and the fail-fast joiner cancels its still-running
 * sibling. The report shows a red (failed) bar and an orange (cancelled) bar that stops short of
 * its natural duration.
 *
 * <pre>{@code
 * mvn -Pfail-fast package exec:exec
 * }</pre>
 */
public final class FailFastDemo {

  private FailFastDemo() {}

  public static void main(String[] args) throws Exception {
    System.out.println("=== FailFastDemo ===");

    DemoSupport.record(
        "fail-fast",
        () -> {
          try (var scope = TracedScope.open("fail-fast")) {
            scope.fork("checkInventory", FailFastDemo::checkInventory);
            scope.fork("slowEnrichment", FailFastDemo::slowEnrichment);
            try {
              scope.join();
            } catch (StructuredTaskScope.FailedException e) {
              System.out.println("scope failed: " + e.getCause().getMessage());
            }
          }
        });
  }

  private static String checkInventory() throws InterruptedException {
    Thread.sleep(50);
    throw new RuntimeException("out of stock");
  }

  private static String slowEnrichment() throws InterruptedException {
    Thread.sleep(500);
    return "enriched";
  }
}
