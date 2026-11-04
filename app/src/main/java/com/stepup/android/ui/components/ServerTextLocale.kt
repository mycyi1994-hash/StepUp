package com.stepup.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * 서버 · 폰이 한국어로만 적어 둔 글(원장 줄의 설명 등)을 이 화면에 보일지.
 *
 * 그 글은 번역이 없어 영어 · 일본어 · 중국어 화면에 그대로 띄우면 한국어가 뜬다. 한국어 화면에서만 보이고,
 * 다른 언어에서는 줄을 숨긴다(번역된 제목은 그대로 남는다).
 */
fun showsKoreanOnlyText(language: String): Boolean = language.lowercase().startsWith("ko")

/** 지금 화면 언어로 [showsKoreanOnlyText] */
@Composable
fun showsKoreanOnlyText(): Boolean = showsKoreanOnlyText(LocalConfiguration.current.locales[0].language)
