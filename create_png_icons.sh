#!/bin/bash
# Create minimal PNG files for launcher icons

# Minimal 48x48 blue PNG (base64 encoded)
PNG_DATA="iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAIAAABkf7SRAAAAJklEQVR4nGP8//8/A/wjY2AGxgEcBnkMJkCGgTkMJkCGgTkMJkCGgQH4dwEJ8wMFgwAAAABJRU5ErkJggg=="

# Create PNG for each density
for dir in mipmap-mdpi mipmap-hdpi mipmap-xhdpi mipmap-xxhdpi mipmap-xxxhdpi; do
    base64 -d <<< "$PNG_DATA" > "/workspaces/MyAndroidApp/app/src/main/res/${dir}/ic_launcher.png"
    echo "Created /workspaces/MyAndroidApp/app/src/main/res/${dir}/ic_launcher.png"
done
