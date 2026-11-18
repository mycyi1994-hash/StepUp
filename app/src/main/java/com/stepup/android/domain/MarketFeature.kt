package com.stepup.android.domain

/**
 * 거래소 거래 멈춤(2026-11-17, 사용자 "일단 지금 판매하기 버튼도 비활성화 해놔야 함. 거래 하면 안 돼").
 *
 * false 면 앱이 판매하기 · 판매 등록 · 구매 입찰 · 즉시 구매 · 즉시 판매를 막고 까닭을 한 줄 보인다. 시세 · 매물 보기와
 * 올린 매물 · 걸어 둔 입찰 거두기는 그대로다. 정본은 서버 설정 `market_trading_enabled`(0108, 기본 false) — 서버가 막으므로
 * 이 값을 모르는 예전 앱에서도 거래가 일어나지 않는다. 다시 열 때는 둘을 함께 켠다.
 */
object MarketFeature {
    const val TRADING = false
}
