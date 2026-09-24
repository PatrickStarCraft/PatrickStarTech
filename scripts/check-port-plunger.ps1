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
        -InitScript (Join-Path $PSScriptRoot 'port-plunger.init.gradle') `
        -TestPattern 'com.gregtechceu.gtceu.common.item.tool.behavior.PlungerBehaviorPortTest'
} finally {
    $env:JAVA_HOME = $previousJavaHome
}

Write-Output 'These selected-source checks execute the real PlungerBehavior transactional sink with NeoForge ItemAccess, FluidResource, SnapshotJournal, and Transaction. ToolHelper and world/block fluid lookup are compile-only fixtures; the checks prove transaction callback timing, not actual GT durability or in-world plunger interaction.'
