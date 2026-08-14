# 검색 인덱싱 큐 — api-v1·v2가 인덱싱 작업을 넣고 worker가 꺼내 OpenSearch에 bulk 인덱싱한다.
#
# 레거시(yologram-legacy)는 워커가 없어 api가 produce·consume을 겸했지만, 우리는 worker가 있으므로
# 발행(api)과 소비(worker)를 분리한다. 조회수 파이프라인이 Kinesis인 것과 달리 여기서 SQS를 쓰는 이유는
# 성격이 다르기 때문이다 — 조회 이벤트는 초당 다건의 순서 있는 스트림이고,
# 인덱싱 작업은 "이 범위를 인덱싱하라"는 저빈도 작업 지시라 큐(재시도·DLQ·가시성 타임아웃)가 맞다.
#
# 표준 큐(FIFO 아님)를 쓴다: 인덱싱은 멱등이라(같은 문서를 다시 넣으면 덮어씀) 중복 전달과
# 순서 뒤바뀜이 문제되지 않는다. FIFO는 처리량 제한과 비용이 붙는다.
#
# 메시지 형식 — 단건도 from == to로 보내 범위 인덱싱 한 경로로 처리한다.
# 경로를 나누지 않는 이유: 레거시는 단건을 SQS 없이 동기 처리해 경로가 둘로 갈렸고,
# 그 결과 단건 경로에만 문서 변환 누락 버그가 남아 있었다(문서로 변환해두고 엔티티를 인덱싱).
#   {"target":"TECH_POST","from":1,"to":20}     범위·풀 인덱싱(20건 단위로 쪼개 발행)
#   {"target":"TECH_POST","from":1200,"to":1200} 단건(CRUD 트리거)
# 대상이 늘어도(TECH_NEWS·INVEST_POST…) target 필드로 흡수하고 큐는 하나로 유지한다 —
# 큐 개수는 과금 요소가 아니지만 대상마다 큐·DLQ를 두면 관리 비용만 늘고 격리 이득은 작다.
# 대량 배치가 실시간 단건을 막는 head-of-line이 실제 문제가 되면 그때 성격별로 큐를 나눈다.
resource "aws_sqs_queue" "search_indexing_prod" {
  name = "yologram-search-indexing-prod"

  # 워커가 한 메시지(범위 인덱싱)를 처리하는 데 걸리는 시간보다 넉넉해야 한다 —
  # 짧으면 처리 중인 메시지가 다시 보이면서 중복 인덱싱이 발생한다
  visibility_timeout_seconds = 300

  # 메시지 보관 4일(기본 4일) — 워커가 내려가 있어도 이 기간 안에 복구하면 작업이 유실되지 않는다
  message_retention_seconds = 345600

  # long polling — 빈 응답 왕복을 줄여 ReceiveMessage 호출 수(=요금)를 낮춘다
  receive_wait_time_seconds = 20

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.search_indexing_dlq_prod.arn
    # 3회 실패하면 DLQ로 보낸다. 레거시에는 DLQ가 없어 포이즌 메시지가 무한 재시도됐다
    maxReceiveCount = 3
  })

  tags = {
    Name = "yologram-search-indexing-prod"
  }
}

# 실패 메시지 격리용. 여기 쌓이면 인덱싱 로직·문서 매핑에 문제가 있다는 신호다.
# 보관 14일(최대) — 원인 파악과 재처리 판단에 시간이 필요하다
resource "aws_sqs_queue" "search_indexing_dlq_prod" {
  name                      = "yologram-search-indexing-dlq-prod"
  message_retention_seconds = 1209600

  tags = {
    Name = "yologram-search-indexing-dlq-prod"
  }
}
