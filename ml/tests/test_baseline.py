import unittest

from ml.baseline import evaluate


class BaselineTest(unittest.TestCase):
    def test_evaluates_chronological_and_cold_start_segments(self) -> None:
        examples = [
            self.example(1, "user-a", "dish-a", 1, 5),
            self.example(2, "user-a", "dish-b", 3, 5),
            self.example(3, "user-b", "dish-a", 5, 5),
            self.example(4, "user-b", "dish-b", 3, 5),
            self.example(5, "user-new", "dish-new", 5, 1),
            self.example(6, "user-a", "dish-a", 1, 5),
        ]

        report = evaluate(examples, train_fraction=0.67)

        self.assertEqual(report["prediction"], 3.0)
        self.assertEqual(report["segments"]["overall"], {"count": 2, "mae": 2.0, "rmse": 2.0})
        self.assertEqual(report["segments"]["newUser"]["count"], 1)
        self.assertEqual(report["segments"]["newDish"]["count"], 1)
        self.assertEqual(report["segments"]["sparseProfile"]["count"], 1)

    def test_requires_enough_examples(self) -> None:
        with self.assertRaisesRegex(ValueError, "at least two"):
            evaluate([])

    def example(
        self,
        sequence: int,
        user_id: str,
        dish_id: str,
        score: int,
        evidence_count: int,
    ) -> dict:
        return {
            "ratedAt": f"2026-01-{sequence:02d}T00:00:00Z",
            "ratingId": f"rating-{sequence}",
            "userId": user_id,
            "dishConceptId": dish_id,
            "menuItemId": f"item-{sequence}",
            "ratingScore": score,
            "tasteEvidenceCount": evidence_count,
        }


if __name__ == "__main__":
    unittest.main()
