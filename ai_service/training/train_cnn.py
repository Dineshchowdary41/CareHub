"""
CareHub 3.0 — CNN Prescription Image Analysis Training
Trains a Convolutional Neural Network on clinical prescription images.
Classifies image quality and document type: Printed Rx, Handwritten Rx, Degraded/Blurry, Non-Prescription.
Calculates Accuracy, Precision, Recall, and F1-Score metrics and saves the model.
"""

import os
import sys
import json
import datetime
import numpy as np
import pandas as pd
from PIL import Image
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, classification_report

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import config
from preprocessing.image_preprocessing import ImagePreprocessor

import keras
from keras import layers

def train_cnn_model():
    print("=" * 65)
    print("CareHub 3.0: Initiating CNN Prescription Image Analysis Training")
    print("=" * 65)

    data_file = os.path.join(config.DATA_DIR, "synthetic_image_dataset.csv")
    if not os.path.exists(data_file):
        raise FileNotFoundError(f"Image dataset index not found at {data_file}. Run generate_datasets.py first.")

    df = pd.read_csv(data_file)
    print(f"Loaded {len(df)} images across {df['class_label'].nunique()} classes.")

    preprocessor = ImagePreprocessor(target_size=config.CNN_IMAGE_SIZE)

    # Load and preprocess all images
    X_list, y_list = [], []
    for _, row in df.iterrows():
        img_path = os.path.join(config.DATA_DIR, row["filepath"])
        if os.path.exists(img_path):
            img = preprocessor.load_image_from_file(img_path)
            tensor = preprocessor.preprocess_for_cnn(img)  # (1, 128, 128, 3)
            X_list.append(tensor[0])
            y_list.append(int(row["class_index"]))

    X = np.array(X_list, dtype=np.float32)
    y = np.array(y_list, dtype=np.int32)
    print(f"Preprocessed image tensors: X shape = {X.shape}, y shape = {y.shape}")

    # Stratified Train/Test split
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.25, random_state=42, stratify=y
    )
    print(f"Train split: {len(X_train)} samples | Test split: {len(X_test)} samples")

    # Convolutional Neural Network Architecture
    # Input(128, 128, 3) -> Conv2D(32) -> MaxPool -> Conv2D(64) -> MaxPool -> Conv2D(64) -> MaxPool -> Flatten -> Dense(64) -> Dropout(0.3) -> Dense(4, softmax)
    model = keras.Sequential([
        layers.Input(shape=(config.CNN_IMAGE_SIZE[0], config.CNN_IMAGE_SIZE[1], 3), name="prescription_image_input"),
        layers.Conv2D(32, (3, 3), activation="relu", padding="same", name="conv2d_block1"),
        layers.BatchNormalization(name="batchnorm_1"),
        layers.MaxPooling2D((2, 2), name="maxpool_1"),

        layers.Conv2D(64, (3, 3), activation="relu", padding="same", name="conv2d_block2"),
        layers.BatchNormalization(name="batchnorm_2"),
        layers.MaxPooling2D((2, 2), name="maxpool_2"),

        layers.Conv2D(64, (3, 3), activation="relu", padding="same", name="conv2d_block3"),
        layers.MaxPooling2D((2, 2), name="maxpool_3"),

        layers.Flatten(name="flatten_features"),
        layers.Dense(64, activation="relu", name="dense_features"),
        layers.Dropout(0.3, name="dropout_features"),
        layers.Dense(config.CNN_NUM_CLASSES, activation="softmax", name="prescription_classification")
    ], name="CareHub_CNN_PrescriptionNet")

    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=0.001),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"]
    )

    print("\n--- CNN Architecture ---")
    model.summary()

    print("\nTraining CNN classifier...")
    history = model.fit(
        X_train, y_train,
        validation_data=(X_test, y_test),
        epochs=15,
        batch_size=16,
        verbose=1
    )

    # Evaluate on test set
    y_pred_probs = model.predict(X_test)
    y_pred = np.argmax(y_pred_probs, axis=1)

    acc = float(accuracy_score(y_test, y_pred))
    prec = float(precision_score(y_test, y_pred, average="weighted", zero_division=0))
    rec = float(recall_score(y_test, y_pred, average="weighted", zero_division=0))
    f1 = float(f1_score(y_test, y_pred, average="weighted", zero_division=0))

    metrics = {
        "model_name": "CNN-PrescriptionNet-v1",
        "task": "Prescription Document & Quality Image Classification",
        "trained_at": datetime.datetime.now().isoformat(),
        "input_resolution": f"{config.CNN_IMAGE_SIZE[0]}x{config.CNN_IMAGE_SIZE[1]}x3",
        "num_classes": config.CNN_NUM_CLASSES,
        "classes": config.CNN_CLASS_LABELS,
        "train_samples": int(len(X_train)),
        "test_samples": int(len(X_test)),
        "evaluation_metrics": {
            "accuracy": round(acc, 4),
            "precision": round(prec, 4),
            "recall": round(rec, 4),
            "f1_score": round(f1, 4)
        },
        "final_train_loss": float(history.history["loss"][-1]),
        "final_val_loss": float(history.history["val_loss"][-1]),
        "final_val_accuracy": float(history.history["val_accuracy"][-1])
    }

    print("\n--- CNN Evaluation Results ---")
    print(f"Accuracy  : {acc * 100:.2f}%")
    print(f"Precision : {prec * 100:.2f}%")
    print(f"Recall    : {rec * 100:.2f}%")
    print(f"F1-Score  : {f1 * 100:.2f}%")
    print("\nClassification Report:\n", classification_report(y_test, y_pred, target_names=config.CNN_CLASS_LABELS, zero_division=0))

    # Persist model, class labels, and metrics
    os.makedirs(config.CNN_DIR, exist_ok=True)
    model.save(config.CNN_MODEL_PATH)

    with open(config.CNN_CLASSES_PATH, "w", encoding="utf-8") as f:
        json.dump(config.CNN_CLASS_LABELS, f, indent=2)

    with open(config.CNN_METRICS_PATH, "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)

    print(f"Saved CNN model to: {config.CNN_MODEL_PATH}")
    print(f"Saved classes to: {config.CNN_CLASSES_PATH}")
    print(f"Saved metrics to: {config.CNN_METRICS_PATH}")
    print("=" * 65)

    return metrics

if __name__ == "__main__":
    train_cnn_model()
