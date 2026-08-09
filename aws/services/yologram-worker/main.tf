resource "aws_ecr_repository" "this" {
  name                 = "yologram-worker"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = false
  }
}

resource "aws_ecr_lifecycle_policy" "this" {
  repository = aws_ecr_repository.this.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last 5 images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 5
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

resource "aws_iam_role" "task" {
  name = "yologram-worker-prod-role"

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

resource "aws_iam_role_policy" "task_ssm_read" {
  name = "ssm-parameter-read"
  role = aws_iam_role.task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ssm:GetParameter",
          "ssm:GetParameters",
          "ssm:GetParametersByPath",
        ]
        Resource = "arn:aws:ssm:ap-northeast-2:${data.aws_caller_identity.current.account_id}:parameter/yologram/service/yologram-worker_*"
      }
    ]
  })
}

################################
## SSM Parameter Store (prod) ##
################################
resource "aws_ssm_parameter" "grafana_metrics_url_prod" {
  name  = "/yologram/service/yologram-worker_prod/management.otlp.metrics.export.url"
  type  = "String"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "grafana_metrics_auth_prod" {
  name  = "/yologram/service/yologram-worker_prod/management.otlp.metrics.export.headers.Authorization"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "grafana_traces_endpoint_prod" {
  name  = "/yologram/service/yologram-worker_prod/management.otlp.tracing.endpoint"
  type  = "String"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "grafana_traces_auth_prod" {
  name  = "/yologram/service/yologram-worker_prod/management.otlp.tracing.headers.Authorization"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "grafana_logs_endpoint_prod" {
  name  = "/yologram/service/yologram-worker_prod/management.otlp.logging.endpoint"
  type  = "String"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "grafana_logs_auth_prod" {
  name  = "/yologram/service/yologram-worker_prod/management.otlp.logging.headers.Authorization"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

# Discord 웹훅 — 채널별 url (요약 알림. enabled는 yaml에서 관리, 실제 값은 콘솔에서 직접 입력)
resource "aws_ssm_parameter" "discord_webhook_tech_url_prod" {
  name  = "/yologram/service/yologram-worker_prod/yologram.discord.webhooks.tech.url"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "discord_webhook_invest_url_prod" {
  name  = "/yologram/service/yologram-worker_prod/yologram.discord.webhooks.invest.url"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "discord_webhook_politics_url_prod" {
  name  = "/yologram/service/yologram-worker_prod/yologram.discord.webhooks.politics.url"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

# LLM API 키 (테크 아티클 요약 — Gemini 1순위, Groq fallback. 실제 값은 콘솔에서 직접 입력)
resource "aws_ssm_parameter" "llm_gemini_api_key_prod" {
  name  = "/yologram/service/yologram-worker_prod/yologram.llm.gemini.api-key"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "llm_groq_api_key_prod" {
  name  = "/yologram/service/yologram-worker_prod/yologram.llm.groq.api-key"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "cache_redis_host_prod" {
  name  = "/yologram/service/yologram-worker_prod/cache.data.redis.host"
  type  = "String"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

###################################
## SSM Parameter Store (DB prod) ##
###################################
resource "aws_ssm_parameter" "db_writer_url_prod" {
  name  = "/yologram/service/yologram-worker_prod/database.main.writer.datasource.url"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_writer_username_prod" {
  name  = "/yologram/service/yologram-worker_prod/database.main.writer.datasource.username"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_writer_password_prod" {
  name  = "/yologram/service/yologram-worker_prod/database.main.writer.datasource.password"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_reader_url_prod" {
  name  = "/yologram/service/yologram-worker_prod/database.main.reader.datasource.url"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_reader_username_prod" {
  name  = "/yologram/service/yologram-worker_prod/database.main.reader.datasource.username"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_reader_password_prod" {
  name  = "/yologram/service/yologram-worker_prod/database.main.reader.datasource.password"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

# 인바운드 트래픽 없음 — egress만 (ECR pull, SSM, OTLP push, RSS fetch)
resource "aws_security_group" "this" {
  name        = "yologram-worker-prod-sg"
  description = "Security group for yologram-worker-prod"
  vpc_id      = data.aws_vpc.prod.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "yologram-worker-prod-sg"
  }
}

resource "aws_ecs_task_definition" "this" {
  family                   = "yologram-worker-prod"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/ecs-task-execution-role"
  task_role_arn            = aws_iam_role.task.arn

  # portMappings 없음 — 외부 노출 불필요 (actuator는 ECS exec로 localhost:5000 접근)
  container_definitions = jsonencode([
    {
      name      = "yologram-worker"
      image     = "${aws_ecr_repository.this.repository_url}:latest"
      essential = true
      environment = [
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = "prod"
        }
      ]
    }
  ])
}

resource "aws_ecs_service" "this" {
  name                   = "yologram-worker-prod"
  cluster                = "ecs-prod"
  task_definition        = aws_ecs_task_definition.this.arn
  desired_count          = 1
  enable_execute_command = true

  capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = 1
  }

  network_configuration {
    subnets          = [data.aws_subnet.pub_a.id, data.aws_subnet.pub_b.id]
    security_groups  = [aws_security_group.this.id]
    assign_public_ip = true
  }
}
