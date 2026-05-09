import logging
import os

from opentelemetry._logs import set_logger_provider
from opentelemetry.exporter.otlp.proto.http._log_exporter import OTLPLogExporter
from opentelemetry.sdk._logs import LoggerProvider, LoggingHandler
from opentelemetry.sdk._logs.export import BatchLogRecordProcessor
from opentelemetry.sdk.resources import Resource


def setup_logging():
    root_logger = logging.getLogger()
    root_logger.setLevel(logging.INFO)

    console_handler = logging.StreamHandler()
    console_handler.setFormatter(logging.Formatter("%(asctime)s %(levelname)s %(name)s - %(message)s"))
    root_logger.addHandler(console_handler)

    endpoint = os.environ.get("OTEL_EXPORTER_OTLP_ENDPOINT")
    if endpoint:
        resource = Resource.create({
            "service.name": os.environ.get("OTEL_SERVICE_NAME", "yologram-api-v2"),
            "deployment.environment.name": os.environ.get("APP_PROFILE", "default"),
            "service.namespace": "yologram",
        })

        logger_provider = LoggerProvider(resource=resource)
        logger_provider.add_log_record_processor(
            BatchLogRecordProcessor(OTLPLogExporter())
        )
        set_logger_provider(logger_provider)

        otel_handler = LoggingHandler(logger_provider=logger_provider)
        root_logger.addHandler(otel_handler)
