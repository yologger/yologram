resource "aws_ecr_repository" "this" {
  name                 = "yologram-web-v2"
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
  name = "yologram-web-v2-prod-role"

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
  name        = "yologram-web-v2-prod-sg"
  description = "Security group for yologram-web-v2-prod"
  vpc_id      = "vpc-00dd45cf23d6d31ee"

  ingress {
    from_port   = 3000
    to_port     = 3000
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
    Name = "yologram-web-v2-prod-sg"
  }
}

resource "aws_ecs_task_definition" "this" {
  family                   = "yologram-web-v2-prod"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = "arn:aws:iam::000000000000:role/ecs-task-execution-role"
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = "yologram-web-v2"
      image     = "${aws_ecr_repository.this.repository_url}:latest"
      essential = true
      portMappings = [
        {
          containerPort = 3000
          protocol      = "tcp"
        }
      ]
    }
  ])
}

#######################
## Cloud Map Service ##
#######################
data "aws_service_discovery_dns_namespace" "this" {
  name = "ecs-prod.internal"
  type = "DNS_PRIVATE"
}

resource "aws_service_discovery_service" "this" {
  name = "yologram-web-v2"

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

#################
## ECS Service ##
#################
resource "aws_ecs_service" "this" {
  name                   = "yologram-web-v2-prod"
  cluster                = "ecs-prod"
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
    registry_arn   = aws_service_discovery_service.this.arn
    container_name = "yologram-web-v2"
    container_port = 3000
  }
}

##############################
## API Gateway: Integration ##
##############################
data "aws_apigatewayv2_apis" "this" {
  protocol_type = "HTTP"
  name          = "yologram-gateway"
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
}

resource "aws_apigatewayv2_route" "this" {
  api_id    = local.api_gateway_id
  route_key = "ANY /{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.this.id}"
}

#################################
## Custom Domain: web.v2       ##
#################################
resource "aws_acm_certificate" "this" {
  domain_name       = "web.v2.yologram.link"
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_route53_record" "cert_validation" {
  for_each = {
    for dvo in aws_acm_certificate.this.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  }

  zone_id = var.route53_zone_id
  name    = each.value.name
  type    = each.value.type
  ttl     = 300
  records = [each.value.record]
}

resource "aws_acm_certificate_validation" "this" {
  certificate_arn         = aws_acm_certificate.this.arn
  validation_record_fqdns = [for record in aws_route53_record.cert_validation : record.fqdn]
}

resource "aws_apigatewayv2_domain_name" "this" {
  domain_name = "web.v2.yologram.link"

  domain_name_configuration {
    certificate_arn = aws_acm_certificate_validation.this.certificate_arn
    endpoint_type   = "REGIONAL"
    security_policy = "TLS_1_2"
  }
}

resource "aws_apigatewayv2_api_mapping" "this" {
  api_id      = local.api_gateway_id
  domain_name = aws_apigatewayv2_domain_name.this.id
  stage       = "$default"
}

resource "aws_route53_record" "this" {
  zone_id = var.route53_zone_id
  name    = "web.v2.yologram.link"
  type    = "A"

  alias {
    name                   = aws_apigatewayv2_domain_name.this.domain_name_configuration[0].target_domain_name
    zone_id                = aws_apigatewayv2_domain_name.this.domain_name_configuration[0].hosted_zone_id
    evaluate_target_health = false
  }
}
