"""
CareHub 3.0 — AI Microservice
Hosts REST endpoints for:
1. LSTM — Long Short-Term Memory Medicine Demand Forecasting
2. CNN — Convolutional Neural Network Prescription Image Analysis
3. NLP — Natural Language Processing Clinical Entity Extraction
"""

import os
import sys
import json
import datetime
import io

# Ensure ai_service root is in sys.path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from flask import Flask, request, jsonify
from flask_cors import CORS
from PIL import Image

import config
from preprocessing.image_preprocessing import ImagePreprocessor
from services.lstm_service import LstmService
from services.cnn_service import CnnService
from services.ocr_service import OcrService
from services.nlp_service import NlpService

app = Flask(__name__)
CORS(app)

# Initialize AI Services once at startup (models loaded in memory)
print("=" * 65)
print("CareHub 3.0: Initializing AI Microservice Engine...")
lstm_service = LstmService()
cnn_service = CnnService()
ocr_service = OcrService()
nlp_service = NlpService()
image_preprocessor = ImagePreprocessor(target_size=config.CNN_IMAGE_SIZE)
print("CareHub 3.0: All AI Services Loaded Successfully!")
print("=" * 65)

@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "UP",
        "service": "CareHub 3.0 AI Microservice (LSTM + CNN + NLP)",
        "timestamp": datetime.datetime.now().isoformat(),
        "models": {
            "lstm_ready": lstm_service.is_ready,
            "cnn_ready": cnn_service.is_ready,
            "nlp_ready": True
        }
    })

@app.route("/ai/models/info", methods=["GET"])
def get_models_info():
    """Returns evaluation metrics, architecture details, and status for all 3 AI models."""
    lstm_metrics = {}
    if os.path.exists(config.LSTM_METRICS_PATH):
        try:
            with open(config.LSTM_METRICS_PATH, "r", encoding="utf-8") as f:
                lstm_metrics = json.load(f)
        except Exception:
            pass

    cnn_metrics = {}
    if os.path.exists(config.CNN_METRICS_PATH):
        try:
            with open(config.CNN_METRICS_PATH, "r", encoding="utf-8") as f:
                cnn_metrics = json.load(f)
        except Exception:
            pass

    nlp_metrics = {}
    if os.path.exists(config.NLP_METRICS_PATH):
        try:
            with open(config.NLP_METRICS_PATH, "r", encoding="utf-8") as f:
                nlp_metrics = json.load(f)
        except Exception:
            pass

    return jsonify({
        "timestamp": datetime.datetime.now().isoformat(),
        "lstm": {
            "modelName": "LSTM-DemandNet-v1",
            "task": "Medicine Demand Time-Series Forecasting",
            "status": "ACTIVE" if lstm_service.is_ready else "UNAVAILABLE",
            "architecture": "Input(14,1) -> LSTM(64) -> Dropout(0.2) -> LSTM(32) -> Dropout(0.2) -> Dense(16) -> Dense(2)",
            "metrics": lstm_metrics.get("evaluation_metrics", {})
        },
        "cnn": {
            "modelName": "CNN-PrescriptionNet-v1",
            "task": "Prescription Image Quality & Document Classification",
            "status": "ACTIVE" if cnn_service.is_ready else "UNAVAILABLE",
            "architecture": "Input(128,128,3) -> Conv2D(32) -> MaxPool -> Conv2D(64) -> MaxPool -> Conv2D(64) -> MaxPool -> Flatten -> Dense(64) -> Dropout(0.3) -> Dense(4)",
            "metrics": cnn_metrics.get("evaluation_metrics", {})
        },
        "nlp": {
            "modelName": "NLP-ClinicalRegexNER-v1",
            "task": "Prescription Clinical Text Understanding & Entity Extraction",
            "status": "ACTIVE",
            "pipeline": "Cleaning -> Tokenization -> Normalization -> Catalog Fuzzy NER -> Dosage/Frequency/Duration Parsing",
            "metrics": nlp_metrics.get("evaluation_metrics", {})
        }
    })

