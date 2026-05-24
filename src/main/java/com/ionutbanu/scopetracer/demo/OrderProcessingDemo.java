package com.ionutbanu.scopetracer.demo;

import com.ionutbanu.scopetracer.core.TracedScope;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;
import java.util.random.RandomGenerator;

/**
 * Realistic multi-level pipeline. Each order runs a fulfillment scope that forks validation, a
 * nested payment pipeline (fraud + auth in parallel, then a sequential capture), and a nested
 * inventory reservation (two warehouses in parallel). High-value orders occasionally fail the
 * fraud check, which cancels their siblings and aborts the order — exactly the kind of behaviour
 * the HTML report makes visible at a glance.
 *
 * <pre>{@code
 * mvn -Porder-processing package exec:exec
 * }</pre>
 */
public final class OrderProcessingDemo {

  private OrderProcessingDemo() {}

  record Order(String id, double amount, List<String> items) {}

  public static void main(String[] args) throws Exception {
    System.out.println("=== OrderProcessingDemo ===");

    var orders =
        List.of(
            new Order("ORD-001", 149.99, List.of("widget", "gadget")),
            new Order("ORD-002", 849.99, List.of("premium-widget")),
            new Order("ORD-003", 39.99, List.of("sticker-pack")));

    DemoSupport.record(
        "order-processing",
        () -> {
          var rng = RandomGenerator.getDefault();
          for (var order : orders) {
            processOrder(order, rng);
          }
        });
  }

  private static void processOrder(Order order, RandomGenerator rng) throws InterruptedException {
    if (runFulfillment(order, rng)) {
      runDispatch(order, rng);
    } else {
      System.out.println(order.id() + " aborted during fulfillment");
    }
  }

  private static boolean runFulfillment(Order order, RandomGenerator rng)
      throws InterruptedException {
    try (var scope = TracedScope.open("fulfillment-" + order.id())) {
      scope.fork("validateOrder", () -> validateOrder(order));
      scope.fork("paymentPipeline", () -> runPaymentPipeline(order, rng));
      scope.fork("inventoryReservation", () -> runInventoryReservation(order, rng));
      scope.join();
      return true;
    } catch (StructuredTaskScope.FailedException e) {
      return false;
    }
  }

  private static void runDispatch(Order order, RandomGenerator rng) throws InterruptedException {
    try (var scope = TracedScope.open("dispatch-" + order.id())) {
      scope.fork("assignCourier", () -> assignCourier(order));
      scope.fork("generateLabel", () -> generateLabel(order));
      scope.fork("notifyCustomer", () -> notifyCustomer(order));
      scope.join();
      System.out.println(order.id() + " dispatched");
    } catch (StructuredTaskScope.FailedException e) {
      System.out.println(order.id() + " dispatch failed: " + e.getCause().getMessage());
    }
  }

  private static String runPaymentPipeline(Order order, RandomGenerator rng) throws Exception {
    try (var scope = TracedScope.open("payment-pipeline-" + order.id())) {
      var fraud = scope.fork("checkFraud", () -> checkFraud(order, rng));
      var auth = scope.fork("authorizeCard", () -> authorizeCard(order));
      scope.join();
      return "captured(" + fraud.get() + "," + auth.get() + ")";
    }
  }

  private static String runInventoryReservation(Order order, RandomGenerator rng) throws Exception {
    try (var scope = TracedScope.open("inventory-reservation-" + order.id())) {
      var a = scope.fork("warehouse-A", () -> checkWarehouse("A", order));
      var b = scope.fork("warehouse-B", () -> checkWarehouse("B", order, rng));
      scope.join();
      return a.get() + "+" + b.get();
    }
  }

  private static String validateOrder(Order order) throws InterruptedException {
    Thread.sleep(40);
    return "valid";
  }

  private static String checkFraud(Order order, RandomGenerator rng) throws InterruptedException {
    Thread.sleep(90);
    if (order.amount() > 500 && rng.nextInt(100) < 35) {
      throw new RuntimeException("fraud check rejected " + order.id());
    }
    return "clean";
  }

  private static String authorizeCard(Order order) throws InterruptedException {
    Thread.sleep(110);
    return "auth-ok";
  }

  private static String checkWarehouse(String name, Order order) throws InterruptedException {
    Thread.sleep(70);
    return name + ":ok";
  }

  private static String checkWarehouse(String name, Order order, RandomGenerator rng)
      throws InterruptedException {
    Thread.sleep(rng.nextInt(100) < 25 ? 260 : 75);
    return name + ":ok";
  }

  private static String assignCourier(Order order) throws InterruptedException {
    Thread.sleep(60);
    return "DHL";
  }

  private static String generateLabel(Order order) throws InterruptedException {
    Thread.sleep(45);
    return "label.pdf";
  }

  private static String notifyCustomer(Order order) throws InterruptedException {
    Thread.sleep(30);
    return "email-sent";
  }
}
