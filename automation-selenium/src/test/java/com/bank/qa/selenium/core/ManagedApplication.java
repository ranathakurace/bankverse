package com.bank.qa.selenium.core;

import com.bank.qa.selenium.config.RunConfig;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class ManagedApplication implements AutoCloseable {
  private Process process;
  public void start(RunConfig config) throws Exception {
    if (!config.managed) return;
    try (var socket = new ServerSocket()) { socket.bind(new InetSocketAddress("127.0.0.1", config.port)); }
    catch (Exception e) { throw new IllegalStateException("Test port is occupied; refusing to reuse an unknown application", e); }
    Path app = config.root.getParent(); if (!Files.exists(app.resolve("pom.xml"))) throw new IllegalStateException("Run Maven from automation-selenium");
    Files.createDirectories(config.root.resolve("target")); boolean windows=System.getProperty("os.name").toLowerCase().contains("win");
    List<String> command=windows ? List.of("cmd.exe","/d","/s","/c","mvn.cmd","--batch-mode","spring-boot:run","-Dspring-boot.run.useTestClasspath=true") : List.of("mvn","--batch-mode","spring-boot:run","-Dspring-boot.run.useTestClasspath=true");
    var builder=new ProcessBuilder(command).directory(app.toFile()).redirectErrorStream(true).redirectOutput(config.root.resolve("target/application.log").toFile());
    var env=builder.environment(); env.put("SERVER_ADDRESS","127.0.0.1"); env.put("SERVER_PORT",String.valueOf(config.port)); env.put("BANKVERSE_DB_URL","jdbc:h2:mem:bankverse_selenium_e2e;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"); env.put("BANKVERSE_DB_USERNAME","sa"); env.put("BANKVERSE_DB_PASSWORD",""); env.put("SPRING_DATASOURCE_DRIVER_CLASS_NAME","org.h2.Driver"); env.put("SPRING_JPA_DATABASE_PLATFORM","org.hibernate.dialect.H2Dialect"); env.put("SPRING_JPA_HIBERNATE_DDL_AUTO","create-drop"); env.put("SPRING_SQL_INIT_MODE","always"); env.put("SPRING_SQL_INIT_SCHEMA_LOCATIONS","file:./src/test/resources/schema.sql"); env.put("SPRING_JPA_DEFER_DATASOURCE_INITIALIZATION","true"); env.put("BANKVERSE_JWT_SECRET","selenium-disposable-only-signing-key-2026"); env.put("LOGGING_LEVEL_ROOT","WARN");
    process=builder.start(); var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(); long end=System.nanoTime()+Duration.ofSeconds(180).toNanos();
    while(System.nanoTime()<end) { if(!process.isAlive()) throw new IllegalStateException("Application exited; read target/application.log"); try { var response=client.send(HttpRequest.newBuilder(URI.create(config.uiUrl+"/health")).timeout(Duration.ofSeconds(2)).build(),HttpResponse.BodyHandlers.ofString()); if(response.statusCode()==200) return; } catch(Exception ignored) {} Thread.sleep(300); }
    throw new IllegalStateException("Application readiness timed out; read target/application.log");
  }
  public void close() { if(process==null)return; var children=new ArrayList<>(process.descendants().toList()); Collections.reverse(children); children.forEach(ProcessHandle::destroy); process.destroy(); try {process.waitFor(5,TimeUnit.SECONDS);} catch(InterruptedException e){Thread.currentThread().interrupt();} if(process.isAlive())process.destroyForcibly(); process=null; }
}
