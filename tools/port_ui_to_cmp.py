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
s = re.sub(r"@(?:androidx\.annotation\.)?DrawableRes\s+(val |var )?(\w+): Int\b", r"\1\2: org.jetbrains.compose.resources.DrawableResource", s)
# 그림 · 글자 번호를 돌려주는 함수(@DrawableRes / @StringRes 가 붙은 fun … : Int)
_RES_TYPES = {"DrawableRes": "DrawableResource", "StringRes": "StringResource", "PluralsRes": "PluralStringResource"}
s = re.sub(r"@(?:androidx\.annotation\.)?(DrawableRes|StringRes|PluralsRes)\s*\n(\s*)((?:(?:internal|private|public) )?(?:fun [^\n]*\)|val \w+(?:\.\w+)*))\s*:\s*Int\b",
           lambda m: f"{m.group(2)}{m.group(3)}: org.jetbrains.compose.resources.{_RES_TYPES[m.group(1)]}", s)
s = s.replace("import androidx.annotation.DrawableRes\n", "")
for fn in ("painterResource", "stringResource", "pluralStringResource"):
    s = s.replace(f"androidx.compose.ui.res.{fn}(", f"org.jetbrains.compose.resources.{fn}(")
# 화면 높이(dp) — CMP 에는 LocalConfiguration 이 없다
s = s.replace("LocalConfiguration.current.screenHeightDp", "with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.height.toDp().value.toInt() }")
s = s.replace("LocalConfiguration.current.screenWidthDp", "with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toDp().value.toInt() }")
s = s.replace("import androidx.compose.ui.platform.LocalConfiguration\n", "")
s = re.sub(r"\bMath\.(PI|sin|cos|tan|asin|acos|atan|sqrt|abs|min|max|atan2|hypot|floor|ceil|exp|ln|log10|pow|round)\b", r"kotlin.math.\1", s)
s = re.sub(r"\bMath\.toRadians\(", "com.stepup.android.domain.toRadians(", s)
s = re.sub(r"\bMath\.toDegrees\(", "com.stepup.android.domain.toDegrees(", s)
s = s.replace("kotlin.math.pow(", "Math_pow_unsupported(")
s = s.replace("kotlin.math.log(", "kotlin.math.ln(")
s = re.sub(r"\n\s*platformStyle = PlatformTextStyle\(includeFontPadding = false\),", "", s)
if "StepUpFonts." in s and "import com.stepup.android.ui.theme.StepUpFonts" not in s and "import com.stepup.android.ui.theme.*" not in s:
    s = re.sub(r"\n(import [^\n]+\n)", r"\nimport com.stepup.android.ui.theme.StepUpFonts\n\1", s, count=1)
s = re.sub(r"@(?:androidx\.annotation\.)?StringRes\s+(val |var )?(\w+): Int", r"\1\2: org.jetbrains.compose.resources.StringResource", s)
s = s.replace("import androidx.annotation.StringRes\n", "")
s = s.replace("import com.stepup.android.ui.Screen\n", "import com.stepup.android.ui.Screen\n")
# 글자(코드 포인트) 세기 — 공용 Text.kt
s = re.sub(r"(\w+)\.codePointCount\(0, \1\.length\)", r"\1.codePointLength()", s)
s = s.replace("preloadShoeArt(context, ", "preloadShoeArt(")
# 시스템 뒤로 가기 — 공용 SystemBackHandler(iOS 는 받지 않는다)
s = s.replace("import androidx.activity.compose.BackHandler\n", "import com.stepup.android.ui.components.SystemBackHandler\n")
s = re.sub(r"(?<![\w.])BackHandler\(", "SystemBackHandler(", s)
s = re.sub(r"(?<![\w.])BackHandler \{", "SystemBackHandler {", s)
s = re.sub(r",\s*decorFitsSystemWindows = false", "", s)
s = re.sub(r"decorFitsSystemWindows = false,\s*", "", s)
# 짧은 알림 — 공용 showToast(앱 뿌리의 ToastHost 가 띄운다)
s = s.replace("import android.widget.Toast\n", "import com.stepup.android.ui.components.showToast\n")
s = re.sub(r"(?:android\.widget\.)?Toast\.makeText\(\s*\w+(?:\.applicationContext)?\s*,\s*", "showToast(", s)
s = re.sub(r",\s*(?:android\.widget\.)?Toast\.LENGTH_SHORT\s*\)\.show\(\)", ")", s)
s = re.sub(r",\s*(?:android\.widget\.)?Toast\.LENGTH_LONG\s*\)\.show\(\)", ", long = true)", s)
# 글자 읽기 — Composable 밖은 공용 getString(suspend)
s = re.sub(r"\b\w*[cC]ontext\.getString\(", "org.jetbrains.compose.resources.getString(", s)
s = s.replace("com.stepup.android.ui.components.returnToSignIn(context)", "com.stepup.android.ui.components.returnToSignIn()")
s = re.sub(r"(?<![\w.])returnToSignIn\(context\)", "returnToSignIn()", s)
# 화면 언어
s = s.replace("LocalConfiguration.current.locales[0].language", "androidx.compose.ui.text.intl.Locale.current.language")
# 빌드 판 값 — 공용 AppBuild
s = s.replace("import com.stepup.android.BuildConfig\n", "")
s = re.sub(r"\b(?:com\.stepup\.android\.)?BuildConfig\.", "com.stepup.android.core.AppBuild.", s)
# 시간대 — kotlinx-datetime
s = s.replace("import java.time.ZoneId\n", "")
s = re.sub(r"(?:java\.time\.)?ZoneId\.systemDefault\(\)", "kotlinx.datetime.TimeZone.currentSystemDefault()", s)
s = re.sub(r":\s*(?:java\.time\.)?ZoneId\b", ": kotlinx.datetime.TimeZone", s)
# 저장소는 공용 AppServices 에서(안드로이드 ServiceLocator 의 화면 쪽 자리)
s = s.replace("import com.stepup.android.core.ServiceLocator\n", "import com.stepup.android.core.AppServices\n")
s = re.sub(r"\b(?:com\.stepup\.android\.core\.)?ServiceLocator\.", "AppServices.", s)
if "AppServices." in s and "import com.stepup.android.core.AppServices" not in s:
    s = re.sub(r"\n(import [^\n]+\n)", "\nimport com.stepup.android.core.AppServices\n\\1", s, count=1)
for fn in ("painterResource", "stringResource", "pluralStringResource"):
    if re.search(rf"(?<![\w.]){fn}\(", s) and f"import org.jetbrains.compose.resources.{fn}\n" not in s:
        s = re.sub(r"\n(import [^\n]+\n)", f"\nimport org.jetbrains.compose.resources.{fn}\n\\1", s, count=1)


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


# 쓰지 않게 된 안드로이드 Context — 선언만 남았으면 지운다
if len(re.findall(r"\bcontext\b", s)) == len(re.findall(r"val context = LocalContext\.current(?:\.applicationContext)?\n", s)):
    s = re.sub(r"[ \t]*val context = LocalContext\.current(?:\.applicationContext)?\n", "", s)
if "LocalContext" not in s.replace("import androidx.compose.ui.platform.LocalContext\n", ""):
    s = s.replace("import androidx.compose.ui.platform.LocalContext\n", "")

s = _formats(s)
dst.parent.mkdir(parents=True, exist_ok=True)
dst.write_text(s)
print(f"{src} → {dst}")

