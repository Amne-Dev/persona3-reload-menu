$ErrorActionPreference = "Stop"

$jdk21 = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-21*" |
    Sort-Object Name -Descending |
    Select-Object -First 1
if (-not $jdk21) {
    throw "JDK 21 was not found under C:\Program Files\Eclipse Adoptium."
}
$env:JAVA_HOME = $jdk21.FullName
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# Use the downloaded Gradle directly
$gradleBat = ".\gradle_tmp\gradle-8.8\bin\gradle.bat"

if (-not (Test-Path $gradleBat)) {
    Write-Host "Error: Local Gradle not found in gradle_tmp. Please run bootstrap_gradle.ps1 again."
    exit 1
}

Write-Host "Launching P3R Menu Mod..."
Write-Host "Java: $env:JAVA_HOME"
Write-Host "Gradle: $gradleBat"

& $gradleBat runClient
