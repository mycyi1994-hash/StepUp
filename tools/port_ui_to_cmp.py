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
# 입출력 일꾼 — 공용 코드는 kotlinx.coroutines.IO 를 들여와야 Dispatchers.IO 가 보인다
if "Dispatchers.IO" in s and "import kotlinx.coroutines.IO\n" not in s:
    s = re.sub(r"\n(import [^\n]+\n)", "\nimport kotlinx.coroutines.IO\n\\1", s, count=1)
# java.time → kotlinx-datetime(공용 규칙과 같은 날짜 값) · 날짜 꼴은 공용 DatePattern(JDK 와 맞대어 검사)
for jt, kx in (("LocalDate", "kotlinx.datetime.LocalDate"), ("LocalTime", "kotlinx.datetime.LocalTime"),
               ("LocalDateTime", "kotlinx.datetime.LocalDateTime"), ("YearMonth", "kotlinx.datetime.YearMonth"),
               ("DayOfWeek", "kotlinx.datetime.DayOfWeek"), ("Instant", "kotlin.time.Instant")):
    s = s.replace(f"import java.time.{jt}\n", f"import {kx}\n")
    s = re.sub(rf"\bjava\.time\.{jt}\b", kx, s)
s = s.replace("import java.time.ZonedDateTime\n", "")
s = s.replace("import java.time.format.DateTimeFormatter\n", "")
s = s.replace("import java.time.format.TextStyle\n", "")
s = s.replace("kotlin.time.Instant.ofEpochMilli(", "kotlin.time.Instant.fromEpochMilliseconds(")
s = re.sub(r"(?<![\w.])Instant\.ofEpochMilli\(", "kotlin.time.Instant.fromEpochMilliseconds(", s)
s = re.sub(r"\.toEpochMilli\(\)", ".toEpochMilliseconds()", s)
s = re.sub(r"\.atZone\(", ".toLocalDateTime(", s)
s = re.sub(r"\.toLocalDate\(\)", ".date", s)
s = re.sub(r"\.toLocalTime\(\)", ".time", s)
s = re.sub(r"\bLocalDate\.now\(\)", "com.stepup.android.domain.todayIn()", s)
s = re.sub(r"\bLocalDate\.now\(", "com.stepup.android.domain.todayIn(", s)
s = re.sub(r"\.toEpochDay\(\)", ".toEpochDays()", s)
s = re.sub(r"\bLocalDate\.ofEpochDay\(", "kotlinx.datetime.LocalDate.fromEpochDays(", s)
s = re.sub(r"\bLocalDate\.of\(", "kotlinx.datetime.LocalDate(", s)
s = re.sub(r"\bLocalTime\.of\(", "kotlinx.datetime.LocalTime(", s)
s = re.sub(r"\bYearMonth\.of\(", "kotlinx.datetime.YearMonth(", s)
for unit, k in (("Days", "DAY"), ("Weeks", "WEEK"), ("Months", "MONTH"), ("Years", "YEAR")):
    s = re.sub(rf"\.plus{unit}\(", f".plus{unit}Kx(", s)
    s = re.sub(rf"\.minus{unit}\(", f".minus{unit}Kx(", s)
s = re.sub(r"\.dayOfMonth\b", ".day", s)
s = re.sub(r"\.monthValue\b", ".month.number", s)
s = re.sub(r"\.dayOfWeek\.value\b", ".dayOfWeek.isoDayNumber", s)
s = re.sub(r"\bZoneOffset\.UTC\b", "kotlinx.datetime.TimeZone.UTC", s)
s = re.sub(r"RunPhotos\.decode\(\s*\w*[cC]ontext\s*,\s*", "RunPhotos.decode(", s)
s = re.sub(r"(?:java\.time\.format\.)?DateTimeFormatter\.ofLocalizedDate\((?:java\.time\.format\.)?FormatStyle\.MEDIUM\)\.format\(",
           "com.stepup.android.domain.DatePattern.mediumDate(androidx.compose.ui.text.intl.Locale.current.language, ", s)
s = re.sub(r"\.getDisplayName\(\s*(?:java\.time\.format\.)?TextStyle\.(NARROW|SHORT|FULL)\s*,(?:[^()]|\([^()]*\))*\)",
           r".displayName(com.stepup.android.domain.DateNameStyle.\1, androidx.compose.ui.text.intl.Locale.current.language)", s)
def _pattern_calls(text):
    out, pos = [], 0
    lang = "androidx.compose.ui.text.intl.Locale.current.language"
    # X.format(DateTimeFormatter.ofPattern(P[, L]))
    pat = re.compile(r"\.format\(\s*(?:java\.time\.format\.)?DateTimeFormatter\.ofPattern\(")
    while True:
        m = pat.search(text, pos)
        if not m: out.append(text[pos:]); break
        inner_end = _call_end(text, m.end() - 1)
        close = inner_end
        while close < len(text) and text[close] in " \n": close += 1
        if inner_end < 0 or close >= len(text) or text[close] != ")": out.append(text[pos:m.end()]); pos = m.end(); continue
        args = _split_args(text[m.end():inner_end - 1])
        # 받는 값(X)은 앞의 식 — 부르는 쪽 그대로 두고 확장 함수로 받는다
        out.append(text[pos:m.start()]); out.append(f".formatPattern({args[0]}, {lang})"); pos = close + 1
    return "".join(out)
