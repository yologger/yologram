output "db_endpoint" {
  value = aws_db_instance.service_prod.endpoint
}

output "db_security_group_id" {
  value = aws_security_group.service_prod.id
}
