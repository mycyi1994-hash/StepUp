package com.stepup.android.ui.theme;

import androidx.compose.runtime.ProvidableCompositionLocal;
import org.jetbrains.compose.resources.ComposeEnvironment;
import org.jetbrains.compose.resources.DensityQualifier;
import org.jetbrains.compose.resources.LanguageQualifier;
import org.jetbrains.compose.resources.RegionQualifier;
import org.jetbrains.compose.resources.ResourceEnvironment;
import org.jetbrains.compose.resources.ResourceEnvironmentKt;
import org.jetbrains.compose.resources.ThemeQualifier;

/**
 * Compose 리소스가 고를 언어 · 밤낮 · 밀도를 화면이 정해 주는 다리(2026-11-03 C8).
 * 라이브러리는 이 자리(LocalComposeEnvironment)를 Kotlin 안쪽(internal)으로만 열어 두어 Java 로 잇는다 — 바이트코드는 공개다.
 * 안드로이드 stringResource 처럼 화면의 설정(LocalConfiguration)을 따르게 하려는 것뿐, 리소스를 고르는 규칙은 라이브러리 그대로다.
 */
public final class ComposeResourcesBridge {
    private ComposeResourcesBridge() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ProvidableCompositionLocal<Object> local() {
        return (ProvidableCompositionLocal) ResourceEnvironmentKt.getLocalComposeEnvironment();
    }

    public static Object environment(String language, String region, boolean dark, float density) {
        final ResourceEnvironment environment = new ResourceEnvironment(
            new LanguageQualifier(language),
            new RegionQualifier(region),
            ThemeQualifier.Companion.selectByValue(dark),
            DensityQualifier.Companion.selectByDensity(density)
        );
        return (ComposeEnvironment) (composer, changed) -> environment;
    }
}
