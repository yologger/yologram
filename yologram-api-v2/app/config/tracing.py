import os
import socket

from opentelemetry import trace
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.sdk.resources import Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor


def setup_tracing(app):
    endpoint = os.environ.get("OTEL_EXPORTER_OTLP_ENDPOINT")
    if not endpoint:
        return

    resource = Resource.create({
        "service.name": os.environ.get("OTEL_SERVICE_NAME", "yologram-api-v2"),
        "deployment.environment.name": os.environ.get("APP_PROFILE", "default"),
        "service.instance.id": socket.gethostname(),
        "service.namespace": "yologram",
    })

    exporter = OTLPSpanExporter()

    provider = TracerProvider(resource=resource)
    provider.add_span_processor(BatchSpanProcessor(exporter))
    trace.set_tracer_provider(provider)

    FastAPIInstrumentor.instrument_app(app)
