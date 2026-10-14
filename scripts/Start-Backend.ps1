[CmdletBinding()]
param([switch]$Demo, [switch]$SeedDemo, [string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $PSBoundParameters.ContainsKey('JavaHome')) {
    $candidate = 'C:\Program Files\Java\jdk-21'
    if (Test-Path -LiteralPath $candidate) { $JavaHome = $candidate }
}
if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\java.exe'))) {
    throw 'Indica un JDK 21: -JavaHome C:\ruta\jdk-21'
}
$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path
$previousSeed = $env:APP_DEMO_ENABLED
$previousProfiles = $env:SPRING_PROFILES_ACTIVE
try {
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$JavaHome\bin;$previousPath"
    . (Join-Path $PSScriptRoot 'Import-LocalEnvironment.ps1')
    if ($Demo) { $env:SPRING_PROFILES_ACTIVE = 'demo' }
    else { $env:SPRING_PROFILES_ACTIVE = '' }
    if ($SeedDemo -or $Demo) { $env:APP_DEMO_ENABLED = 'true' }
    else { $env:APP_DEMO_ENABLED = 'false' }
    Push-Location (Join-Path $projectRoot 'backend')
    try {
        & .\mvnw.cmd spring-boot:run
        if ($LASTEXITCODE -ne 0) { throw 'El backend no pudo iniciarse.' }
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
    $env:APP_DEMO_ENABLED = $previousSeed
    $env:SPRING_PROFILES_ACTIVE = $previousProfiles
}
