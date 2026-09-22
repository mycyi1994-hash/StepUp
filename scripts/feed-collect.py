#!/usr/bin/env python3
"""러닝·건강 뉴스를 모아 Supabase 의 news_articles 표에 넣는다.

.github/workflows/feed-collect.yml 이 한 시간마다 부른다.

── 왜 서버에서 모으는가 ────────────────────────────────────────────

앱이 언론사를 직접 읽으면 세 가지가 무너진다.

  1. 기기마다 파싱한다. 사이트가 모양을 바꾸면 모든 폰이 한꺼번에 깨지고,
     고치려면 새 APK 를 내야 한다.
  2. 사용자 수만큼 남의 서버를 두드린다. 차단당할 짓이다.
  3. 인증 정보가 앱 번들에 들어간다. anon 키처럼 꺼내 볼 수 있게 된다.

그래서 한 곳에서 모아 표에 넣고, 앱은 그 표만 읽는다.

── 무엇을 담는가 ───────────────────────────────────────────────────

제목 · 언론사 · 발행일 · 원문 링크. 본문은 담지 않는다.

검색 API 가 주는 description 은 **검색 설명**이지 요약이 아니다. 둘을
섞으면 "본문을 읽고 정리한 것"처럼 보이는데 실제로는 읽지 않았다. 그래서
summary_type 으로 갈라 두고, 출처가 허락한 범위 안에서만 내보낸다.

── 출처 ────────────────────────────────────────────────────────────

출처와 이용 범위는 **표(content_sources)가 정한다.** 코드에 박아 두면
권한이 바뀔 때 새로 배포해야 하고, 그동안 허락되지 않은 것을 계속 가져온다.

지금 켜져 있는 어댑터가 하나도 없으면 아무것도 하지 않고 끝낸다. 이것이
정상 동작이다 — 연결되지 않았다는 사실을 숨기려고 아무 데서나 긁어 오지
않는다.

── 쓰는 법 ─────────────────────────────────────────────────────────

    python3 scripts/feed-collect.py --self-test   # 네트워크 없이 검사
    python3 scripts/feed-collect.py --dry-run     # 모아서 화면에 찍기만
    python3 scripts/feed-collect.py               # 모아서 표에 넣기

환경 변수는 .env.example 을 보라. **비밀값은 저장소에 적지 않는다.**
"""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import html
import json
import os
import re
import sys
import unicodedata
import urllib.error
import urllib.parse
import urllib.request

UA = "StepUp-feed-bot/1.0 (+https://github.com/mycyi1994-hash/StepUp)"
TIMEOUT = 20

# ── 검색어 ──────────────────────────────────────────────────────────
#
# 검색 연산자(AND/OR/따옴표)에 기대지 않는다. 지원 여부가 문서로 확인되지
# 않았고, 지원하지 않으면 조용히 엉뚱한 결과가 온다. 낱말마다 따로 묻고
# 중복을 거른다.
REQUIRED_QUERIES = ["러닝", "RUN", "Running", "달리기", "걷기", "조깅"]
EXTRA_QUERIES = [
    "마라톤", "하프마라톤", "트레일러닝", "러닝 부상", "러닝 회복",
    "운동 건강", "걷기 건강", "러닝 수분", "러닝 영양", "러닝화",
]

# ── 관련성 ──────────────────────────────────────────────────────────
#
# 낱말이 들어 있다고 러닝 기사가 아니다. "딥러닝"이 제일 흔한 오해이고,
# RUN 은 회사 이름·프로그램 실행·뱅크런으로도 쓰인다.
BLOCK_PHRASES = [
    "딥러닝", "머신러닝", "강화학습", "러닝머신 판매", "러닝타임", "런타임",
    "e러닝", "이러닝", "블렌디드러닝", "러닝메이트", "러닝 메이트",
    "뱅크런", "bank run", "드라이런", "dry run", "런칭", "롱런",
    "러닝개런티", "러닝 개런티", "숏폼", "런닝맨", "러닝맨",
]

# 제목에 이 중 하나는 있어야 운동 이야기로 친다.
SPORT_TERMS = [
    "러닝", "달리기", "마라톤", "러너", "조깅", "걷기", "걷는", "산책",
    "트레일", "하프", "완주", "레이스", "페이스", "러닝화", "운동",
    "유산소", "심박", "부상", "회복", "스트레칭", "근력", "체력",
]

