resource "aws_s3_bucket" "state_bucket" {
  bucket = "klaud-tf-state-bucket2"

  tags = {
    Name = "State Bucket"
    Description = "Holds Terraform State"
  }
}