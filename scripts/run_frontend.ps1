$ErrorActionPreference = "Stop"
conda run -n gpt_smarthome mvn -f frontend-java/pom.xml javafx:run
