$ErrorActionPreference = 'Stop'

$apiRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$outputDir = Join-Path $apiRoot 'build\test-classes'
$normalizedSourceDir = Join-Path $apiRoot 'build\test-sources'
$sourceFiles = @()
$testFile = Join-Path $apiRoot 'tests\ApiContractTest.java'
$classpath = Join-Path $apiRoot 'lib\*'

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    throw 'A JDK is required to run the API tests (javac was not found).'
}

if (-not (Test-Path (Join-Path $apiRoot 'lib\gson-2.14.0.jar'))) {
    throw 'The bundled Gson dependency was not found under QuanLiQuanCafeAPI/lib.'
}

New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
New-Item -ItemType Directory -Force -Path $normalizedSourceDir | Out-Null

# One existing API source starts with a UTF-8 BOM, which javac rejects. Normalize
# copies under the ignored build directory rather than modifying application files.
foreach ($source in Get-ChildItem (Join-Path $apiRoot 'src') -Filter '*.java' -File) {
    $bytes = [System.IO.File]::ReadAllBytes($source.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        $bytes = $bytes[3..($bytes.Length - 1)]
    }
    $normalizedSource = Join-Path $normalizedSourceDir $source.Name
    [System.IO.File]::WriteAllBytes($normalizedSource, $bytes)
    $sourceFiles += $normalizedSource
}

Write-Host 'Compiling API sources and dependency-light contract tests...'
& javac -encoding UTF-8 -cp $classpath -d $outputDir @sourceFiles $testFile
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host 'Running API contract tests (no database connection is used)...'
& java -cp "$outputDir;$classpath" ApiContractTest
exit $LASTEXITCODE