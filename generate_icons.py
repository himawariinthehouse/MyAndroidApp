#!/usr/bin/env python3
import base64
import os

# Minimal PNG bytes (1x1 white pixel)
png_data = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg=="
)

base_path = "/workspaces/MyAndroidApp/app/src/main/res"
densities = ["mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"]

for density in densities:
    path = os.path.join(base_path, density, "ic_launcher.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png_data)
    print(f"Created {path}")

print("✅ All launcher icons created successfully")
