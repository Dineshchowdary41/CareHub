"""
CareHub 3.0 — Synthetic / Demo Dataset Generator
Clearly labeled synthetic dataset generation for LSTM and CNN model training and evaluation.
Complies with Module 13 rules: kept separate from real hospital records and explicitly labeled.
"""

import os
import random
import datetime
import json
import numpy as np
import pandas as pd
from PIL import Image, ImageDraw, ImageFont, ImageFilter

DATA_DIR = os.path.dirname(os.path.abspath(__file__))
IMAGES_DIR = os.path.join(DATA_DIR, "images")
os.makedirs(IMAGES_DIR, exist_ok=True)

MEDICINES = [
    ("Paracetamol", 25, 4.0, "Stable"),
    ("Amoxicillin", 18, 3.5, "Surging"),
    ("Cetirizine", 12, 2.0, "Stable"),
    ("Metformin", 30, 5.0, "Stable"),
    ("Pantoprazole", 15, 3.0, "Declining"),
    ("Atorvastatin", 20, 3.5, "Stable"),
    ("Azithromycin", 10, 2.5, "Surging"),
    ("Ibuprofen", 22, 4.0, "Stable"),
    ("Omeprazole", 14, 2.5, "Stable"),
    ("Ciprofloxacin", 8, 1.8, "Declining")
]

def generate_demand_dataset(days=180, output_csv="synthetic_demand_dataset.csv"):
    """
    Generates realistic daily dispensing time-series records for 10 medicines.
    Labels dataset explicitly as synthetic demo data.
    """
    random.seed(42)
    np.random.seed(42)

    records = []
    start_date = datetime.date(2026, 1, 1)

    for med_id, (med_name, base_demand, std_dev, trend_type) in enumerate(MEDICINES, start=1):
        for d in range(days):
            current_date = start_date + datetime.timedelta(days=d)
            day_of_week = current_date.weekday()

            # Weekly seasonality (higher on Mon/Tue OPD, lower on Sun)
            weekday_factor = 1.15 if day_of_week in (0, 1) else (0.75 if day_of_week == 6 else 1.0)

            # Trend progression
            if trend_type == "Surging":
                trend_factor = 1.0 + (0.35 * (d / days))
            elif trend_type == "Declining":
                trend_factor = 1.0 - (0.25 * (d / days))
            else:
                trend_factor = 1.0 + 0.05 * np.sin(2 * np.pi * d / 30.0)

            mean_qty = base_demand * weekday_factor * trend_factor
            dispensed = max(0, int(round(np.random.normal(mean_qty, std_dev))))

            records.append({
                "date": current_date.isoformat(),
                "day_index": d,
                "medicine_id": med_id,
                "medicine_name": med_name,
                "quantity_dispensed": dispensed,
                "is_synthetic_demo_data": True
            })

    df = pd.DataFrame(records)
    out_path = os.path.join(DATA_DIR, output_csv)
    df.to_csv(out_path, index=False)
    print(f"Generated synthetic LSTM demand dataset: {len(df)} rows saved to {out_path}")
    return df

def generate_prescription_images(samples_per_class=40, output_csv="synthetic_image_dataset.csv"):
    """
    Generates realistic prescription document images and non-prescription images
    for CNN training. Labeled as synthetic data.
    """
    random.seed(42)
    np.random.seed(42)

    image_records = []
    classes = [
        "VALID_PRESCRIPTION_PRINTED",
        "VALID_PRESCRIPTION_HANDWRITTEN",
        "POOR_QUALITY_DEGRADED",
        "NON_PRESCRIPTION_DOCUMENT"
    ]

    for class_idx, class_name in enumerate(classes):
        class_folder = os.path.join(IMAGES_DIR, class_name)
        os.makedirs(class_folder, exist_ok=True)

        for i in range(samples_per_class):
            img = Image.new("RGB", (320, 240), color=(255, 255, 255))
            draw = ImageDraw.Draw(img)

            if class_name == "VALID_PRESCRIPTION_PRINTED":
                # Clean prescription document header and lines
                draw.rectangle([(10, 10), (310, 45)], outline=(13, 71, 161), width=2)
                draw.text((20, 18), "CAREHUB CLINICAL PRESCRIPTION", fill=(13, 71, 161))
                draw.line([(10, 55), (310, 55)], fill=(180, 180, 180), width=1)
                draw.text((20, 65), f"Date: 2026-10-30   Rx No: 40{i:03d}", fill=(50, 50, 50))
                # Medicine lines
                med1 = random.choice(MEDICINES)[0]
                med2 = random.choice(MEDICINES)[0]
                draw.text((20, 95), f"1. {med1} 500mg - 1-0-1 (5 days)", fill=(20, 20, 20))
                draw.text((20, 125), f"2. {med2} 250mg - 1-0-0 (7 days)", fill=(20, 20, 20))
                draw.line([(200, 200), (290, 200)], fill=(80, 80, 80), width=1)
                draw.text((210, 205), "Dr. Smith, MD", fill=(80, 80, 80))

            elif class_name == "VALID_PRESCRIPTION_HANDWRITTEN":
                # Slanted text simulating cursive prescription handwriting
                draw.rectangle([(8, 8), (312, 232)], outline=(120, 120, 120), width=1)
                draw.text((25, 20), "Rx - CareHub OPD Clinic", fill=(0, 51, 102))
                med = random.choice(MEDICINES)[0]
                # Draw organic handwritten-like strokes
                for line_y in [60, 95, 130, 165]:
                    points = [(25 + x * 10, line_y + int(np.sin(x) * 4)) for x in range(25)]
                    draw.line(points, fill=(20, 30, 80), width=2)
                draw.text((30, 70), f"Tab {med} 650mg BD x 5d", fill=(10, 20, 60))
                draw.text((30, 140), "Cap Amoxicillin 500mg TID x 7d", fill=(10, 20, 60))
                draw.text((180, 195), "Dr. Verified", fill=(10, 20, 60))

            elif class_name == "POOR_QUALITY_DEGRADED":
                # Blurry, heavily degraded, low-contrast image
                draw.rectangle([(10, 10), (310, 230)], fill=(220, 220, 220))
                draw.text((25, 30), "Rx Clinical Memo", fill=(170, 170, 170))
                draw.text((25, 80), "Unreadable prescription text...", fill=(160, 160, 160))
                # Heavy Gaussian blur to degrade sharpness
                img = img.filter(ImageFilter.GaussianBlur(radius=random.uniform(3.5, 6.0)))
                # Add noise
                arr = np.array(img, dtype=np.int16)
                noise = np.random.normal(0, 25, arr.shape).astype(np.int16)
                arr = np.clip(arr + noise, 0, 255).astype(np.uint8)
                img = Image.fromarray(arr)

            else:  # NON_PRESCRIPTION_DOCUMENT
                # Random shapes, receipt, landscape, non-medical pattern
                bg_color = (random.randint(200, 240), random.randint(200, 240), random.randint(200, 240))
                draw.rectangle([(0, 0), (320, 240)], fill=bg_color)
                draw.rectangle([(30, 40), (280, 180)], outline=(150, 50, 50), width=3)
                draw.ellipse([(60, 60), (140, 140)], fill=(70, 130, 180))
                draw.polygon([(180, 70), (250, 140), (170, 150)], fill=(210, 105, 30))
                draw.text((40, 200), "INVOICE #99824 - HARDWARE STORE", fill=(40, 40, 40))

            filename = f"sample_{class_name}_{i:03d}.png"
            filepath = os.path.join(class_folder, filename)
            img.save(filepath)

            image_records.append({
                "filename": filename,
                "filepath": os.path.relpath(filepath, DATA_DIR),
                "class_label": class_name,
                "class_index": class_idx,
                "is_synthetic_demo_data": True
            })

    df = pd.DataFrame(image_records)
    out_path = os.path.join(DATA_DIR, output_csv)
    df.to_csv(out_path, index=False)
    print(f"Generated synthetic CNN prescription image dataset: {len(df)} images saved to {out_path}")
    return df