# 러닝 낱말이 없어도 러너에게 바로 쓸모 있는 건강 이야기는 들인다.
HEALTH_TERMS = [
    "유산소", "심폐", "무릎", "발목", "아킬레스", "족저근막", "근육통",
    "스트레칭", "수분", "탈수", "회복", "부상 예방", "관절", "혈압", "체중",
]

# 주제. 앞에 있는 것이 이기므로 좁은 것부터 둔다.
CATEGORY_RULES = [
    ("INJURY", ["부상", "무릎", "발목", "아킬레스", "족저근막", "통증", "재활"]),
    ("TRAINING", ["훈련", "러닝법", "페이스", "인터벌", "회복", "스트레칭", "근력"]),
    ("RACE_NEWS", ["대회", "마라톤 대회", "참가 접수", "완주", "우승", "출전"]),
    ("WALK_JOG", ["걷기", "걷는", "산책", "조깅"]),
    ("RUNNING", ["러닝", "달리기", "러너", "러닝화", "트레일"]),
    ("HEALTH", ["건강", "혈압", "체중", "심폐", "유산소", "수분"]),
]

MAX_PER_QUERY = 100
MAX_AGE_DAYS = 45
MIN_SCORE = 3.0


class Article:
    __slots__ = (
        "title", "publisher_name", "publisher_domain", "original_url", "fallback_url",
        "published_at", "description", "category", "keywords", "score", "source_id",
    )

    def __init__(self, **kw):
        for k in self.__slots__:
            setattr(self, k, kw.get(k))

    def row(self, rights: str) -> dict:
        """표에 넣을 한 줄.

        description 은 출처가 설명 표시를 허락한 경우에만 담는다. 담아 두고
        화면에서 가리는 방식은, 권한이 바뀌었을 때 이미 저장된 것을 되돌릴
        길이 없어 쓰지 않는다.
        """
        show_desc = rights in ("DESCRIPTION_OK", "FULL_OK")
        return {
            "title": self.title,
            "publisher_name": self.publisher_name or "",
            "publisher_domain": self.publisher_domain or "",
            "original_url": self.original_url,
            "fallback_url": self.fallback_url,
            "published_at": self.published_at.astimezone(dt.timezone.utc).isoformat()
            if self.published_at else None,
            "description": (self.description or "") if show_desc else "",
            "summary": None,
            # 본문을 읽지 않았으므로 요약이 아니다. 설명을 요약인 척하지 않는다.
            "summary_type": "SEARCH_DESCRIPTION" if (show_desc and self.description) else "NONE",
            "category": self.category,
            "keywords": self.keywords or [],
            "relevance_score": round(self.score, 2),
            "source_id": self.source_id,
            "content_hash": hashlib.sha256(
                (self.title + "|" + (self.description or "")).encode()
            ).hexdigest()[:32],
            "rights_status": rights,
            "review_status": "AUTO",
            "visibility": "PUBLIC",
        }

    def __repr__(self) -> str:
        when = f"{self.published_at:%Y-%m-%d}" if self.published_at else "날짜모름"
        return f"{when} [{self.publisher_name}] {self.title}  ({self.score:.1f})"


# ── 글 다듬기 ───────────────────────────────────────────────────────


def clean_text(raw: str) -> str:
    """검색 결과의 강조 태그와 HTML 엔티티를 걷어낸다.

    바깥 HTML 을 그대로 화면에 올리지 않는다. <b> 하나가 남으면 그 뒤로
    무엇이든 올 수 있다.
    """
    if not raw:
        return ""
    text = re.sub(r"<[^>]*>", "", raw)
    text = html.unescape(text)
    text = unicodedata.normalize("NFC", text)
    return re.sub(r"\s+", " ", text).strip()


def hostname_of(url: str) -> str:
    """주소의 실제 hostname.

    문자열 포함으로 견주면 "sbs.co.kr.evil.com" 이 SBS 로 통과한다.
    반드시 파싱해서 host 를 꺼낸다.
    """
    try:
        host = (urllib.parse.urlparse(url).hostname or "").lower()
    except ValueError:
        return ""
    return host[4:] if host.startswith("www.") else host


