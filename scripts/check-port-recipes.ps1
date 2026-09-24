param([string]$JavaHome = $env:JAVA_HOME, [string]$GradleExecutable)
$ErrorActionPreference = 'Stop'
if (-not $JavaHome) { throw 'Set JAVA_HOME to Java 25 or pass -JavaHome.' }
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$gradle = if ($GradleExecutable) { [IO.Path]::GetFullPath($GradleExecutable) } else { Join-Path $projectRoot 'gradlew.bat' }
$libraryRoot = Join-Path $projectRoot 'ports/ModularUI'
$previousJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = $JavaHome
    try {
        . (Join-Path $PSScriptRoot 'port-junit-runner.ps1')
        Invoke-PortJUnitTests -GradleExecutable $gradle -ProjectDirectory $libraryRoot `
            -InitScript (Join-Path $PSScriptRoot 'port-recipes.init.gradle') `
            -TestPattern 'com.gregtechceu.gtceu.api.recipe.*PortTest'
    } finally {
        # Remove the test-only mixin declaration from generated ModularUI metadata.
        # No checked-in ModularUI metadata is modified by the test harness.
        $restoreInitArguments = @()
        $offlinePluginInit = Join-Path $projectRoot 'build/luna-offline-plugin-cache.init.gradle'
        if (Test-Path -LiteralPath $offlinePluginInit) {
            $restoreInitArguments += @('-I', $offlinePluginInit)
        }
        & $gradle -p $libraryRoot @restoreInitArguments processResources --max-workers=1 --console=plain --offline
        if ($LASTEXITCODE -ne 0) { throw 'Could not restore normal ModularUI generated metadata.' }
    }
} finally {
    $env:JAVA_HOME = $previousJavaHome
}
Write-Output 'These isolated recipe checks do not load the full GT mod.'
