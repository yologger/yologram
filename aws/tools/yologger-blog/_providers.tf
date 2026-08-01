terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  profile = "yologram"
  region  = "ap-northeast-2"
}

# CloudFront 용 ACM 인증서는 us-east-1 에만 발급 가능
provider "aws" {
  alias   = "us_east_1"
  profile = "yologram"
  region  = "us-east-1"
}
