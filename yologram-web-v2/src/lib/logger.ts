import { logs, SeverityNumber } from '@opentelemetry/api-logs'

const logger = logs.getLogger('yologram-web-v2')

export function logInfo(message: string, attributes?: Record<string, string>) {
  logger.emit({
    severityNumber: SeverityNumber.INFO,
    severityText: 'INFO',
    body: message,
    attributes,
  })
}

export function logError(message: string, attributes?: Record<string, string>) {
  logger.emit({
    severityNumber: SeverityNumber.ERROR,
    severityText: 'ERROR',
    body: message,
    attributes,
  })
}
