#!/usr/bin/env python3
"""
안드로이드 화면 코드(Jetpack Compose) → Compose Multiplatform 공용 코드로 옮길 때 기계적으로 바꾸는 것(2026-11-03 iOS C).

    python3 tools/port_ui_to_cmp.py <app/src/main/java/.../X.kt> <sharedUi/src/commonMain/kotlin/.../X.kt>

- R.string · R.plurals · R.drawable · R.font → Res.*(빌드할 때 res 에서 만드는 composeResources — tools/sync_compose_resources.py)
- androidx.compose.ui.res.{stringResource, pluralStringResource, painterResource} → org.jetbrains.compose.resources.*
- 글꼴 맨 위 값 StepUpSans · StepUpNumbers → StepUpFonts.sans · .numbers(테마가 글꼴을 읽어 넣는 값), System.currentTimeMillis → epochMillis
패키지 · 이름 · 화면 모양은 그대로다. 남는 안드로이드 전용(Context · BuildConfig · 기기 기능)은 컴파일 오류로 드러나 손으로 고친다.
"""
import re
import sys
from pathlib import Path

src, dst = Path(sys.argv[1]), Path(sys.argv[2])
s = src.read_text()
s = s.replace("import com.stepup.android.R\n", "import com.stepup.ui.res.*\n")
for fn in ("stringResource", "pluralStringResource", "painterResource"):
    s = s.replace(f"import androidx.compose.ui.res.{fn}\n", f"import org.jetbrains.compose.resources.{fn}\n")
s = re.sub(r"\bR\.(string|plurals|drawable|font)\.", r"Res.\1.", s)
s = s.replace("import com.stepup.android.ui.theme.StepUpSans\n", "import com.stepup.android.ui.theme.StepUpFonts\n")
s = s.replace("import com.stepup.android.ui.theme.StepUpNumbers\n", "")
s = re.sub(r"\bStepUpSans\b", "StepUpFonts.sans", s)
s = re.sub(r"\bStepUpNumbers\b", "StepUpFonts.numbers", s)
s = s.replace("System.currentTimeMillis()", "com.stepup.android.domain.epochMillis()")
s = s.replace("com.stepup.android.ui.components.cachedPainterResource(", "painterResource(")
s = s.replace("cachedPainterResource(", "painterResource(")
s = s.replace("com.stepup.android.ui.components.painterResource(", "painterResource(")
s = re.sub(r"@(?:androidx\.annotation\.)?DrawableRes\s+(\w+): Int", r"\1: org.jetbrains.compose.resources.DrawableResource", s)
for fn in ("painterResource", "stringResource", "pluralStringResource"):
    s = s.replace(f"androidx.compose.ui.res.{fn}(", f"org.jetbrains.compose.resources.{fn}(")
# 화면 높이(dp) — CMP 에는 LocalConfiguration 이 없다
s = s.replace("LocalConfiguration.current.screenHeightDp", "with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.height.toDp().value.toInt() }")
s = s.replace("LocalConfiguration.current.screenWidthDp", "with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toDp().value.toInt() }")
s = s.replace("import androidx.compose.ui.platform.LocalConfiguration\n", "")
s = re.sub(r"\bMath\.(PI|sin|cos|sqrt|abs|min|max|atan2|hypot)\b", r"kotlin.math.\1", s)
s = re.sub(r"\n\s*platformStyle = PlatformTextStyle\(includeFontPadding = false\),", "", s)
if "StepUpFonts." in s and "import com.stepup.android.ui.theme.StepUpFonts" not in s and "import com.stepup.android.ui.theme.*" not in s:
    s = re.sub(r"\n(import [^\n]+\n)", r"\nimport com.stepup.android.ui.theme.StepUpFonts\n\1", s, count=1)
s = re.sub(r"@(?:androidx\.annotation\.)?StringRes\s+(val |var )?(\w+): Int", r"\1\2: org.jetbrains.compose.resources.StringResource", s)
s = s.replace("import androidx.annotation.StringRes\n", "")
s = s.replace("import com.stepup.android.ui.Screen\n", "import com.stepup.android.ui.Screen\n")


def _call_end(text, start):
    """text[start] == '(' — 짝이 맞는 ')' 뒤 자리"""
    depth, i, quote = 0, start, None
    while i < len(text):
        ch = text[i]
        if quote:
            if ch == "\\": i += 2; continue
            if ch == quote: quote = None
        elif ch in "\"'": quote = ch
        elif ch == "(": depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0: return i + 1
        i += 1
    return -1


def _formats(text):
    """\"%.2f\".format(x) · \"%,d\".format(x) · \"%02d\".format(x) → 공용 함수(Platform.kt — 안드로이드와 같은 글자)"""
    out, pos = [], 0
    pat = re.compile(r'"(%\.(\d)f|%,d|%,\.(\d)f|%0(\d)d|%d)"\.format\(')
    while True:
        m = pat.search(text, pos)
        if not m: out.append(text[pos:]); break
        end = _call_end(text, m.end() - 1)
        if end < 0: out.append(text[pos:]); break
        arg = text[m.end():end - 1]
        fmt = m.group(1)
        if fmt.startswith("%.") and fmt.endswith("f"): rep = f"com.stepup.android.domain.formatFixed(({arg}).toDouble(), {m.group(2)})"
        elif fmt == "%,d": rep = f"com.stepup.android.domain.formatGrouped(({arg}).toLong())"
        elif fmt.startswith("%,."): rep = f"com.stepup.android.domain.formatGrouped(({arg}).toDouble(), {m.group(3)})"
        elif fmt.startswith("%0"): rep = f"com.stepup.android.domain.zeroPad(({arg}).toLong(), {m.group(4)})"
        else: rep = f"({arg}).toString()"
        out.append(text[pos:m.start()]); out.append(rep); pos = end
    return "".join(out)


s = _formats(s)
dst.parent.mkdir(parents=True, exist_ok=True)
dst.write_text(s)
print(f"{src} → {dst}")
