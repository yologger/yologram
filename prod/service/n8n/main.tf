resource "aws_lightsail_instance" "n8n" {
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

    mkdir -p /opt/n8n/data
    chown -R 1000:1000 /opt/n8n/data

    cat > /opt/n8n/docker-compose.yml << 'COMPOSE'
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
      n8n:
        image: n8nio/n8n
        restart: always
        environment:
          - WEBHOOK_URL=https://n8n.yologram.link
          - N8N_HOST=n8n.yologram.link
          - N8N_PROTOCOL=https
        volumes:
          - ./data:/home/node/.n8n
    volumes:
      caddy_data:
      caddy_config:
    COMPOSE

    cat > /opt/n8n/Caddyfile << 'CADDY'
    n8n.yologram.link {
      reverse_proxy n8n:5678
    }
    CADDY

    cd /opt/n8n && docker-compose up -d
  USERDATA
}

resource "aws_lightsail_static_ip" "n8n" {
  name = "${var.instance_name}-ip"
}

resource "aws_lightsail_static_ip_attachment" "n8n" {
  static_ip_name = aws_lightsail_static_ip.n8n.name
  instance_name  = aws_lightsail_instance.n8n.name
}

resource "aws_route53_record" "n8n" {
  zone_id = var.route53_zone_id
  name    = "n8n.yologram.link"
  type    = "A"
  ttl     = 300
  records = [aws_lightsail_static_ip.n8n.ip_address]
}

resource "aws_lightsail_instance_public_ports" "n8n" {
  instance_name = aws_lightsail_instance.n8n.name

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

}

