"""List every production/pilot image and sound in the Sep-25 handoff, with its app use."""

from __future__ import annotations

import csv
import json
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
MODULAR = ROOT / "design/redesign-2026-09/assets/modular"
PROFILE = ROOT / "design/redesign-2026-09/assets/profile"
AUDIO = ROOT / "design/redesign-2026-09/audio"
DRAWABLE = ROOT / "app/src/main/res/drawable-nodpi"
RAW = ROOT / "app/src/main/res/raw"
REPORT = ROOT / "docs/redesign/MEDIA-APPLICATION-2026-09-25.csv"
rows: list[dict[str, str]] = []
covered_images: set[Path] = set()


def relative(path: Path | None) -> str:
    return path.relative_to(ROOT).as_posix() if path else ""


def add(kind: str, source: Path, app: Path | None, web: Path | None, use: str, status: str) -> None:
    assert source.is_file(), source
    if app:
        assert app.is_file(), app
    if web:
        assert web.is_file(), web
    rows.append(dict(kind=kind, source=relative(source), app=relative(app), web=relative(web), use=use, status=status))
    if kind == "image":
        covered_images.add(source)


def valid_product(source: Path, runtime: Path) -> None:
    with Image.open(source) as original, Image.open(runtime) as packaged:
        assert original.size == (1254, 1254) and original.mode == "RGBA", source
        assert min(packaged.size) >= 640 and "A" in packaged.getbands(), runtime


sneaker_code = (ROOT / "app/src/main/java/com/stepup/android/ui/components/SneakerImage.kt").read_text(encoding="utf-8")
outfit_code = (ROOT / "app/src/main/java/com/stepup/android/ui/components/AvatarArtRes.kt").read_text(encoding="utf-8")
for folder, use, code in (
    (MODULAR / "sneakers", "신발 카탈로그·꾸미기 상품 그림", sneaker_code),
    (MODULAR / "outfits", "의상 카탈로그·꾸미기 상품 그림", outfit_code),
):
    for source in sorted(folder.glob("*.png")):
        runtime = DRAWABLE / f"{source.stem}.webp"
        valid_product(source, runtime)
        assert f"R.drawable.{source.stem}" in code, source
        add("image", source, runtime, None, use, "APP_ACTIVE")

scene_map = {
    "scene-riverside-blue-night-grounded.png": "scene_home_blue_night.png",
    "scene-riverside-dawn-grounded.png": "scene_home_dawn.png",
    "scene-riverside-run-night.png": "scene_run_night.png",
    "scene-riverside-run-sunset.png": "scene_run_sunset.png",
}
for source_name, app_name in scene_map.items():
    add("image", MODULAR / source_name, DRAWABLE / app_name, None,
        "홈·내 정보 또는 러닝의 교체 가능한 독립 배경", "APP_ACTIVE")

run_map = {
    "lumi-run-frame-a-pilot.png": "run_frame_lumi_a.webp",
    "lumi-run-frame-b-pilot.png": "run_frame_lumi_b.webp",
    "runo-run-frame-a-source.webp": "run_frame_runo_a.webp",
    "runo-run-frame-b-pilot.png": "run_frame_runo_b.webp",
}
for source_name, app_name in run_map.items():
    add("image", MODULAR / "running" / source_name, DRAWABLE / app_name, None,
        "러닝 중 기본 착장·NFT 신발 미착용 시 2프레임", "APP_CONDITIONAL")

profile_map = {
    "bench-riverside-v2.png": "prop_profile_bench_v2.png",
    "lumi-seated-pink-v2.png": "avatar_lumi_sit_studio_pink_v2.png",
    "runo-seated-pink-v2.png": "avatar_runo_sit_studio_pink_v2.png",
}
for source_name, app_name in profile_map.items():
    add("image", PROFILE / source_name, DRAWABLE / app_name, None,
        "내 정보의 분리된 벤치·앉은 캐릭터; 핑크/WND-010 정확 일치 시", "APP_CONDITIONAL")

add("image", ROOT / "design/redesign-2026-09/assets/mystery-box/closed.png",
    DRAWABLE / "mystery_box_closed.webp", ROOT / "web/assets/mystery-box-closed.webp",
    "앱·뽑기 웹의 닫힌 상자; 버튼/문구와 분리", "APP_ACTIVE")

pilot_notes = {
    "bench-riverside-pilot.png": "v2 벤치로 교체된 초기 시안",
    "lumi-sit-pink-pilot.png": "v2 루미 앉기 그림으로 교체된 초기 시안",
    "runo-sit-pink-pilot.png": "v2 루노 앉기 그림으로 교체된 초기 시안",
    "preview-render.png": "분리 조합 미리보기; 앱 그림 아님",
}
for source in sorted(MODULAR.rglob("*")):
    if source.suffix.lower() not in {".png", ".webp"} or source in covered_images:
        continue
    add("image", source, None, None,
        pilot_notes.get(source.name, "몸·의상·신발/후면 포즈 합성 시험; 출시 화면에 미사용"),
        "REFERENCE_ONLY")

web_draw = {
    "draw_charge.wav", "draw_box_open.wav", "draw_reveal_common.wav",
    "draw_reveal_rare.wav", "draw_reveal_epic.wav", "draw_reveal_legendary.wav",
    "draw_cancel.wav", "draw_fail.wav",
}
manifest = json.loads((AUDIO / "manifest.json").read_text(encoding="utf-8"))
assert len(manifest) == 29
for entry in manifest:
    source = AUDIO / entry["file"]
    is_ambient = source.parent.name == "ambience"
    is_web_draw = source.name in web_draw
    add("sound", source, RAW / source.name,
        ROOT / "web/assets/sounds" / source.name if is_web_draw else None,
        entry["use"],
        "AMBIENCE_OPT_IN" if is_ambient else "WEB_TRANSACTION_GATED" if is_web_draw else "APP_EVENT_ACTIVE")

for source in sorted(AUDIO.glob("preview_*.wav")) + sorted(AUDIO.glob("preview_*.mp3")):
    add("sound-preview", source, None, None, "묶음 미리듣기; 런타임 이벤트에 사용하지 않음", "PREVIEW_ONLY")

assert len([row for row in rows if row["kind"] == "image"]) == 89
assert len([row for row in rows if row["kind"] == "sound"]) == 29
assert len([row for row in rows if row["kind"] == "sound-preview"]) == 6
with REPORT.open("w", newline="", encoding="utf-8-sig") as stream:
    writer = csv.DictWriter(stream, fieldnames=["kind", "source", "app", "web", "use", "status"])
    writer.writeheader()
    writer.writerows(rows)
print(f"PASS: {len(rows)} files audited = 89 images + 29 production sounds + 6 audio previews; {REPORT}")