def domain_allowed(url: str, allowed: list[str]) -> bool:
    """허용 목록에 든 도메인인가. 공식 하위 도메인은 통과시킨다."""
    host = hostname_of(url)
    if not host:
        return False
    for d in allowed:
        d = d.strip().lower()
        if not d:
            continue
        if host == d or host.endswith("." + d):
            return True
    return False


def is_https_url(url: str) -> bool:
    if not url:
        return False
    try:
        parts = urllib.parse.urlparse(url)
    except ValueError:
        return False
    return parts.scheme in ("http", "https") and bool(parts.hostname)


def parse_rfc822(raw: str) -> dt.datetime | None:
    import email.utils
    try:
        parsed = email.utils.parsedate_to_datetime((raw or "").strip())
    except (TypeError, ValueError):
        return None
    if parsed is None:
        return None
    return parsed.replace(tzinfo=dt.timezone.utc) if parsed.tzinfo is None else parsed


# ── 관련성 판정 ─────────────────────────────────────────────────────


def relevance(title: str, description: str) -> tuple[float, str, list[str]]:
    """점수 · 주제 · 걸린 낱말.

    점수가 [MIN_SCORE] 에 못 미치면 싣지 않는다. 기준을 코드에 둔 것은
    지금 단계의 선택이고, 표로 옮길 수 있게 값만 위에 모아 두었다.
    """
    text = f"{title} {description}"
    low = text.lower()

    for bad in BLOCK_PHRASES:
        if bad.lower() in low:
            return 0.0, "RUNNING", []

    hits = [t for t in SPORT_TERMS if t in text]
    health_hits = [t for t in HEALTH_TERMS if t in text]

    if not hits and not health_hits:
        return 0.0, "RUNNING", []

    score = 0.0
    # 제목에 있는 것이 본문에 있는 것보다 무겁다. 제목은 그 기사가 무엇에
    # 관한 글인지를 말한다.
    score += 2.5 * len([t for t in SPORT_TERMS if t in title])
    score += 1.0 * len([t for t in SPORT_TERMS if t in description and t not in title])
    score += 1.2 * len([t for t in HEALTH_TERMS if t in title])
    score += 0.5 * len([t for t in HEALTH_TERMS if t in description and t not in title])

    # RUN / Running 이 홀로 쓰인 영문 제목은 운동 문맥인지 알기 어렵다.
    if re.search(r"\brun(ning)?\b", low) and not hits:
        score -= 2.0

    category = "HEALTH"
    for name, terms in CATEGORY_RULES:
        if any(t in text for t in terms):
            category = name
            break

    return score, category, sorted(set(hits + health_hits))[:8]


def recent_enough(when: dt.datetime | None, now: dt.datetime) -> bool:
    if when is None:
        # 발행일을 모르면 최신인지 알 수 없다. 최신 목록에 올리지 않는다.
        return False
    return dt.timedelta(0) <= (now - when) <= dt.timedelta(days=MAX_AGE_DAYS)


# ── 어댑터 ──────────────────────────────────────────────────────────


