resource "aws_lightsail_instance" "n8n" {
  name              = var.instance_name
  availability_zone = "${var.aws_region}a"
  blueprint_id      = var.blueprint_id
  bundle_id         = var.bundle_id
  ip_address_type   = "ipv4"
}

resource "aws_lightsail_static_ip" "n8n" {
  name = "${var.instance_name}-ip"
}

resource "aws_lightsail_static_ip_attachment" "n8n" {
  static_ip_name = aws_lightsail_static_ip.n8n.name
  instance_name  = aws_lightsail_instance.n8n.name
}

resource "aws_lightsail_instance_public_ports" "n8n" {
  instance_name = aws_lightsail_instance.n8n.name

  port_info {
    protocol    = "tcp"
    from_port   = 22
    to_port     = 22
    cidrs       = ["0.0.0.0/0"]
    ipv6_cidrs  = ["::/0"]
  }

  port_info {
    protocol    = "tcp"
    from_port   = 80
    to_port     = 80
    cidrs       = ["0.0.0.0/0"]
    ipv6_cidrs  = ["::/0"]
  }

  port_info {
    protocol    = "tcp"
    from_port   = 443
    to_port     = 443
    cidrs       = ["0.0.0.0/0"]
    ipv6_cidrs  = ["::/0"]
  }

  port_info {
    protocol    = "tcp"
    from_port   = 5678
    to_port     = 5678
    cidrs       = ["0.0.0.0/0"]
    ipv6_cidrs  = ["::/0"]
  }
}

