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

# 조회수 이벤트 컨슈머(Spring Cloud Stream Kinesis binder, KCL 모드) — 스트림 읽기 + KCL 리스 테이블
resource "aws_iam_role_policy" "task_kinesis_get" {
  name = "kinesis-get"
  role = aws_iam_role.task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "kinesis:DescribeStream",
          "kinesis:DescribeStreamSummary",
          "kinesis:GetRecords",
          "kinesis:GetShardIterator",
          "kinesis:ListShards",
        ]
        Resource = "arn:aws:kinesis:ap-northeast-2:${data.aws_caller_identity.current.account_id}:stream/yologram-post-view-event-prod"
      },
      {
        # binder가 스트림 존재 확인 시 사용 (리소스 한정 불가 액션)
        Effect   = "Allow"
        Action   = ["kinesis:ListStreams"]
        Resource = "*"
      },
      {
        # KCL 리스 테이블 — 샤드 리스와 체크포인트를 이 테이블 하나로 관리.
        # 테이블은 tf가 아니라 KCL이 부팅 시 자동 생성한다(없을 때만 CreateTable, 있으면 DescribeTable만 — 소스·실측 확인).
        # 리스 테이블은 KCL 내부 상태라 수명주기를 KCL에 맡기는 편이 단순하다. Scan은 리스 목록 조회용
        Effect = "Allow"
        Action = [
          "dynamodb:CreateTable",
          "dynamodb:DescribeTable",
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:UpdateItem",
          "dynamodb:DeleteItem",
          "dynamodb:Query",
          "dynamodb:Scan",
        ]
        Resource = "arn:aws:dynamodb:ap-northeast-2:${data.aws_caller_identity.current.account_id}:table/yologram-post-view-event-lease-prod"
      }
    ]
  })
}

################################
## SSM Parameter Store (prod) ##
################################

# 검색 인덱싱 큐 소비 — 메시지를 받아 DB 조회 후 OpenSearch에 bulk 인덱싱한다.
# 발행은 api-v1·v2 몫이라 SendMessage는 주지 않는다.
# ChangeMessageVisibility는 처리 실패 시 재시도 시점을 조정하는 데 쓴다(레거시 패턴).
# DLQ는 SQS가 리드라이브 정책으로 직접 옮기므로 워커에 DLQ 권한이 필요하지 않다
resource "aws_iam_role_policy" "task_sqs_receive" {
  name = "sqs-receive"
  role = aws_iam_role.task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:ChangeMessageVisibility",
          "sqs:GetQueueAttributes",
          "sqs:GetQueueUrl",
        ]
        Resource = "arn:aws:sqs:ap-northeast-2:${data.aws_caller_identity.current.account_id}:yologram-search-indexing-prod"
      }
    ]
  })
}

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

# Discord 웹훅 — 채널별 url (요약 알림. enabled는 yaml에서 관리, 실제 값은 콘솔에서 직접 입력).
# 프로퍼티 경로 개편(yologram.webhooks.discord.{채널}-news)으로 이관 완료 —
# 구 이름(yologram.discord.webhooks.{채널}.url) 3개는 배포·알림 확인 후 삭제했다(tf 관리 밖이라 CLI 삭제)
resource "aws_ssm_parameter" "discord_webhook_tech_url_prod" {
  name  = "/yologram/service/yologram-worker_prod/yologram.webhooks.discord.tech-news.url"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "discord_webhook_invest_url_prod" {
  name  = "/yologram/service/yologram-worker_prod/yologram.webhooks.discord.invest-news.url"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "discord_webhook_politics_url_prod" {
  name  = "/yologram/service/yologram-worker_prod/yologram.webhooks.discord.politics-news.url"
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
  # 0.25vCPU/512MB에서 조회 이벤트 소비가 멈췄다 — KCL의 Netty 이벤트 루프가 CPU를 얻지 못해
  # "Acquire operation took longer than the configured maximum time"(SDK 커넥션 획득 타임아웃)이 반복됐고,
  # 뉴스 배치(RSS 크롤링 + LLM 호출 read timeout 60초)와 같은 코어를 다투는 구조가 원인이었다.
  # 0.5vCPU/1GB로 상향 (Fargate Spot 월 약 $3 → $6)
  cpu                = "512"
  memory             = "1024"
  execution_role_arn = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/ecs-task-execution-role"
  task_role_arn      = aws_iam_role.task.arn

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

# OpenSearch(셀프호스팅, Lightsail) 접속 설정 — security plugin의 basic auth.
# uri는 tf가 실제 값을 관리하고, 자격증명만 PLACEHOLDER로 두고 콘솔에서 채운다.
# on/off 스위치(opensearch.main.enabled)는 SSM이 아니라 각 앱의 application-{env}.yaml에 둔다 — 환경별 고정값이라
# (tf 코드·state에 비밀이 남지 않게 — 다른 시크릿 파라미터와 동일 패턴)
resource "aws_ssm_parameter" "opensearch_uri_prod" {
  name  = "/yologram/service/yologram-worker_prod/opensearch.main.uri"
  type  = "SecureString"
  value = "https://opensearch.yologram.link"
}

resource "aws_ssm_parameter" "opensearch_username_prod" {
  name  = "/yologram/service/yologram-worker_prod/opensearch.main.username"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "opensearch_password_prod" {
  name  = "/yologram/service/yologram-worker_prod/opensearch.main.password"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}
