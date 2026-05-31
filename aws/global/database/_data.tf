data "aws_vpc" "prod" {
  filter {
    name   = "tag:Name"
    values = ["vpc-prod"]
  }
}

data "aws_subnet" "pub_a" {
  filter {
    name   = "tag:Name"
    values = ["pub-a"]
  }
}

data "aws_subnet" "pub_b" {
  filter {
    name   = "tag:Name"
    values = ["pub-b"]
  }
}
