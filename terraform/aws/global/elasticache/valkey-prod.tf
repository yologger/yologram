resource "aws_security_group" "valkey_prod" {
  name        = "valkey-prod-sg"
  description = "Security group for Valkey"
  vpc_id      = data.aws_vpc.prod.id

  ingress {
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.prod.cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "valkey-prod-sg"
  }
}

resource "aws_elasticache_subnet_group" "valkey_prod" {
  name       = "valkey-prod-subnet-group"
  subnet_ids = [data.aws_subnet.pub_a.id, data.aws_subnet.pub_b.id]
}

resource "aws_elasticache_parameter_group" "valkey_prod" {
  name   = "valkey-prod-parameter-group"
  family = "valkey8"
}

resource "aws_elasticache_replication_group" "valkey_prod" {
  replication_group_id = "valkey-prod"
  description          = "Valkey prod"
  engine               = "valkey"
  engine_version       = "8.0"
  node_type            = "cache.t4g.micro"
  num_cache_clusters   = 1
  port                 = 6379

  apply_immediately          = true
  auto_minor_version_upgrade = false

  parameter_group_name = aws_elasticache_parameter_group.valkey_prod.name
  subnet_group_name    = aws_elasticache_subnet_group.valkey_prod.name
  security_group_ids   = [aws_security_group.valkey_prod.id]

  tags = {
    Name = "valkey-prod"
  }
}
