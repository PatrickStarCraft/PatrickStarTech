param([string]$JavaHome = $env:JAVA_HOME, [string]$GradleExecutable)
$ErrorActionPreference = 'Stop'
if (-not $JavaHome) { throw 'Set JAVA_HOME to Java 25 or pass -JavaHome.' }
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$gradleExecutable = if ($GradleExecutable) { [IO.Path]::GetFullPath($GradleExecutable) } else { Join-Path $projectRoot 'gradlew.bat' }
$libraryRoot = Join-Path $projectRoot 'ports/ModularUI'
$previousJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = $JavaHome
    . (Join-Path $PSScriptRoot 'port-junit-runner.ps1')
    Invoke-PortJUnitTests -GradleExecutable $gradleExecutable -ProjectDirectory $libraryRoot `
        -InitScript (Join-Path $PSScriptRoot 'port-valueproviders.init.gradle') `
        -TestPattern 'com.gregtechceu.gtceu.common.valueprovider.PortValueProviderCheck'
} finally {
    $env:JAVA_HOME = $previousJavaHome
}
Write-Output 'These isolated checks use the NeoForge loader but do not load the full GT mod or validate its registration.'
