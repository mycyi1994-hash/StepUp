#!/usr/bin/env python3
"""러닝 이벤트 소식을 모아 Supabase 의 news_items 표에 넣는다.

매일 아침 9시(KST)에 GitHub Actions 가 부른다.
(.github/workflows/news-refresh.yml)

── 왜 이렇게 하는가 ────────────────────────────────────────────────

앱이 바깥 사이트를 직접 읽으면 세 가지가 무너진다.

  1. 기기마다 파싱한다. 사이트가 모양을 바꾸면 모든 폰이 한꺼번에 깨지고,
     고치려면 새 APK 를 내야 한다.
  2. 사용자 수만큼 남의 서버를 두드린다. 차단당할 짓이다.
  3. 폰이 켜져 있어야 갱신된다. "매일 아침 9시"가 지켜지지 않는다.

그래서 하루 한 번 **한 곳에서** 모아 표에 넣고, 앱은 그 표만 읽는다.

── 드는 돈 ─────────────────────────────────────────────────────────

  * 모으는 일    GitHub Actions. 하루 한 번 1분 남짓이라 무료 한도 안이다.
  * 담아 두는 곳 이미 쓰고 있는 Supabase 표. 수십 줄이라 용량은 없는 셈.
  * 읽는 일      앱이 하루 한 번 몇 KB 를 받는다.

서버를 새로 띄우지 않았으므로 **새로 나가는 돈이 없다.**

── 어디서 가져오는가 ───────────────────────────────────────────────

구글 뉴스 RSS 다. 키가 필요 없고, 검색어로 받을 수 있고, 한 곳만 두드리면
여러 매체가 함께 온다. 매체마다 RSS 주소를 모아 두면 그중 하나가 주소를
바꿀 때마다 이 파일을 고쳐야 하는데, 그 일이 제일 자주 깨진다.

── 저작권 ──────────────────────────────────────────────────────────

본문은 가져오지 않는다. 제목 · 출처 · 날짜 · 원문 링크까지만 담고, 읽으려면
원문으로 보낸다. 요약도 원문이 준 것만 쓰고 지어내지 않는다.

── 쓰는 법 ─────────────────────────────────────────────────────────

    python3 scripts/news-fetch.py --self-test   # 파서 검사 (네트워크 없이)
    python3 scripts/news-fetch.py --dry-run     # 모아서 화면에 찍기만
    python3 scripts/news-fetch.py               # 모아서 표에 넣기

표에 넣으려면 두 가지가 환경 변수로 있어야 한다.

    SUPABASE_URL          https://xxxx.supabase.co
    SUPABASE_SERVICE_KEY  service_role 키

**service_role 키는 저장소에 적지 않는다.** GitHub 의 Settings → Secrets
에만 둔다. 이 키는 RLS 를 지나가므로 유출되면 표 전체를 남이 고칠 수 있다.
"""

from __future__ import annotations

import argparse
import datetime as dt
import email.utils
import hashlib
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

UA = "StepUp-news-bot/1.0 (+https://github.com/mycyi1994-hash/StepUp)"

# 검색어. 하나가 비어도 나머지가 채우도록 여럿을 둔다.
#
# "대회"를 넣는 이유는 러닝 이벤트 탭이기 때문이다. 러닝 일반 기사는 건강
# 뉴스 쪽이지 이벤트가 아니다.
QUERIES = [
    "마라톤 대회",
    "러닝 대회 참가 접수",
    "마라톤 참가자 모집",
    "러닝 페스티벌",
]

# 제목에 이 중 하나는 있어야 이벤트로 친다. 검색어만 믿으면 "마라톤 협상"
# 같은 비유 표현이 섞여 들어온다.
KEEP = ("마라톤", "러닝", "달리기", "러너", "하프", "완주", "레이스", "10K", "5K")

# 이것이 제목에 있으면 버린다. 스포츠 기사의 비유와 부고·사고 기사다.
DROP = ("협상 마라톤", "마라톤 회의", "마라톤 협상", "마라톤회의", "숨져", "사망")

MAX_ITEMS = 40
MAX_AGE_DAYS = 30
# 표에 남겨 둘 기간. 이보다 오래된 줄은 지운다 — 표가 무한히 자라지 않게.
KEEP_DAYS = 60


