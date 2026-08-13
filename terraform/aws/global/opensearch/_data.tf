data "aws_vpc" "prod" {
  filter {
    name   = "tag:Name"
    values = ["vpc-prod"]
  }
}

data "aws_subnet" "pub_a" {
  vpc_id = data.aws_vpc.prod.id

  filter {
    name   = "tag:Name"
    values = ["pub-a"]
  }
}

data "aws_caller_identity" "current" {}
