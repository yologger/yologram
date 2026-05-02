resource "aws_security_group" "service_prod" {
  name        = "db-service-prod-sg"
  description = "Managed by Terraform"
  vpc_id      = "vpc-00dd45cf23d6d31ee"

  ingress {
    description = "prod vpc"
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = ["172.31.0.0/16"]
  }

  ingress {
    description = "my ip (study cafe 1)"
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = ["121.140.17.192/32"]
  }

  ingress {
    description = "my ip (home)"
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = ["125.133.54.66/32"]
  }

  ingress {
    description = "motel"
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = ["211.238.121.136/32"]
  }

  ingress {
    description = "my ip (study cafe 2)"
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = ["210.183.35.165/32"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "db-service-prod-sg"
  }
}

resource "aws_db_subnet_group" "service_prod" {
  name        = "db-subnets-prod"
  description = "Managed by Terraform"
  subnet_ids = [
    "subnet-02695a768e3c457df",
    "subnet-0c36f6f4a5208338a",
  ]

  tags = {
    Name = "db-subnets-prod"
  }
}

resource "aws_db_parameter_group" "service_prod" {
  name        = "service-prod-mysql80"
  family      = "mysql8.0"
  description = "service-prod-mysql80"

  parameter {
    name  = "max_connections"
    value = "1000"
  }

  parameter {
    name  = "wait_timeout"
    value = "28800"
  }

  tags = {
    Name = "service-prod-mysql80"
  }
}

resource "aws_db_instance" "service_prod" {
  identifier     = "serivce-prod"
  engine         = "mysql"
  engine_version = "8.0.42"
  instance_class = "db.t4g.micro"

  allocated_storage = 20
  storage_type      = "gp2"
  storage_encrypted = true
  kms_key_id        = "arn:aws:kms:ap-northeast-2:000000000000:key/5d8f3857-36f3-42bd-943e-73c1711397c2"

  username                    = "admin"
  manage_master_user_password = false

  db_subnet_group_name   = aws_db_subnet_group.service_prod.name
  parameter_group_name   = aws_db_parameter_group.service_prod.name
  vpc_security_group_ids = [aws_security_group.service_prod.id]

  publicly_accessible = true
  multi_az            = false
  availability_zone   = "ap-northeast-2b"

  backup_retention_period = 0

  auto_minor_version_upgrade = false
  deletion_protection        = false
  copy_tags_to_snapshot      = false
  skip_final_snapshot        = true

  ca_cert_identifier = "rds-ca-rsa2048-g1"

  tags = {
    Name = "serivce-prod"
  }
}
