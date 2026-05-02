output "instance_name" {
  value = aws_lightsail_instance.yologram.name
}

output "static_ip" {
  value = aws_lightsail_static_ip.yologram.ip_address
}
