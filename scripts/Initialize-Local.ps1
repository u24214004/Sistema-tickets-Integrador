[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $projectRoot '.env'
if (Test-Path -LiteralPath $envFile) {
    Write-Host '.env ya existe; se conserva su configuración.'
    return
}
function New-LocalSecret {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
$content = @(
    'DB_URL=jdbc:postgresql://localhost:55432/tickets'
    'DB_USER=tickets'
    ('DB_PASSWORD=' + (New-LocalSecret))
    ('JWT_SECRET=' + (New-LocalSecret))
    'CORS_ORIGIN=http://localhost:5173'
)
[IO.File]::WriteAllLines($envFile, $content, (New-Object Text.UTF8Encoding $false))
Write-Host 'Configuración local creada en .env. El archivo está excluido de Git.'