class Item:
    __slots__ = ("url", "title", "source", "summary", "published_at")

    def __init__(self, url: str, title: str, source: str, summary: str, published_at: dt.datetime):
        self.url = url
        self.title = title
        self.source = source
        self.summary = summary
        self.published_at = published_at

    def row(self) -> dict:
        return {
            "url": self.url,
            "title": self.title,
            "source": self.source,
            "summary": self.summary,
            "kind": "RUN_EVENT",
            "published_at": self.published_at.astimezone(dt.timezone.utc).isoformat(),
        }

    def __repr__(self) -> str:  # 사람이 --dry-run 으로 볼 때만 쓴다
        return f"{self.published_at:%Y-%m-%d} [{self.source}] {self.title}"


# ── 읽기 ────────────────────────────────────────────────────────────


def google_news_url(query: str) -> str:
    q = urllib.parse.quote(query)
    return f"https://news.google.com/rss/search?q={q}&hl=ko&gl=KR&ceid=KR:ko"


def fetch(url: str, timeout: int = 20) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.read()


# ── 파싱 ────────────────────────────────────────────────────────────


def text_of(node, *names: str) -> str:
    """RSS 와 Atom 이 태그 이름을 달리 쓰므로 여러 이름을 차례로 본다."""
    for name in names:
        found = node.find(name)
        if found is not None:
            if found.text:
                return found.text.strip()
            href = found.get("href")
            if href:
                return href.strip()
    return ""


def strip_tags(s: str) -> str:
    """요약에 섞여 오는 HTML 을 걷어낸다. 화면에 <a href=...> 를 보여 줄 수는 없다."""
    s = re.sub(r"<[^>]+>", " ", s)
    s = re.sub(r"&nbsp;?", " ", s)
    s = s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", '"')
    return re.sub(r"\s+", " ", s).strip()


def parse_date(raw: str) -> dt.datetime | None:
    raw = (raw or "").strip()
    if not raw:
        return None
    # RSS 는 RFC 2822 ("Mon, 15 Sep 2025 09:00:00 GMT")
    try:
        parsed = email.utils.parsedate_to_datetime(raw)
        if parsed is not None:
            if parsed.tzinfo is None:
                parsed = parsed.replace(tzinfo=dt.timezone.utc)
            return parsed
    except (TypeError, ValueError):
        pass
    # Atom 은 ISO 8601 ("2025-09-15T09:00:00Z")
    try:
        return dt.datetime.fromisoformat(raw.replace("Z", "+00:00"))
    except ValueError:
        return None


def parse_feed(data: bytes, fallback_source: str = "") -> list[Item]:
    """RSS 2.0 과 Atom 을 함께 읽는다. 한 줄이 깨져도 나머지는 살린다."""
    try:
        root = ET.fromstring(data)
    except ET.ParseError:
        return []

    atom = "{http://www.w3.org/2005/Atom}"
    entries = root.findall(".//item") or root.findall(f".//{atom}entry")

    out: list[Item] = []
    for node in entries:
        title = strip_tags(text_of(node, "title", f"{atom}title"))
        link = text_of(node, "link", f"{atom}link")
        if not title or not link:
            continue

        published = parse_date(
            text_of(node, "pubDate", "published", f"{atom}published", f"{atom}updated")
        )
        if published is None:
            continue

        # 구글 뉴스는 <source> 에 매체 이름을 넣어 준다. 없으면 링크의 도메인.
        source = text_of(node, "source", f"{atom}source")
        if not source:
            host = urllib.parse.urlparse(link).netloc
            source = host[4:] if host.startswith("www.") else host
        if not source:
            source = fallback_source

        summary = strip_tags(text_of(node, "description", "summary", f"{atom}summary"))
        # 구글 뉴스의 description 은 기사 목록 HTML 이라 요약이 아니다.
        # 제목이 그대로 들어 있으면 요약으로 치지 않는다.
        if title[:20] and title[:20] in summary:
            summary = ""
        if len(summary) > 300:
            summary = summary[:297].rstrip() + "…"

        out.append(Item(link, title, source, summary, published))
    return out


def wanted(item: Item, now: dt.datetime) -> bool:
    title = item.title
    if any(bad in title for bad in DROP):
        return False
    if not any(good in title for good in KEEP):
        return False
    age = now - item.published_at
    return dt.timedelta(0) <= age <= dt.timedelta(days=MAX_AGE_DAYS)


def dedupe(items: list[Item]) -> list[Item]:
    """같은 기사가 검색어마다 한 번씩 오므로 추린다.

    주소가 같으면 당연히 같은 글이고, 주소가 달라도 제목이 같으면 같은
    기사를 다른 매체가 받아쓴 것이다. 그것까지 한 줄로 친다.
    """
    seen_url: set[str] = set()
    seen_title: set[str] = set()
    out: list[Item] = []
    for item in items:
        key = hashlib.sha1(re.sub(r"\s+", "", item.title).encode()).hexdigest()
        if item.url in seen_url or key in seen_title:
            continue
        seen_url.add(item.url)
        seen_title.add(key)
        out.append(item)
    return out


