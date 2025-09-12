AIDestinyMaster - models.zip layout (place in app/src/main/assets/ as models.zip)

Expected ZIP structure (all files inside the ZIP):

models/
  tinyllama-q8.onnx                # main ONNX model (decoder-only)
  tinyllama-q8.onnx.sha256         # SHA-256 of the .onnx file (hex lowercase)
  tokenizer/
    tokenizer.json                 # Hugging Face-style tokenizer data (if applicable)
    vocab.json                     # or sentencepiece.model depending on tokenizer type
    merges.txt                     # optional (BPE)

Notes:
- The app unzips assets/models.zip into files/models/ on first launch.
- If tinyllama-q8.onnx.sha256 is present, the installer validates checksum automatically.
- You can change file names, but keep the .sha256 name as <onnx-name>.onnx.sha256 to enable auto-verify.
- This README is informational and not used by code.
