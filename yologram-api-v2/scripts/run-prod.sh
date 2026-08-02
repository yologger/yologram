#!/bin/bash
export APP_PROFILE=prod
export AWS_PROFILE=yologram
export DB_URL=$(aws ssm get-parameter --name "/yologram/service/yologram-api-v2_prod/database.main.writer.datasource.url" --query "Parameter.Value" --output text --with-decryption --profile yologram)
export DB_USERNAME=$(aws ssm get-parameter --name "/yologram/service/yologram-api-v2_prod/database.main.writer.datasource.username" --query "Parameter.Value" --output text --with-decryption --profile yologram)
export DB_PASSWORD=$(aws ssm get-parameter --name "/yologram/service/yologram-api-v2_prod/database.main.writer.datasource.password" --query "Parameter.Value" --output text --with-decryption --profile yologram)
export JWT_SECRET=$(aws ssm get-parameter --name "/yologram/service/yologram-api-v2_prod/yologram.auth.jwt.secret" --query "Parameter.Value" --output text --with-decryption --profile yologram)
export ADMIN_JWT_SECRET=$(aws ssm get-parameter --name "/yologram/service/yologram-api-v2_prod/yologram.auth.admin-jwt.secret" --query "Parameter.Value" --output text --with-decryption --profile yologram)
export CACHE_REDIS_HOST=$(aws ssm get-parameter --name "/yologram/service/yologram-api-v2_prod/cache.data.redis.host" --query "Parameter.Value" --output text --with-decryption --profile yologram)
uv run uvicorn app.main:app --reload --port 5002
