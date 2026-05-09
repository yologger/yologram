import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http'
import { resourceFromAttributes } from '@opentelemetry/resources'
import { NodeSDK } from '@opentelemetry/sdk-node'

const globalForOtel = globalThis as typeof globalThis & {
  __yologramWebV2OtelSdk?: NodeSDK
}

function getEnvironmentName() {
  return (
    process.env.APP_ENV ??
    process.env.NEXT_PUBLIC_APP_ENV ??
    process.env.NODE_ENV ??
    'development'
  )
}

function getServiceInstanceId() {
  return (
    process.env.HOSTNAME ??
    process.env.ECS_CONTAINER_METADATA_URI_V4 ??
    process.pid.toString()
  )
}

function createSdk() {
  return new NodeSDK({
    traceExporter: new OTLPTraceExporter(),
    resource: resourceFromAttributes({
      'service.name': process.env.OTEL_SERVICE_NAME ?? 'yologram-web-v2',
      'service.namespace': 'yologram',
      'deployment.environment.name': getEnvironmentName(),
      'service.instance.id': getServiceInstanceId(),
    }),
  })
}

function registerShutdown(sdk: NodeSDK) {
  const shutdown = async () => {
    await sdk.shutdown().catch(() => undefined)
  }

  process.once('SIGTERM', shutdown)
  process.once('SIGINT', shutdown)
}

const endpoint =
  process.env.OTEL_EXPORTER_OTLP_TRACES_ENDPOINT ??
  process.env.OTEL_EXPORTER_OTLP_ENDPOINT

if (endpoint && !globalForOtel.__yologramWebV2OtelSdk) {
  const sdk = createSdk()
  sdk.start()
  registerShutdown(sdk)
  globalForOtel.__yologramWebV2OtelSdk = sdk
}
