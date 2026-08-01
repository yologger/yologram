variable "route53_zone_id" {
  type    = string
  default = "Z0365311288PJQTGLT9S3"
}

variable "domain" {
  type    = string
  default = "blog.yologram.link"
}

# S3 버킷 이름은 전역에서 유일해야 한다. apply 시 충돌하면 이 값을 바꾼다.
variable "bucket_name" {
  type    = string
  default = "yologger-blog"
}
