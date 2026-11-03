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
s = s.replace("cachedPainterResource(", "painterResource(")
s = re.sub(r"\bMath\.(PI|sin|cos|sqrt|abs|min|max|atan2|hypot)\b", r"kotlin.math.\1", s)
s = re.sub(r"\n\s*platformStyle = PlatformTextStyle\(includeFontPadding = false\),", "", s)
dst.parent.mkdir(parents=True, exist_ok=True)
dst.write_text(s)
print(f"{src} → {dst}")
