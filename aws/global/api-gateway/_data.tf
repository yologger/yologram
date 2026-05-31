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

data "aws_subnet" "pub_b" {
  vpc_id = data.aws_vpc.prod.id

  filter {
    name   = "tag:Name"
    values = ["pub-b"]
  }
}

data "aws_service_discovery_dns_namespace" "this" {
  name = "ecs-prod.internal"
  type = "DNS_PRIVATE"
}
