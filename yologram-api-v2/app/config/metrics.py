import os
import socket

from opentelemetry import metrics
from opentelemetry.exporter.otlp.proto.http.metric_exporter import OTLPMetricExporter
from opentelemetry.sdk.metrics import MeterProvider
from opentelemetry.sdk.metrics.export import PeriodicExportingMetricReader
from opentelemetry.sdk.resources import Resource

from opentelemetry.instrumentation.system_metrics import SystemMetricsInstrumentor


def setup_metrics():
    endpoint = os.environ.get("OTEL_EXPORTER_OTLP_ENDPOINT")
    if not endpoint:
        return

    resource = Resource.create({
        "service.name": os.environ.get("OTEL_SERVICE_NAME", "yologram-api-v2"),
        "deployment.environment.name": os.environ.get("APP_PROFILE", "default"),
        "service.instance.id": socket.gethostname(),
        "service.namespace": "yologram",
    })

    exporter = OTLPMetricExporter()

    reader = PeriodicExportingMetricReader(exporter, export_interval_millis=60000)
    provider = MeterProvider(resource=resource, metric_readers=[reader])
    metrics.set_meter_provider(provider)

    SystemMetricsInstrumentor().instrument()
