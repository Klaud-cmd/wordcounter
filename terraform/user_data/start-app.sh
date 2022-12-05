#! /bin/bash
set -eu

AWS_REGION="eu-west-1"
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query 'Account' --region "$AWS_REGION" --output text)

sudo yum update -y
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user
sudo aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID".dkr.ecr."$AWS_REGION".amazonaws.com
sudo docker pull "$AWS_ACCOUNT_ID".dkr.ecr."$AWS_REGION".amazonaws.com/klaud-wordcounter2:latest
sudo docker run --name wordcount -p 8080:8080 -d "$AWS_ACCOUNT_ID".dkr.ecr."$AWS_REGION".amazonaws.com/klaud-wordcounter2:latest