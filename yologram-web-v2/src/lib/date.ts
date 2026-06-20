/**
 * ISO 절대 시각을 상대 시간 문자열로 변환.
 * 백엔드는 절대 시각(ISO)만 내려주고, 표시용 상대시간은 프론트에서 계산한다.
 */
export function formatRelativeTime(iso: string, now: Date = new Date()): string {
  const date = new Date(iso)
  const diffMs = now.getTime() - date.getTime()
  const diffSec = Math.floor(diffMs / 1000)

  if (diffSec < 0) return '방금 전'
  if (diffSec < 60) return '방금 전'

  const diffMin = Math.floor(diffSec / 60)
  if (diffMin < 60) return `${diffMin}분 전`

  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}시간 전`

  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 7) return `${diffDay}일 전`

  // 일주일 이상은 날짜로 표기 (YYYY.MM.DD)
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `${yyyy}.${mm}.${dd}`
}