@app.route("/predict-demand", methods=["POST"])
@app.route("/ai/demand/predict", methods=["POST"])
def predict_demand():
    """
    Module 1: LSTM Time-Series Demand Forecasting.
    Receives current stock and historical dispensing series.
    Returns 7-day forecast, 30-day forecast, stockout risk, and days left.
    """
    data = request.get_json() or {}
    medicine_id = data.get("medicineId", 0)
    medicine_name = data.get("medicineName", "Generic Medicine")
    current_stock = int(data.get("currentStock", 0))

    # Accept either full daily usage array or 30d/60d aggregations
    history = data.get("usageHistory", [])
    if not history:
        usage_30d = int(data.get("usage30d", 0))
        usage_60d = int(data.get("usage60d", 0))
        if usage_30d > 0:
            # Reconstruct daily approximation for 14-day sequence window
            daily_approx = usage_30d / 30.0
            history = [max(0, int(round(daily_approx + (i % 3 - 1)))) for i in range(14)]
        elif usage_60d > 0:
            daily_approx = usage_60d / 60.0
            history = [max(0, int(round(daily_approx))) for i in range(14)]
        else:
            history = [0] * 14

    result = lstm_service.predict_demand(
        medicine_id=medicine_id,
        medicine_name=medicine_name,
        current_stock=current_stock,
        usage_history=history
    )

    # Maintain backward compatibility with previous field names while adding new LSTM fields
    result["predictedNextMonthDemand"] = result["predicted30DayDemand"]
    result["historicalUsage30d"] = sum(history) if history else 0
    result["trend"] = "Surging" if result["predicted30DayDemand"] > current_stock else "Stable"

    return jsonify(result)

@app.route("/ocr-prescription", methods=["POST"])
@app.route("/ai/prescription/analyze", methods=["POST"])
def analyze_prescription():
    """
    Module 4: Unified Pipeline combining CNN + OCR + NLP.
    Image -> CNN Quality & Classification -> OCR Text Extraction -> NLP Entity Extraction.
    """
    filename = "prescription_scan.png"
    pil_image = None

    if "file" in request.files:
        f = request.files["file"]
        filename = f.filename or "prescription_scan.png"
        try:
            image_bytes = f.read()
            pil_image = image_preprocessor.load_image_from_bytes(image_bytes)
        except Exception as e:
            return jsonify({
                "success": False,
                "message": f"Invalid image format: {e}"
            }), 400
    else:
        # Generate clean default clinical prescription canvas
        pil_image = Image.new("RGB", (320, 240), color=(255, 255, 255))

    # 1. CNN Image Analysis & Quality Assessment
    cnn_result = cnn_service.analyze_image(pil_image)

    # 2. Text Extraction (OCR)
    raw_text = ocr_service.extract_text(pil_image, filename=filename, cnn_result=cnn_result)

    # 3. NLP Clinical Entity Extraction
    nlp_result = nlp_service.parse_prescription_text(raw_text)

    return jsonify({
        "success": True,
        "fileName": filename,
        "cnn": cnn_result,
        "rawText": raw_text,
        "nlp": nlp_result,
        "detectedMedicines": nlp_result["detectedMedicines"],
        "analyzedAt": datetime.datetime.now().isoformat()
    })

@app.route("/ai/nlp/parse", methods=["POST"])
def parse_text_nlp():
    """Module 3: Standalone NLP endpoint for clinical prescription text parsing."""
    data = request.get_json() or {}
    text = data.get("text", "")
    catalog = data.get("catalogMedicines", [])
    result = nlp_service.parse_prescription_text(text, catalog_medicines=catalog)
    return jsonify(result)

if __name__ == "__main__":
    print(f"CareHub 3.0 AI Microservice running on http://{config.HOST}:{config.PORT}")
    app.run(host=config.HOST, port=config.PORT, debug=config.DEBUG)
