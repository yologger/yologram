resource "aws_ecr_repository" "this" {
  name                 = "yologram-api-v2"
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
  name = "yologram-api-v2-prod-role"

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
        Resource = "arn:aws:ssm:ap-northeast-2:${data.aws_caller_identity.current.account_id}:parameter/yologram/service/yologram-api-v2_*"
      }
    ]
  })
}

resource "aws_iam_role_policy" "task_ses_send" {
  name = "ses-send-email"
  role = aws_iam_role.task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ses:SendEmail",
          "ses:SendRawEmail",
        ]
        Resource = "arn:aws:ses:ap-northeast-2:${data.aws_caller_identity.current.account_id}:identity/yologram.link"
        Condition = {
          StringEquals = {
            "ses:FromAddress" = "no-reply@yologram.link"
          }
        }
      }
    ]
  })
}

resource "aws_iam_role_policy" "execution_ssm_read" {
  name = "yologram-api-v2-ssm-read"
  role = "ecs-task-execution-role"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ssm:GetParameters",
        ]
        Resource = "arn:aws:ssm:ap-northeast-2:${data.aws_caller_identity.current.account_id}:parameter/yologram/service/yologram-api-v2_*"
      }
    ]
  })
}

resource "aws_iam_role_policy" "task_kinesis_put" {
  name = "kinesis-put"
  role = aws_iam_role.task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "kinesis:PutRecord",
          "kinesis:PutRecords",
        ]
        Resource = "arn:aws:kinesis:ap-northeast-2:${data.aws_caller_identity.current.account_id}:stream/yologram-post-view-event-prod"
      }
    ]
  })
}

################################
## SSM Parameter Store (prod) ##
################################

# 검색 인덱싱 큐 발행 — 어드민 인덱싱 요청(풀·범위)과 게시글 CRUD 시 단건 메시지를 넣는다.
# 소비는 worker 전담이라 여기에는 Send 권한만 준다(ReceiveMessage 없음)
resource "aws_iam_role_policy" "task_sqs_send" {
  name = "sqs-send"
  role = aws_iam_role.task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:SendMessage",
          "sqs:GetQueueUrl",
        ]
        Resource = "arn:aws:sqs:ap-northeast-2:${data.aws_caller_identity.current.account_id}:yologram-search-indexing-prod"
      }
    ]
  })
}

resource "aws_ssm_parameter" "jwt_secret_prod" {
  name  = "/yologram/service/yologram-api-v2_prod/yologram.auth.jwt.secret"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}
resource "aws_ssm_parameter" "cache_redis_host_prod" {
  name  = "/yologram/service/yologram-api-v2_prod/cache.data.redis.host"
  type  = "String"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "admin_jwt_secret_prod" {
  name  = "/yologram/service/yologram-api-v2_prod/yologram.auth.admin-jwt.secret"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}
resource "aws_ssm_parameter" "otel_endpoint_prod" {
  name  = "/yologram/service/yologram-api-v2_prod/OTEL_EXPORTER_OTLP_ENDPOINT"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "otel_headers_prod" {
  name  = "/yologram/service/yologram-api-v2_prod/OTEL_EXPORTER_OTLP_HEADERS"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

#################################
## SSM Parameter Store (DB prod) ##
#################################
resource "aws_ssm_parameter" "db_writer_url_prod" {
  name  = "/yologram/service/yologram-api-v2_prod/database.main.writer.datasource.url"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_writer_username_prod" {
  name  = "/yologram/service/yologram-api-v2_prod/database.main.writer.datasource.username"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_writer_password_prod" {
  name  = "/yologram/service/yologram-api-v2_prod/database.main.writer.datasource.password"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_reader_url_prod" {
  name  = "/yologram/service/yologram-api-v2_prod/database.main.reader.datasource.url"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_reader_username_prod" {
  name  = "/yologram/service/yologram-api-v2_prod/database.main.reader.datasource.username"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_reader_password_prod" {
  name  = "/yologram/service/yologram-api-v2_prod/database.main.reader.datasource.password"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_security_group" "this" {
  name        = "yologram-api-v2-prod-sg"
  description = "Security group for yologram-api-v2-prod"
  vpc_id      = data.aws_vpc.prod.id

  ingress {
    from_port   = 5000
    to_port     = 5000
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
    Name = "yologram-api-v2-prod-sg"
  }
}

resource "aws_ecs_task_definition" "this" {
  family                   = "yologram-api-v2-prod"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/ecs-task-execution-role"
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = "yologram-api-v2"
      image     = "${aws_ecr_repository.this.repository_url}:latest"
      essential = true
      portMappings = [
        {
          containerPort = 5000
          protocol      = "tcp"
        }
      ]
      environment = [
        {
          name  = "APP_PROFILE"
          value = "prod"
        }
      ]
      secrets = [
        {
          name      = "OTEL_EXPORTER_OTLP_ENDPOINT"
          valueFrom = aws_ssm_parameter.otel_endpoint_prod.arn
        },
        {
          name      = "OTEL_EXPORTER_OTLP_HEADERS"
          valueFrom = aws_ssm_parameter.otel_headers_prod.arn
        },
        {
          name      = "DB_URL"
          valueFrom = aws_ssm_parameter.db_writer_url_prod.arn
        },
        {
          name      = "DB_USERNAME"
          valueFrom = aws_ssm_parameter.db_writer_username_prod.arn
        },
        {
          name      = "DB_PASSWORD"
          valueFrom = aws_ssm_parameter.db_writer_password_prod.arn
        },
        {
          name      = "JWT_SECRET"
          valueFrom = aws_ssm_parameter.jwt_secret_prod.arn
        },
        {
          name      = "ADMIN_JWT_SECRET"
          valueFrom = aws_ssm_parameter.admin_jwt_secret_prod.arn
        },
        {
          name      = "CACHE_REDIS_HOST"
          valueFrom = aws_ssm_parameter.cache_redis_host_prod.arn
        },
        # 검색(OpenSearch) 접속 — 스위치(OPENSEARCH_MAIN_ENABLED)는 Dockerfile ENV.
        # api-v1은 앱이 SSM을 직접 읽지만 api-v2는 컨테이너 주입 방식이라 여기에 매핑이 필요하다
        {
          name      = "OPENSEARCH_MAIN_URI"
          valueFrom = aws_ssm_parameter.opensearch_uri_prod.arn
        },
        {
          name      = "OPENSEARCH_MAIN_USERNAME"
          valueFrom = aws_ssm_parameter.opensearch_username_prod.arn
        },
        {
          name      = "OPENSEARCH_MAIN_PASSWORD"
          valueFrom = aws_ssm_parameter.opensearch_password_prod.arn
        },
      ]
    }
  ])
}

