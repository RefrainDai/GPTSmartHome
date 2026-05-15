$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $ProjectRoot
try {
    $gpuOptions = "-Dprism.order=d3d -Dprism.vsync=true"
    if ([string]::IsNullOrWhiteSpace($env:JDK_JAVA_OPTIONS)) {
        $env:JDK_JAVA_OPTIONS = $gpuOptions
    }
    elseif ($env:JDK_JAVA_OPTIONS -notlike "*prism.order*") {
        $env:JDK_JAVA_OPTIONS = "$env:JDK_JAVA_OPTIONS $gpuOptions"
    }
    conda run --no-capture-output -n gpt_smarthome mvn -f frontend-java/pom.xml javafx:run
}
finally {
    Pop-Location
}
