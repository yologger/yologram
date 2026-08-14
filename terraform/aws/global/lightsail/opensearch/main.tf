
resource "aws_lightsail_instance" "opensearch" {
  name              = var.instance_name
  availability_zone = "${var.aws_region}a"
  blueprint_id      = var.blueprint_id
  bundle_id         = var.bundle_id
  ip_address_type   = "ipv4"

  # 비밀번호가 user_data에 렌더링되므로 콘솔에서 확인 가능하다 —
  # 노출 경로를 줄이려면 최초 기동 후 security plugin의 internal user API로 교체하는 편이 낫다
  user_data = <<-USERDATA
    #!/bin/bash
    set -euo pipefail

    # ── swap 2GB: 2GB 인스턴스에서 OpenSearch(heap ${var.opensearch_heap}) + Dashboards가 공존하려면 필요.
    #    OpenSearch는 스와핑을 싫어하지만(bootstrap.memory_lock 권장) 여기서는 OOM kill 방지가 우선이다
    if [ ! -f /swapfile ]; then
      dd if=/dev/zero of=/swapfile bs=1M count=2048
      chmod 600 /swapfile
      mkswap /swapfile
      swapon /swapfile
      echo '/swapfile none swap sw 0 0' >> /etc/fstab
    fi

    # ── mmap 카운트: OpenSearch가 Lucene 인덱스를 mmapfs로 열 때 필요한 커널 한도
    echo 'vm.max_map_count=262144' > /etc/sysctl.d/99-opensearch.conf
    sysctl -p /etc/sysctl.d/99-opensearch.conf

    # ── Docker
    dnf install -y docker
    systemctl enable --now docker

    # ── docker compose: 버전·체크섬 고정 (latest 다운로드는 공급망 위험)
    COMPOSE_VERSION="v5.4.0"
    COMPOSE_SHA256="837fd1d35bf6a494f41b5b5988269a7be79de337cf1a1a6ff0e45ab51bb4e9be"
    mkdir -p /usr/local/lib/docker/cli-plugins
    curl -fsSL "https://github.com/docker/compose/releases/download/$COMPOSE_VERSION/docker-compose-linux-x86_64" \
      -o /usr/local/lib/docker/cli-plugins/docker-compose
    echo "$COMPOSE_SHA256  /usr/local/lib/docker/cli-plugins/docker-compose" | sha256sum -c -
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

    # ── 데이터 디렉토리: 인스턴스 디스크에 두어 컨테이너 재시작·인스턴스 재부팅에도 유지된다.
    #    OpenSearch 컨테이너는 uid 1000으로 실행되므로 소유권을 맞춘다
    mkdir -p /opt/opensearch/data
    chown -R 1000:1000 /opt/opensearch/data

    # ── 비밀번호는 .env로 분리한다. compose 파일에 직접 쓰면 값의 `$`가 변수 보간으로 먹히고
    #    `#`는 YAML 주석으로 잘린다(실측: `A$B-C`가 `A-C`로 들어가 로그인 실패).
    #    env_file은 보간 없이 그대로 컨테이너에 전달된다
    umask 077
    cat > /opt/opensearch/os.env << 'ENVFILE'
    OPENSEARCH_INITIAL_ADMIN_PASSWORD=${var.admin_password}
    ENVFILE
    umask 022

    cat > /opt/opensearch/docker-compose.yml << 'COMPOSE'
    services:
      opensearch:
        image: opensearchproject/opensearch:${var.opensearch_version}
        restart: always
        # nori(한국어 형태소 분석기)는 공식 이미지에 없다 — standard analyzer는 한글을 글자 단위로 쪼개
        # 키워드 검색이 무의미해지므로 기동 시 설치한다.
        # 이미지를 빌드하지 않는 이유: docker compose v5의 build는 buildx 0.17+를 요구하고
        # compose 플러그인만 설치한 환경에서 "compose build requires buildx" 로 실패한다(실측).
        # 설치는 멱등이라(list로 확인 후 install) 컨테이너 재시작에도 안전하다
        command:
          - sh
          - -c
          - |
            if ! ./bin/opensearch-plugin list | grep -q analysis-nori; then
              ./bin/opensearch-plugin install --batch analysis-nori
            fi
            exec ./opensearch-docker-entrypoint.sh
        env_file:
          - ./os.env
        environment:
          - discovery.type=single-node
          - bootstrap.memory_lock=true
          - "OPENSEARCH_JAVA_OPTS=-Xms${var.opensearch_heap} -Xmx${var.opensearch_heap}"
        ulimits:
          memlock: { soft: -1, hard: -1 }
          nofile: { soft: 65536, hard: 65536 }
        volumes:
          - ./data:/usr/share/opensearch/data
        # 포트를 호스트에 노출하지 않는다 — 외부 접근은 Caddy(443)만 통과한다
        expose:
          - "9200"

      dashboards:
        image: opensearchproject/opensearch-dashboards:${var.opensearch_version}
        restart: always
        environment:
          - OPENSEARCH_HOSTS=["https://opensearch:9200"]
          # OpenSearch security plugin의 기본 인증서는 self-signed이라 검증을 끈다.
          # 컨테이너 네트워크 내부 통신이고 외부 구간은 Caddy가 정식 인증서로 감싼다
          - OPENSEARCH_SSL_VERIFICATIONMODE=none
        expose:
          - "5601"
        depends_on:
          - opensearch

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
        depends_on:
          - opensearch
          - dashboards

    volumes:
      caddy_data:
      caddy_config:
    COMPOSE

    # ── Caddy: 도메인별 TLS 종료 + 리버스 프록시. 인증서 발급·갱신은 Caddy가 자동 처리(Let's Encrypt).
    #    업스트림이 self-signed HTTPS라 tls_insecure_skip_verify가 필요하다 —
    #    OpenSearch basic auth는 그대로 살아 있으므로 인증은 OpenSearch가 담당한다
    cat > /opt/opensearch/Caddyfile << 'CADDY'
    ${var.domain_api} {
      reverse_proxy https://opensearch:9200 {
        transport http {
          tls_insecure_skip_verify
        }
      }
    }

    ${var.domain_dashboards} {
      reverse_proxy dashboards:5601
    }
    CADDY

    cd /opt/opensearch && docker compose up -d
  USERDATA

  # 자동 스냅샷(일 1회) — 인스턴스 손상·실수 삭제 대비.
  # 보관은 7일 롤링 고정이다(개수·기간 지정 불가 — 최신 7개를 넘기면 가장 오래된 것이 밀려난다).
  # 증분 저장이라 7개가 용량 7배가 되지는 않는다: $0.05/GB-월 기준 월 $0.5 미만.
  # 인스턴스를 삭제하면 자동 스냅샷도 함께 삭제되므로, 남겨야 할 시점은 수동 스냅샷으로 복사한다
  add_on {
    type          = "AutoSnapshot"
    snapshot_time = "19:00" # UTC — KST 04:00, 트래픽 최저 시간대
    status        = "Enabled"
  }

  tags = {
    Name = var.instance_name
  }

  lifecycle {
    # user_data 변경을 인스턴스 교체로 처리하지 않는다.
    # user_data에 admin 비밀번호가 렌더링되므로, apply 때 다른 값을 입력하면
    # Lightsail이 인스턴스를 파괴·재생성하고 그 순간 인덱스 데이터가 사라진다(실측 확인).
    # 프로비저닝 스크립트를 실제로 바꿔야 할 때는 의도적으로 taint 후 교체하고,
    # 데이터가 있으면 그 전에 스냅샷을 뜬다
    ignore_changes = [user_data]
  }
}

resource "aws_lightsail_static_ip" "opensearch" {
  name = "${var.instance_name}-static-ip"
}

resource "aws_lightsail_static_ip_attachment" "opensearch" {
  static_ip_name = aws_lightsail_static_ip.opensearch.name
  instance_name  = aws_lightsail_instance.opensearch.name
}

resource "aws_lightsail_instance_public_ports" "opensearch" {
  instance_name = aws_lightsail_instance.opensearch.name

  port_info {
    protocol  = "tcp"
    from_port = 80
    to_port   = 80
  }

  port_info {
    protocol  = "tcp"
    from_port = 443
    to_port   = 443
  }
}

resource "aws_route53_record" "api" {
  zone_id = var.route53_zone_id
  name    = var.domain_api
  type    = "A"
  ttl     = 300
  records = [aws_lightsail_static_ip_attachment.opensearch.ip_address]
}

resource "aws_route53_record" "dashboards" {
  zone_id = var.route53_zone_id
  name    = var.domain_dashboards
  type    = "A"
  ttl     = 300
  records = [aws_lightsail_static_ip_attachment.opensearch.ip_address]
}
