package com.ionutbanu.scopetracer.demo;

import com.ionutbanu.scopetracer.core.TracedScope;

/**
 * Happy path: three independent service calls run on virtual threads in parallel, all succeed. The
 * generated report shows three green bars and annotates the slowest one as the critical path.
 *
 * <pre>{@code
 * mvn -Pparallel-fetch package exec:exec
 * }</pre>
 */
public final class ParallelFetchDemo {

  private ParallelFetchDemo() {}

  public static void main(String[] args) throws Exception {
    System.out.println("=== ParallelFetchDemo ===");

    DemoSupport.record(
        "parallel-fetch",
        () -> {
          try (var scope = TracedScope.open("parallel-fetch")) {
            var price = scope.fork("fetchPrice", ParallelFetchDemo::fetchPrice);
            var inventory = scope.fork("fetchInventory", ParallelFetchDemo::fetchInventory);
            var shipping = scope.fork(ParallelFetchDemo::fetchShipping);
            scope.join();
            System.out.println("price     : " + price.get());
            System.out.println("inventory : " + inventory.get());
            System.out.println("shipping  : " + shipping.get());
          }
        });
  }

  private static String fetchPrice() throws InterruptedException {
    Thread.sleep(100);
    return "$29.99";
  }

  private static String fetchInventory() throws InterruptedException {
    Thread.sleep(150);
    return "42 units in stock";
  }

  private static String fetchShipping() throws InterruptedException {
    Thread.sleep(80);
    return "arrives in 2 days";
  }
}
