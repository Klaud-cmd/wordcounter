terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.19.0"
    }
  }
  backend "s3" {
    bucket = "klaud-tf-state-bucket2"
    key    = "state"
    region = "eu-west-1"
  }
}