def generate_nlp_eval_dataset(output_json="synthetic_nlp_eval_cases.json"):
    """
    Creates clinical prescription test cases with ground-truth entity labels
    to rigorously compute NLP extraction metrics (Precision, Recall, F1).
    """
    test_cases = [
        {
            "raw_text": "Tab Paracetamol 500 mg twice daily for 5 days",
            "expected_medicine": "Paracetamol",
            "expected_dosage": "500 mg",
            "expected_frequency": "Twice daily",
            "expected_duration": "5 days"
        },
        {
            "raw_text": "Amoxicillin 500mg 3 times a day for 7 days after food",
            "expected_medicine": "Amoxicillin",
            "expected_dosage": "500mg",
            "expected_frequency": "Thrice daily",
            "expected_duration": "7 days"
        },
        {
            "raw_text": "Take Cetirizine 10 mg once daily at bedtime for 10 days",
            "expected_medicine": "Cetirizine",
            "expected_dosage": "10 mg",
            "expected_frequency": "Once daily",
            "expected_duration": "10 days"
        },
        {
            "raw_text": "Metformin 500 mg 1-0-1 for 30 days regularly",
            "expected_medicine": "Metformin",
            "expected_dosage": "500 mg",
            "expected_frequency": "Twice daily",
            "expected_duration": "30 days"
        },
        {
            "raw_text": "Cap Pantoprazole 40 mg OD before breakfast for 14 days",
            "expected_medicine": "Pantoprazole",
            "expected_dosage": "40 mg",
            "expected_frequency": "Once daily",
            "expected_duration": "14 days"
        },
        {
            "raw_text": "Atorvastatin 20 mg 0-0-1 for 1 month",
            "expected_medicine": "Atorvastatin",
            "expected_dosage": "20 mg",
            "expected_frequency": "Once daily",
            "expected_duration": "1 month"
        },
        {
            "raw_text": "Azithromycin 500 mg once daily for 3 days",
            "expected_medicine": "Azithromycin",
            "expected_dosage": "500 mg",
            "expected_frequency": "Once daily",
            "expected_duration": "3 days"
        },
        {
            "raw_text": "Tab Ibuprofen 400 mg twice daily for 4 days",
            "expected_medicine": "Ibuprofen",
            "expected_dosage": "400 mg",
            "expected_frequency": "Twice daily",
            "expected_duration": "4 days"
        },
        {
            "raw_text": "Omeprazole 20 mg OD for 14 days",
            "expected_medicine": "Omeprazole",
            "expected_dosage": "20 mg",
            "expected_frequency": "Once daily",
            "expected_duration": "14 days"
        },
        {
            "raw_text": "Ciprofloxacin 500 mg BD for 7 days",
            "expected_medicine": "Ciprofloxacin",
            "expected_dosage": "500 mg",
            "expected_frequency": "Twice daily",
            "expected_duration": "7 days"
        }
    ]

    out_path = os.path.join(DATA_DIR, output_json)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(test_cases, f, indent=2)
    print(f"Generated NLP evaluation benchmark: {len(test_cases)} cases saved to {out_path}")
    return test_cases

if __name__ == "__main__":
    generate_demand_dataset()
    generate_prescription_images()
    generate_nlp_eval_dataset()
