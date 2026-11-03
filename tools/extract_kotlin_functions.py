#!/usr/bin/env python3
"""
코틀린 파일에서 맨 위 함수 몇 개만 꺼내 새 파일로(2026-11-03 iOS C — 큰 부품 파일 전체를 옮기기 어려울 때).

    python3 tools/extract_kotlin_functions.py <원본.kt> <새.kt> 함수이름 [함수이름 …]

원본의 package · import 를 그대로 두고, 이름이 같은 맨 위 fun(앞 KDoc · 애노테이션 포함)만 담는다. 쓰지 않는 import 는 컴파일러가 알려 준다.
"""
import re
import sys
from pathlib import Path

src, dst, names = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3:]
text = src.read_text()
head_end = max(m.end() for m in re.finditer(r"^(package|import) [^\n]*\n", text, re.M))
head = text[:head_end]
parts = []
for name in names:
    m = re.search(rf"^(?:@[\w.]+(?:\([^)]*\))?\s*\n)*(?:(?:internal|private|public) )?fun (?:[\w.<>]+\.)?{re.escape(name)}\b", text, re.M)
    if not m:
        sys.exit(f"없는 함수: {name}")
    start = m.start()
    doc = text.rfind("/**", 0, start)
    if doc >= 0 and text[text.find("*/", doc) + 2:start].strip() == "":
        start = doc
    i = text.index("(", m.end())
    depth = 0
    while True:
        ch = text[i]
        if ch in "({": depth += 1
        elif ch in ")}":
            depth -= 1
            if depth == 0 and ch == "}": break
        if depth == 0 and ch == ")":
            nxt = re.match(r"\s*(:[^{=]*)?\s*([{=])", text[i + 1:])
            if nxt and nxt.group(2) == "=":
                end = text.find("\n\n", i)
                i = end if end > 0 else len(text) - 1
                break
        i += 1
    parts.append(text[start:i + 1].replace("private fun ", "fun ").replace("internal fun ", "fun "))
dst.parent.mkdir(parents=True, exist_ok=True)
dst.write_text(head + "\n" + "\n\n".join(parts) + "\n")
print(f"{src} → {dst}: {', '.join(names)}")
