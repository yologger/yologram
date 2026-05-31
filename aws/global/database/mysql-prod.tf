variable "db_password" {
  type      = string
  sensitive = true

  validation {
    condition     = can(regex("^[^/@\" ]+$", var.db_password)) && length(var.db_password) >= 8
    error_message = "8자 이상, '/', '@', '\"', 공백 사용 불가."
  }
}

resource "aws_security_group" "mysql_prod" {
  name   = "mysql-prod-sg"
  vpc_id = data.aws_vpc.prod.id

  ingress {
    from_port   = 3306
    to_port     = 3306
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
    Name = "mysql-prod-sg"
  }
}

resource "aws_db_subnet_group" "mysql_prod" {
  name       = "mysql-prod-subnet-group"
  subnet_ids = [data.aws_subnet.pub_a.id, data.aws_subnet.pub_b.id]

  tags = {
    Name = "mysql-prod-subnet-group"
  }
}

resource "aws_db_parameter_group" "mysql_prod" {
  name   = "mysql-prod-parameter-group"
  family = "mysql8.0"

  parameter {
    name  = "max_connections"
    value = "1000"
  }

  parameter {
    name  = "wait_timeout"
    value = "28800"
  }

  tags = {
    Name = "mysql-prod-parameter-group"
  }
}

resource "aws_db_instance" "mysql_prod" {
  identifier = "mysql-prod"

  engine         = "mysql"
  engine_version = "8.0"
  instance_class = "db.t4g.micro"

  allocated_storage = 20
  storage_type      = "gp2"
  storage_encrypted = false

  username = "master"
  password = var.db_password

  parameter_group_name   = aws_db_parameter_group.mysql_prod.name
  db_subnet_group_name   = aws_db_subnet_group.mysql_prod.name
  vpc_security_group_ids = [aws_security_group.mysql_prod.id]

  availability_zone          = "ap-northeast-2a"
  publicly_accessible        = true
  multi_az                   = false
  backup_retention_period    = 0
  auto_minor_version_upgrade = false
  skip_final_snapshot        = true

  tags = {
    Name = "mysql-prod"
  }

  lifecycle {
    ignore_changes = [password]
  }
}
