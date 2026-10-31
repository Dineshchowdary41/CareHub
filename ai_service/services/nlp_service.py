"""
CareHub 3.0 — NLP Clinical Text Understanding Service
Processes raw OCR text through tokenization, clinical entity recognition,
and catalog matching to generate structured prescription records.
"""

import os
import sys

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import config
from preprocessing.text_preprocessing import PrescriptionNlpPipeline

class NlpService:
    def __init__(self):
        self.pipeline = PrescriptionNlpPipeline(vocab_path=config.NLP_VOCAB_PATH)

    def update_catalog_vocabulary(self, medicines):
        """Allows dynamic synchronization with database medicines."""
        self.pipeline.update_vocabulary(medicines)

    def parse_prescription_text(self, text, catalog_medicines=None):
        """
        Executes complete NLP pipeline on prescription text.
        Returns structured entities and model metadata.
        """
        if catalog_medicines:
            self.pipeline.update_vocabulary(catalog_medicines)

        result = self.pipeline.process_text(text)
        return {
            "modelName": "NLP-ClinicalRegexNER-v1",
            "task": "Prescription Clinical Text Understanding",
            "detectedMedicines": result["detectedMedicines"],
            "totalEntities": result["totalEntities"],
            "status": result["extractionStatus"],
            "pipeline": result["pipeline"]
        }
