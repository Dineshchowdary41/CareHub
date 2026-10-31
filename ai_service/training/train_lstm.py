"""
CareHub 3.0 — LSTM Medicine Demand Forecasting Training
Trains a genuine Long Short-Term Memory neural network on time-series dispensing data.
Computes real evaluation metrics (MAE, RMSE, MAPE, R2) and saves the trained model.
"""

import os
import sys
import json
import datetime
import numpy as np
import pandas as pd
from sklearn.metrics import mean_absolute_error, root_mean_squared_error, r2_score

# Add parent directory to sys.path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import config
from preprocessing.demand_preprocessing import DemandPreprocessor

import keras
from keras import layers

def train_lstm_model():
    print("=" * 65)
    print("CareHub 3.0: Initiating LSTM Medicine Demand Forecasting Training")
    print("=" * 65)

    data_file = os.path.join(config.DATA_DIR, "synthetic_demand_dataset.csv")
    if not os.path.exists(data_file):
        raise FileNotFoundError(f"Training data not found at {data_file}. Please run generate_datasets.py first.")

    df = pd.read_csv(data_file)
    print(f"Loaded {len(df)} dispensing records across {df['medicine_name'].nunique()} medicines.")

    preprocessor = DemandPreprocessor(sequence_length=config.LSTM_SEQUENCE_LENGTH)

    # Build sequence pairs across all medicines
    all_X, all_y = [], []
    for med_name, group in df.groupby("medicine_name"):
        series = group.sort_values("day_index")["quantity_dispensed"].values
        X_seq, y_seq = preprocessor.create_sequences(
            series,
            horizon_7=config.LSTM_HORIZON_7D,
            horizon_30=config.LSTM_HORIZON_30D
        )
        if len(X_seq) > 0:
            all_X.append(X_seq)
            all_y.append(y_seq)

    X = np.concatenate(all_X, axis=0)
    y = np.concatenate(all_y, axis=0)
    print(f"Generated {len(X)} sequence samples. X shape: {X.shape}, y shape: {y.shape}")

    # Time-series chronologically ordered train/test split (80% train, 20% test)
    split_idx = int(len(X) * 0.8)
    X_train, X_test = X[:split_idx], X[split_idx:]
    y_train, y_test = y[:split_idx], y[split_idx:]

    print(f"Train samples: {len(X_train)}, Test samples: {len(X_test)}")

    # Neural Network Architecture:
    # Input -> LSTM(64, return_sequences=True) -> Dropout(0.2) -> LSTM(32) -> Dropout(0.2) -> Dense(16) -> Dense(2)
    model = keras.Sequential([
        layers.Input(shape=(config.LSTM_SEQUENCE_LENGTH, 1), name="sequence_input"),
        layers.LSTM(64, return_sequences=True, name="lstm_layer_1"),
        layers.Dropout(0.2, name="dropout_1"),
        layers.LSTM(32, return_sequences=False, name="lstm_layer_2"),
        layers.Dropout(0.2, name="dropout_2"),
        layers.Dense(16, activation="relu", name="dense_features"),
        layers.Dense(2, activation="relu", name="demand_predictions")  # Outputs: [7-day forecast, 30-day forecast]
    ], name="CareHub_LSTM_DemandNet")

    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=0.003),
        loss="mean_squared_error",
        metrics=["mae"]
    )

    print("\n--- Model Architecture ---")
    model.summary()

    # Train model
    print("\nTraining LSTM network...")
    history = model.fit(
        X_train, y_train,
        validation_data=(X_test, y_test),
        epochs=35,
        batch_size=16,
        verbose=1
    )

    # Evaluate on holdout test set
    y_pred = model.predict(X_test)

    # 7-Day Metrics
    mae_7d = float(mean_absolute_error(y_test[:, 0], y_pred[:, 0]))
    rmse_7d = float(root_mean_squared_error(y_test[:, 0], y_pred[:, 0]))
    mape_7d = float(np.mean(np.abs((y_test[:, 0] - y_pred[:, 0]) / np.maximum(y_test[:, 0], 1.0))) * 100)

    # 30-Day Metrics
    mae_30d = float(mean_absolute_error(y_test[:, 1], y_pred[:, 1]))
    rmse_30d = float(root_mean_squared_error(y_test[:, 1], y_pred[:, 1]))
    mape_30d = float(np.mean(np.abs((y_test[:, 1] - y_pred[:, 1]) / np.maximum(y_test[:, 1], 1.0))) * 100)
    r2_30d = float(r2_score(y_test[:, 1], y_pred[:, 1]))

    metrics = {
        "model_name": "LSTM-DemandNet-v1",
        "task": "Medicine Demand Time-Series Forecasting",
        "trained_at": datetime.datetime.now().isoformat(),
        "training_samples": int(len(X_train)),
        "test_samples": int(len(X_test)),
        "sequence_length_days": config.LSTM_SEQUENCE_LENGTH,
        "evaluation_metrics": {
            "7_day_forecast": {
                "MAE": round(mae_7d, 2),
                "RMSE": round(rmse_7d, 2),
                "MAPE_percent": round(mape_7d, 2)
            },
            "30_day_forecast": {
                "MAE": round(mae_30d, 2),
                "RMSE": round(rmse_30d, 2),
                "MAPE_percent": round(mape_30d, 2),
                "R2_score": round(r2_30d, 4)
            }
        },
        "final_train_loss": float(history.history["loss"][-1]),
        "final_val_loss": float(history.history["val_loss"][-1])
    }

    print("\n--- LSTM Evaluation Results ---")
    print(f"7-Day  Forecast  ->  MAE: {mae_7d:.2f} units | RMSE: {rmse_7d:.2f} | MAPE: {mape_7d:.1f}%")
    print(f"30-Day Forecast  ->  MAE: {mae_30d:.2f} units | RMSE: {rmse_30d:.2f} | MAPE: {mape_30d:.1f}% | R²: {r2_30d:.3f}")

    # Persist model, scaler, and metrics
    os.makedirs(config.LSTM_DIR, exist_ok=True)
    model.save(config.LSTM_MODEL_PATH)
    preprocessor.save_scaler(config.LSTM_SCALER_PATH)

    with open(config.LSTM_METRICS_PATH, "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)

    print(f"\nModel saved to: {config.LSTM_MODEL_PATH}")
    print(f"Scaler saved to: {config.LSTM_SCALER_PATH}")
    print(f"Metrics saved to: {config.LSTM_METRICS_PATH}")
    print("=" * 65)

    return metrics

if __name__ == "__main__":
    train_lstm_model()
