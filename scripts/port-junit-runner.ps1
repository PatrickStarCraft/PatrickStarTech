function Invoke-PortJUnitTests {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$GradleExecutable,
        [Parameter(Mandatory)][string]$ProjectDirectory,
        [Parameter(Mandatory)][string]$InitScript,
        [Parameter(Mandatory)][string]$TestPattern,
        [string[]]$AdditionalArguments = @()
    )

    foreach ($path in @($GradleExecutable, $ProjectDirectory, $InitScript)) {
        if (-not (Test-Path -LiteralPath $path)) {
            throw "Required test runner path does not exist: $path"
        }
    }

    $repositoryRoot = Split-Path -Parent (Split-Path -Parent ([IO.Path]::GetFullPath($ProjectDirectory)))
    $offlinePluginInit = Join-Path $repositoryRoot 'build/luna-offline-plugin-cache.init.gradle'
    $arguments = @('-p', $ProjectDirectory)
    if (Test-Path -LiteralPath $offlinePluginInit) {
        $arguments += @('-I', $offlinePluginInit)
    }
    $arguments += @(
        '-I', $InitScript,
        'cleanTest', 'test',
        '-PportDiagnostics',
        '--tests', $TestPattern,
        '--max-workers=1',
        '--console=plain',
        '--offline',
        '--no-build-cache'
    ) + $AdditionalArguments

    & $GradleExecutable @arguments
    $gradleExit = $LASTEXITCODE
    if ($gradleExit -ne 0) {
        throw "Gradle test command failed with exit code $gradleExit."
    }

    $reportDirectory = Join-Path $ProjectDirectory 'build/test-results/test'
    $reports = @(Get-ChildItem -LiteralPath $reportDirectory -Filter 'TEST-*.xml' -File -ErrorAction SilentlyContinue)
    if ($reports.Count -eq 0) {
        throw "Gradle reported success but produced no JUnit XML test reports for '$TestPattern'."
    }

    $testCount = 0
    $failureCount = 0
    $errorCount = 0
    $skippedCount = 0
    foreach ($report in $reports) {
        [xml]$document = Get-Content -LiteralPath $report.FullName -Raw
        $suites = @($document.SelectNodes('//testsuite'))
        foreach ($suite in $suites) {
            $testCount += [int]$suite.GetAttribute('tests')
            $failureCount += [int]$suite.GetAttribute('failures')
            $errorCount += [int]$suite.GetAttribute('errors')
            $skippedCount += [int]$suite.GetAttribute('skipped')
        }
    }

    $passedCount = $testCount - $failureCount - $errorCount - $skippedCount
    Write-Output "JUnit results for '$TestPattern': $passedCount passed, $failureCount failed, $errorCount errors, $skippedCount skipped, $testCount discovered."
    if ($testCount -eq 0 -or ($passedCount -eq 0 -and $skippedCount -gt 0)) {
        throw "No tests executed for '$TestPattern'; an empty or fully skipped selection is not a passing suite."
    }
    if ($failureCount -ne 0 -or $errorCount -ne 0) {
        throw "JUnit reported failures for '$TestPattern'."
    }
}
