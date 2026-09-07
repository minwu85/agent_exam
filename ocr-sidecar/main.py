"""
OCR sidecar (Stage 10): a small FastAPI service the Java backend calls to extract text
from scanned/handwritten notes - as opposed to Stage 1's text-native PDF path, which uses
Spring AI's PagePdfDocumentReader directly in Java with no OCR involved.

Python + pytesseract/Tesseract is used here rather than a Java OCR library because
Tesseract's Python bindings and surrounding tooling (Pillow for image handling) are more
mature and better documented than Java's OCR options - a narrow, well-scoped reason to add
a second language for one slice, not a reason to rewrite the app.

Run:
    pip install -r requirements.txt
    uvicorn main:app --port 8000

The Java backend calls POST /ocr with a multipart image file and expects
{"text": "...", "engine": "tesseract", "filename": "..."} back.
"""
import io
import os
import shutil

import pytesseract
from fastapi import FastAPI, File, HTTPException, UploadFile
from PIL import Image

app = FastAPI(title="Exam Agent OCR Sidecar")

# On Windows, tesseract.exe isn't necessarily on PATH for every process that launches
# uvicorn (a fresh shell may not have re-read the registry PATH update yet). Point
# pytesseract at the known install location if the bare command isn't resolvable, rather
# than failing every request with a cryptic TesseractNotFoundError.
_default_windows_path = r"C:\Program Files\Tesseract-OCR\tesseract.exe"
if shutil.which("tesseract") is None and os.path.exists(_default_windows_path):
    pytesseract.pytesseract.tesseract_cmd = _default_windows_path


@app.get("/health")
def health():
    """The Java backend can call this before offering OCR upload, so a missing Tesseract install fails fast and legibly instead of on every /ocr call."""
    try:
        version = str(pytesseract.get_tesseract_version())
    except Exception as exc:  # pytesseract raises if the binary truly can't be found/run
        raise HTTPException(status_code=503, detail=f"Tesseract not available: {exc}") from exc
    return {"status": "ok", "tesseractVersion": version}


@app.post("/ocr")
async def ocr(file: UploadFile = File(...)):
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail=f"Expected an image file, got content-type: {file.content_type}")

    contents = await file.read()
    try:
        image = Image.open(io.BytesIO(contents))
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Could not read image: {exc}") from exc

    text = pytesseract.image_to_string(image)
    return {"text": text, "engine": "tesseract", "filename": file.filename}