# 앱 밖 열기 · 언어 — 공용 ExternalIntents · AppLocale(Context 없이)
s = re.sub(r"ExternalIntents\.(\w+)\(\s*\w*[cC]ontext\s*,\s*", r"ExternalIntents.\1(", s)
s = re.sub(r"ExternalIntents\.(\w+)\(\s*\w*[cC]ontext\s*\)", r"ExternalIntents.\1()", s)
s = re.sub(r"AppLocale\.change\(\s*\w*[cC]ontext\s*,\s*", "AppLocale.change(", s)
s = s.replace("com.stepup.android.core.ExternalIntents.", "com.stepup.android.core.ExternalIntents.")
# 이름이 글자 번호(…Res)인 Int 칸 — 애노테이션이 없어도 글자(StringResource)로. 그림 이름(icon · image · art · drawable · photo)은 그림으로
s = re.sub(r"\b(val |var )?(\w*(?:[lL]abel|[tT]itle|[bB]ody|[tT]ext|[dD]esc|[nN]ame|[hH]int|[mM]essage|[sS]ub|[sS]ummary|[cC]aption|[nN]ote|[dD]etail)\w*Res): Int\b",
           r"\1\2: org.jetbrains.compose.resources.StringResource", s)
s = re.sub(r"\b(val |var )?(\w*(?:[iI]con|[iI]mage|[aA]rt|[dD]rawable|[pP]hoto|[pP]icture)\w*Res): Int\b",
           r"\1\2: org.jetbrains.compose.resources.DrawableResource", s)
# 테스트 APK 스스로 업데이트는 안드로이드 시험 판에만 — 공용 화면에서는 빌드 이름만
s = s.replace("com.stepup.android.core.TestUpdates.buildLabel", "com.stepup.android.core.AppBuild.VERSION_NAME")
s = s.replace("if (com.stepup.android.core.TestUpdates.enabled) com.stepup.android.ui.components.TestUpdateSettingsLine()", "")
# 동시성 · 테스트 표시 — 공용(Concurrent.kt · kotlin.concurrent)
s = s.replace("import java.util.concurrent.ConcurrentHashMap\n", "import com.stepup.android.domain.ConcurrentMap\nimport com.stepup.android.domain.ConcurrentSet\n")
s = re.sub(r"(?:java\.util\.concurrent\.)?ConcurrentHashMap\.newKeySet<([^>]+)>\(\)", r"com.stepup.android.domain.ConcurrentSet<\1>()", s)
s = re.sub(r"(?:java\.util\.concurrent\.)?ConcurrentHashMap<", "com.stepup.android.domain.ConcurrentMap<", s)
s = s.replace("import java.util.concurrent.atomic.AtomicInteger\n", "import com.stepup.android.domain.AtomicCounter\n")
s = re.sub(r"(?:java\.util\.concurrent\.atomic\.)?AtomicInteger\(", "com.stepup.android.domain.AtomicCounter(", s)
s = re.sub(r"(?<![\w.])@Volatile\b", "@kotlin.concurrent.Volatile", s)
s = s.replace("import androidx.annotation.VisibleForTesting\n", "")
s = re.sub(r"@VisibleForTesting(?:\([^)]*\))?\s*", "", s)
s = re.sub(r"(?:java\.util\.)?UUID\.randomUUID\(\)\.toString\(\)", "com.stepup.android.domain.randomUuid()", s)
s = s.replace("import java.util.UUID\n", "")
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


def _split_args(text):
    """맨 위 쉼표로 인자를 나눈다(괄호 · 따옴표 안 쉼표는 그대로)"""
    args, depth, cur, quote, i = [], 0, [], None, 0
    while i < len(text):
        ch = text[i]
        if quote:
            cur.append(ch)
            if ch == "\\": cur.append(text[i + 1]); i += 2; continue
            if ch == quote: quote = None
        elif ch in "\"'": quote = ch; cur.append(ch)
        elif ch in "([{": depth += 1; cur.append(ch)
        elif ch in ")]}": depth -= 1; cur.append(ch)
        elif ch == "," and depth == 0: args.append("".join(cur).strip()); cur = []
        else: cur.append(ch)
        i += 1
    if "".join(cur).strip(): args.append("".join(cur).strip())
    return args


_SPEC = re.compile(r"%(?:(\d+)\$)?(,)?(0\d+)?(?:\.(\d+))?([dsf%])")


def _spec_expr(flag_group, zero, prec, conv, arg):
    if conv == "s": return f"{arg}"
    if conv == "d":
        if flag_group: return f"com.stepup.android.domain.formatGrouped(({arg}).toLong())"
        if zero: return f"com.stepup.android.domain.zeroPad(({arg}).toLong(), {int(zero[1:])})"
        return f"{arg}"
    if conv == "f":
        n = int(prec) if prec is not None else 6
        if flag_group: return f"com.stepup.android.domain.formatGrouped(({arg}).toDouble(), {n})"
        return f"com.stepup.android.domain.formatFixed(({arg}).toDouble(), {n})"
    return None


