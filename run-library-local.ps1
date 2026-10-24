$ErrorActionPreference = "Stop"

$tomcatHome = "C:\Users\CHANDU\Downloads\tomcat-10.1.36-windows-x64\apache-tomcat-10.1.36"
$env:CATALINA_HOME = $tomcatHome
$env:CATALINA_BASE = $tomcatHome
$javaExecutable = Get-Command java.exe -ErrorAction Stop
$javaBin = Split-Path -Parent $javaExecutable.Source
$env:JAVA_HOME = Split-Path -Parent $javaBin
$catalina = Join-Path $tomcatHome "bin\catalina.bat"
$warFile = Join-Path $PSScriptRoot "target\smart-library.war"
$deployedWar = Join-Path $tomcatHome "webapps\smart-library.war"

if (-not (Test-Path $catalina)) {
    throw "Tomcat was not found at '$tomcatHome'. Update the tomcatHome path in run-library-local.ps1."
}

if (-not (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    throw "Java was not found at '$env:JAVA_HOME'. Install a JDK and ensure java.exe is on PATH."
}

if (-not (Test-Path $warFile)) {
    throw "The project WAR was not found. Build the app with 'mvn clean test package', then run this script again."
}

$env:LIBRARY_DB_URL = $null
$env:LIBRARY_DB_USER = $null
$env:LIBRARY_DB_PASSWORD = $null
$env:APP_ENV = $null

try {
    $listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
    if ($listener) {
        Write-Host "Stopping the existing Tomcat instance..."
        & $env:ComSpec /c "call `"$catalina`" stop"

        $attempt = 0
        do {
            Start-Sleep -Seconds 1
            $listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
            $attempt++
        } while ($listener -and $attempt -lt 30)

        if ($listener) {
            throw "Tomcat did not stop on port 8080. Close its existing terminal, then run this script again."
        }
    }

    Copy-Item -Path $warFile -Destination $deployedWar -Force
    Write-Host "Starting the local-only library without sign-in."
    Write-Host "Database file: $HOME\.alliance-smart-library\library.mv.db"
    Write-Host "Keep this PowerShell window open while using the library."
    & $env:ComSpec /c "call `"$catalina`" run"
    if ($LASTEXITCODE -ne 0) {
        throw "Tomcat exited with code $LASTEXITCODE."
    }
}
finally {
    $env:LIBRARY_DB_URL = $null
    $env:LIBRARY_DB_USER = $null
    $env:LIBRARY_DB_PASSWORD = $null
}
