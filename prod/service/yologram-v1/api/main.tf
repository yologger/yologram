resource "aws_ecr_repository" "this" {
  name                 = "yologram-api-v1"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = false
  }
}

resource "aws_iam_role" "task" {
  name = "yologram-api-v1-prod-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy" "task_exec_ssm" {
  name = "ecs-exec-ssm"
  role = aws_iam_role.task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ssmmessages:CreateControlChannel",
          "ssmmessages:CreateDataChannel",
          "ssmmessages:OpenControlChannel",
          "ssmmessages:OpenDataChannel",
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_security_group" "this" {
  name        = "yologram-api-v1-prod-sg"
  description = "Security group for yologram-api-v1-prod"
  vpc_id      = "vpc-00dd45cf23d6d31ee"

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "yologram-api-v1-prod-sg"
  }
}

resource "aws_ecs_task_definition" "this" {
  family                   = "yologram-api-v1-prod"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = "arn:aws:iam::000000000000:role/ecs-task-execution-role"
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = "yologram-api-v1"
      image     = "${aws_ecr_repository.this.repository_url}:latest"
      essential = true
      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]
    }
  ])
}

data "aws_service_discovery_dns_namespace" "prod" {
  name = "prod.internal"
  type = "DNS_PRIVATE"
}

data "aws_service_discovery_service" "this" {
  name         = "yologram-api-v1"
  namespace_id = data.aws_service_discovery_dns_namespace.prod.id
}

resource "aws_ecs_service" "this" {
  name                   = "yologram-api-v1-prod"
  cluster                = "prod"
  task_definition        = aws_ecs_task_definition.this.arn
  desired_count          = 1
  enable_execute_command = true

  capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = 1
  }

  network_configuration {
    subnets = [
      "subnet-02695a768e3c457df",
      "subnet-0c36f6f4a5208338a",
    ]
    security_groups  = [aws_security_group.this.id]
    assign_public_ip = true
  }

  service_registries {
    registry_arn   = data.aws_service_discovery_service.this.arn
    container_name = "yologram-api-v1"
    container_port = 8080
  }
}
