# Cloud Map namespace
resource "aws_service_discovery_private_dns_namespace" "prod" {
  name = "prod.internal"
  vpc  = "vpc-00dd45cf23d6d31ee"
}

# VPC Link
resource "aws_apigatewayv2_vpc_link" "prod" {
  name               = "prod-vpc-link"
  security_group_ids = [aws_security_group.vpc_link.id]
  subnet_ids = [
    "subnet-02695a768e3c457df",
    "subnet-0c36f6f4a5208338a",
  ]
}

resource "aws_security_group" "vpc_link" {
  name        = "api-gateway-vpc-link-sg"
  description = "Security group for API Gateway VPC Link"
  vpc_id      = "vpc-00dd45cf23d6d31ee"

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "api-gateway-vpc-link-sg"
  }
}

# HTTP API Gateway
resource "aws_apigatewayv2_api" "api" {
  name          = "api-yologram"
  protocol_type = "HTTP"
}

# Stage
resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.api.id
  name        = "$default"
  auto_deploy = true

  default_route_settings {
    throttling_burst_limit = 10
    throttling_rate_limit  = 10
  }
}

# Custom domain: api.yologram.link
resource "aws_acm_certificate" "api" {
  domain_name       = "api.yologram.link"
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_route53_record" "api_cert_validation" {
  for_each = {
    for dvo in aws_acm_certificate.api.domain_validation_options : dvo.domain_name => {
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

resource "aws_acm_certificate_validation" "api" {
  certificate_arn         = aws_acm_certificate.api.arn
  validation_record_fqdns = [for record in aws_route53_record.api_cert_validation : record.fqdn]
}

resource "aws_apigatewayv2_domain_name" "api" {
  domain_name = "api.yologram.link"

  domain_name_configuration {
    certificate_arn = aws_acm_certificate_validation.api.certificate_arn
    endpoint_type   = "REGIONAL"
    security_policy = "TLS_1_2"
  }
}

resource "aws_apigatewayv2_api_mapping" "api" {
  api_id      = aws_apigatewayv2_api.api.id
  domain_name = aws_apigatewayv2_domain_name.api.id
  stage       = aws_apigatewayv2_stage.default.id
}

resource "aws_route53_record" "api" {
  zone_id = var.route53_zone_id
  name    = "api.yologram.link"
  type    = "A"

  alias {
    name                   = aws_apigatewayv2_domain_name.api.domain_name_configuration[0].target_domain_name
    zone_id                = aws_apigatewayv2_domain_name.api.domain_name_configuration[0].hosted_zone_id
    evaluate_target_health = false
  }
}

# ============================================
# yologram-api-v1 (Spring Boot)
# ============================================
resource "aws_service_discovery_service" "yologram_api_v1" {
  name = "yologram-api-v1"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.prod.id

    dns_records {
      ttl  = 10
      type = "SRV"
    }
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}

resource "aws_apigatewayv2_integration" "yologram_api_v1" {
  api_id             = aws_apigatewayv2_api.api.id
  integration_type   = "HTTP_PROXY"
  integration_uri    = aws_service_discovery_service.yologram_api_v1.arn
  integration_method = "ANY"
  connection_type    = "VPC_LINK"
  connection_id      = aws_apigatewayv2_vpc_link.prod.id
}

resource "aws_apigatewayv2_route" "yologram_api_v1" {
  api_id    = aws_apigatewayv2_api.api.id
  route_key = "ANY /v1/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.yologram_api_v1.id}"
}