class NaverNewsAdapter:
    """네이버 뉴스 검색 API.

    문서: https://developers.naver.com/docs/serviceapi/search/news/news.md

    인증 정보는 서버 환경 변수로만 들어온다. 앱 번들·로그에는 넣지 않는다.
    """

    id = "naver-news"

    def __init__(self, client_id: str, client_secret: str):
        self.client_id = client_id
        self.client_secret = client_secret

    @staticmethod
    def configured() -> bool:
        return bool(os.environ.get("NAVER_CLIENT_ID") and os.environ.get("NAVER_CLIENT_SECRET"))

    def search(self, query: str, display: int = MAX_PER_QUERY) -> list[dict]:
        url = (
            "https://openapi.naver.com/v1/search/news.json"
            f"?query={urllib.parse.quote(query)}&display={display}&start=1&sort=date"
        )
        req = urllib.request.Request(url, headers={
            "X-Naver-Client-Id": self.client_id,
            "X-Naver-Client-Secret": self.client_secret,
            "User-Agent": UA,
        })
        with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
            return json.loads(resp.read().decode()).get("items", [])

    @staticmethod
    def to_article(item: dict, allowed: list[str], source_id: str) -> Article | None:
        """검색 결과 한 줄을 기사로.

        originallink 가 언론사 원문이다. 그것이 쓸 수 없을 때만 link 를 쓰되,
        **출처를 확인할 수 없는 결과를 아무 언론사로 분류하지 않는다.**
        """
        title = clean_text(item.get("title", ""))
        original = (item.get("originallink") or "").strip()
        fallback = (item.get("link") or "").strip()
        if not title:
            return None

        target = original if is_https_url(original) else ""
        if not target:
            return None
        if allowed and not domain_allowed(target, allowed):
            return None

        host = hostname_of(target)
        return Article(
            title=title,
            publisher_name="",         # 아래에서 출처 표의 이름으로 채운다
            publisher_domain=host,
            original_url=target,
            fallback_url=fallback if is_https_url(fallback) else None,
            published_at=parse_rfc822(item.get("pubDate", "")),
            description=clean_text(item.get("description", "")),
            category="RUNNING",
            keywords=[],
            score=0.0,
            source_id=source_id,
        )


# ── Supabase ────────────────────────────────────────────────────────


class Supabase:
    def __init__(self, base_url: str, service_key: str):
        self.rest = base_url.rstrip("/") + "/rest/v1"
        self.key = service_key

    def _req(self, path: str, method: str, body: bytes | None, prefer: str = "") -> tuple[int, bytes]:
        req = urllib.request.Request(self.rest + path, data=body, method=method)
        req.add_header("apikey", self.key)
        req.add_header("Authorization", f"Bearer {self.key}")
        req.add_header("Content-Type", "application/json")
        req.add_header("User-Agent", UA)
        if prefer:
            req.add_header("Prefer", prefer)
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.status, resp.read()

    def sources(self) -> list[dict]:
        _, body = self._req(
            "/content_sources?select=*&order=id", "GET", None
        )
        return json.loads(body.decode())

    def upsert_articles(self, rows: list[dict]) -> int:
        if not rows:
            return 0
        body = json.dumps(rows, ensure_ascii=False).encode()
        status, _ = self._req(
            "/news_articles?on_conflict=original_url", "POST", body,
            # 운영자가 손본 기사는 덮어쓰지 않는다 — locked 인 줄은 아래에서 뺀다.
            "resolution=merge-duplicates,return=minimal",
        )
        return status

    def locked_urls(self) -> set[str]:
        _, body = self._req(
            "/news_articles?select=original_url&or=(locked.eq.true,review_status.eq.REJECTED)",
            "GET", None,
        )
        return {r["original_url"] for r in json.loads(body.decode())}

    def mark_source(self, source_id: str, ok: bool, error: str = "") -> None:
        now = dt.datetime.now(dt.timezone.utc).isoformat()
        patch = ({"last_success_at": now, "last_error": None, "failure_streak": 0}
                 if ok else {"last_error": error[:500], "last_error_at": now})
        self._req(
            f"/content_sources?id=eq.{urllib.parse.quote(source_id)}", "PATCH",
            json.dumps(patch).encode(), "return=minimal",
        )

    def bump_failure(self, source_id: str, streak: int) -> None:
        self._req(
            f"/content_sources?id=eq.{urllib.parse.quote(source_id)}", "PATCH",
            json.dumps({"failure_streak": streak + 1}).encode(), "return=minimal",
        )


# ── 모으기 ──────────────────────────────────────────────────────────


def should_run(source: dict, now: dt.datetime) -> bool:
    """이 출처를 지금 두드려도 되는가.

    연달아 실패한 출처는 물러선다(백오프). 막힌 곳을 한 시간마다 계속
    두드리면 남의 서버에도 우리 한도에도 좋을 것이 없다.
    """
    if not source.get("enabled"):
        return False
    streak = int(source.get("failure_streak") or 0)
    interval = int(source.get("fetch_interval_minutes") or 60)
    if streak:
        interval = min(interval * (2 ** min(streak, 5)), 24 * 60)
    last = source.get("last_success_at") or source.get("last_error_at")
    if not last:
        return True
    try:
        when = dt.datetime.fromisoformat(last.replace("Z", "+00:00"))
    except ValueError:
        return True
    return (now - when) >= dt.timedelta(minutes=interval)


