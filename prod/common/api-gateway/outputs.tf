output "api_gateway_id" {
  value = aws_apigatewayv2_api.api.id
}

output "api_gateway_endpoint" {
  value = aws_apigatewayv2_api.api.api_endpoint
}

output "vpc_link_id" {
  value = aws_apigatewayv2_vpc_link.prod.id
}

output "cloud_map_namespace_id" {
  value = aws_service_discovery_private_dns_namespace.prod.id
}

output "yologram_api_v1_discovery_arn" {
  value = aws_service_discovery_service.yologram_api_v1.arn
}
