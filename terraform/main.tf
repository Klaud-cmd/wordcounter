variable "region" {
  default = "eu-west-1"
}

variable "project-name" {
  default = "klaud-wordcounter2"
}

data "aws_canonical_user_id" "current" {}

provider "aws" {
  region = var.region
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "all" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

resource "aws_s3_bucket" "bucket" {
  bucket = "${var.project-name}-bucket"

  tags = {
    Description = "Bucket to store input/output files for the Wordcounter App"
  }
}

resource "aws_s3_bucket_policy" "bucket-policy" {
  bucket = aws_s3_bucket.bucket.id
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowOutputReadEveryone",
      "Principal": "*",
      "Action": [
        "s3:ListBucket",
        "s3:GetObject"
      ],
      "Effect": "Allow",
      "Resource": ["${aws_s3_bucket.bucket.arn}", "${aws_s3_bucket.bucket.arn}/output/*"]
    },
    {
      "Sid": "AllowOutputWriteBackend",
      "Principal": "*",
      "Action": [
        "s3:ListBucket",
        "s3:PutObject"
      ],
      "Effect": "Allow",
      "Resource": ["${aws_s3_bucket.bucket.arn}", "${aws_s3_bucket.bucket.arn}/output/*"],
      "Condition": {
        "IpAddress": {
          "aws:SourceIp": [
            "${aws_instance.backend.private_ip}"
          ]
        }
      }
    },
    {
      "Sid": "AllowInputReadBackend",
      "Principal": "*",
      "Action": [
        "s3:ListBucket",
        "s3:GetObject"
      ],
      "Effect": "Allow",
      "Resource": ["${aws_s3_bucket.bucket.arn}", "${aws_s3_bucket.bucket.arn}/input/*"],
      "Condition": {
        "IpAddress": {
          "aws:SourceIp": [
            "${aws_instance.backend.private_ip}"
          ]
        }
      }
    },
    {
      "Sid": "AllowInputWriteEveryone",
      "Principal": "*",
      "Action": [
        "s3:ListBucket",
        "s3:PutObject"
      ],
      "Effect": "Allow",
      "Resource": ["${aws_s3_bucket.bucket.arn}", "${aws_s3_bucket.bucket.arn}/input/*"]
    }
  ]
}
EOF
}

resource "aws_s3_bucket_cors_configuration" "bucket-cors-config" {
  bucket = aws_s3_bucket.bucket.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["POST", "PUT", "GET", "HEAD"]
    allowed_origins = ["*"]
    expose_headers  = []
    max_age_seconds = 3000
  }
}

resource "aws_s3_bucket_acl" "bucket_acl" {
  bucket = aws_s3_bucket.bucket.id
  access_control_policy {
    grant {
      grantee {
        id   = data.aws_canonical_user_id.current.id
        type = "CanonicalUser"
      }
      permission = "FULL_CONTROL"
    }

    grant {
      grantee {
        type = "Group"
        uri  = "http://acs.amazonaws.com/groups/global/AllUsers"
      }
      permission = "READ_ACP"
    }

    owner {
      id = data.aws_canonical_user_id.current.id
    }
  }
}

resource "aws_s3_bucket_public_access_block" "bucket-public-access-block" {
  bucket = aws_s3_bucket.bucket.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_ecr_repository" "ecr-repo" {
  name = "${var.project-name}"

  image_scanning_configuration {
    scan_on_push = true
  }
}

//Push to ECR on infra deployment
resource "null_resource" "ecr_upload" {
  depends_on = [aws_ecr_repository.ecr-repo]

  triggers = { // Trigger everytime
    build_number = timestamp()
  }

  provisioner "local-exec" {
    command = "/bin/bash uploadToECR.sh"
  }
}

resource "aws_iam_role" "ec2_role" {
  name = "ec2_role_wordcounter"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Effect": "Allow"
    }
  ]
}
EOF
}

resource "aws_iam_instance_profile" "ec2_profile_wordcounter" {
  name = "ec2_profile_wordcounter"
  role = aws_iam_role.ec2_role.name
}

resource "aws_iam_role_policy" "ec2_policy" {
  name = "ec2_policy"
  role = aws_iam_role.ec2_role.id

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:GetRepositoryPolicy",
        "ecr:DescribeRepositories",
        "ecr:ListImages",
        "ecr:DescribeImages",
        "ecr:BatchGetImage",
        "ecr:GetAuthorizationToken"
      ],
      "Effect": "Allow",
      "Resource": "*"
    },
    {
      "Action": [
        "s3:ListBucket",
        "s3:ListObjects",
        "s3:GetObject"
      ],
      "Effect": "Allow",
      "Resource": ["${aws_s3_bucket.bucket.arn}", "${aws_s3_bucket.bucket.arn}/input/*"]
    },
    {
      "Action": [
        "s3:ListBucket",
        "s3:ListObjects",
        "s3:PutObject"
      ],
      "Effect": "Allow",
      "Resource": ["${aws_s3_bucket.bucket.arn}", "${aws_s3_bucket.bucket.arn}/output/*"]
    }
  ]
}
EOF
}

data "aws_ami" "amazon_linux_2" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-ebs"]
  }
}

resource "aws_instance" "backend" {
  depends_on    = [null_resource.ecr_upload]
  ami           = data.aws_ami.amazon_linux_2.id
  instance_type = "t2.micro"

  vpc_security_group_ids = [
    module.ec2_sg.security_group_id,
  ]
  iam_instance_profile = aws_iam_instance_profile.ec2_profile_wordcounter.name

  tags = {
    Name        = "WordCounter"
    Description = "Backend service that calculates the wordcount of the input files by requestID"
  }
  user_data = file("user_data/start-app.sh")
}