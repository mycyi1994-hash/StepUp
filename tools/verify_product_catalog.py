"""Verify the complete high-resolution product-art package against the catalog."""

from __future__ import annotations

import csv
import hashlib
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
REQUIREMENTS = ROOT / "docs/redesign/ASSET-PRODUCT-REQUIREMENTS.csv"
ASSETS = ROOT / "design/redesign-2026-09/assets/modular"


def candidate_path(product_id: str) -> Path:
    if product_id.startswith(("FIR-", "WAT-", "LIT-", "WND-")):
        faction, number = product_id.split("-")
        name = {"FIR": "fire", "WAT": "water", "LIT": "lightning", "WND": "wind"}[faction]
        return ASSETS / "sneakers" / f"sneaker_{name}_{int(number):02d}.png"
    if product_id.startswith("FEMALE-CLO-"):
        return ASSETS / "outfits" / f"outfit_lum_clo_{int(product_id[-3:]):03d}.png"
    if product_id.startswith("MALE-CLO-"):
        return ASSETS / "outfits" / f"outfit_clo_{int(product_id[-3:]):03d}.png"
    raise ValueError(f"No new product path for {product_id}")


def main() -> None:
    with REQUIREMENTS.open(encoding="utf-8-sig", newline="") as handle:
        rows = list(csv.DictReader(handle))
    assert len(rows) == 67, f"Catalog changed: {len(rows)} entries"
    assert len({row["id"] for row in rows}) == 67, "Duplicate product ID"

    errors: list[str] = []
    expected: set[Path] = set()
    digests: dict[str, str] = {}
    rendered = reused = 0
    for row in rows:
        if row["status"] == "REUSE_VISUAL_REVIEW":
            reused += 1
            source = ROOT / row["existing"]
            if not source.is_file():
                errors.append(f"{row['id']}: missing reuse source {source}")
                continue
            with Image.open(source) as image:
                if min(image.size) < 1024 or "A" not in image.getbands():
                    errors.append(f"{row['id']}: reuse source lacks 1024px transparent art")
            continue
        if row["status"] != "RENDER_HIGH_RES":
            errors.append(f"{row['id']}: unknown requirement status {row['status']}")
            continue
        rendered += 1
        asset = candidate_path(row["id"])
        expected.add(asset)
        if not asset.is_file():
            errors.append(f"{row['id']}: missing {asset}")
            continue
        digest = hashlib.sha256(asset.read_bytes()).hexdigest()
        if digest in digests:
            errors.append(f"{row['id']}: duplicate pixels/file with {digests[digest]}")
        digests[digest] = row["id"]
        with Image.open(asset) as image:
            if image.size != (1254, 1254) or image.mode != "RGBA":
                errors.append(f"{row['id']}: expected 1254x1254 RGBA, got {image.size} {image.mode}")
                continue
            alpha = image.getchannel("A")
            if not alpha.point(lambda value: 255 if value > 128 else 0).getbbox():
                errors.append(f"{row['id']}: no visible product")
            for xy in ((0, 0), (0, 1253), (1253, 0), (1253, 1253)):
                if alpha.getpixel(xy) > 1:
                    errors.append(f"{row['id']}: opaque corner {xy}")

    actual = set((ASSETS / "sneakers").glob("*.png")) | set((ASSETS / "outfits").glob("*.png"))
    for extra in sorted(actual - expected):
        errors.append(f"Unexpected product image: {extra}")
    runtime_sources = "\n".join(
        (ROOT / path).read_text(encoding="utf-8")
        for path in (
            "app/src/main/java/com/stepup/android/ui/components/SneakerImage.kt",
            "app/src/main/java/com/stepup/android/ui/components/AvatarArtRes.kt",
        )
    )
    for asset in expected:
        if f"R.drawable.{asset.stem}" not in runtime_sources:
            errors.append(f"No existing app product ID mapping for {asset.stem}")
    assert (rendered, reused) == (62, 5), f"Catalog family count changed: {rendered}/{reused}"
    if errors:
        raise SystemExit("\n".join(errors))
    print(f"PASS: {len(rows)} product IDs = {rendered} new 1254px RGBA images + {reused} reusable sources; no missing or duplicate files")


if __name__ == "__main__":
    main()
