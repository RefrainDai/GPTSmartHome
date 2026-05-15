$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $ProjectRoot
try {
    conda run --no-capture-output -n gpt_smarthome mvn -f frontend-java/pom.xml javafx:run
}
finally {
    Pop-Location
}
