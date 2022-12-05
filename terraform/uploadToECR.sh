##!/usr/bin/env zsh
#set -eu

AWS_REGION=$(aws configure get region)
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query 'Account' --region "$AWS_REGION" --output text)

export REPOSITORY_NAME=klaud-wordcounter2
export IMAGE_NAME=klaud-wordcounter2

echo "Logging in to ECR..."
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID".dkr.ecr."$AWS_REGION".amazonaws.com

echo "Uploading to ECR..."
docker tag $IMAGE_NAME "$AWS_ACCOUNT_ID".dkr.ecr."$AWS_REGION".amazonaws.com/$IMAGE_NAME
docker push "$AWS_ACCOUNT_ID".dkr.ecr."$AWS_REGION".amazonaws.com/$IMAGE_NAME