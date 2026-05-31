data "aws_vpc" "prod" {
  filter {
    name   = "tag:Name"
    values = ["vpc-prod"]
  }
}
