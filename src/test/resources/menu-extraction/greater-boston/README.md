# Greater Boston menu extraction fixtures

This small fixed set is for repeatable parser evaluation, not for serving menu data to users.

- Sources are official restaurant menu pages in Greater Boston.
- Only a few factual item names and prices are retained.
- `capturedDate` records when the facts were checked.
- Prices may change and must not be treated as current availability.
- The text is normalized to represent OCR output; it is not a full copy of any menu.

The automated accuracy gate measures exact section, item-name, and price matches. Extra output is
also penalized, and each metric must remain at or above 90%.