def rights_of(source: dict) -> str:
    if source.get("can_fetch_body") and source.get("can_summarize"):
        return "FULL_OK"
    if source.get("can_show_description"):
        return "DESCRIPTION_OK"
    return "LINK_ONLY"


def collect(sources: list[dict], now: dt.datetime, dry: bool) -> tuple[list[dict], list[str]]:
    rows: list[dict] = []
    notes: list[str] = []
    seen: set[str] = set()

    naver = next((s for s in sources if s["id"] == "naver-news"), None)
    if naver is None or not should_run(naver, now):
        notes.append("naver-news: 꺼져 있거나 아직 부를 때가 아님")
        return rows, notes
    if not naver.get("can_discover"):
        notes.append("naver-news: 검색 권한이 표에서 꺼져 있음")
        return rows, notes
    if not NaverNewsAdapter.configured():
        notes.append("naver-news: NAVER_CLIENT_ID / NAVER_CLIENT_SECRET 없음")
        return rows, notes

    # 언론사별 출처 표를 도메인으로 찾을 수 있게 미리 정리한다.
    publishers = [s for s in sources if s["id"] not in ("naver-news", "manual")]
    adapter = NaverNewsAdapter(
        os.environ["NAVER_CLIENT_ID"], os.environ["NAVER_CLIENT_SECRET"]
    )

    allowed_all: list[str] = []
    for s in publishers:
        allowed_all += list(s.get("allowed_domains") or [])

    failures = 0
    for query in REQUIRED_QUERIES + EXTRA_QUERIES:
        try:
            items = adapter.search(query)
        except (urllib.error.URLError, TimeoutError, OSError, ValueError) as err:
            failures += 1
            notes.append(f"'{query}' 실패: {err}")
            continue

        for item in items:
            art = NaverNewsAdapter.to_article(item, allowed_all, "naver-news")
            if art is None or art.original_url in seen:
                continue
            if not recent_enough(art.published_at, now):
                continue

            score, category, keywords = relevance(art.title, art.description)
            if score < MIN_SCORE:
                continue

            owner = next(
                (s for s in publishers
                 if domain_allowed(art.original_url, list(s.get("allowed_domains") or []))),
                None,
            )
            if owner is None:
                continue
            art.publisher_name = owner["name"]
            art.source_id = owner["id"]
            art.category = category
            art.keywords = keywords
            art.score = score

            seen.add(art.original_url)
            rows.append(art.row(rights_of(owner)))

    if failures == len(REQUIRED_QUERIES + EXTRA_QUERIES):
        raise RuntimeError("모든 검색어가 실패했습니다")
    return rows, notes


# ── 자체 검사 ───────────────────────────────────────────────────────

SAMPLE_ITEMS = [
    {  # 통과해야 하는 것
        "title": "<b>러닝</b> 초보가 가장 많이 다치는 무릎, 이렇게 지킨다",
        "originallink": "https://news.sbs.co.kr/news/endPage.do?news_id=1",
        "link": "https://n.news.naver.com/mnews/article/055/1",
        "description": "전문가들은 준비운동과 <b>러닝</b> 후 스트레칭을 강조했다",
        "pubDate": "",
    },
    {  # 딥러닝 — 운동과 무관
        "title": "<b>딥러닝</b> 모델 학습 시간 절반으로",
        "originallink": "https://news.kbs.co.kr/news/view.do?ncd=2",
        "link": "https://n.news.naver.com/2",
        "description": "머신러닝 연구진이 발표했다",
        "pubDate": "",
    },
    {  # 허용 목록을 흉내 낸 도메인
        "title": "걷기 운동의 효과",
        "originallink": "https://news.sbs.co.kr.evil.example/a",
        "link": "https://n.news.naver.com/3",
        "description": "하루 30분 걷기",
        "pubDate": "",
    },
    {  # 원문 주소가 없는 결과
        "title": "달리기 대회 소식",
        "originallink": "",
        "link": "https://n.news.naver.com/4",
        "description": "",
        "pubDate": "",
    },
]


