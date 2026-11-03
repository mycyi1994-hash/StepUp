#!/usr/bin/env python3
"""
안드로이드 res → Compose Multiplatform composeResources (2026-11-03 iOS C0).

화면 글자 · 글꼴의 원본은 하나 — 안드로이드 `app/src/main/res` 다. iOS 화면(:sharedUi)은 빌드할 때 이 스크립트가 만든
composeResources 를 쓴다(저장소에 사본을 두지 않는다 — 글자 5천여 개 × 4개 언어 · 글꼴 6.6MB 가 두 벌이 되지 않게).
글자를 고칠 때는 지금처럼 `res/values*/*.xml` 만 고친다. check-strings.py · check-copy.py 가 보는 곳도 그대로다.

    python3 tools/sync_compose_resources.py <app/src/main/res> <출력 폴더> [<더할 정적 composeResources 폴더>]

- values(영어, 기본) · values-ko · values-ja · values-zh 의 <string> · <plurals> · <string-array> 를 언어마다 strings.xml 하나로 모은다.
  색 · bool · 스타일 같은 다른 값은 뺀다.
- 안드로이드 글자의 이스케이프 중 Compose 리소스가 모르는 \\@ · \\? 는 풀고, 자리 번호 없는 %s · %d 는 차례로 %1$s … 로 바꾼다
  (Compose 리소스는 자리 번호가 있는 %N$s · %N$d 만 채운다).
- font/ 의 글꼴(.ttf · .otf), drawable-nodpi 의 그림 · drawable 의 벡터는 그대로 복사한다.
- 정적 폴더(:sharedUi 의 src/commonMain/composeResources — 로고 그림 등)를 함께 복사한다.
"""
import re
import shutil
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

LOCALES = ["values", "values-ko", "values-ja", "values-zh"]
KINDS = ("string", "plurals", "string-array")


def normalize(text: str) -> str:
    if text is None:
        return text
    text = text.replace("\\@", "@").replace("\\?", "?")
    if re.search(r"%(?!\d+\$)(?!%)[sd]", text) and not re.search(r"%\d+\$[sd]", text):
        n = 0

        def number(m):
            nonlocal n
            n += 1
            return f"%{n}${m.group(1)}"

        text = re.sub(r"%(?!%)([sd])", number, text)
    return text


def normalize_element(el: ET.Element) -> None:
    el.text = normalize(el.text)
    for child in el:
        normalize_element(child)
        child.tail = normalize(child.tail)


def merge(locale_dir: Path) -> ET.Element:
    out = ET.Element("resources")
    seen = set()
    for f in sorted(locale_dir.glob("*.xml")):
        root = ET.parse(f).getroot()
        for el in root:
            if el.tag not in KINDS:
                continue
            key = (el.tag, el.get("name"))
            if key in seen:
                continue
            seen.add(key)
            clean = ET.Element(el.tag, {"name": el.get("name")})
            clean.text = el.text
            for child in el:
                item = ET.SubElement(clean, child.tag, {k: v for k, v in child.attrib.items() if k == "quantity"})
                item.text = child.text
            normalize_element(clean)
            out.append(clean)
    return out


def main() -> int:
    if len(sys.argv) < 3:
        print(__doc__)
        return 2
    res = Path(sys.argv[1])
    out = Path(sys.argv[2])
    static = Path(sys.argv[3]) if len(sys.argv) > 3 else None
    if out.exists():
        shutil.rmtree(out)
    out.mkdir(parents=True)
    counts = {}
    for loc in LOCALES:
        src = res / loc
        if not src.is_dir():
            continue
        tree = merge(src)
        ET.indent(tree, space="    ")
        target = out / loc
        target.mkdir(parents=True, exist_ok=True)
        ET.ElementTree(tree).write(target / "strings.xml", encoding="utf-8", xml_declaration=True)
        counts[loc] = len(tree)
    fonts = out / "font"
    fonts.mkdir(exist_ok=True)
    copied = 0
    for f in sorted((res / "font").glob("*")):
        if f.suffix.lower() in (".ttf", ".otf"):
            shutil.copy2(f, fonts / f.name)
            copied += 1
    # 그림 — drawable-nodpi(사진 · 신발 · 로고, webp/png)와 drawable 의 벡터(xml 중 <vector> 만 — 안드로이드 아이콘 겹 같은 것은 뺀다)
    drawables = out / "drawable"
    drawables.mkdir(exist_ok=True)
    pictures = 0
    for d in ("drawable", "drawable-nodpi"):
        for f in sorted((res / d).glob("*")) if (res / d).is_dir() else []:
            if f.suffix.lower() in (".webp", ".png", ".jpg", ".jpeg"):
                shutil.copy2(f, drawables / f.name)
                pictures += 1
            elif f.suffix.lower() == ".xml" and ET.parse(f).getroot().tag == "vector":
                shutil.copy2(f, drawables / f.name)
                pictures += 1
    if static and static.is_dir():
        shutil.copytree(static, out, dirs_exist_ok=True)
    print("composeResources:", ", ".join(f"{k} {v}" for k, v in counts.items()), f"· font {copied} · drawable {pictures}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
