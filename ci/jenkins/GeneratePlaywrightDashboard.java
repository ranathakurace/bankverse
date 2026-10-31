import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/** Usage: java ci/jenkins/GeneratePlaywrightDashboard.java [repository] [output-directory] */
class GeneratePlaywrightDashboard {
    static String esc(Object value) {
        return String.valueOf(value).replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
    static int count(Element e, String key) {
        return e.hasAttribute(key) ? Integer.parseInt(e.getAttribute(key)) : 0;
    }
    static String seconds(String value) {
        try { return String.format(Locale.ROOT, "%.2f s", Double.parseDouble(value)); }
        catch (NumberFormatException e) { return "Unavailable"; }
    }
    static String metric(String label, Object value, String detail) {
        return "<article class='metric'><h2>" + esc(label) + "</h2><strong>" + esc(value)
            + "</strong><p>" + esc(detail) + "</p></article>";
    }
    static Document readXml(Path file) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(file.toFile());
    }
    static String relative(Path output, Path target) {
        try { return output.relativize(target).toString().replace('\\', '/').replace(" ", "%20"); }
        catch (IllegalArgumentException e) { return ""; }
    }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        Path output = (args.length > 1 ? Path.of(args[1]) : root.resolve("playwright-dashboard")).toAbsolutePath().normalize();
        Files.createDirectories(output);
        Path report = root.resolve("automation/test-results/junit.xml");
        Element summary = null;
        NodeList suites = null;
        String problem = "";
        try {
            Document document = readXml(report);
            summary = document.getDocumentElement();
            if (!Set.of("testsuites", "testsuite").contains(summary.getTagName()))
                throw new IllegalArgumentException("Unexpected JUnit root element");
            suites = document.getElementsByTagName("testsuite");
            // Validate before writing the page, including malformed numeric attributes.
            for (String key : List.of("tests", "failures", "errors", "skipped")) count(summary, key);
        } catch (Exception e) {
            summary = null;
            problem = "JUnit results are unavailable or unreadable. Check test execution and the report path.";
        }
        int total = summary == null ? 0 : count(summary, "tests");
        int failed = summary == null ? 0 : count(summary, "failures") + count(summary, "errors");
        int skipped = summary == null ? 0 : count(summary, "skipped");
        int passed = Math.max(0, total - failed - skipped);
        String exitCode = env("CI_TEST_EXIT_CODE", "unknown");
        boolean executionFailed = !exitCode.equals("unknown") && !exitCode.equals("0");
        String status = executionFailed || failed > 0 ? "Tests failed"
            : summary == null || total == 0 ? "Results unavailable"
            : skipped > 0 ? "Passed with skipped tests" : "All tests passed";
        String tone = executionFailed || failed > 0 ? "failed" : summary == null || total == 0 || skipped > 0 ? "attention" : "passed";
        String runTime = summary == null ? "Unavailable" : seconds(summary.getAttribute("time"));
        String reportDate = suites != null && suites.getLength() > 0
            ? ((Element)suites.item(0)).getAttribute("timestamp") : "Unavailable";
        StringBuilder html = new StringBuilder("<!doctype html><html lang='en'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1'><title>BankVerse | CI dashboard</title><link rel='stylesheet' href='dashboard.css'></head><body><main>");
        html.append("<header><a class='brand' href='#overview'><span class='logo'>B</span> BANKVERSE <span class='muted'>/ QUALITY ENGINEERING</span></a><span class='chip'>PLAYWRIGHT CI</span></header>")
            .append("<section id='overview' class='heading'><p class='eyebrow'>CONTINUOUS INTEGRATION</p><h1>Build confidence.<br><span>Every test, in view.</span></h1><p class='muted'>API and browser execution evidence for ")
            .append(esc(env("JOB_NAME", "BankVerse \u00b7 local report"))).append(".</p></section>")
            .append("<section class='status ").append(tone).append("'><div><p class='eyebrow'>TEST EXECUTION</p><h2>").append(status)
            .append("</h2><p>").append(summary == null ? esc(problem) : passed + " passed \u00b7 " + failed + " failed / errors \u00b7 " + skipped + " skipped")
            .append("</p></div><div class='status-number'>").append(summary == null || total == 0 ? "\u2014" : String.format(Locale.ROOT, "%.1f%%", 100.0 * passed / total))
            .append("<small>overall pass rate</small></div></section>")
            .append("<section class='metrics' aria-label='Test metrics'>")
            .append(metric("TOTAL TESTS", summary == null ? "\u2014" : total, "Includes environment setup"))
            .append(metric("PASSED", summary == null ? "\u2014" : passed, "Successful test cases"))
            .append(metric("FAILED / ERRORS", summary == null ? "\u2014" : failed, "JUnit failures and errors"))
            .append(metric("RUN DURATION", runTime, "JUnit report wall time")).append("</section>")
            .append("<div class='columns'><section class='panel'><div class='section-title'><h2>Execution summary</h2><span class='muted'>By project and suite</span></div><div class='table-scroll'><table><thead><tr><th scope='col'>Project / suite</th><th scope='col'>Passed</th><th scope='col'>Failed</th><th scope='col'>Skipped</th><th scope='col'>Duration</th></tr></thead><tbody>");
        if (summary != null && suites != null) for (int i = 0; i < suites.getLength(); i++) {
            Element suite = (Element)suites.item(i);
            int failures = count(suite, "failures") + count(suite, "errors");
            html.append("<tr><td><span class='project'>").append(esc(suite.getAttribute("hostname"))).append("</span><span class='suite'>")
                .append(esc(suite.getAttribute("name").replace('\\', '/'))).append("</span></td><td class='good'>")
                .append(Math.max(0, count(suite, "tests") - failures - count(suite, "skipped")))
                .append("</td><td>").append(failures).append("</td><td>").append(count(suite, "skipped"))
                .append("</td><td>").append(esc(seconds(suite.getAttribute("time")))).append("</td></tr>");
        }
        else html.append("<tr><td colspan='5'>No current test results are available.</td></tr>");
        html.append("</tbody></table></div></section><aside class='panel'><h2>Build context</h2><dl>");
        String[][] metadata = {{"Build", env("BUILD_NUMBER", "Local generation")}, {"Branch", env("GIT_BRANCH", "Not supplied")},
            {"Commit", env("GIT_COMMIT", "Not supplied")}, {"Test exit code", exitCode}, {"Report timestamp", reportDate}};
        for (String[] item : metadata) html.append("<dt>").append(esc(item[0])).append("</dt><dd>").append(esc(item[1])).append("</dd>");
        html.append("</dl><p class='note'>This page is a build snapshot. It does not auto-refresh or show historical trends. Test status does not confirm the final Jenkins build result.</p></aside></div>")
            .append("<section class='panel'><h2>Reports &amp; evidence</h2><div class='links'>");
        String[][] evidence = {{"automation/playwright-report/index.html", "Playwright HTML report", "Detailed steps and attachments"},
            {"automation/test-results/junit.xml", "JUnit XML", "Machine-readable test results"}};
        for (String[] item : evidence) {
            Path file = root.resolve(item[0]);
            if (Files.exists(file)) html.append("<a class='report-link' href='").append(esc(relative(output, file))).append("'><strong>")
                .append(esc(item[1])).append(" \u2197</strong><span>").append(esc(item[2])).append("</span></a>");
            else html.append("<div class='report-link muted'><strong>").append(esc(item[1])).append("</strong><span>Not available</span></div>");
        }
        html.append("</div><p class='note'>Jenkins may block JavaScript in the Playwright report. Download its complete folder to view it locally. Keep this dashboard and its CSS together.</p></section>")
            .append("<footer><span>BANKVERSE \u00b7 CI INTELLIGENCE</span><span>Generated ").append(esc(Instant.now())).append(" \u00b7 Source: Playwright JUnit</span></footer></main></body></html>");
        Files.writeString(output.resolve("index.html"), html);
        Files.copy(root.resolve("ci/jenkins/playwright-dashboard.css"), output.resolve("dashboard.css"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Dashboard generated: " + output.resolve("index.html") + " | " + status);
    }
}
