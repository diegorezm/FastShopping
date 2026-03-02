import requests
import csv
import time
from datetime import datetime

BASE_URL = "http://localhost:8080/actuator/metrics"
INTERVAL_SECONDS = 2
OUTPUT_FILE = "metrics_output.csv"


def get_metric(metric_name):
    try:
        r = requests.get(f"{BASE_URL}/{metric_name}")
        data = r.json()
        return data["measurements"][0]["value"]
    except Exception:
        return None


def collect_metrics():
    metrics = {
        "hikaricp.connections.active": get_metric("hikaricp.connections.active"),
        "hikaricp.connections.pending": get_metric("hikaricp.connections.pending"),
        "tomcat.threads.busy": get_metric("tomcat.threads.busy"),
        "tomcat.threads.config.max": get_metric("tomcat.threads.config.max"),
        "process.cpu.usage": get_metric("process.cpu.usage"),
        "system.cpu.usage": get_metric("system.cpu.usage"),
    }
    return metrics


def main():
    with open(OUTPUT_FILE, mode="w", newline="") as file:
        writer = csv.writer(file)
        writer.writerow([
            "timestamp",
            "hikari_active",
            "hikari_pending",
            "tomcat_busy",
            "tomcat_max",
            "process_cpu",
            "system_cpu"
        ])

        print("Starting monitoring... Press Ctrl+C to stop.")
        try:
            while True:
                now = datetime.now().isoformat()
                metrics = collect_metrics()

                writer.writerow([
                    now,
                    metrics["hikaricp.connections.active"],
                    metrics["hikaricp.connections.pending"],
                    metrics["tomcat.threads.busy"],
                    metrics["tomcat.threads.config.max"],
                    metrics["process.cpu.usage"],
                    metrics["system.cpu.usage"]
                ])

                file.flush()
                time.sleep(INTERVAL_SECONDS)

        except KeyboardInterrupt:
            print("Monitoring stopped.")


if __name__ == "__main__":
    main()