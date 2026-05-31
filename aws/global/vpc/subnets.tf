resource "aws_subnet" "pub_a" {
  vpc_id                  = aws_vpc.prod.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "ap-northeast-2a"
  map_public_ip_on_launch = true

  tags = {
    Name = "pub-a"
  }
}

resource "aws_subnet" "pub_b" {
  vpc_id                  = aws_vpc.prod.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "ap-northeast-2b"
  map_public_ip_on_launch = true

  tags = {
    Name = "pub-b"
  }
}
