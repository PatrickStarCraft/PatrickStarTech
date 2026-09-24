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
        -InitScript (Join-Path $PSScriptRoot 'port-spoil-context.init.gradle') `
        -TestPattern 'com.gregtechceu.gtceu.common.item.SpoilableItemStackPortTest'
} finally {
    $env:JAVA_HOME = $previousJavaHome
}

Write-Output 'These selected-source checks execute SpoilableItemStack with real ItemStack, CustomData, and NBT. Spoilage transformations, live server time, capabilities, player/block inventory adapters, and client tooltip rendering are not exercised.'
