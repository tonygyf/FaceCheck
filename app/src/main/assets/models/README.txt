Place your TFLite models here:

- yunet_fp16_multi.tflite   // YuNet face detector (fp16, multi-face, input 320x320)
- yunet_fp32_single.tflite  // YuNet face detector (fp32, single-face, input 320x320)
- mobilefacenet.tflite      // MobileFaceNet embedder (outputs 128-d embeddings)

Notes:
- File names must match exactly as above.
- Attendance (multi-face) uses `yunet_fp16_multi.tflite` by default.
- If you need single-face precision detection via YuNet, use `yunet_fp32_single.tflite`.
- If models are missing, YuNet/MobileFaceNet flow will log an error and skip.