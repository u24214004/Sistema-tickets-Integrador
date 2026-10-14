$projectRoot = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $projectRoot '.env'
if (Test-Path -LiteralPath $envFile) {
    foreach ($line in [IO.File]::ReadAllLines($envFile)) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) { continue }
        $parts = $line.Split('=', 2)
        if ($parts.Length -ne 2 -or $parts[0] -notmatch '^(DB_URL|DB_USER|DB_PASSWORD|JWT_SECRET|CORS_ORIGIN)$') {
            throw 'Variable no permitida o línea inválida en .env'
        }
        [Environment]::SetEnvironmentVariable($parts[0], $parts[1], 'Process')
    }
}
