#!/usr/bin/env python3
"""Evaluate a reproducible non-personalized rating baseline."""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
from typing import Any

SCHEMA_VERSION = "recommendation-training-example-v1"


def load_examples(path: Path) -> list[dict[str, Any]]:
    examples: list[dict[str, Any]] = []
    with path.open(encoding="utf-8") as source:
        for line_number, line in enumerate(source, start=1):
            if not line.strip():
                continue
            example = json.loads(line)
            if example.get("schemaVersion") != SCHEMA_VERSION:
                raise ValueError(f"line {line_number} has an unsupported schema version")
            score = example.get("ratingScore")
            if not isinstance(score, int) or not 1 <= score <= 5:
                raise ValueError(f"line {line_number} has an invalid rating score")
            examples.append(example)
    return examples


def evaluate(examples: list[dict[str, Any]], train_fraction: float = 0.8) -> dict[str, Any]:
    if len(examples) < 2:
        raise ValueError("at least two training examples are required")
    if not 0 < train_fraction < 1:
        raise ValueError("train fraction must be between zero and one")
    ordered = sorted(examples, key=lambda row: (row["ratedAt"], row["ratingId"]))
    split_index = max(1, min(len(ordered) - 1, int(len(ordered) * train_fraction)))
    training = ordered[:split_index]
    testing = ordered[split_index:]
    prediction = sum(row["ratingScore"] for row in training) / len(training)
    training_users = {row["userId"] for row in training}
    training_dishes = {_dish_key(row) for row in training}
    segments = {
        "overall": testing,
        "newUser": [row for row in testing if row["userId"] not in training_users],
        "newDish": [row for row in testing if _dish_key(row) not in training_dishes],
        "sparseProfile": [row for row in testing if row["tasteEvidenceCount"] < 3],
    }
    return {
        "baselineVersion": "global-mean-v1",
        "schemaVersion": SCHEMA_VERSION,
        "trainCount": len(training),
        "testCount": len(testing),
        "prediction": round(prediction, 6),
        "segments": {name: _metrics(rows, prediction) for name, rows in segments.items()},
    }


def _dish_key(example: dict[str, Any]) -> str:
    return example.get("dishConceptId") or example["menuItemId"]


def _metrics(examples: list[dict[str, Any]], prediction: float) -> dict[str, Any]:
    if not examples:
        return {"count": 0, "mae": None, "rmse": None}
    errors = [prediction - row["ratingScore"] for row in examples]
    return {
        "count": len(errors),
        "mae": round(sum(abs(error) for error in errors) / len(errors), 6),
        "rmse": round(math.sqrt(sum(error * error for error in errors) / len(errors)), 6),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("dataset", type=Path, help="Versioned JSONL training dataset")
    parser.add_argument("--output", type=Path, required=True, help="Metrics JSON destination")
    parser.add_argument("--train-fraction", type=float, default=0.8)
    args = parser.parse_args()
    report = evaluate(load_examples(args.dataset), args.train_fraction)
    args.output.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
