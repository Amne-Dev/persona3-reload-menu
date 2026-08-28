param(
    [ValidateSet("1.20.1", "1.21.8", "1.21.11", "26.2")]
    [string]$Version,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$ErrorActionPreference = "Stop"
$repositoryRoot = $PSScriptRoot

if ([string]::IsNullOrWhiteSpace($Version)) {
    Write-Host "Select the Minecraft version to run:"
    Write-Host "  1) 1.20.1"
    Write-Host "  2) 1.21.8"
    Write-Host "  3) 1.21.11"
    Write-Host "  4) 26.2"

    $selection = Read-Host "Version [1-4]"
    $Version = switch ($selection.Trim()) {
        "1" { "1.20.1" }
        "2" { "1.21.8" }
        "3" { "1.21.11" }
        "4" { "26.2" }
        default { throw "Unknown selection '$selection'. Choose 1, 2, 3, or 4." }
    }
}

$target = switch ($Version) {
    "1.20.1" {
        @{
            ProjectDirectory = $repositoryRoot
            Wrapper = Join-Path $repositoryRoot "gradlew.bat"
            JavaVersion = 21
        }
    }
    "1.21.8" {
        @{
            ProjectDirectory = Join-Path $repositoryRoot "versions\1.21.8"
            Wrapper = Join-Path $repositoryRoot "versions\26.2\gradlew.bat"
            JavaVersion = 21
        }
    }
    "1.21.11" {
        @{
            ProjectDirectory = Join-Path $repositoryRoot "versions\1.21.11"
            Wrapper = Join-Path $repositoryRoot "versions\26.2\gradlew.bat"
            JavaVersion = 21
        }
    }
    "26.2" {
        @{
            ProjectDirectory = Join-Path $repositoryRoot "versions\26.2"
            Wrapper = Join-Path $repositoryRoot "versions\26.2\gradlew.bat"
            JavaVersion = 25
        }
    }
}

$jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-$($target.JavaVersion)*" |
    Sort-Object Name -Descending |
    Select-Object -First 1

if (-not $jdk) {
    throw "JDK $($target.JavaVersion) was not found under C:\Program Files\Eclipse Adoptium."
}
if (-not (Test-Path -LiteralPath $target.ProjectDirectory -PathType Container)) {
    throw "The project directory for Minecraft $Version does not exist: $($target.ProjectDirectory)"
}
if (-not (Test-Path -LiteralPath $target.Wrapper -PathType Leaf)) {
    throw "The Gradle wrapper does not exist: $($target.Wrapper)"
}

$env:JAVA_HOME = $jdk.FullName
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Write-Host "Launching Minecraft $Version"
Write-Host "Project: $($target.ProjectDirectory)"
Write-Host "Java:   $env:JAVA_HOME"

Push-Location $target.ProjectDirectory
try {
    & $target.Wrapper runClient @GradleArgs
    $gradleExitCode = $LASTEXITCODE
}
finally {
    Pop-Location
}

exit $gradleExitCode
