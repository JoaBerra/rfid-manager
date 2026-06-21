FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY server.py .

ENV DASHBOARD_URL=http://dashboard:8000
ENV MQTT_BROKER=mosquitto
ENV MQTT_PORT=1883

ENTRYPOINT ["python", "server.py"]
