resource "aws_ecr_repository" "this" {
  name                 = "yologram-api-v1"
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
        Resource = "arn:aws:ssm:ap-northeast-2:${data.aws_caller_identity.current.account_id}:parameter/yologram/service/yologram-api-v1_*"
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
  name  = "/yologram/service/yologram-api-v1_prod/yologram.auth.jwt.secret"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "admin_jwt_secret_prod" {
  name  = "/yologram/service/yologram-api-v1_prod/yologram.auth.admin-jwt.secret"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "redis_host_prod" {
  name  = "/yologram/service/yologram-api-v1_prod/cache.data.redis.host"
  type  = "String"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}
resource "aws_ssm_parameter" "grafana_metrics_url_prod" {
  name  = "/yologram/service/yologram-api-v1_prod/management.otlp.metrics.export.url"
  type  = "String"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "grafana_metrics_auth_prod" {
  name  = "/yologram/service/yologram-api-v1_prod/management.otlp.metrics.export.headers.Authorization"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "grafana_traces_endpoint_prod" {
  name  = "/yologram/service/yologram-api-v1_prod/management.otlp.tracing.endpoint"
  type  = "String"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "grafana_traces_auth_prod" {
  name  = "/yologram/service/yologram-api-v1_prod/management.otlp.tracing.headers.Authorization"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "grafana_logs_endpoint_prod" {
  name  = "/yologram/service/yologram-api-v1_prod/management.otlp.logging.endpoint"
  type  = "String"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "grafana_logs_auth_prod" {
  name  = "/yologram/service/yologram-api-v1_prod/management.otlp.logging.headers.Authorization"
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
  name  = "/yologram/service/yologram-api-v1_prod/database.main.writer.datasource.url"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_writer_username_prod" {
  name  = "/yologram/service/yologram-api-v1_prod/database.main.writer.datasource.username"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_writer_password_prod" {
  name  = "/yologram/service/yologram-api-v1_prod/database.main.writer.datasource.password"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_reader_url_prod" {
  name  = "/yologram/service/yologram-api-v1_prod/database.main.reader.datasource.url"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_reader_username_prod" {
  name  = "/yologram/service/yologram-api-v1_prod/database.main.reader.datasource.username"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_reader_password_prod" {
  name  = "/yologram/service/yologram-api-v1_prod/database.main.reader.datasource.password"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_security_group" "this" {
  name        = "yologram-api-v1-prod-sg"
  description = "Security group for yologram-api-v1-prod"
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
    Name = "yologram-api-v1-prod-sg"
  }
}

resource "aws_ecs_task_definition" "this" {
  family                   = "yologram-api-v1-prod"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/ecs-task-execution-role"
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = "yologram-api-v1"
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
          name  = "SPRING_PROFILES_ACTIVE"
          value = "prod"
        }
      ]
    }
  ])
}

resource "aws_service_discovery_service" "this" {
  name = "yologram-api-v1"

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
  name                   = "yologram-api-v1-prod"
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
    container_name = "yologram-api-v1"
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

  # 원 클라이언트 IP 전달 — HTTP API + private integration(VPC Link)에서는 백엔드가 보는
  # remoteAddr이 VPC 내부 주소다(실측: 조회 이벤트 ip에 10.0.2.195 기록 → dedup이 사설 IP 하나로 수렴).
  # X-Forwarded-For는 파라미터 매핑 예약 헤더라 채울 수 없어, 원 IP를 얻는 유일한 경로인
  # $context.identity.sourceIp를 커스텀 헤더로 넘긴다 (api-v1 ClientIpResolver가 이 헤더를 우선 사용).
  # overwrite인 이유: 클라이언트가 같은 헤더를 위조해 보내도 게이트웨이 값으로 덮인다(조회수 조작 방지)
  request_parameters = {
    "overwrite:header.X-Client-Ip" = "$context.identity.sourceIp"
  }
}

resource "aws_apigatewayv2_route" "this" {
  api_id    = local.api_gateway_id
  route_key = "ANY /api/v1/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.this.id}"
}

# OpenSearch(셀프호스팅, Lightsail) 접속 설정 — 검색 API 구현 전 미리 자리를 잡아둔다.
# 지금은 이 서비스가 읽지 않는다: 인덱싱 발행은 SQS(IAM Task Role)로 끝나고 OpenSearch에 직접 붙지 않는다.
# 검색 API를 붙일 때 yaml에서 enabled를 올리면 된다.
# 이름은 worker와 동일(opensearch.main.*) — 같은 클러스터를 가리키는 값이 서비스마다 다른 키를 갖지 않게.
# uri는 tf가 실제 값을 관리하고, 자격증명만 PLACEHOLDER로 두고 콘솔에서 채운다.
# on/off 스위치(opensearch.main.enabled)는 SSM이 아니라 각 앱의 application-{env}.yaml에 둔다 — 환경별 고정값이라
resource "aws_ssm_parameter" "opensearch_uri_prod" {
  name  = "/yologram/service/yologram-api-v1_prod/opensearch.main.uri"
  type  = "SecureString"
  value = "https://opensearch.yologram.link"
}

resource "aws_ssm_parameter" "opensearch_username_prod" {
  name  = "/yologram/service/yologram-api-v1_prod/opensearch.main.username"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "opensearch_password_prod" {
  name  = "/yologram/service/yologram-api-v1_prod/opensearch.main.password"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }
}
