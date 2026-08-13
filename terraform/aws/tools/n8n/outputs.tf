output "instance_name" {
  value = aws_lightsail_instance.n8n.name
}

output "static_ip" {
  value = aws_lightsail_static_ip.n8n.ip_address
}
