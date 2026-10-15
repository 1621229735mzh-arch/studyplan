param(
    [switch]$Offline,
    [switch]$UnitTestsOnly
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$releaseId = Get-Date -Format 'yyyyMMdd-HHmmss'
$outputRoot = Join-Path $projectRoot 'release'
$bundlePath = Join-Path $outputRoot "study-$releaseId"
if (Test-Path $bundlePath) { throw 'Output already exists' }
New-Item -ItemType Directory -Path $bundlePath | Out-Null
try {
    Push-Location (Join-Path $projectRoot 'backend')
    $mavenArgs = @('-B', '-Dmaven.repo.local=../.m2repo')
    if ($Offline) { $mavenArgs += '-o' }
    if ($UnitTestsOnly) {
        $mavenArgs += '-Dtest=MemoServiceTest,TargetServiceTest'
        Write-Warning 'Database integration tests are NOT being executed. This bundle is not cleared for production yet.'
    }
    $mavenArgs += 'verify'
    & .\mvnw.cmd @mavenArgs
    if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed' }
} finally { Pop-Location }
try {
    Push-Location (Join-Path $projectRoot 'frontend')
    & pnpm run test
    if ($LASTEXITCODE -ne 0) { throw 'Frontend tests failed' }
    & pnpm run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
} finally { Pop-Location }

New-Item -ItemType Directory -Path (Join-Path $bundlePath 'artifacts'), (Join-Path $bundlePath 'scripts'), (Join-Path $bundlePath 'nginx') | Out-Null
$jars = @(Get-ChildItem (Join-Path $projectRoot 'backend/target') -Filter '*.jar' | Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' })
if ($jars.Count -ne 1) { throw 'Expected exactly one application jar' }
Copy-Item $jars[0].FullName (Join-Path $bundlePath 'artifacts/app.jar')
Copy-Item (Join-Path $projectRoot 'frontend/dist') (Join-Path $bundlePath 'artifacts/frontend') -Recurse
Copy-Item (Join-Path $projectRoot 'deploy/compose.bundle.yaml') $bundlePath
Copy-Item (Join-Path $projectRoot 'deploy/nginx/bundle.conf.template') (Join-Path $bundlePath 'nginx')
foreach ($name in @('bundle-init.sh', 'bundle-import.sh', 'bundle-cert.sh', 'bundle-backup.sh')) {
    Copy-Item (Join-Path $projectRoot "deploy/scripts/$name") (Join-Path $bundlePath 'scripts')
}
Copy-Item (Join-Path $projectRoot 'docs/operations/manual-deploy.md') (Join-Path $bundlePath 'README.md')
$utf8 = New-Object System.Text.UTF8Encoding($false)
$checks = Get-ChildItem $bundlePath -Recurse -File | Sort-Object FullName | ForEach-Object {
    $relative = $_.FullName.Substring($bundlePath.Length + 1).Replace('\', '/')
    '{0}  {1}' -f (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant(), $relative
}
# Linux sha256sum treats CR in CRLF as part of the filename. Always emit LF.
[IO.File]::WriteAllText((Join-Path $bundlePath 'SHA256SUMS'), ($checks -join "`n") + "`n", $utf8)
$validation = if ($UnitTestsOnly) { 'Frontend tests/build + backend unit tests/package passed. MySQL integration and container runtime NOT verified.' } else { 'Frontend tests/build + backend verify passed. Container runtime still requires verification.' }
[IO.File]::WriteAllText((Join-Path $bundlePath 'VALIDATION.txt'), $validation, $utf8)
$archive = Join-Path $outputRoot "study-$releaseId.tar.gz"
& tar -czf $archive -C $outputRoot "study-$releaseId"
if ($LASTEXITCODE -ne 0) { throw 'Archive creation failed' }
Get-Item $archive | Select-Object FullName, Length
Get-FileHash $archive -Algorithm SHA256
