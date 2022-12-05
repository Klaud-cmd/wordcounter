# Wordcounter

## What is it?
Wordcounter is the backend component that ingests .txt files from an S3 prefix and generates 4 files which hold the sum of
frequency of the words in the files

## How to run it?
To run it, follow these steps:
1. Assemble the JAR `./gradlew assemble`
2. Connect your AWS account to awscli (You may use localstack so as not to incur any costs, however you'd need to
repoint the application to the localstack bucket)
3. Build the docker image `docker build -t klaud-wordcounter2:latest .  `
4. cd into the terraform folder and run `terraform apply`
5. Place some files under `<BUCKET_NAME>/input/<UUID>/*` and note down the UUID as you will need to send it to 
the controller