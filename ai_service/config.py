"""
CareHub 3.0 AI Service Configuration
Defines file paths, model locations, feature parameters, and server settings.
"""

import os
import tempfile

# Prevent Matplotlib / TF cache permission issues on Windows
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"
temp_cache = os.path.join(tempfile.gettempdir(), "carehub_ai_cache")
os.makedirs(temp_cache, exist_ok=True)
os.environ["MPLCONFIGDIR"] = temp_cache

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.join(BASE_DIR, "models")
DATA_DIR = os.path.join(BASE_DIR, "data")

# LSTM Paths & Hyperparameters
LSTM_DIR = os.path.join(MODELS_DIR, "lstm")
LSTM_MODEL_PATH = os.path.join(LSTM_DIR, "lstm_demand_model.keras")
LSTM_SCALER_PATH = os.path.join(LSTM_DIR, "scaler.joblib")
LSTM_METRICS_PATH = os.path.join(LSTM_DIR, "metrics.json")
LSTM_SEQUENCE_LENGTH = 14  # 14 days lookback sequence
LSTM_HORIZON_7D = 7        # 7-day forecast
LSTM_HORIZON_30D = 30      # 30-day forecast

# CNN Paths & Hyperparameters
CNN_DIR = os.path.join(MODELS_DIR, "cnn")
CNN_MODEL_PATH = os.path.join(CNN_DIR, "cnn_prescription_model.keras")
CNN_CLASSES_PATH = os.path.join(CNN_DIR, "classes.json")
CNN_METRICS_PATH = os.path.join(CNN_DIR, "metrics.json")
CNN_IMAGE_SIZE = (128, 128)
CNN_NUM_CLASSES = 4
CNN_CLASS_LABELS = [
    "VALID_PRESCRIPTION_PRINTED",
    "VALID_PRESCRIPTION_HANDWRITTEN",
    "POOR_QUALITY_DEGRADED",
    "NON_PRESCRIPTION_DOCUMENT"
]

# NLP Paths & Vocabulary
NLP_DIR = os.path.join(MODELS_DIR, "nlp")
NLP_VOCAB_PATH = os.path.join(NLP_DIR, "catalog_vocab.json")
NLP_METRICS_PATH = os.path.join(NLP_DIR, "metrics.json")

# Server Config
HOST = "127.0.0.1"
PORT = 5000
DEBUG = False
