variable "db_password" {
  type      = string
  sensitive = true

  validation {
    condition     = can(regex("^[^/@\" ]+$", var.db_password)) && length(var.db_password) >= 8
    error_message = "8자 이상, '/', '@', '\"', 공백 사용 불가."
  }
}

# 개발자 로컬에서 prod DB에 직접 붙는 접속 지점(publicly_accessible = true인 이유).
# 콘솔에서 그때그때 추가하던 규칙들을 코드로 옮겼다 — 코드에 없으면 apply마다 삭제되고,
# 그러면 그 자리에서 DB에 접속하지 못한다. 접속 위치가 바뀌면 여기에 추가하고 apply할 것.
locals {
  mysql_dev_access_cidrs = {
    "121.140.17.192/32" = "my ip (study cafe 1F)"
    "210.183.35.41/32"  = "my ip (study cafe 1F 2)"
    "121.140.16.113/32" = "my ip (study cafe 1F 3)"
    "52.78.71.26/32"    = "my ip (study cafe 1F + VPN)"
    "121.170.223.38/32" = "my ip (study cafe 2F)"
    "125.133.54.66/32"  = "my ip (home)"
    "210.221.231.2/32"  = "bunjang office"
  }
}

resource "aws_security_group" "mysql_prod" {
  name   = "mysql-prod-sg"
  vpc_id = data.aws_vpc.prod.id

  # VPC 내부(ECS 태스크·worker)
  ingress {
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.prod.cidr_block]
  }

  # 개발자 로컬 — CIDR 하나당 규칙 하나로 만든다(description이 규칙 단위 속성이라 어디서 온 접속인지 남는다)
  dynamic "ingress" {
    for_each = local.mysql_dev_access_cidrs

    content {
      from_port   = 3306
      to_port     = 3306
      protocol    = "tcp"
      cidr_blocks = [ingress.key]
      description = ingress.value
    }
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

# 파라미터 그룹의 family는 엔진 메이저 버전에 묶여 있어 8.0 그룹을 8.4 인스턴스에 붙일 수 없다.
# 기존 그룹의 family만 고치면 그룹이 재생성되는데, 인스턴스가 참조하는 동안에는 삭제가 막혀 apply가 실패한다.
# 그래서 8.4용 그룹을 새 리소스로 두고 인스턴스가 이쪽을 참조하게 한다.
# 구 8.0 그룹(mysql-prod-parameter-group)은 전환이 끝나 미참조가 된 뒤 이 파일에서 제거한다.
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

resource "aws_db_parameter_group" "mysql_prod_84" {
  name   = "mysql-prod-parameter-group-mysql84"
  family = "mysql8.4"

  parameter {
    name  = "max_connections"
    value = "1000"
  }

  parameter {
    name  = "wait_timeout"
    value = "28800"
  }

  tags = {
    Name = "mysql-prod-parameter-group-mysql84"
  }
}

resource "aws_db_instance" "mysql_prod" {
  identifier = "mysql-prod"

  engine = "mysql"

  # 8.4 LTS. 8.0은 표준 지원이 끝나 Extended Support 요금(vCPU-시간당 $0.12)이 붙는데,
  # t4g.micro(2 vCPU)면 월 약 $166 — 인스턴스 요금($17)의 10배다. 8.4로 올리면 그 요금이 사라진다.
  # 마이너까지 명시한다 — "8.4"만 쓰면 provider가 prefix로 처리하지 않아 매 plan에 8.4.11 → 8.4 drift가 남는다
  engine_version = "8.4.11"

  # 메이저 업그레이드는 명시적으로 허용해야 apply가 통과한다(실수로 엔진이 올라가는 것을 막는 안전장치)
  allow_major_version_upgrade = true

  instance_class = "db.t4g.micro"

  allocated_storage = 20
  storage_type      = "gp2"
  storage_encrypted = false

  username = "master"
  password = var.db_password

  parameter_group_name   = aws_db_parameter_group.mysql_prod_84.name
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
