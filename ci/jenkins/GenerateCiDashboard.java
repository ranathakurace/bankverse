import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/** Build-local, script-free dashboard. Run: java ci/jenkins/GenerateCiDashboard.java . */
class GenerateCiDashboard {
    static Path root;
    static List<String> issues = new ArrayList<>();
    record Tests(int total, int failed, int errors, int skipped, boolean available) {
        boolean passed() { return available && total > 0 && failed == 0 && errors == 0 && skipped == 0; }
        String text() { return available ? (total-failed-errors-skipped) + " / " + total + " passed" : "Unavailable"; }
    }
    record Event(String kind, String test, String thread, String session, Instant at) {}
    static String esc(Object value) {
        return String.valueOf(value).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    static String env(String name, String fallback) { return System.getenv().getOrDefault(name, fallback); }
    static Document xml(Path path) throws Exception {
        var f = DocumentBuilderFactory.newInstance();
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return f.newDocumentBuilder().parse(path.toFile());
    }
    static int number(Element e, String attribute) { return Integer.parseInt(e.getAttribute(attribute)); }
    static Tests tests(String folder) {
        int total=0, failed=0, errors=0, skipped=0;
        try (var paths=Files.list(root.resolve(folder))) {
            var reports=paths.filter(p -> p.getFileName().toString().startsWith("TEST-")
                    && p.toString().endsWith(".xml")).toList();
            if(reports.isEmpty()) throw new IllegalStateException("No test XML files");
            for(var path:reports) {
                Element e=xml(path).getDocumentElement();
                if(!"testsuite".equals(e.getTagName())) throw new IllegalStateException("Unexpected test XML format");
                total+=number(e,"tests");failed+=number(e,"failures");errors+=number(e,"errors");skipped+=number(e,"skipped");
            }
            return new Tests(total,failed,errors,skipped,true);
        } catch(Exception e) { issues.add(folder+": "+e.getMessage());return new Tests(0,0,0,0,false); }
    }
    static String card(String name, String value, String detail, boolean good) {
        return "<article class='card'><h2>"+esc(name)+"</h2><strong class='"+(good?"good":"neutral")+"'>"
                +esc(value)+"</strong><p>"+esc(detail)+"</p></article>";
    }
    static String link(String path,String text) {
        return Files.exists(root.resolve(path)) ? "<a href='../"+esc(path.replace(" ","%20"))+"'>"+esc(text)+"</a>"
                : "<span>"+esc(text)+" — unavailable</span>";
    }
    static String testDetail(Tests t) { return t.available ? t.failed+" failures · "+t.errors+" errors · "+t.skipped+" skipped" : "Stage may not have run or reports are missing."; }
    public static void main(String[] args) throws Exception {
        root=Path.of(args.length>0?args[0]:".").toAbsolutePath().normalize();
        Path output=root.resolve("ci-dashboard");Files.createDirectories(output);
        Tests backend=tests("target/surefire-reports"),ui=tests("automation-selenium/target/surefire-reports");
        double instructions=-1,branches=-1,threshold=-1;
        try {
            Element report=xml(root.resolve("target/site/jacoco/jacoco.xml")).getDocumentElement();
            for(Node n=report.getFirstChild();n!=null;n=n.getNextSibling()) {
                if(n instanceof Element e && "counter".equals(e.getTagName())) {
                    int covered=number(e,"covered"),missed=number(e,"missed");
                    double ratio=covered+missed==0?-1:100.0*covered/(covered+missed);
                    if("INSTRUCTION".equals(e.getAttribute("type"))) instructions=ratio;
                    if("BRANCH".equals(e.getAttribute("type"))) branches=ratio;
                }
            }
            var values=xml(root.resolve("pom.xml")).getElementsByTagName("jacoco.minimum.instruction.coverage");
            if(values.getLength()>0) threshold=100*Double.parseDouble(values.item(0).getTextContent().trim());
        } catch(Exception e) { issues.add("Coverage: "+e.getMessage()); }
        int findings=-1,analysisErrors=-1;
        try {
            var report=xml(root.resolve("target/spotbugsXml.xml"));
            findings=report.getElementsByTagName("BugInstance").getLength();
            var errors=report.getElementsByTagName("Errors");
            if(errors.getLength()==0) throw new IllegalStateException("Analysis completion metadata missing");
            analysisErrors=number((Element)errors.item(0),"errors");
        } catch(Exception e) { issues.add("SpotBugs: "+e.getMessage()); }
        List<Event> events=new ArrayList<>();Set<String> workers=new TreeSet<>(),sessions=new HashSet<>(),closed=new HashSet<>();
        int peak=0,active=0;double span=-1;boolean lifecycleAvailable=false;
        try {
            for(String line:Files.readAllLines(root.resolve("automation-selenium/target/browser-sessions.tsv"))) {
                var x=line.split("\t",-1);if(x.length!=5) throw new IllegalStateException("Invalid lifecycle record");
                if(!Set.of("OPEN","CLOSED").contains(x[0])) throw new IllegalStateException("Unknown lifecycle event");
                events.add(new Event(x[0],x[1],x[2],x[3],Instant.parse(x[4])));
            }
            if(events.isEmpty()) throw new IllegalStateException("Empty lifecycle report");
            events.sort(Comparator.comparing(Event::at).thenComparing(Event::kind));
            for(Event e:events) {
                workers.add(e.thread);
                if(e.kind.equals("OPEN")){sessions.add(e.session);active++;peak=Math.max(peak,active);}
                else {closed.add(e.session);active--;}
            }
            span=Duration.between(events.get(0).at,events.get(events.size()-1).at).toNanos()/1e9;
            lifecycleAvailable=true;
        } catch(Exception e) { issues.add("Browser lifecycle: "+e.getMessage()); }
        boolean uiGate=false;
        try { uiGate=Files.readString(root.resolve("automation-selenium/target/ui-readiness.txt")).trim().startsWith("UI_GATE_PASS "); }
        catch(Exception e) { issues.add("UI readiness marker missing"); }
        String buildResult=env("CI_BUILD_RESULT","UNKNOWN");
        boolean backendGate=env("BACKEND_QUALITY_PASSED","false").equals("true");
        boolean uiGateRan=env("UI_GATE_PASSED","false").equals("true");
        boolean complete=backend.available&&ui.available&&instructions>=0&&threshold>=0&&findings>=0&&analysisErrors>=0&&lifecycleAvailable;
        boolean lifecycleHealthy=lifecycleAvailable&&sessions.size()==ui.total&&sessions.equals(closed)&&active==0;
        boolean ready=complete&&backend.passed()&&ui.passed()&&instructions>=threshold&&findings==0&&analysisErrors==0
                &&lifecycleHealthy&&uiGate&&backendGate&&uiGateRan&&buildResult.equals("SUCCESS");
        String status=ready?"AUTOMATED GATES PASSED":buildResult.equals("FAILURE")||buildResult.equals("ABORTED")
                ||(backend.available&&!backend.passed())||(ui.available&&!ui.passed())?"BLOCKED":"INCOMPLETE / GATES NOT CONFIRMED";
        StringBuilder html=new StringBuilder("<!doctype html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1'><title>BankVerse CI execution dashboard</title><link rel='stylesheet' href='dashboard.css'></head><body><main>");
        html.append("<header><p class='eyebrow'>BANKVERSE · QA EXECUTION EVIDENCE</p><h1>CI execution dashboard</h1><p>")
            .append(esc(env("JOB_NAME","Local verification"))).append(" · Build ").append(esc(env("BUILD_NUMBER","unassigned")))
            .append(" · Commit <code>").append(esc(env("BANKVERSE_COMMIT","unknown"))).append("</code></p></header>");
        html.append("<section class='banner ").append(ready?"passed":"attention").append("'><h2>").append(status)
            .append("</h2><p>Jenkins result: ").append(esc(buildResult)).append(". Backend gates: ").append(backendGate?"passed":"not confirmed")
            .append(". UI gate: ").append(uiGate&&uiGateRan?"passed":"not confirmed")
            .append(".</p><p>Automated release-readiness evidence; deployment approval, security scans and business signoff are separate.</p></section>");
        html.append("<section class='cards'>").append(card("Backend tests",backend.text(),testDetail(backend),backend.passed()))
            .append(card("Selenium tests",ui.text(),testDetail(ui),ui.passed()))
            .append(card("Instruction coverage",instructions<0?"Unavailable":String.format(Locale.ROOT,"%.2f%%",instructions),
                threshold<0?"Threshold unavailable":String.format(Locale.ROOT,"Configured minimum: %.0f%%; included classes only",threshold),instructions>=threshold&&threshold>=0))
            .append(card("Branch coverage",branches<0?"Unavailable":String.format(Locale.ROOT,"%.2f%%",branches),"Displayed metric; no branch threshold is enforced",branches>=0))
            .append(card("SpotBugs",findings<0?"Unavailable":findings+" findings",analysisErrors<0?"Analysis status unavailable":analysisErrors+" analysis errors; configured filters apply",findings==0&&analysisErrors==0))
            .append(card("Parallel browser execution",lifecycleAvailable?workers.size()+" workers · peak "+peak+" sessions":"Unavailable",
                lifecycleAvailable?String.format(Locale.ROOT,"%d opened / %d closed · %.2fs measured session span",sessions.size(),closed.size(),span):"No lifecycle evidence",lifecycleHealthy)).append("</section>");
        html.append("<section><h2>Browser execution timeline</h2><p>OPEN begins after WebDriver creation; CLOSED records a successful quit call. This span excludes compilation and application startup. A single run cannot establish sequential-versus-parallel speedup.</p><div class='scroll'><table><thead><tr><th>Event</th><th>Test</th><th>Worker</th><th>Session</th><th>Timestamp (UTC)</th></tr></thead><tbody>");
        for(Event e:events) html.append("<tr><td>").append(esc(e.kind)).append("</td><td>").append(esc(e.test)).append("</td><td>").append(esc(e.thread)).append("</td><td><code>").append(esc(e.session)).append("</code></td><td>").append(esc(e.at)).append("</td></tr>");
        html.append("</tbody></table></div></section><section><h2>Failure screenshots</h2>");
        int images=0;Path evidence=root.resolve("automation-selenium/target/artifacts");
        if(Files.isDirectory(evidence)) try(var paths=Files.walk(evidence)) {
            for(Path p:paths.filter(p->p.getFileName().toString().equals("failure.png")).sorted().toList()) {
                images++;String relative=root.relativize(p).toString().replace('\\','/');
                html.append("<figure><a href='../").append(esc(relative)).append("'><img loading='lazy' src='../").append(esc(relative)).append("' alt='Failure screenshot for ").append(esc(p.getParent().getFileName())).append("'></a><figcaption>").append(esc(p.getParent().getFileName())).append(" · ")
                    .append(link(root.relativize(p.getParent().resolve("url.txt")).toString().replace('\\','/'),"Browser URL")).append(" · ")
                    .append(link(root.relativize(p.getParent().resolve("execution.txt")).toString().replace('\\','/'),"Execution metadata")).append("</figcaption></figure>");
            }
        }
        if(images==0) html.append("<p>No browser screenshots were captured in this build. This is expected for passing tests; a failure before browser creation may also produce none.</p>");
        html.append("</section><section><h2>Build evidence</h2><ul>");
        String[][] links={{"target/surefire-reports/emailable-report.html","Backend TestNG report"},{"automation-selenium/target/surefire-reports/emailable-report.html","Selenium TestNG report"},{"target/site/jacoco/index.html","JaCoCo coverage report"},{"target/spotbugsXml.xml","SpotBugs XML"},{"automation-selenium/target/application.log","Application log"},{"automation-selenium/target/browser-sessions.tsv","Browser lifecycle records"},{"automation-selenium/target/ui-readiness.txt","UI readiness result"}};
        for(var l:links) html.append("<li>").append(link(l[0],l[1])).append("</li>");
        html.append("</ul></section>");
        if(!issues.isEmpty()) {html.append("<section><h2>Missing or unreadable evidence</h2><ul>");for(String issue:issues) html.append("<li>").append(esc(issue)).append("</li>");html.append("</ul></section>");}
        html.append("<footer>Generated ").append(esc(Instant.now())).append(" · Build-specific report; screenshot retention follows Jenkins build retention.</footer></main></body></html>");
        Files.writeString(output.resolve("index.html"),html);
        Files.writeString(output.resolve("summary.txt"),status+"\nJenkins result="+buildResult+"\nBackend="+backend.text()+"\nSelenium="+ui.text()+"\nInstruction coverage="+instructions+"\nBranch coverage="+branches+"\nSpotBugs findings="+findings+"\nWorkers="+workers.size()+"\nPeak sessions="+peak+"\nScreenshots="+images+"\n");
        Files.copy(root.resolve("ci/jenkins/dashboard.css"),output.resolve("dashboard.css"),StandardCopyOption.REPLACE_EXISTING);
        System.out.println("DASHBOARD "+output.resolve("index.html")+" | "+status);
    }
}
