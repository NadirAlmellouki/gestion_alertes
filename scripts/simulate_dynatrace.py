#!/usr/bin/env python3
"""Simulate a Dynatrace webhook call against the local AlertOps ingestion endpoint."""

import argparse
import json
import sys
from pathlib import Path

try:
    import requests
except ImportError:
    print("Install requests: pip install requests", file=sys.stderr)
    sys.exit(1)


def main() -> int:
    parser = argparse.ArgumentParser(description="Simulate Dynatrace alert ingestion")
    parser.add_argument(
        "--fixture",
        default="src/test/resources/fixtures/dynatrace-problem.json",
        help="Path to JSON fixture file",
    )
    parser.add_argument(
        "--url",
        default="http://localhost:8080/api/v1/ingestion/dynatrace",
        help="Ingestion endpoint URL",
    )
    parser.add_argument(
        "--token",
        default="dev-ingestion-token",
        help="X-Ingestion-Token header value",
    )
    args = parser.parse_args()

    fixture_path = Path(args.fixture)
    if not fixture_path.exists():
        print(f"Fixture not found: {fixture_path}", file=sys.stderr)
        return 1

    payload = json.loads(fixture_path.read_text(encoding="utf-8"))
    response = requests.post(
        args.url,
        json=payload,
        headers={
            "Content-Type": "application/json",
            "X-Ingestion-Token": args.token,
        },
        timeout=30,
    )

    print(f"Status: {response.status_code}")
    print(response.text)
    return 0 if response.ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
