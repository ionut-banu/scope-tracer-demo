package com.ionutbanu.scopetracer.demo;

import com.ionutbanu.scopetracer.core.TracedScope;

/**
 * Nesting: a task forked by the outer scope opens its own inner {@link TracedScope}. The analyzer
 * detects the parent/child relationship from the JFR thread id and renders the inner scope indented
 * beneath the task that spawned it.
 *
 * <pre>{@code
 * mvn -Pnested-scopes package exec:exec
 * }</pre>
 */
public final class NestedScopesDemo {

  private NestedScopesDemo() {}

  public static void main(String[] args) throws Exception {
    System.out.println("=== NestedScopesDemo ===");

    DemoSupport.record(
        "nested-scopes",
        () -> {
          try (var outer = TracedScope.open("order-processing")) {
            var payment = outer.fork("processPayment", NestedScopesDemo::processPayment);
            var notification = outer.fork("sendNotification", NestedScopesDemo::sendNotification);
            outer.join();
            System.out.println("payment      : " + payment.get());
            System.out.println("notification : " + notification.get());
          }
        });
  }

  private static String processPayment() throws Exception {
    try (var inner = TracedScope.open("payment-steps")) {
      var authorise = inner.fork("authorise", NestedScopesDemo::authorise);
      var capture = inner.fork("capture", NestedScopesDemo::capture);
      inner.join();
      return authorise.get() + " + " + capture.get();
    }
  }

  private static String authorise() throws InterruptedException {
    Thread.sleep(80);
    return "authorised";
  }

  private static String capture() throws InterruptedException {
    Thread.sleep(60);
    return "captured";
  }

  private static String sendNotification() throws InterruptedException {
    Thread.sleep(120);
    return "notification sent";
  }
}
