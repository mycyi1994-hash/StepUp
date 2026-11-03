#!/usr/bin/env python3
"""
쓰지 않는 import 빼기(2026-11-03 iOS C — 큰 파일에서 함수 몇 개만 꺼냈을 때, 남은 안드로이드 import 가 공용 컴파일을 막지 않게).

    python3 tools/prune_imports.py <파일.kt> [<파일.kt> …]

이름이 본문에 한 번도 안 나오는 import 만 뺀다. '*' import 와 연산자 · 위임 함수(getValue · setValue · plus …)는 남긴다.
"""
import re
import sys
from pathlib import Path

KEEP = {"getValue", "setValue", "provideDelegate", "plus", "minus", "times", "div", "rem", "unaryMinus", "contains",
        "get", "set", "invoke", "compareTo", "iterator", "component1", "component2", "rangeTo", "plusAssign", "minusAssign"}
for name in sys.argv[1:]:
    p = Path(name)
    s = p.read_text()
    lines = s.split("\n")
    imports = [(i, l) for i, l in enumerate(lines) if l.startswith("import ")]
    body = "\n".join(l for l in lines if not l.startswith("import ") and not l.startswith("package "))
    drop = set()
    for i, l in imports:
        target = l[len("import "):].strip()
        alias = re.search(r" as (\w+)$", target)
        simple = alias.group(1) if alias else target.split(".")[-1]
        if simple == "*" or simple in KEEP:
            continue
        if not re.search(rf"(?<![\w]){re.escape(simple)}(?![\w])", body):
            drop.add(i)
    p.write_text("\n".join(l for i, l in enumerate(lines) if i not in drop))
    print(p, f"− import {len(drop)}")
