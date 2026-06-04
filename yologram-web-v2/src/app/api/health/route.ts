import { logInfo } from '@/lib/logger'

export async function GET() {
  logInfo('GET /api/health')
  return Response.json({ status: 'ok' })
}
