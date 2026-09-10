<#
.SYNOPSIS
Runs a supported Minecraft development client.

.EXAMPLE
.\run.ps1 -Version 1.21.11 -Essential

.DESCRIPTION
Use -Essential to stage the official Essential Fabric container for this launch.
The downloaded jar is cached under .gradle and removed from the selected run/mods
directory when Gradle exits.
#>
param(
    [ValidateSet("1.20.1", "1.21.8", "1.21.11", "26.2")]
    [string]$Version,

    [switch]$Essential,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$ErrorActionPreference = "Stop"
$repositoryRoot = $PSScriptRoot

function Test-EssentialContainerJar {
    param([Parameter(Mandatory = $true)][string]$Path)

    $archive = $null
    $reader = $null
    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $archive = [System.IO.Compression.ZipFile]::OpenRead($Path)
        $metadataEntry = $archive.GetEntry("fabric.mod.json")
        if (-not $metadataEntry) {
            return $false
        }
        $reader = [System.IO.StreamReader]::new($metadataEntry.Open())
        $metadata = $reader.ReadToEnd() | ConvertFrom-Json
        return $metadata.id -eq "essential-container"
    }
    catch {
        return $false
    }
    finally {
        if ($reader) {
            $reader.Dispose()
        }
        if ($archive) {
            $archive.Dispose()
        }
    }
}

function Test-JavaHomeVersion {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][int]$MajorVersion
    )

    $javaExecutable = Join-Path $Path "bin\java.exe"
    if (-not (Test-Path -LiteralPath $javaExecutable -PathType Leaf)) {
        return $false
    }

    try {
        $versionLine = (& $javaExecutable --version 2>&1 | Select-Object -First 1).ToString()
        return $versionLine -match "^(?:openjdk|java) $MajorVersion(?:\.|\s)"
    }
    catch {
        return $false
    }
}

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
            EssentialPlatform = "fabric_1-20-1"
            EssentialRunTask = "runClient"
        }
    }
    "1.21.8" {
        @{
            ProjectDirectory = Join-Path $repositoryRoot "versions\1.21.8"
            Wrapper = Join-Path $repositoryRoot "versions\26.2\gradlew.bat"
            JavaVersion = 21
            EssentialPlatform = "fabric_1-21-8"
            EssentialRunTask = "runEssentialClient"
        }
    }
    "1.21.11" {
        @{
            ProjectDirectory = Join-Path $repositoryRoot "versions\1.21.11"
            Wrapper = Join-Path $repositoryRoot "versions\26.2\gradlew.bat"
            JavaVersion = 21
            EssentialPlatform = "fabric_1-21-11"
            EssentialRunTask = "runEssentialClient"
        }
    }
    "26.2" {
        @{
            ProjectDirectory = Join-Path $repositoryRoot "versions\26.2"
            Wrapper = Join-Path $repositoryRoot "versions\26.2\gradlew.bat"
            JavaVersion = 25
            EssentialPlatform = "fabric_26-2"
            EssentialRunTask = "runEssentialClient"
        }
    }
}

$jdkHome = $null
if ($env:JAVA_HOME -and (Test-JavaHomeVersion -Path $env:JAVA_HOME -MajorVersion $target.JavaVersion)) {
    $jdkHome = (Resolve-Path -LiteralPath $env:JAVA_HOME).Path
}
else {
    $jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-$($target.JavaVersion)*" -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        Select-Object -First 1
    if ($jdk) {
        $jdkHome = $jdk.FullName
    }
}

if (-not $jdkHome) {
    throw "JDK $($target.JavaVersion) was not found in JAVA_HOME or under C:\Program Files\Eclipse Adoptium."
}
if (-not (Test-Path -LiteralPath $target.ProjectDirectory -PathType Container)) {
    throw "The project directory for Minecraft $Version does not exist: $($target.ProjectDirectory)"
}
if (-not (Test-Path -LiteralPath $target.Wrapper -PathType Leaf)) {
    throw "The Gradle wrapper does not exist: $($target.Wrapper)"
}

$env:JAVA_HOME = $jdkHome
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$stagedEssentialJar = $null
if ($Essential) {
    # Pin the compatibility launch so it remains reproducible. Essential's
    # official download is a container which updates/loads the matching client.
    $essentialRelease = "1.4.1.1"
    $essentialFileName = "Essential_$($essentialRelease.Replace('.', '-'))_$($target.EssentialPlatform).jar"
    $essentialCacheDirectory = Join-Path $repositoryRoot ".gradle\essential-cache\$essentialRelease"
    $essentialCacheJar = Join-Path $essentialCacheDirectory $essentialFileName
    $essentialDownloadUrl = "https://downloads.essential.gg/v1/mods/essential/essential-pinned/updates/$essentialRelease/$($target.EssentialPlatform)/download"
    $runModsDirectory = Join-Path $target.ProjectDirectory "run\mods"

    $existingEssentialJar = Get-ChildItem -LiteralPath $runModsDirectory -Filter "*.jar" -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match "(?i)^essential(?:_|-|\.)" } |
        Select-Object -First 1

    if ($existingEssentialJar) {
        Write-Host "Essential: using existing $($existingEssentialJar.FullName)"
    }
    else {
        New-Item -ItemType Directory -Path $essentialCacheDirectory -Force | Out-Null
        if ((Test-Path -LiteralPath $essentialCacheJar -PathType Leaf) -and
                -not (Test-EssentialContainerJar -Path $essentialCacheJar)) {
            Remove-Item -LiteralPath $essentialCacheJar -Force
        }
        if (-not (Test-Path -LiteralPath $essentialCacheJar -PathType Leaf)) {
            $temporaryDownload = "$essentialCacheJar.download"
            Write-Host "Essential: downloading official $essentialRelease container"
            try {
                Invoke-WebRequest -Uri $essentialDownloadUrl -OutFile $temporaryDownload
                Move-Item -LiteralPath $temporaryDownload -Destination $essentialCacheJar
            }
            finally {
                if (Test-Path -LiteralPath $temporaryDownload) {
                    Remove-Item -LiteralPath $temporaryDownload -Force
                }
            }
        }
        if (-not (Test-EssentialContainerJar -Path $essentialCacheJar)) {
            Remove-Item -LiteralPath $essentialCacheJar -Force
            throw "The Essential download was not a valid essential-container jar."
        }

        New-Item -ItemType Directory -Path $runModsDirectory -Force | Out-Null
        $stagedEssentialJar = Join-Path $runModsDirectory $essentialFileName
        Copy-Item -LiteralPath $essentialCacheJar -Destination $stagedEssentialJar
        Write-Host "Essential: staged $stagedEssentialJar"
    }
}

Write-Host "Launching Minecraft $Version"
Write-Host "Project: $($target.ProjectDirectory)"
Write-Host "Java:   $env:JAVA_HOME"
$runTask = if ($Essential) { $target.EssentialRunTask } else { "runClient" }
Write-Host "Task:   $runTask"

Push-Location $target.ProjectDirectory
try {
    & $target.Wrapper $runTask @GradleArgs
    $gradleExitCode = $LASTEXITCODE
}
finally {
    Pop-Location
    if ($stagedEssentialJar -and (Test-Path -LiteralPath $stagedEssentialJar)) {
        Remove-Item -LiteralPath $stagedEssentialJar -Force
        Write-Host "Essential: removed staged launch jar"
    }
}

exit $gradleExitCode
