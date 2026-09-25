"""Read-only dimensions and alpha sanity check for the modular art pilots."""

from pathlib import Path
import json

from PIL import Image, ImageChops


ROOT = Path(__file__).parent
NAMES = [
    "scene-riverside-blue-night-grounded.png",
    "scene-riverside-dawn-grounded.png",
    "scene-riverside-run-sunset.png",
    "scene-riverside-run-night.png",
    "lumi-idle-base-pilot.png",
    "lumi-idle-outfit-pink-pilot.png",
    "lumi-idle-outfit-lavender-pilot.png",
    "lumi-idle-outfit-lavender-v2-pilot.png",
    "lumi-idle-shoes-teal-pilot.png",
    "lumi-idle-shoes-water-pilot.png",
    "lumi-idle-shoes-water-v2-pilot.png",
    "lumi-idle-cap-pink-pilot.png",
    "lumi-sit-pink-pilot.png",
    "runo-sit-pink-pilot.png",
    "lumi-run-rear-pink-pilot.png",
    "runo-run-rear-pink-pilot.png",
    "bench-riverside-pilot.png",
]

alphas = {}
for name in NAMES:
    image = Image.open(ROOT / name)
    print(f"{name}: {image.size} {image.mode}")
    if image.mode != "RGBA":
        continue
    alpha = image.getchannel("A")
    alphas[name] = alpha
    solid = alpha.point(lambda value: 255 if value > 128 else 0)
    histogram = alpha.histogram()
    print(f"  solid pixels={sum(histogram[129:]):,}; bounds={solid.getbbox()}; semi-transparent={sum(histogram[1:128]):,}")
    print(f"  corners={[alpha.getpixel((x, y)) for x, y in ((0, 0), (image.width - 1, 0), (0, image.height - 1), (image.width - 1, image.height - 1))]}")

base = alphas["lumi-idle-base-pilot.png"].point(lambda value: 255 if value > 128 else 0)
for name in (
    "lumi-idle-outfit-pink-pilot.png",
    "lumi-idle-outfit-lavender-pilot.png",
    "lumi-idle-shoes-teal-pilot.png",
    "lumi-idle-shoes-water-pilot.png",
    "lumi-idle-cap-pink-pilot.png",
):
    layer = alphas[name].point(lambda value: 255 if value > 128 else 0)
    outside = ImageChops.subtract(layer, base)
    print(f"{name}: solid pixels outside base={sum(outside.histogram()[1:]):,}/{sum(layer.histogram()[1:]):,}")

metadata = json.loads((ROOT / "scene-metadata.json").read_text(encoding="utf-8"))
ids = [scene["id"] for scene in metadata["scenes"]]
assert len(ids) == len(set(ids)), "duplicate scene ID"
for scene in metadata["scenes"]:
    image = Image.open(ROOT / scene["file"])
    assert image.size == tuple(scene["size"]), scene["id"]
    assert image.mode == "RGB", scene["id"]
    assert scene["uses"] and all(0 <= v <= 1 for v in scene["footAnchor"]), scene["id"]
print(f"scene metadata: {len(ids)} entries valid")
