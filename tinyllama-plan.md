# TinyLlama ONNX 導入計畫

## 下載與版本
- 來源：Hugging Face TinyLlama/TinyLlama-1.1B-Chat-v1.0。
- 權重：pytorch_model.bin 約 2.2 GB。
- Tokenizer：	okenizer.model (sentencepiece) 與 	okenizer.json。

## 轉換與量化流程
1. 建立 Python 3.10 環境；安裝套件：
   `ash
   pip install transformers==4.40.0 onnx onnxruntime onnxruntime-tools optimum accelerate
   `
2. 使用 optimum 匯出 decoder-only ONNX：
   `python
   from transformers import AutoTokenizer, AutoModelForCausalLM
   from optimum.onnxruntime import ORTModelForCausalLM

   model_id = "TinyLlama/TinyLlama-1.1B-Chat-v1.0"
   tokenizer = AutoTokenizer.from_pretrained(model_id)
   ort_model = ORTModelForCausalLM.from_pretrained(
       model_id,
       export=True,
       use_auth_token=True,
       file_name="tinyllama-chat.onnx",
       provider="CPUExecutionProvider",
       do_constant_folding=True
   )
   tokenizer.save_pretrained("onnx-export/tokenizer")
   `
3. 進行 8-bit 量化：
   `ash
   python -m onnxruntime.transformers.optimizer_cli \
     --input tinyllama-chat.onnx \
     --output tinyllama-chat-8bit.onnx \
     --use_gpu \
     --opt_level 1 \
     --save_as_external_data
   `
   若無 GPU，可改用 onnxruntime-tools 的 quantize_dynamic：
   `python
   from onnxruntime.quantization import quantize_dynamic, QuantType
   quantize_dynamic(
       model_input="tinyllama-chat.onnx",
       model_output="tinyllama-chat-8bit.onnx",
       weight_type=QuantType.QUInt8,
       optimize_model=True
   )
   `
4. 驗證：在 Python 中載入 onnxruntime.InferenceSession，測試短提示確保輸出與原模型相符（允許少量浮點誤差）。

## 導入專案
- 檔案放置：
  - core/ai/src/main/assets/model/tinyllama-chat-8bit.onnx
  - core/ai/src/main/assets/model/tokenizer.json / 	okenizer.model
- 於 OnnxLlamaSession 新增權重名稱常數與 SHA-256；提供工具腳本 scripts/hash_model.ps1 計算雜湊。
- 更新 	ask.md 對應項目並記錄權重來源 SHA-256。

---
此流程需具 Hugging Face 帳號以存取模型；線上作業需約 5 GB 暫存空間。## �ثe�ɮ׵������� (2025-09-17)

- �w���� 8-bit ONNX �v�����A�ɮ׳]�Y�� core/ai/src/main/assets/model/tinyllama-chat-8bit.onnx
- tokenizer �����ɮ׳]�Y�� core/ai/src/main/assets/model/

| �ɮ� | �d�� | SHA-256 |
| --- | --- | --- |
| tinyllama-chat-8bit.onnx | core/ai/src/main/assets/model/tinyllama-chat-8bit.onnx | 62B6DFA60E3651F71D70AD17C39FAF0D84B967B3F352D6457BF2DD4A7EBA5AED |
| tokenizer.json | core/ai/src/main/assets/model/tokenizer.json | BF467C9E0F536BDA271283C6EF85EB1A943E3196B621C8A912D64953B205DF83 |
| tokenizer.model | core/ai/src/main/assets/model/tokenizer.model | 9E556AFD44213B6BD1BE2B850EBBBD98F5481437A8021AFAF58EE7FB1818D347 |
