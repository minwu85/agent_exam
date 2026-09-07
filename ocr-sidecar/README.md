# OCR sidecar

Small Python/FastAPI service that extracts text from scanned or handwritten note images
(via [Tesseract](https://github.com/tesseract-ocr/tesseract)) for the main Java backend to
call — see [DEVLOG.md](../DEVLOG.md#stage-10--ocr--python-sidecar) for why this is a
separate Python service instead of a Java library.

## Prerequisites

- Python 3.10+
- Tesseract OCR installed and on your PATH (or at the default
  `C:\Program Files\Tesseract-OCR\tesseract.exe` on Windows, which `main.py` falls back to
  automatically). On Windows: `winget install --id UB-Mannheim.TesseractOCR -e`

## Run

```bash
pip install -r requirements.txt
uvicorn main:app --port 8000
```

Then `GET http://localhost:8000/health` should return `{"status": "ok", ...}`.

## API

- `GET /health` — confirms Tesseract is actually reachable.
- `POST /ocr` — multipart `file` (an image: jpg/png/etc). Returns
  `{"text": "...", "engine": "tesseract", "filename": "..."}`.

The Java backend's `app.ocr-sidecar-url` property (default `http://localhost:8000`) points
here — see `OcrClient` in the backend.
