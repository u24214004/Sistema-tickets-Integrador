[CmdletBinding()]
param([string]$JavaHome = 'C:\Program Files\Java\jdk-21')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'Import-LocalEnvironment.ps1')
if (-not $env:DB_PASSWORD) { throw 'Ejecuta Initialize-Local.ps1 antes de esta prueba.' }
$previousUrl = $env:SPRING_DATASOURCE_URL
$previousUser = $env:SPRING_DATASOURCE_USERNAME
$previousPassword = $env:SPRING_DATASOURCE_PASSWORD
$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path
Push-Location $projectRoot
try {
    $exists = & docker compose -p tickets-apf3 exec -T db psql -U tickets -d postgres -Atc "SELECT 1 FROM pg_database WHERE datname = 'tickets_test'"
    if ($LASTEXITCODE -ne 0) { throw 'El PostgreSQL del proyecto no está disponible.' }
    if ($exists -ne '1') {
        & docker compose -p tickets-apf3 exec -T db createdb -U tickets tickets_test
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la base de pruebas.' }
    }
    $env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:55432/tickets_test'
    $env:SPRING_DATASOURCE_USERNAME = 'tickets'
    $env:SPRING_DATASOURCE_PASSWORD = $env:DB_PASSWORD
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$JavaHome\bin;$previousPath"
    Push-Location backend
    try {
        & .\mvnw.cmd test
        if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas de API sobre PostgreSQL.' }
    } finally { Pop-Location }
} finally {
    Pop-Location
    $env:SPRING_DATASOURCE_URL = $previousUrl
    $env:SPRING_DATASOURCE_USERNAME = $previousUser
    $env:SPRING_DATASOURCE_PASSWORD = $previousPassword
    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
}