def self_test() -> int:
    now = dt.datetime.now(dt.timezone.utc)
    recent = "Mon, 01 Jan 2035 09:00:00 +0900"
    checks: list[tuple[bool, str]] = []

    def ok(cond, label):
        checks.append((bool(cond), label))

    # ── 주소·도메인 ──
    ok(hostname_of("https://WWW.News.SBS.co.kr/a") == "news.sbs.co.kr",
       "hostname 을 소문자로 꺼내고 www 는 뗀다")
    ok(domain_allowed("https://news.sbs.co.kr/a", ["sbs.co.kr"]),
       "공식 하위 도메인은 통과한다")
    ok(not domain_allowed("https://news.sbs.co.kr.evil.example/a", ["sbs.co.kr"]),
       "허용 목록을 흉내 낸 도메인은 막힌다")
    ok(not domain_allowed("https://notsbs.co.kr/a", ["sbs.co.kr"]),
       "이름이 비슷한 다른 도메인은 막힌다")
    ok(not is_https_url("javascript:alert(1)"), "javascript 스킴은 주소가 아니다")
    ok(not is_https_url("data:text/html,x"), "data 스킴은 주소가 아니다")
    ok(is_https_url("https://runable.me/race"), "https 주소는 통과한다")

    # ── 글 다듬기 ──
    ok(clean_text("<b>러닝</b> &amp; 걷기") == "러닝 & 걷기",
       "강조 태그와 엔티티를 걷어낸다")
    ok("<" not in clean_text("<script>x</script>제목"), "태그가 남지 않는다")

    # ── 관련성 ──
    for bad in ["딥러닝 모델 발표", "머신러닝 강의 개설", "영화 러닝타임 2시간",
                "e러닝 플랫폼 출시", "뱅크런 우려 확산", "런닝맨 시청률"]:
        ok(relevance(bad, "")[0] < MIN_SCORE, f"오탐을 거른다 — {bad}")
    for good in ["러닝 초보 무릎 부상 예방법", "하루 30분 걷기의 혈압 효과",
                 "마라톤 완주를 위한 훈련법", "달리기 후 회복 스트레칭"]:
        ok(relevance(good, "")[0] >= MIN_SCORE, f"러닝 기사는 통과한다 — {good}")

    score, category, _ = relevance("러닝 후 무릎 통증, 재활이 먼저", "")
    ok(category == "INJURY", f"부상 기사를 부상으로 분류한다 ({category})")
    score, category, _ = relevance("하루 30분 걷기의 힘", "")
    ok(category == "WALK_JOG", f"걷기 기사를 걷기로 분류한다 ({category})")

    # ── 어댑터 ──
    allowed = ["sbs.co.kr", "kbs.co.kr"]
    items = [dict(i, pubDate=recent) for i in SAMPLE_ITEMS]
    arts = [NaverNewsAdapter.to_article(i, allowed, "naver-news") for i in items]
    ok(arts[0] is not None, "언론사 원문이 있는 결과는 기사로 만든다")
    ok(arts[0].original_url.startswith("https://news.sbs.co.kr/"),
       "originallink 를 원문 주소로 쓴다")
    ok(arts[0].publisher_domain == "news.sbs.co.kr", "출처 도메인이 원문 주소와 같다")
    ok(arts[2] is None, "흉내 낸 도메인은 기사로 만들지 않는다")
    ok(arts[3] is None, "원문 주소가 없으면 임의의 언론사로 분류하지 않는다")

    # ── 표에 넣을 줄 ──
    art = arts[0]
    art.score, art.category, art.keywords = 9.0, "INJURY", ["러닝"]
    link_only = art.row("LINK_ONLY")
    ok(link_only["description"] == "", "설명 권한이 없으면 설명을 담지 않는다")
    ok(link_only["summary_type"] == "NONE", "권한이 없으면 표시 방식도 없음이다")
    desc_ok = art.row("DESCRIPTION_OK")
    ok(desc_ok["description"] != "", "설명 권한이 있으면 설명을 담는다")
    ok(desc_ok["summary_type"] == "SEARCH_DESCRIPTION",
       "검색 설명은 검색 설명이라고 적는다 — AI 요약이 아니다")
    ok(desc_ok["summary"] is None, "본문을 읽지 않았으므로 요약은 비어 있다")

    # ── 백오프 ──
    base = {"enabled": True, "fetch_interval_minutes": 60, "failure_streak": 0}
    ok(should_run(dict(base), now), "처음이면 바로 돈다")
    ok(not should_run(dict(base, enabled=False), now), "꺼진 출처는 돌지 않는다")
    just_now = (now - dt.timedelta(minutes=5)).isoformat()
    ok(not should_run(dict(base, last_success_at=just_now), now),
       "방금 성공했으면 아직 부르지 않는다")
    ok(not should_run(dict(base, failure_streak=3,
                           last_error_at=(now - dt.timedelta(hours=2)).isoformat()), now),
       "연달아 실패한 출처는 물러선다")
    ok(should_run(dict(base, failure_streak=3,
                       last_error_at=(now - dt.timedelta(days=2)).isoformat()), now),
       "충분히 지나면 다시 시도한다")

    # ── 이용 범위 ──
    ok(rights_of({"can_show_description": False}) == "LINK_ONLY",
       "아무것도 확인되지 않았으면 링크만")
    ok(rights_of({"can_show_description": True}) == "DESCRIPTION_OK", "설명까지 허락된 경우")
    ok(rights_of({"can_fetch_body": True, "can_summarize": True}) == "FULL_OK",
       "본문과 요약이 모두 허락된 경우")
    ok(rights_of({"can_summarize": True}) != "FULL_OK",
       "본문 권한 없이 요약 권한만으로는 전체 허용이 아니다")

    failed = 0
    for good, label in checks:
        print(f"  {'OK  ' if good else 'FAIL'} {label}")
        if not good:
            failed += 1
    print()
    if failed:
        print(f"✗ {failed}개 실패")
        return 1
    print(f"✓ {len(checks)}개 모두 통과")
    return 0


