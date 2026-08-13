output "static_ip" {
  description = "Lightsail 정적 IP (Route 53 A 레코드가 가리키는 주소)"
  value       = aws_lightsail_static_ip_attachment.opensearch.ip_address
}

output "api_endpoint" {
  description = "OpenSearch REST API (basic auth — 사용자 admin)"
  value       = "https://${var.domain_api}"
}

output "dashboards_endpoint" {
  description = "OpenSearch Dashboards 웹 UI"
  value       = "https://${var.domain_dashboards}"
}
