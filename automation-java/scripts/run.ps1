param(
  [ValidateSet('all','api','ui','smoke','edge')][string]$Suite = 'all',
  [ValidateSet('chromium','firefox','webkit')][string]$Browser = 'chromium',
  [switch]$InstallBrowsers,
  [switch]$Headed
)
$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
  if (-not $env:PLAYWRIGHT_BROWSERS_PATH) { $env:PLAYWRIGHT_BROWSERS_PATH = Join-Path (Get-Location) '.browsers' }
  $mavenCommand = (Get-Command mvn -ErrorAction Stop).Source
  if ($InstallBrowsers) {
    & $mavenCommand --batch-mode test-compile exec:java '-Dexec.mainClass=com.microsoft.playwright.CLI' "-Dexec.args=install $Browser"
    if ($LASTEXITCODE -ne 0) { throw 'Browser installation failed.' }
  }
  $arguments = @('--batch-mode', 'test', "-Dbrowser=$Browser", "-Dheadless=$((-not $Headed.IsPresent).ToString().ToLowerInvariant())")
  if ($Suite -ne 'all') { $arguments += "-P$Suite" }
  & $mavenCommand @arguments
  if ($LASTEXITCODE -ne 0) { throw 'Acceptance tests failed. See target/surefire-reports and target/application.log.' }
} finally { Pop-Location }
