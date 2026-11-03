#!/usr/bin/env python3
"""
코틀린 파일에서 맨 위 함수를 뺀다(2026-11-03 iOS C — 옮긴 파일 중 iOS 에 쓰지 않는 예전 부품, 예: 캐릭터 그림).

    python3 tools/drop_kotlin_functions.py <파일.kt> 함수이름 [함수이름 …]

앞 KDoc · 애노테이션까지 함께 뺀다. 함수 끝은 맨 앞 칸의 '}' 줄(또는 한 줄 '=' 함수의 다음 빈 줄)로 본다.
"""
import re
import sys
from pathlib import Path

path, names = Path(sys.argv[1]), sys.argv[2:]
lines = path.read_text().split("\n")
for name in names:
    pat = re.compile(rf"^(?:(?:internal|private|public) )?(?:suspend )?fun (?:[\w.<>]+\.)?{re.escape(name)}\b")
    start = next((i for i, l in enumerate(lines) if pat.match(l)), None)
    if start is None:
        sys.exit(f"없는 함수: {name}")
    head = start
    while head > 0 and (lines[head - 1].startswith("@") or lines[head - 1].startswith(" *") or lines[head - 1].startswith("/**") or lines[head - 1].startswith("//")):
        head -= 1
    end = start
    while end < len(lines) and lines[end] != "}" and not (lines[end] == "" and "{" not in "".join(lines[start:end])):
        end += 1
    del lines[head:end + 1]
path.write_text("\n".join(lines))
print(path, "−", ", ".join(names))
