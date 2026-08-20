variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "instance_name" {
  description = "Lightsail instance name"
  type        = string
  default     = "yologram-opensearch"
}

# 관리형 OpenSearch(t3.small.search $40.88/월) 대체 목적이라 인스턴스 비용이 판단 기준이다.
# small_3_0(2GB)에서 medium_3_0(4GB)으로 상향($12 → $24/월): heap 512m + Dashboards(~400MB) + Caddy를
# 2GB에 올리면 여유가 없어 swap에 의존했고, 검색 응답이 느렸다.
#
# 주의: bundle_id는 aws_lightsail_instance의 재생성 속성이다 — apply하면 인스턴스가 파괴되고 새로 만들어져
# 인덱스가 사라진다. 데이터는 어드민 인덱싱(게시글·뉴스 전체 발행)으로 복구 가능하고 user_data가
# OpenSearch·Dashboards·Caddy를 다시 구성하지만, 재색인을 전제로만 apply할 것.
# 데이터를 유지하려면 콘솔에서 스냅샷 → 큰 번들로 새 인스턴스 생성 후 state를 맞추는 경로를 써야 한다.
variable "bundle_id" {
  description = "Lightsail bundle (micro_3_0: 1GB, small_3_0: 2GB, medium_3_0: 4GB)"
  type        = string
  default     = "medium_3_0"
}

variable "blueprint_id" {
  description = "Lightsail instance OS blueprint"
  type        = string
  default     = "amazon_linux_2023"
}

variable "route53_zone_id" {
  description = "Route 53 hosted zone ID for yologram.link"
  type        = string
  default     = "Z0365311288PJQTGLT9S3"
}

variable "domain_api" {
  description = "OpenSearch REST API 도메인"
  type        = string
  default     = "opensearch.yologram.link"
}

variable "domain_dashboards" {
  description = "OpenSearch Dashboards(웹 UI) 도메인"
  type        = string
  default     = "opensearch-dashboard.yologram.link"
}

variable "opensearch_version" {
  description = "OpenSearch·Dashboards 이미지 태그 (2.x 계열 — Spring Data OpenSearch 호환 대역)"
  type        = string
  default     = "2.19.6"
}

# JVM heap. 물리 메모리의 절반 이하 권장이고, 2GB 인스턴스에서 Dashboards와 공존하려면 512m가 상한에 가깝다
variable "opensearch_heap" {
  description = "OpenSearch JVM heap (-Xms/-Xmx)"
  type        = string
  default     = "512m"
}

# security plugin의 admin 비밀번호. 2.12+는 이 값이 없으면 컨테이너가 기동에 실패한다.
# public 레포라 코드·tfvars에 두지 않고 apply 시 입력받는다 (database·opensearch 기존 방식과 동일).
# 대문자·소문자·숫자·특수문자를 포함한 강한 비밀번호여야 부트스트랩 검사를 통과한다
variable "admin_password" {
  description = "OpenSearch security plugin admin 비밀번호 (apply 시 입력)"
  type        = string
  sensitive   = true
}
