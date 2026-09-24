param([string]$JavaHome = $env:JAVA_HOME, [string]$GradleExecutable)
$ErrorActionPreference = 'Stop'
if (-not $JavaHome) { throw 'Set JAVA_HOME to Java 25 or pass -JavaHome.' }

$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$gradle = if ($GradleExecutable) { [IO.Path]::GetFullPath($GradleExecutable) } else { Join-Path $projectRoot 'gradlew.bat' }
$libraryRoot = Join-Path $projectRoot 'ports/ModularUI'
$previousJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = $JavaHome
    . (Join-Path $PSScriptRoot 'port-junit-runner.ps1')
    Invoke-PortJUnitTests -GradleExecutable $gradle -ProjectDirectory $libraryRoot `
        -InitScript (Join-Path $PSScriptRoot 'port-fluid-storage.init.gradle') `
        -TestPattern 'com.gregtechceu.gtceu.api.misc.*FluidResourceHandlerPortTest'
} finally {
    $env:JAVA_HOME = $previousJavaHome
}

Write-Output 'These isolated checks execute the selected GT fluid handlers with real NeoForge item access, fluid resources, components, and transactions; the GT data-component registration is represented by a test fixture, so live mod capability registration remains unverified.'
