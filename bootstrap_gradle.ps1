$ErrorActionPreference = "Continue"
$ver = "8.8"
$zip = "gradle.zip"
$tmp = "gradle_tmp"

# Explicitly use the found JDK 21
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Write-Host "Using Java from: $env:JAVA_HOME"
java -version

# We already downloaded it, check if we need to re-download
if (-not (Test-Path "$tmp\gradle-$ver")) {
    $url = "https://services.gradle.org/distributions/gradle-$ver-bin.zip"
    Write-Host "Downloading Gradle $ver..."
    Invoke-WebRequest -Uri $url -OutFile $zip
    
    Write-Host "Extracting..."
    if (-not (Test-Path $tmp)) { New-Item -ItemType Directory -Path $tmp -Force }
    Expand-Archive -Path $zip -DestinationPath $tmp -Force
}

$gradleHome = "$tmp\gradle-$ver"
$bin = "$gradleHome\bin\gradle.bat"

Write-Host "Running Wrapper Task..."
# Run in current process context to inherit JAVA_HOME
& $bin wrapper --gradle-version $ver --no-daemon

if (Test-Path "gradlew.bat") {
    Write-Host "Wrapper generated successfully!"
    if (Test-Path $zip) { Remove-Item -Path $zip -Force }
    # Keep tmp for run_dev.ps1 usage if wrapper fails
}
else {
    Write-Host "ERROR: gradlew.bat was not found."
}
