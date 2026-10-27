#!/usr/bin/env python3
"""화면 글자에 개발자 말투가 다시 들어오지 않게 막는다(2026-10-26 사용자 "데모 · 없음 · 개발자들이 볼 만한 말 다 빼").

values*/ 의 <string> 값에서 서버 · 데모 · 디버그 · 테스트 · "준비 중" 같은 말을 찾는다. 사람에게 보이지 않거나(디버그 · 테스트 빌드 전용,
데모 모드 안, 블록체인 판 전용) 써야 하는 곳(개인정보 안내 · 계정 삭제)은 ALLOW 에 이름을 적어 둔다 — 새 글자는 사람 말로 쓴다.

    python3 scripts/check-copy.py
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res"

BANNED = {
    "values-ko": [r"서버", r"데모", r"디버그", r"테스트", r"준비 ?중"],
    "values": [r"\bserver\b", r"\bdemo\b", r"\bdebug\b", r"\bcoming soon\b"],
    "values-ja": [r"サーバー", r"デモ", r"デバッグ", r"準備中"],
    "values-zh": [r"服务器", r"演示", r"调试", r"准备中"],
}

# 이 이름들은 그대로 둔다 — 까닭은 줄 끝에
ALLOW = {
    # 개인정보 · 계정 삭제 안내: 어디에 보관되는지 사실대로 적어야 한다
    "set_stored_body", "set_delete_heading", "privacy_data_body",
    # 데모 모드 안에서만 보인다(설정의 데모 줄은 이미 켠 사람에게만 보인다)
    "settings_demo", "settings_demo_note", "demo_badge", "demo_publisher", "feed_demo_note", "market_outfit_try",
    "market_demo_note", "market_demo_toast", "customize_trial_on",
    # 디버그 · 테스트 빌드 전용(TEST_TOOLS · SELF_UPDATE)
    "run_simulate", "finish_simulated", "test_update_body", "test_update_row", "test_update_failed_signature",
    # 블록체인 판 전용(스토어 판 WEB3 = false 에서는 숨는다)
    "ca_guide_intro", "about_network_value", "wl_web3_unavailable", "wl_web3_soon", "wc_soon",
    # 예전 화면(캐릭터 · 옛 상점 · 옛 소식 · 옛 시작 메뉴 배지 — 지금 길에서는 열리지 않는다)
    "store_soon", "feed_soon", "journey_soon", "market_outfit_soon", "feed_deal_store_title", "avatar_art_base_look", "customize_preview_missing_badge",
    "customize_equipped_art_pending", "avatar_art_base_shoes", "avatar_art_base_outfit",
    # 러닝 시작 메뉴의 아직 열지 않은 방법(챌린지 · 코스 · 다이어트 · 코칭) — 자물쇠와 "준비 중"을 보이라는 사용자 결정(2026-10-27)
    "runflow_soon", "runflow_soon_toast",
    # 주간 포인트 판은 서버가 순위를 줄 때만 보인다(없으면 판째 숨는다)
    "lb_points_unavailable_title", "lb_points_unavailable_body",
}

STRING = re.compile(r'<string name="([^"]+)"[^>]*>(.*?)</string>', re.S)


def main() -> int:
    problems = []
    for folder, words in BANNED.items():
        pattern = re.compile("|".join(words), re.I)
        for path in sorted((ROOT / folder).glob("*.xml")):
            text = re.sub(r"<!--.*?-->", "", path.read_text(encoding="utf-8"), flags=re.S)
            for name, value in STRING.findall(text):
                if name in ALLOW:
                    continue
                hit = pattern.search(value)
                if hit:
                    problems.append(f"{folder}/{path.name}: {name} — \"{hit.group(0)}\" · {value.strip()[:80]}")
    if problems:
        print("화면 글자에 개발자 말투가 있어요. 사람 말로 바꾸거나, 보이지 않는 글자면 scripts/check-copy.py 의 ALLOW 에 까닭과 함께 적어 주세요.")
        print("\n".join(problems))
        return 1
    print("화면 글자 말투 이상 없음")
    return 0


if __name__ == "__main__":
    sys.exit(main())
