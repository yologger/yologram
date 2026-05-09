import { NextRequest, NextResponse } from 'next/server'
import { logInfo, logError } from '@/lib/logger'

export async function GET(request: NextRequest) {
  const ip = request.headers.get('x-forwarded-for') ?? 'unknown'
  const userAgent = request.headers.get('user-agent') ?? 'unknown'

  logInfo('GET /api/test/echo', { ip, userAgent })

  return NextResponse.json({
    ip,
    userAgent,
    method: request.method,
    url: request.url,
  })
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    logInfo('POST /api/test/echo', { bodyKeys: Object.keys(body).join(',') })
    return NextResponse.json({ received: body })
  } catch {
    logError('POST /api/test/echo - invalid JSON')
    return NextResponse.json({ error: 'invalid JSON' }, { status: 400 })
  }
}
