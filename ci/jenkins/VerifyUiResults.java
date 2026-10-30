import java.nio.file.*;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/** Run from automation-selenium: java ../ci/jenkins/VerifyUiResults.java target 6 */
class VerifyUiResults {
    public static void main(String[] args) throws Exception {
        Path target = Path.of(args.length > 0 ? args[0] : "target");
        int minimumTests = args.length > 1 ? Integer.parseInt(args[1]) : 6;
        if (minimumTests < 1) throw new IllegalArgumentException("Minimum test count must be positive");
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var report = factory.newDocumentBuilder().parse(
                target.resolve("surefire-reports/testng-results.xml").toFile());
        var methods = report.getElementsByTagName("test-method");
        Set<String> tests = new HashSet<>();
        for (int i = 0; i < methods.getLength(); i++) {
            var method = (Element) methods.item(i);
            if (!"PASS".equals(method.getAttribute("status"))) {
                throw new IllegalStateException("Failed/skipped test or configuration: "
                        + method.getAttribute("name") + " status=" + method.getAttribute("status"));
            }
            if (!"true".equals(method.getAttribute("is-config"))) {
                String name = ((Element) method.getParentNode()).getAttribute("name")
                        + "." + method.getAttribute("name");
                if (!tests.add(name)) throw new IllegalStateException("Duplicate/retried test: " + name);
            }
        }
        if (tests.size() < minimumTests) {
            throw new IllegalStateException("Expected at least " + minimumTests + " tests; got " + tests.size());
        }
        Map<String, String[]> opened = new HashMap<>();
        Set<String> closed = new HashSet<>();
        Set<String> workers = new HashSet<>();
        Set<String> browserTests = new HashSet<>();
        for (String line : Files.readAllLines(target.resolve("browser-sessions.tsv"))) {
            String[] event = line.split("\t", -1);
            if (event.length != 5) throw new IllegalStateException("Invalid browser lifecycle record");
            String session = event[3];
            if ("OPEN".equals(event[0])) {
                if (opened.putIfAbsent(session, event) != null) {
                    throw new IllegalStateException("Reused browser session: " + session);
                }
                workers.add(event[2]);
                browserTests.add(event[1]);
            } else if ("CLOSED".equals(event[0])) {
                String[] owner = opened.get(session);
                if (owner == null || !owner[1].equals(event[1]) || !owner[2].equals(event[2])
                        || !closed.add(session)) {
                    throw new IllegalStateException("Browser ownership/cleanup mismatch: " + session);
                }
            } else throw new IllegalStateException("Unknown lifecycle event: " + event[0]);
        }
        Set<String> shortTestNames = new HashSet<>();
        for (String test : tests) {
            String[] parts = test.split("\\.");
            shortTestNames.add(parts[parts.length - 2] + "." + parts[parts.length - 1]);
        }
        if (opened.size() != tests.size() || !closed.equals(opened.keySet())
                || !browserTests.equals(shortTestNames)) {
            throw new IllegalStateException("Not every test has an independently closed browser session");
        }
        String summary = "UI_GATE_PASS tests=" + tests.size() + " sessions=" + opened.size()
                + " workers=" + workers.size();
        Files.writeString(target.resolve("ui-readiness.txt"), summary + System.lineSeparator());
        System.out.println(summary);
    }
}