def collect(now: dt.datetime) -> list[Item]:
    gathered: list[Item] = []
    failed: list[str] = []
    for query in QUERIES:
        url = google_news_url(query)
        try:
            gathered += parse_feed(fetch(url))
        except (urllib.error.URLError, TimeoutError, OSError) as err:
            # 하나가 막혀도 나머지로 채운다. 전부 막히면 아래에서 멈춘다.
            failed.append(f"{query}: {err}")

    if failed:
        print(f"[warn] 가져오지 못한 검색어 {len(failed)}/{len(QUERIES)}", file=sys.stderr)
        for line in failed:
            print(f"       {line}", file=sys.stderr)
    if len(failed) == len(QUERIES):
        raise SystemExit("모든 검색어가 실패했습니다 — 표를 건드리지 않고 멈춥니다")

    items = [i for i in gathered if wanted(i, now)]
    items.sort(key=lambda i: i.published_at, reverse=True)
    return dedupe(items)[:MAX_ITEMS]


# ── 보내기 ──────────────────────────────────────────────────────────


def request_json(url: str, method: str, key: str, body: bytes | None) -> int:
    req = urllib.request.Request(url, data=body, method=method)
    req.add_header("apikey", key)
    req.add_header("Authorization", f"Bearer {key}")
    req.add_header("Content-Type", "application/json")
    # 같은 주소가 다시 오면 덮어쓴다. 제목이 고쳐지는 일이 있다.
    req.add_header("Prefer", "resolution=merge-duplicates,return=minimal")
    req.add_header("User-Agent", UA)
    with urllib.request.urlopen(req, timeout=30) as resp:
        return resp.status


def upload(items: list[Item], base_url: str, key: str, now: dt.datetime) -> None:
    endpoint = f"{base_url.rstrip('/')}/rest/v1/news_items"

    body = json.dumps([i.row() for i in items], ensure_ascii=False).encode()
    status = request_json(f"{endpoint}?on_conflict=url", "POST", key, body)
    print(f"[ok] {len(items)}줄 보냄 (HTTP {status})")

    # 오래된 줄을 지운다. 지우지 않으면 표는 늘기만 한다.
    cutoff = (now - dt.timedelta(days=KEEP_DAYS)).astimezone(dt.timezone.utc).isoformat()
    query = urllib.parse.quote(cutoff, safe="")
    status = request_json(f"{endpoint}?published_at=lt.{query}", "DELETE", key, None)
    print(f"[ok] {KEEP_DAYS}일보다 오래된 줄 정리 (HTTP {status})")


# ── 자체 검사 ───────────────────────────────────────────────────────
#
# 네트워크 없이 파서만 두드려 본다. 매일 도는 일이라, 깨진 것을 사람이
# 눈으로 알아채기까지 며칠이 걸린다 — 그 전에 CI 가 잡게 한다.

SAMPLE_RSS = """<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0"><channel>
  <item>
    <title>서울하프마라톤 참가 접수 시작</title>
    <link>https://example.com/a</link>
    <pubDate>{recent}</pubDate>
    <description>&lt;a href="x"&gt;3월 대회&lt;/a&gt; 접수가 열렸다</description>
    <source url="https://example.com">달리기신문</source>
  </item>
  <item>
    <title>여야 협상 마라톤 회의 계속</title>
    <link>https://example.com/b</link>
    <pubDate>{recent}</pubDate>
  </item>
  <item>
    <title>작년 마라톤 대회 결산</title>
    <link>https://example.com/c</link>
    <pubDate>{old}</pubDate>
  </item>
  <item>
    <title>반포 러닝 페스티벌 열린다</title>
    <link>https://www.example.org/d</link>
    <pubDate>{recent}</pubDate>
  </item>
  <item>
    <title>제목만 있고 날짜가 없는 글</title>
    <link>https://example.com/e</link>
  </item>
</channel></rss>
"""

SAMPLE_ATOM = """<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <entry>
    <title>부산 마라톤 코스 공개</title>
    <link href="https://atom.example.com/1"/>
    <published>{recent_iso}</published>
    <summary>코스가 바뀐다</summary>
  </entry>
</feed>
"""


