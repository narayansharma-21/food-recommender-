#!/usr/bin/env python3

import json
import sys
from pathlib import Path


def escape(value: str) -> str:
    return value.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def main() -> None:
    report = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    for run in report.get("runs", []):
        for result in run.get("results", []):
            rule = result.get("ruleId", "Container vulnerability")
            message = result.get("message", {}).get("text", "Critical vulnerability found")
            print(f"::error title={escape(rule)}::{escape(message)}")


if __name__ == "__main__":
    main()