resource "aws_service_discovery_service" "this" {
  name = "yologram-api-v2"

  dns_config {
    namespace_id = data.aws_service_discovery_dns_namespace.this.id

    dns_records {
      ttl  = 10
      type = "SRV"
    }
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}

resource "aws_ecs_service" "this" {
  name                   = "yologram-api-v2-prod"
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

  service_registries {
    registry_arn   = aws_service_discovery_service.this.arn
    container_name = "yologram-api-v2"
    container_port = 5000
  }
}

locals {
  api_gateway_id = tolist(data.aws_apigatewayv2_apis.this.ids)[0]
}

resource "aws_apigatewayv2_integration" "this" {
  api_id             = local.api_gateway_id
  integration_type   = "HTTP_PROXY"
  integration_uri    = aws_service_discovery_service.this.arn
  integration_method = "ANY"
  connection_type    = "VPC_LINK"
  connection_id      = var.vpc_link_id

  # api-v1과 동일 — 원 클라이언트 IP를 커스텀 헤더로 전달 (조회 이벤트 발행이 이 IP를 dedup 키로 쓴다).
  # HTTP API private integration은 remoteAddr이 VPC 내부 주소이고 X-Forwarded-For는 매핑 예약 헤더라
  # $context.identity.sourceIp가 원 IP를 얻는 유일한 경로다. overwrite로 클라이언트 위조값을 덮는다
  request_parameters = {
    "overwrite:header.X-Client-Ip" = "$context.identity.sourceIp"
  }
}

resource "aws_apigatewayv2_route" "this" {
  api_id    = local.api_gateway_id
  route_key = "ANY /api/v2/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.this.id}"
}

# OpenSearch(셀프호스팅, Lightsail) 접속 설정 — 검색 API 구현 전 미리 자리를 잡아둔다.
# 지금은 이 서비스가 읽지 않는다: 인덱싱 발행은 SQS(IAM Task Role)로 끝나고 OpenSearch에 직접 붙지 않는다.
# 검색 API를 붙일 때 yaml에서 enabled를 올리면 된다 (컨테이너 주입이라 task definition의 secrets 매핑도 함께 추가해야 한다).
# 이름은 worker와 동일(opensearch.main.*) — 같은 클러스터를 가리키는 값이 서비스마다 다른 키를 갖지 않게.
# uri는 tf가 실제 값을 관리하고, 자격증명만 PLACEHOLDER로 두고 콘솔에서 채운다.
# on/off 스위치(opensearch.main.enabled)는 SSM이 아니라 각 앱의 application-{env}.yaml에 둔다 — 환경별 고정값이라
resource "aws_ssm_parameter" "opensearch_uri_prod" {
  name  = "/yologram/service/yologram-api-v2_prod/opensearch.main.uri"
  type  = "SecureString"
  value = "https://opensearch.yologram.link"
}

resource "aws_ssm_parameter" "opensearch_username_prod" {
  name  = "/yologram/service/yologram-api-v2_prod/opensearch.main.username"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "opensearch_password_prod" {
  name  = "/yologram/service/yologram-api-v2_prod/opensearch.main.password"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}
