param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$GradleUserHome = $env:GRADLE_USER_HOME,
    [string]$JavacExecutable,
    [string]$JavaExecutable
)
$ErrorActionPreference = 'Stop'
if (-not $JavaHome) { throw 'Set JAVA_HOME to Java 25 or pass -JavaHome.' }
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (-not $GradleUserHome) { $GradleUserHome = Join-Path $env:USERPROFILE '.gradle' }
$javac = if ($JavacExecutable) { [IO.Path]::GetFullPath($JavacExecutable) } else { Join-Path $JavaHome 'bin/javac.exe' }
$java = if ($JavaExecutable) { [IO.Path]::GetFullPath($JavaExecutable) } else { Join-Path $JavaHome 'bin/java.exe' }
$libraryRoot = Join-Path $projectRoot 'ports/ModularUI'
$manifest = Join-Path $libraryRoot 'build/tmp/createMinecraftArtifacts/nfrt_artifact_manifest.properties'
$minecraftJar = Join-Path $libraryRoot 'build/moddev/artifacts/minecraft-patched-26.2.0.88.jar'
$runId = [Guid]::NewGuid().ToString('N')
$outputDirectory = Join-Path $projectRoot "build/port-nbt-check/run-$runId"
if (-not (Test-Path -LiteralPath $manifest) -or -not (Test-Path -LiteralPath $minecraftJar)) {
    throw 'Run the ModularUI createMinecraftArtifacts Gradle task first.'
}
if (-not (Test-Path -LiteralPath $javac) -or -not (Test-Path -LiteralPath $java)) {
    throw 'The configured Java 25 javac.exe and java.exe must exist.'
}
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$predicateClasspath = @(Get-Content -LiteralPath $manifest | Where-Object {
    $_ -match '=' -and $_ -notmatch '-sources.jar$'
} | ForEach-Object {
    (($_ -split '=', 2)[1]).Replace('\:', ':').Replace('\\', '\')
})
$predicateClasspath += $minecraftJar
$predicateClasspath += $outputDirectory
foreach ($artifact in @('org.junit.jupiter/junit-jupiter-api/5.9.2',
        'org.opentest4j/opentest4j', 'org.junit.platform/junit-platform-commons', 'org.apiguardian/apiguardian-api')) {
    $artifactPath = Join-Path $GradleUserHome ('caches/modules-2/files-2.1/' + $artifact)
    $jar = Get-ChildItem -LiteralPath $artifactPath -Recurse -Filter '*.jar' |
        Where-Object { $_.Name -notmatch 'sources|javadoc' } | Select-Object -First 1 -ExpandProperty FullName
    if (-not $jar) { throw "Missing cached test dependency: $artifact" }
    $predicateClasspath += $jar
}
$sourceDirectory = Join-Path $projectRoot 'src/main/java/com/gregtechceu/gtceu/api/recipe/ingredient/nbtpredicate'
$sourceFiles = @('NBTPredicate', 'NBTPredicateUtils', 'ComparisonNBTPredicate', 'EqualsNBTPredicate',
    'AllNBTPredicate', 'AnyNBTPredicate', 'NotNBTPredicate', 'TrueNBTPredicate', 'NBTPredicates') |
    ForEach-Object { Join-Path $sourceDirectory "${_}.java" }
$sourceFiles += Join-Path $projectRoot 'src/test/java/com/gregtechceu/gtceu/api/recipe/ingredient/nbtpredicate/PortNBTPredicateCheck.java'
# The actual predicate factory imports Rhino's @HideFromJS and its registered codecs include TrueNBTPredicate.
$forgeVersionCatalog = Join-Path $projectRoot 'gradle/forge.versions.toml'
$rhinoVersionLine = Select-String -Path $forgeVersionCatalog -Pattern '^rhino\s*=\s*"([^"]+)"' | Select-Object -First 1
if (-not $rhinoVersionLine) { throw 'Could not resolve the configured Rhino compileOnly version.' }
$rhinoVersion = $rhinoVersionLine.Matches[0].Groups[1].Value
$rhinoCache = Join-Path $GradleUserHome "caches/modules-2/files-2.1/dev.latvian.mods/rhino-forge/$rhinoVersion"
$rhinoJar = Get-ChildItem -LiteralPath $rhinoCache -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch 'sources|javadoc' } | Select-Object -First 1 -ExpandProperty FullName
if (-not $rhinoJar) { throw "Missing configured Rhino compileOnly artifact: $rhinoCache" }
$predicateClasspath += $rhinoJar
$buildText = Get-Content -LiteralPath (Join-Path $projectRoot 'build.gradle') -Raw
$lombokVersionMatch = [regex]::Match($buildText, 'lombok\s*\{\s*version\s*=\s*"([^"]+)"')
if (-not $lombokVersionMatch.Success) { throw 'Could not resolve the configured Lombok version.' }
$lombokVersion = $lombokVersionMatch.Groups[1].Value
$lombokCache = Join-Path $GradleUserHome "caches/modules-2/files-2.1/org.projectlombok/lombok/$lombokVersion"
$lombokJar = Get-ChildItem -LiteralPath $lombokCache -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch 'sources|javadoc' } | Select-Object -First 1 -ExpandProperty FullName
if (-not $lombokJar) { throw "Missing configured Lombok compileOnly artifact: $lombokCache" }
# Lombok is only needed here to resolve TrueNBTPredicate's annotation; the check uses -proc:none.
$predicateClasspath += $lombokJar
$compileOutput = & $javac -proc:none -sourcepath $outputDirectory `
    -cp ($predicateClasspath -join ';') -d $outputDirectory @sourceFiles 2>&1
$compileExit = $LASTEXITCODE
$compileOutput | ForEach-Object { Write-Output $_ }
if ($compileExit -ne 0 -or ($compileOutput -join "`n") -match 'An exception has occurred in the compiler|Exception in thread') {
    throw "GT NBT predicate compilation failed with exit code $compileExit."
}
$testClass = Join-Path $outputDirectory 'com/gregtechceu/gtceu/api/recipe/ingredient/nbtpredicate/PortNBTPredicateCheck.class'
if (-not (Test-Path -LiteralPath $testClass)) {
    throw 'GT NBT predicate compilation produced no current check class.'
}
$testOutput = & $java -cp ($predicateClasspath -join ';') `
    com.gregtechceu.gtceu.api.recipe.ingredient.nbtpredicate.PortNBTPredicateCheck 2>&1
$testExit = $LASTEXITCODE
$testOutput | ForEach-Object { Write-Output $_ }
if ($testExit -ne 0) { throw "GT NBT predicate checks failed with exit code $testExit." }
$summary = [regex]::Match(($testOutput -join "`n"), '(?m)^NBT predicates: (\d+) checks passed$')
if (-not $summary.Success -or [int]$summary.Groups[1].Value -eq 0) {
    throw 'GT NBT predicate runner completed without reporting a nonzero check count.'
}
Write-Output "These isolated checks passed $($summary.Groups[1].Value) assertions; they do not load GT or verify component-backed ingredients and recipe registration."
