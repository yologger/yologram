#!/bin/bash
export APP_PROFILE=prod

JDBC_URL=$(aws ssm get-parameter --name "/yologram/service/yologram-api-v2_prod/database.main.writer.datasource.url" --query "Parameter.Value" --output text --with-decryption --profile yologram)
export DB_USERNAME=$(aws ssm get-parameter --name "/yologram/service/yologram-api-v2_prod/database.main.writer.datasource.username" --query "Parameter.Value" --output text --with-decryption --profile yologram)
export DB_PASSWORD=$(aws ssm get-parameter --name "/yologram/service/yologram-api-v2_prod/database.main.writer.datasource.password" --query "Parameter.Value" --output text --with-decryption --profile yologram)

# JDBC URL → SQLAlchemy URL 변환 (jdbc:mysql://host:port/db?params → mysql+pymysql://user:pass@host:port/db)
HOST_PORT_DB=$(echo "$JDBC_URL" | sed 's|jdbc:mysql://||' | sed 's|?.*||')
export DB_URL="mysql+pymysql://${DB_USERNAME}:${DB_PASSWORD}@${HOST_PORT_DB}"

uv run uvicorn app.main:app --reload --port 5002
