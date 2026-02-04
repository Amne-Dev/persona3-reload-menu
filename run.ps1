$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Write-Host "Launch with Java: $env:JAVA_HOME"
./gradlew runClient
