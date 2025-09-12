# Models Packaging Guide (assets/models.zip)

Place a `models.zip` under `app/src/main/assets/` with the following structure inside the ZIP:

```
models/
  tinyllama-q8.onnx                # main ONNX model (decoder-only recommended)
  tinyllama-q8.onnx.sha256         # SHA-256 checksum (hex lowercase)
  tokenizer/
    tokenizer.json                 # HF tokenizer data (if applicable)
    vocab.json                     # or sentencepiece.model depending on tokenizer type
    merges.txt                     # optional (BPE)
```

## Installation at runtime
- On first app start, the installer extracts `assets/models.zip` to `files/models/`.
- If `<onnx>.onnx.sha256` exists next to the `.onnx`, checksum is auto-verified.
- Logs: see `Model install check: true/false` in Logcat with tag `AIDM`.

## File naming
- You may change the ONNX file name. To auto-verify, keep the `.sha256` filename as `<onnx-name>.onnx.sha256`.
- The tokenizer directory name can be changed, but ensure your engine is initialized with the correct path.

## Notes
- Do not commit any model weights to the repository.
- Use the provided `scripts/export_to_onnx.py` to export and write checksums.
- Target device runtime: ONNX Runtime Mobile (to be wired into `OnnxAiEngine`).

## Troubleshooting

1. Zip structure incorrect (not nested under `models/`)
   - Symptom: Installer runs but `files/models/` is empty or missing `.onnx`.
   - Fix: Ensure the ZIP root contains `models/` directory. Inside it put the `.onnx`, `.onnx.sha256`, and `tokenizer/` files.

2. Checksum mismatch (`.sha256` does not match)
   - Symptom: Logcat shows `Model install check: false` or the debug button shows "SHA 不一致".
   - Fix: Re-run `scripts/export_to_onnx.py` to regenerate the ONNX and `.sha256`. Confirm the `.sha256` is hex lowercase and matches the extracted `.onnx`.

3. Assets too large or missing at runtime
   - Symptom: App size too big, build timeouts, or `assets/models.zip` not found.
   - Fix: Keep only the minimal files in the ZIP. Verify the assets path is `app/src/main/assets/models.zip`. For debug-only tests, you can push the model to device `files/` manually via `adb`.

4. Wrong filesDir path when initializing the engine
   - Symptom: Engine cannot open model, session is null.
   - Fix: Use `File(context.filesDir, "models/tinyllama-q8.onnx")` and ensure installer has run (first app launch will unzip automatically).

5. Logs to check
   - Tag: `AIDM`, Message: `Model install check: true/false`
   - If false, confirm the ZIP structure, presence of `.onnx` and `.sha256`, and re-check file permissions.
