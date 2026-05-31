variable "opensearch_master_password" {
  type      = string
  sensitive = true
}

resource "aws_opensearch_domain" "prod" {
  domain_name    = "opensearch-prod"
  engine_version = "OpenSearch_2.19"

  cluster_config {
    instance_type          = "t3.small.search"
    instance_count         = 1
    zone_awareness_enabled = false
  }

  ebs_options {
    ebs_enabled = true
    volume_type = "gp3"
    volume_size = 10
  }

  advanced_security_options {
    enabled                        = true
    internal_user_database_enabled = true

    master_user_options {
      master_user_name     = "master"
      master_user_password = var.opensearch_master_password
    }
  }

  encrypt_at_rest {
    enabled = true
  }

  node_to_node_encryption {
    enabled = true
  }

  domain_endpoint_options {
    enforce_https       = true
    tls_security_policy = "Policy-Min-TLS-1-2-2019-07"
  }

  access_policies = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root" }
        Action    = "es:*"
        Resource  = "arn:aws:es:ap-northeast-2:${data.aws_caller_identity.current.account_id}:domain/opensearch-prod/*"
      }
    ]
  })

  tags = {
    Name = "opensearch-prod"
  }
}
