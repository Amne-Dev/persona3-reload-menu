$ErrorActionPreference = "Stop"

# Use the compatible JDK 21 found on the system
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
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
