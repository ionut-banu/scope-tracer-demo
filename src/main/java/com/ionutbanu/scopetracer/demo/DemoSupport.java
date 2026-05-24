package com.ionutbanu.scopetracer.demo;

import com.ionutbanu.scopetracer.analyzer.HtmlRenderer;
import com.ionutbanu.scopetracer.analyzer.JfrParser;
import java.nio.file.Files;
import java.nio.file.Path;
import jdk.jfr.Recording;

/**
 * Records a JFR recording around a demo action, then turns the resulting {@code .jfr} file into a
 * self-contained HTML report using the published {@code scope-tracer-analyzer} artifact.
 *
 * <p>This mirrors what a real application would do when it wants an in-process report. In
 * production you would more typically attach the agent and let it auto-generate the HTML on {@code
 * JFR.stop} (see {@link AgentDemo}); this helper keeps the {@code TracedScope}-based demos
 * single-command.
 */
final class DemoSupport {

  private DemoSupport() {}

  @FunctionalInterface
  interface DemoAction {
    void run() throws Exception;
  }

  /**
   * Runs {@code action} under a JFR recording that captures the {@code
   * com.ionutbanu.scopetracer.*} event family, dumps {@code target/<name>.jfr}, and renders {@code
   * target/<name>.html}.
   */
  static void record(String name, DemoAction action) throws Exception {
    Path targetDir = Path.of("target");
    Files.createDirectories(targetDir);
    Path jfr = targetDir.resolve(name + ".jfr");
    Path html = targetDir.resolve(name + ".html");

    try (var recording = new Recording()) {
      recording.enable("com.ionutbanu.scopetracer.*");
      recording.start();
      try {
        action.run();
      } finally {
        recording.stop();
      }
      recording.dump(jfr);
    }

    var model = JfrParser.parse(jfr);
    Files.writeString(html, HtmlRenderer.render(model));

    System.out.println("JFR  -> " + jfr.toAbsolutePath());
    System.out.println("HTML -> " + html.toAbsolutePath());
  }
}
