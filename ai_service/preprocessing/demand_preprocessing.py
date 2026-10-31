"""
Time-Series Demand Preprocessing for LSTM Forecasting
Handles normalization, sliding sequence creation, and array reshaping.
"""

import numpy as np
import pandas as pd
from sklearn.preprocessing import MinMaxScaler
import joblib
import os

class DemandPreprocessor:
    def __init__(self, sequence_length=14):
        self.sequence_length = sequence_length
        self.scaler = MinMaxScaler(feature_range=(0.0, 1.0))
        self.is_fitted = False

    def fit(self, data_series):
        """Fit scaler on 1D or 2D array of historical dispensing quantities."""
        arr = np.array(data_series).reshape(-1, 1).astype(np.float32)
        self.scaler.fit(arr)
        self.is_fitted = True
        return self

    def transform(self, data_series):
        """Scale values between 0 and 1."""
        arr = np.array(data_series).reshape(-1, 1).astype(np.float32)
        if not self.is_fitted:
            # Fallback scaling if not yet fitted
            max_val = max(1.0, float(np.max(arr)))
            return arr / max_val
        return self.scaler.transform(arr)

    def inverse_transform(self, scaled_series):
        """Inverse scale predictions back to unit quantities."""
        arr = np.array(scaled_series).reshape(-1, 1).astype(np.float32)
        if not self.is_fitted:
            return arr
        return self.scaler.inverse_transform(arr)

    def create_sequences(self, daily_series, horizon_7=7, horizon_30=30):
        """
        Creates sliding window sequences for multi-horizon LSTM training.
        X: (batch_size, sequence_length, 1)
        y: (batch_size, 2) -> [7-day future sum, 30-day future sum]
        """
        data = np.array(daily_series, dtype=np.float32).flatten()
        n = len(data)
        X, y = [], []

        for i in range(n - self.sequence_length - horizon_30 + 1):
            seq_x = data[i : i + self.sequence_length]
            future_7 = np.sum(data[i + self.sequence_length : i + self.sequence_length + horizon_7])
            future_30 = np.sum(data[i + self.sequence_length : i + self.sequence_length + horizon_30])
            X.append(seq_x)
            y.append([future_7, future_30])

        X = np.array(X, dtype=np.float32)
        y = np.array(y, dtype=np.float32)

        if len(X) == 0:
            return np.empty((0, self.sequence_length, 1)), np.empty((0, 2))

        # Scale X using feature scaler
        X_scaled = self.scaler.fit_transform(X.reshape(-1, 1)).reshape(X.shape)
        self.is_fitted = True

        # Reshape X to 3D for LSTM: (samples, time_steps, features)
        X_scaled = np.expand_dims(X_scaled, axis=-1)

        return X_scaled, y

    def prepare_inference_sequence(self, recent_history):
        """
        Prepares a single lookback sequence of length `sequence_length`
        from recent dispensing history. If shorter, pads with mean.
        """
        hist = [float(x) for x in recent_history]
        if not hist:
            hist = [0.0]

        if len(hist) < self.sequence_length:
            mean_val = float(np.mean(hist))
            pad = [mean_val] * (self.sequence_length - len(hist))
            hist = pad + hist
        else:
            hist = hist[-self.sequence_length:]

        arr = np.array(hist, dtype=np.float32).reshape(-1, 1)
        if self.is_fitted:
            arr_scaled = self.scaler.transform(arr)
        else:
            max_v = max(1.0, float(np.max(arr)))
            arr_scaled = arr / max_v

        # Reshape to (1, sequence_length, 1)
        return arr_scaled.reshape(1, self.sequence_length, 1)

    def save_scaler(self, path):
        """Persist fitted scaler using joblib."""
        os.makedirs(os.path.dirname(path), exist_ok=True)
        joblib.dump(self.scaler, path)

    def load_scaler(self, path):
        """Load fitted scaler from joblib file."""
        if os.path.exists(path):
            self.scaler = joblib.load(path)
            self.is_fitted = True
            return True
        return False
