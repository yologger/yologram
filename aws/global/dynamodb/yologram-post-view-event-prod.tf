resource "aws_dynamodb_table" "yologram_post_view_event_checkpoint_prod" {
  name           = "yologram-post-view-event-checkpoint-prod"
  billing_mode   = "PROVISIONED"
  read_capacity  = 5
  write_capacity = 5
  hash_key       = "metadataKey"

  attribute {
    name = "metadataKey"
    type = "S"
  }

  ttl {
    attribute_name = "expireAt"
    enabled        = true
  }

  tags = {
    Name = "yologram-post-view-event-checkpoint-prod"
  }
}

resource "aws_dynamodb_table" "yologram_post_view_event_lock_prod" {
  name           = "yologram-post-view-event-lock-prod"
  billing_mode   = "PROVISIONED"
  read_capacity  = 5
  write_capacity = 5
  hash_key       = "lockKey"

  attribute {
    name = "lockKey"
    type = "S"
  }

  ttl {
    attribute_name = "expireAt"
    enabled        = true
  }

  tags = {
    Name = "yologram-post-view-event-lock-prod"
  }
}
