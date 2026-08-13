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
# small_3_0(2GB)은 OpenSearch heap 512m + Dashboards(~400MB) + Caddy를 올리면 여유가 적어 swap으로 보완한다.
# 부족하면 medium_3_0(4GB)으로 리사이즈 — Lightsail은 스냅샷 경유로 상향이 간단하다
variable "bundle_id" {
  description = "Lightsail bundle (micro_3_0: 1GB, small_3_0: 2GB, medium_3_0: 4GB)"
  type        = string
  default     = "small_3_0"
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