def self_test() -> int:
    now = dt.datetime.now(dt.timezone.utc)
    recent = email.utils.format_datetime(now - dt.timedelta(days=1))
    old = email.utils.format_datetime(now - dt.timedelta(days=MAX_AGE_DAYS + 5))

    checks: list[tuple[bool, str]] = []

    rss = parse_feed(SAMPLE_RSS.format(recent=recent, old=old).encode())
    checks.append((len(rss) == 4, f"날짜 없는 글은 버린다 (읽은 줄 {len(rss)}, 기대 4)"))

    kept = [i for i in rss if wanted(i, now)]
    titles = [i.title for i in kept]
    checks.append((len(kept) == 2, f"거를 것을 거른다 (남은 줄 {len(kept)}, 기대 2): {titles}"))
    checks.append((
        all("협상" not in t for t in titles),
        "'협상 마라톤' 같은 비유는 이벤트가 아니다",
    ))
    checks.append((
        all("결산" not in t for t in titles),
        f"{MAX_AGE_DAYS}일보다 오래된 글은 버린다",
    ))

    first = kept[0]
    checks.append((first.source == "달리기신문", f"매체 이름을 읽는다 ({first.source})"))
    checks.append((
        "<a" not in first.summary and "3월 대회" in first.summary,
        f"요약의 HTML 을 걷어낸다 ({first.summary!r})",
    ))
    domain_item = [i for i in kept if i.url.endswith("/d")]
    checks.append((
        bool(domain_item) and domain_item[0].source == "example.org",
        "매체 이름이 없으면 도메인을 쓴다 (www. 는 뗀다)",
    ))

    atom = parse_feed(SAMPLE_ATOM.format(recent_iso=now.isoformat()).encode())
    checks.append((len(atom) == 1, f"Atom 도 읽는다 (읽은 줄 {len(atom)})"))
    checks.append((
        bool(atom) and atom[0].url == "https://atom.example.com/1",
        "Atom 의 링크는 href 속성에 있다",
    ))

    doubled = dedupe(rss + rss)
    checks.append((len(doubled) == len(dedupe(rss)), "같은 글이 두 번 오면 한 줄로 친다"))

    same_title = [
        Item("https://a.example/1", "같은 제목", "A", "", now),
        Item("https://b.example/2", "같은 제목", "B", "", now),
    ]
    checks.append((len(dedupe(same_title)) == 1, "주소가 달라도 제목이 같으면 한 줄로 친다"))

    checks.append((parse_feed(b"<html>not a feed</html>") == [], "피드가 아니면 빈 목록을 준다"))
    checks.append((parse_feed(b"\x00\x01broken") == [], "깨진 응답에도 죽지 않는다"))

    row = first.row()
    checks.append((row["kind"] == "RUN_EVENT", "보내는 줄의 갈래는 러닝 이벤트다"))
    checks.append((row["published_at"].endswith("+00:00"), "날짜는 UTC 로 보낸다"))

    failed = 0
    for ok, label in checks:
        print(f"  {'OK  ' if ok else 'FAIL'} {label}")
        if not ok:
            failed += 1

    print()
    if failed:
        print(f"✗ {failed}개 실패")
        return 1
    print(f"✓ {len(checks)}개 모두 통과")
    return 0


# ── 실행 ────────────────────────────────────────────────────────────


def main() -> int:
    parser = argparse.ArgumentParser(description="러닝 이벤트 소식을 모아 Supabase 에 넣는다")
    parser.add_argument("--dry-run", action="store_true", help="표에 넣지 않고 화면에 찍기만")
    parser.add_argument("--self-test", action="store_true", help="네트워크 없이 파서만 검사")
    args = parser.parse_args()

    if args.self_test:
        return self_test()

    now = dt.datetime.now(dt.timezone.utc)
    items = collect(now)

    if not items:
        # 빈 목록으로 표를 덮으면 어제까지 잘 보이던 소식이 사라진다.
        # 아무것도 안 하는 편이 낫다.
        print("[warn] 조건에 맞는 소식이 없습니다 — 표를 건드리지 않습니다", file=sys.stderr)
        return 0

    if args.dry_run:
        for item in items:
            print(item)
        print(f"\n{len(items)}줄")
        return 0

    base_url = os.environ.get("SUPABASE_URL", "").strip()
    key = os.environ.get("SUPABASE_SERVICE_KEY", "").strip()
    if not base_url or not key:
        print("SUPABASE_URL 과 SUPABASE_SERVICE_KEY 가 필요합니다", file=sys.stderr)
        return 2

    upload(items, base_url, key, now)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
