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
        -InitScript (Join-Path $PSScriptRoot 'port-fluid-ingredient.init.gradle') `
        -TestPattern 'com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderFluidIngredientPortTest'
} finally {
    $env:JAVA_HOME = $previousJavaHome
}

Write-Output 'The suite exercises production ranged-fluid matching, collapse, network transport, and lookup-key hashing. Test shims disable only optional KubeJS tag lookup; this does not cover full GT recipe registration.'
