const { spawn } = require('node:child_process');
const path = require('node:path');

// Maven's test classpath supplies the existing H2 dependency and sequence schema.
// These settings apply only to this child process; no developer database is used.
const args = ['--batch-mode', 'spring-boot:run', '-Dspring-boot.run.useTestClasspath=true'];
const windows = process.platform === 'win32';
const child = spawn(windows ? 'cmd.exe' : 'mvn', windows ? ['/d', '/s', '/c', 'mvn.cmd', ...args] : args, {
  cwd: path.resolve(__dirname, '../..'),
  stdio: 'inherit',
  windowsHide: true,
  env: {
    ...process.env,
    SERVER_ADDRESS: '127.0.0.1',
    SERVER_PORT: process.env.TEST_PORT || '18080',
    SPRING_DATASOURCE_URL: 'jdbc:h2:mem:bankverse_playwright;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE',
    SPRING_DATASOURCE_USERNAME: 'sa',
    SPRING_DATASOURCE_PASSWORD: '',
    SPRING_DATASOURCE_DRIVER_CLASS_NAME: 'org.h2.Driver',
    SPRING_JPA_DATABASE_PLATFORM: 'org.hibernate.dialect.H2Dialect',
    SPRING_JPA_HIBERNATE_DDL_AUTO: 'create-drop',
    SPRING_JPA_SHOW_SQL: 'false',
    SPRING_SQL_INIT_MODE: 'always',
    SPRING_SQL_INIT_SCHEMA_LOCATIONS: 'file:./src/test/resources/schema.sql',
    SPRING_JPA_DEFER_DATASOURCE_INITIALIZATION: 'true',
    LOGGING_LEVEL_ROOT: 'WARN',
    BANKVERSE_JWT_SECRET: 'playwright-disposable-test-only-signing-key-2026',
  },
});
child.on('error', error => { console.error(error.message); process.exit(1); });
child.on('exit', code => process.exit(code ?? 1));
// Playwright terminates the server process group, including Maven and its JVM.
for (const signal of ['SIGTERM', 'SIGINT']) process.on(signal, () => child.kill(signal));
