#!/usr/bin/env python3
"""공용 화면 코드의 그라디언트는 StopBrush 로 — Brush.*Gradient( 를 바로 쓰면 막는다.

StopBrush(sharedUi components/StopBrush.kt)는 실제 폰 · iOS 에서 Brush 를 그대로 돌려주고, 안드로이드 에뮬레이터에서만
여러 색 그라디언트를 미리 그린 그림으로 대신한다 — 기기 검사 에뮬레이터(SwiftShader)가 여러 색 그라디언트를 그리다 죽었다(2026-11-04).
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent / "sharedUi" / "src" / "commonMain"
PATTERN = re.compile(r"(?<![\w])(?:androidx\.compose\.ui\.graphics\.)?Brush\.(vertical|horizontal|linear|radial|sweep)Gradient\(")
bad = []
for path in sorted(ROOT.rglob("*.kt")):
    if path.name == "StopBrush.kt":
        continue
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if PATTERN.search(line):
            bad.append(f"{path.relative_to(ROOT)}:{number}: {line.strip()}")
if bad:
    print("그라디언트는 StopBrush.*Gradient 로 써 주세요(에뮬레이터 기기 검사가 죽지 않게):")
    print("\n".join(bad))
    sys.exit(1)
print("gradients OK")