# ── 실행 ────────────────────────────────────────────────────────────


def main() -> int:
    parser = argparse.ArgumentParser(description="러닝·건강 뉴스를 모은다")
    parser.add_argument("--dry-run", action="store_true", help="표에 넣지 않고 화면에 찍기만")
    parser.add_argument("--self-test", action="store_true", help="네트워크 없이 검사")
    args = parser.parse_args()

    if args.self_test:
        return self_test()

    base_url = os.environ.get("SUPABASE_URL", "").strip()
    key = os.environ.get("SUPABASE_SERVICE_KEY", "").strip()
    if not base_url or not key:
        print("SUPABASE_URL 과 SUPABASE_SERVICE_KEY 가 필요합니다", file=sys.stderr)
        return 2

    db = Supabase(base_url, key)
    now = dt.datetime.now(dt.timezone.utc)
    sources = db.sources()

    enabled = [s["id"] for s in sources if s.get("enabled")]
    print(f"[info] 켜진 출처: {', '.join(enabled) or '없음'}")

    try:
        rows, notes = collect(sources, now, args.dry_run)
    except Exception as err:  # noqa: BLE001 — 실패해도 표를 건드리지 않고 기록만 남긴다
        print(f"[error] 수집 실패: {err}", file=sys.stderr)
        naver = next((s for s in sources if s["id"] == "naver-news"), None)
        if naver and not args.dry_run:
            db.mark_source("naver-news", False, str(err))
            db.bump_failure("naver-news", int(naver.get("failure_streak") or 0))
        # 직전 정상 데이터는 그대로 둔다. 비워 버리면 어제 보이던 것까지 사라진다.
        return 0

    for note in notes:
        print(f"[note] {note}")

    if not rows:
        print("[info] 새로 담을 기사가 없습니다 — 표를 건드리지 않습니다")
        return 0

    if args.dry_run:
        for row in rows[:40]:
            print(f"  {row['published_at']} [{row['publisher_name']}] {row['title']}")
        print(f"\n{len(rows)}건")
        return 0

    # 운영자가 손본 기사는 덮어쓰지 않는다.
    locked = db.locked_urls()
    keep = [r for r in rows if r["original_url"] not in locked]
    status = db.upsert_articles(keep)
    print(f"[ok] {len(keep)}건 보냄 (HTTP {status}), 운영자 보호 {len(rows) - len(keep)}건 건너뜀")
    db.mark_source("naver-news", True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
