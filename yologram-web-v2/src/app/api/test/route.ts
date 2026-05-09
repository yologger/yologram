import { NextResponse } from 'next/server'
import { logInfo } from '@/lib/logger'

export async function GET() {
  logInfo('GET /api/test')
  return NextResponse.json({ message: 'ok' })
}
