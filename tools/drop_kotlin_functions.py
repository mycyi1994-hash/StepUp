#!/usr/bin/env python3
"""
코틀린 파일에서 맨 위 함수를 뺀다(2026-11-03 iOS C — 옮긴 파일 중 공용에 이미 있는 것 · 쓰지 않는 예전 부품).

    python3 tools/drop_kotlin_functions.py <파일.kt> 함수이름 [함수이름 …]

앞 KDoc · 애노테이션까지 함께 뺀다. 끝은 괄호를 맞춰 찾는다(본문 { } 이든 = 식이든). 없는 이름은 알리고 건너뛴다.
"""
import re
import sys
from pathlib import Path


def body_end(text, start):
    """start: 함수 줄 시작 — 함수가 끝나는 글자 위치(그 줄 끝 다음)"""
    depth, i, quote, seen = 0, start, None, False
    n = len(text)
    while i < n:
        ch = text[i]
        if quote:
            if ch == "\\": i += 2; continue
            if text.startswith('"""', i) and quote == '"""': quote = None; i += 3; continue
            if ch == quote and quote != '"""': quote = None
            i += 1; continue
        if text.startswith("//", i):
            i = text.find("\n", i); i = n if i < 0 else i; continue
        if text.startswith("/*", i):
            j = text.find("*/", i + 2); i = n if j < 0 else j + 2; continue
        if text.startswith('"""', i): quote = '"""'; i += 3; continue
        if ch in "\"'": quote = ch
        elif ch in "({[": depth += 1; seen = True
        elif ch in ")}]": depth -= 1
        elif ch == "\n" and depth == 0 and seen:
            # 다음 줄이 이어지는 식(. ?: + 등)이면 계속
            nxt = text[i + 1:].lstrip(" ")
            if not re.match(r"(\.|\?\.|\?:|\+|-|\*|/|&&|\|\||\)|\})", nxt):
                return i + 1
        i += 1
    return n


path, names = Path(sys.argv[1]), sys.argv[2:]
text = path.read_text()
done = []
for name in names:
    m = re.search(rf"^(?:(?:internal|private|public) )?(?:suspend )?fun (?:[\w.<>]+\.)?{re.escape(name)}\b", text, re.M)
    if not m:
        print(f"없는 함수: {name}"); continue
    start = m.start()
    # 앞 애노테이션 · KDoc
    head = start
    while True:
        prev_end = text.rfind("\n", 0, head - 1)
        line = text[prev_end + 1:head - 1] if head > 0 else ""
        s = line.strip()
        if head > 0 and (s.startswith("@") or s.startswith("*") or s.startswith("/**") or s.startswith("//")):
            head = prev_end + 1
        else:
            break
    end = body_end(text, m.end())
    text = text[:head] + text[end:]
    done.append(name)
path.write_text(text)
print(path, "−", ", ".join(done))