def _formats(text):
    """\"…%.2f km\".format(x) 처럼 String.format 꼴 → 공용 함수를 쓴 글자 틀(Platform.kt — 안드로이드와 같은 글자)"""
    out, pos = [], 0
    pat = re.compile(r'"((?:[^"\\$]|\\.)*%(?:[^"\\]|\\.)*)"\.format\(')
    while True:
        m = pat.search(text, pos)
        if not m: out.append(text[pos:]); break
        end = _call_end(text, m.end() - 1)
        if end < 0: out.append(text[pos:]); break
        fmt = m.group(1)
        args = _split_args(text[m.end():end - 1])
        pieces, k, ok, last = [], 0, True, 0
        for sm in _SPEC.finditer(fmt):
            pieces.append(fmt[last:sm.start()].replace("$", "\\$"))
            last = sm.end()
            idx, group, zero, prec, conv = sm.groups()
            if conv == "%": pieces.append("%"); continue
            ai = int(idx) - 1 if idx else k
            k += 0 if idx else 1
            if ai >= len(args): ok = False; break
            e = _spec_expr(group, zero, prec, conv, args[ai])
            if e is None: ok = False; break
            pieces.append("${" + e + "}")
        pieces.append(fmt[last:].replace("$", "\\$"))
        if not ok or "%" in "".join(p for p in pieces if not p.startswith("${")).replace("%", "", fmt.count("%%")):
            out.append(text[pos:end]); pos = end; continue
        out.append(text[pos:m.start()]); out.append('"' + "".join(pieces) + '"'); pos = end
    return "".join(out)


# 쓰지 않게 된 안드로이드 Context — 선언만 남았으면 지운다
_CTX = r"val context = (?:androidx\.compose\.ui\.platform\.)?LocalContext\.current(?:\.applicationContext)?\n"
if len(re.findall(r"\bcontext\b", s)) == len(re.findall(_CTX, s)):
    s = re.sub(r"[ \t]*" + _CTX, "", s)
s = s.replace("import java.util.Locale\n", "")
if "LocalContext" not in s.replace("import androidx.compose.ui.platform.LocalContext\n", ""):
    s = s.replace("import androidx.compose.ui.platform.LocalContext\n", "")

def _string_format(text):
    """String.format(Locale.X, "…", a) · String.format("…", a) → "…".format(a) — 이어서 _formats 가 바꾼다"""
    pat = re.compile(r'(?:java\.lang\.)?String\.format\(\s*(?:(?:java\.util\.)?Locale\.\w+\s*,\s*)?(?=")')
    out, pos = [], 0
    while True:
        m = pat.search(text, pos)
        if not m: out.append(text[pos:]); break
        end = _call_end(text, m.end() - 1 - (len(m.group(0)) - len(m.group(0).rstrip())) if False else text.index("(", m.start()))
        if end < 0: out.append(text[pos:]); break
        inner = text[m.end():end - 1]
        args = _split_args(inner)
        out.append(text[pos:m.start()])
        out.append(args[0] + ".format(" + ", ".join(args[1:]) + ")")
        pos = end
    return "".join(out)


s = _pattern_calls(s)
# 쓰는 확장 함수 · 속성 들여오기
_NEED = [
    (r"\.(?:plus|minus)(?:Days|Weeks|Months|Years)Kx\(", ["com.stepup.android.domain.plusDaysKx", "com.stepup.android.domain.minusDaysKx",
        "com.stepup.android.domain.plusWeeksKx", "com.stepup.android.domain.minusWeeksKx", "com.stepup.android.domain.plusMonthsKx",
        "com.stepup.android.domain.minusMonthsKx", "com.stepup.android.domain.plusYearsKx", "com.stepup.android.domain.minusYearsKx"]),
    (r"\.formatPattern\(", ["com.stepup.android.domain.formatPattern"]),
    (r"\.displayName\(com\.stepup\.android\.domain\.DateNameStyle", ["com.stepup.android.domain.displayName"]),
    (r"\.toLocalDateTime\(", ["kotlinx.datetime.toLocalDateTime"]),
    (r"\.month\.number\b", ["kotlinx.datetime.number"]),
    (r"\.isoDayNumber\b", ["kotlinx.datetime.isoDayNumber"]),
    (r"\.toEpochDays\(\)", []),
    (r"\bcodePointLength\(\)", ["com.stepup.android.domain.codePointLength"]),
]
for rx, names in _NEED:
    if re.search(rx, s):
        for n in names:
            if f"import {n}\n" not in s:
                s = re.sub(r"\n(import [^\n]+\n)", f"\nimport {n}\n\\1", s, count=1)
s = _string_format(s)
s = _formats(s)
dst.parent.mkdir(parents=True, exist_ok=True)
dst.write_text(s)
print(f"{src} → {dst}")

