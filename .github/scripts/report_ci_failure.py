#!/usr/bin/env python3

import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def escape(value: str) -> str:
    return value.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def failed_tests(results_directory: Path) -> list[str]:
    failures = []
    for report in sorted(results_directory.glob("TEST-*.xml")):
        root = ET.parse(report).getroot()
        for case in root.findall("testcase"):
            problem = case.find("failure")
            if problem is None:
                problem = case.find("error")
            if problem is None:
                continue
            name = f"{case.get('classname', '')}.{case.get('name', '')}".strip(".")
            detail = (problem.get("message") or problem.text or "Test failed").strip().splitlines()[0]
            failures.append(f"{name}: {detail}")
    return failures


def gradle_errors(log_file: Path) -> list[str]:
    if not log_file.is_file():
        return ["Gradle failed before producing a log or test report."]
    lines = [line.strip() for line in log_file.read_text(encoding="utf-8").splitlines()]
    markers = ("> Task ", "FAILURE:", "* What went wrong:", "Execution failed for task", "Caused by:")
    matches = [line for line in lines if line and line.startswith(markers)]
    return matches[-8:] or ["Gradle failed; open the Java build step for details."]


def main() -> None:
    log_file = Path(sys.argv[1])
    results_directory = Path(sys.argv[2])
    messages = failed_tests(results_directory) or gradle_errors(log_file)
    for message in messages[:20]:
        print(f"::error title=Java CI failure::{escape(message)}")


if __name__ == "__main__":
    main()
