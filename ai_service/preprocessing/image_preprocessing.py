"""
Image Preprocessing & Quality Assessment for CNN Prescription Analysis
Handles image resizing, tensor conversion, sharpness/blurriness scoring, and ROI extraction.
"""

import io
import numpy as np
from PIL import Image, ImageOps, ImageFilter

class ImagePreprocessor:
    def __init__(self, target_size=(128, 128)):
        self.target_size = target_size

    def load_image_from_bytes(self, image_bytes):
        """Loads and converts bytes to PIL RGB Image."""
        try:
            img = Image.open(io.BytesIO(image_bytes))
            if img.mode != "RGB":
                img = img.convert("RGB")
            return img
        except Exception as e:
            raise ValueError(f"Invalid image content: {e}")

    def load_image_from_file(self, file_path):
        """Loads an image from disk."""
        img = Image.open(file_path)
        if img.mode != "RGB":
            img = img.convert("RGB")
        return img

    def preprocess_for_cnn(self, pil_image):
        """
        Preprocesses a PIL Image for CNN inference:
        Resizes to target_size, normalizes pixel values to [0, 1], returns (1, H, W, 3) tensor.
        """
        resized = pil_image.resize(self.target_size, Image.Resampling.BILINEAR)
        arr = np.array(resized, dtype=np.float32) / 255.0
        # Expand batch dimension
        return np.expand_dims(arr, axis=0)

    def assess_image_quality(self, pil_image):
        """
        Computes sharpness, brightness, and contrast to determine quality score.
        Uses PIL kernel edge filter approximation of Laplacian variance.
        """
        gray = pil_image.convert("L")
        gray_arr = np.array(gray, dtype=np.float32)

        # 1. Brightness: mean luminosity
        brightness = float(np.mean(gray_arr))

        # 2. Contrast: standard deviation of pixels
        contrast = float(np.std(gray_arr))

        # 3. Sharpness / Blur estimation via 3x3 Laplacian edge convolution
        # Laplacian kernel: [[0, 1, 0], [1, -4, 1], [0, 1, 0]]
        laplacian_filtered = gray.filter(ImageFilter.Kernel((3, 3), [0, 1, 0, 1, -4, 1, 0, 1, 0], 1, 0))
        lap_arr = np.array(laplacian_filtered, dtype=np.float32)
        sharpness_variance = float(np.var(lap_arr))

        # Categorize image quality
        if sharpness_variance >= 120 and contrast >= 35 and 40 <= brightness <= 230:
            quality = "EXCELLENT"
            confidence = 0.96
            description = "High resolution prescription with crisp clinical typography."
        elif sharpness_variance >= 50 and contrast >= 25:
            quality = "GOOD"
            confidence = 0.88
            description = "Clear prescription with good legibility for clinical extraction."
        elif sharpness_variance >= 20:
            quality = "BLURRY"
            confidence = 0.65
            description = "Mild motion blur or lower camera resolution detected. Clinician review required."
        else:
            quality = "DEGRADED"
            confidence = 0.45
            description = "Severe blur, low contrast or degraded image. Manual transcription recommended."

        return {
            "quality": quality,
            "sharpnessScore": round(sharpness_variance, 2),
            "brightnessScore": round(brightness, 2),
            "contrastScore": round(contrast, 2),
            "confidence": confidence,
            "description": description
        }

    def extract_prescription_roi(self, pil_image):
        """
        Extracts primary prescription bounding box / Region of Interest (ROI)
        to isolate the handwritten / printed medication block.
        """
        w, h = pil_image.size
        # Exclude outer 5% margin (binder clips, background tables)
        left = int(w * 0.05)
        top = int(h * 0.08)
        right = int(w * 0.95)
        bottom = int(h * 0.92)
        roi_img = pil_image.crop((left, top, right, bottom))
        return roi_img, (left, top, right - left, bottom - top)
