"""
CareHub 3.0 — Natural Language Processing (NLP) Pipeline Evaluation
Evaluates clinical entity extraction accuracy, precision, recall, and F1-score
across labeled test prescriptions, and saves the NLP benchmark metrics.
"""

import os
import sys
import json
import datetime
from sklearn.metrics import precision_score, recall_score, f1_score, accuracy_score

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import config
from preprocessing.text_preprocessing import PrescriptionNlpPipeline

def evaluate_nlp_pipeline():
    print("=" * 65)
    print("CareHub 3.0: Initiating NLP Clinical Entity Extraction Evaluation")
    print("=" * 65)

    cases_file = os.path.join(config.DATA_DIR, "synthetic_nlp_eval_cases.json")
    if not os.path.exists(cases_file):
        raise FileNotFoundError(f"Evaluation cases not found at {cases_file}.")

    with open(cases_file, "r", encoding="utf-8") as f:
        cases = json.load(f)

    print(f"Loaded {len(cases)} benchmark prescription evaluation test cases.")

    pipeline = PrescriptionNlpPipeline()

    med_correct = 0
    dose_correct = 0
    freq_correct = 0
    dur_correct = 0
    total = len(cases)

    y_true_med = []
    y_pred_med = []

    for c in cases:
        raw_text = c["raw_text"]
        result = pipeline.process_text(raw_text)
        detected = result["detectedMedicines"]

        if detected:
            first_item = detected[0]
            pred_med = first_item["medicineName"].strip().lower()
            pred_dose = first_item["dosage"].strip().lower().replace(" ", "")
            pred_freq = first_item["frequency"].strip().lower()
            pred_dur = first_item["duration"].strip().lower()
        else:
            pred_med = ""
            pred_dose = ""
            pred_freq = ""
            pred_dur = ""

        exp_med = c["expected_medicine"].strip().lower()
        exp_dose = c["expected_dosage"].strip().lower().replace(" ", "")
        exp_freq = c["expected_frequency"].strip().lower()
        exp_dur = c["expected_duration"].strip().lower()

        y_true_med.append(exp_med)
        y_pred_med.append(pred_med)

        if exp_med in pred_med or pred_med in exp_med:
            med_correct += 1
        if exp_dose == pred_dose or exp_dose in pred_dose:
            dose_correct += 1
        if exp_freq in pred_freq or pred_freq in exp_freq:
            freq_correct += 1
        if exp_dur in pred_dur or pred_dur in exp_dur:
            dur_correct += 1

    med_acc = med_correct / total
    dose_acc = dose_correct / total
    freq_acc = freq_correct / total
    dur_acc = dur_correct / total

    # Precision, recall, and F1 for medicine entity matching
    med_prec = precision_score(y_true_med, y_pred_med, average="weighted", zero_division=0)
    med_rec = recall_score(y_true_med, y_pred_med, average="weighted", zero_division=0)
    med_f1 = f1_score(y_true_med, y_pred_med, average="weighted", zero_division=0)

    overall_pipeline_acc = (med_acc + dose_acc + freq_acc + dur_acc) / 4.0

    metrics = {
        "model_name": "NLP-ClinicalRegexNER-v1",
        "task": "Prescription Clinical Text Understanding & Entity Extraction",
        "evaluated_at": datetime.datetime.now().isoformat(),
        "total_test_cases": total,
        "evaluation_metrics": {
            "overall_accuracy": round(overall_pipeline_acc, 4),
            "medicine_entity_accuracy": round(med_acc, 4),
            "medicine_precision": round(med_prec, 4),
            "medicine_recall": round(med_rec, 4),
            "medicine_f1_score": round(med_f1, 4),
            "dosage_extraction_accuracy": round(dose_acc, 4),
            "frequency_extraction_accuracy": round(freq_acc, 4),
            "duration_extraction_accuracy": round(dur_acc, 4)
        },
        "pipeline_stages": [
            "1. Text Cleaning & Header Filtering",
            "2. Line Tokenization",
            "3. Normalization & Canonical Sig Translation",
            "4. Catalog Vocabulary & Levenshtein Matching",
            "5. Dosage & Quantity Arithmetic Extraction",
            "6. Structured JSON Transformation"
        ]
    }

    print("\n--- NLP Evaluation Results ---")
    print(f"Overall Pipeline Accuracy  : {overall_pipeline_acc * 100:.2f}%")
    print(f"Medicine Entity Accuracy   : {med_acc * 100:.2f}% (Precision: {med_prec * 100:.1f}%, Recall: {med_rec * 100:.1f}%, F1: {med_f1 * 100:.1f}%)")
    print(f"Dosage Extraction Accuracy : {dose_acc * 100:.2f}%")
    print(f"Frequency Extraction Acc   : {freq_acc * 100:.2f}%")
    print(f"Duration Extraction Acc    : {dur_acc * 100:.2f}%")

    os.makedirs(config.NLP_DIR, exist_ok=True)
    with open(config.NLP_METRICS_PATH, "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)

    with open(config.NLP_VOCAB_PATH, "w", encoding="utf-8") as f:
        json.dump(pipeline.catalog_vocab, f, indent=2)

    print(f"Saved NLP metrics to: {config.NLP_METRICS_PATH}")
    print(f"Saved catalog vocabulary to: {config.NLP_VOCAB_PATH}")
    print("=" * 65)

    return metrics

if __name__ == "__main__":
    evaluate_nlp_pipeline()
