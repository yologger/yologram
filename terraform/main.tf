resource "aws_lightsail_instance" "yologram" {
  name              = var.instance_name
  availability_zone = "${var.aws_region}a"
  blueprint_id      = var.blueprint_id
  bundle_id         = var.bundle_id
  ip_address_type   = "ipv4"

  user_data = <<-USERDATA
    #!/bin/bash
    dnf install -y docker
    systemctl enable --now docker

    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose

    mkdir -p /opt/yologram

    cat > /opt/yologram/docker-compose.yml << 'COMPOSE'
    services:
      caddy:
        image: caddy:2
        restart: always
        ports:
          - "80:80"
          - "443:443"
        volumes:
          - ./Caddyfile:/etc/caddy/Caddyfile
          - caddy_data:/data
          - caddy_config:/config

      v1-backend:
        image: v1-backend:latest
        restart: always

      v2-backend:
        image: v2-backend:latest
        restart: always

      v2-frontend:
        image: v2-frontend:latest
        restart: always

    volumes:
      caddy_data:
      caddy_config:
    COMPOSE

    cat > /opt/yologram/Caddyfile << 'CADDY'
    api.v1.yologram.link {
      reverse_proxy v1-backend:8080
    }

    api.v2.yologram.link {
      reverse_proxy v2-backend:8000
    }

    web-v2.yologram.link {
      reverse_proxy v2-frontend:3000
    }
    CADDY

    cd /opt/yologram && docker-compose up -d
  USERDATA
}

resource "aws_lightsail_static_ip" "yologram" {
  name = "${var.instance_name}-ip"
}

resource "aws_lightsail_static_ip_attachment" "yologram" {
  static_ip_name = aws_lightsail_static_ip.yologram.name
  instance_name  = aws_lightsail_instance.yologram.name
}

resource "aws_lightsail_instance_public_ports" "yologram" {
  instance_name = aws_lightsail_instance.yologram.name

  port_info {
    protocol   = "tcp"
    from_port  = 80
    to_port    = 80
    cidrs      = ["0.0.0.0/0"]
    ipv6_cidrs = ["::/0"]
  }

  port_info {
    protocol   = "tcp"
    from_port  = 443
    to_port    = 443
    cidrs      = ["0.0.0.0/0"]
    ipv6_cidrs = ["::/0"]
  }
}

resource "aws_route53_record" "api_v1" {
  zone_id = var.route53_zone_id
  name    = "api.v1.yologram.link"
  type    = "A"
  ttl     = 300
  records = [aws_lightsail_static_ip.yologram.ip_address]
}

resource "aws_route53_record" "api_v2" {
  zone_id = var.route53_zone_id
  name    = "api.v2.yologram.link"
  type    = "A"
  ttl     = 300
  records = [aws_lightsail_static_ip.yologram.ip_address]
}

resource "aws_route53_record" "web_v2" {
  zone_id = var.route53_zone_id
  name    = "web-v2.yologram.link"
  type    = "A"
  ttl     = 300
  records = [aws_lightsail_static_ip.yologram.ip_address]
}
