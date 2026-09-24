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
        -InitScript (Join-Path $PSScriptRoot 'port-item-ingredient.init.gradle') `
        -TestPattern 'com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredientPortTest'
} finally {
    $env:JAVA_HOME = $previousJavaHome
}

Write-Output 'These isolated checks execute the production ranged item ingredient, map dispatcher, and item map-key factories. A small RecipeCapability boundary supplies the selected host content-class contract; unrelated circuit, NBT, fluid-container branches and CommonProxy registration use compile-only fixtures and are not covered.'
