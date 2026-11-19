// push-send — 보낼 푸시 목록(public.push_outbox)을 비우며 FCM 으로 보낸다.
//
// 누구에게 무엇을 보낼지는 데이터베이스(0017_push_outbox.sql)가 정한다. 여기서는
//   1. 한 묶음을 가져가고(push_claim_batch — 동시에 깨어나도 겹치지 않는다)
//   2. 받는 사람의 언어로 글을 짓고
//   3. 그 사람의 폰마다 FCM HTTP v1 으로 보낸 뒤
//   4. 보냈다고 적는다(push_mark). 앱을 지운 폰은 토큰을 지운다.
//
// 깨우는 쪽: Database Webhook(push_outbox INSERT) 또는 1분 cron.
//
// 필요한 비밀(Supabase › Edge Functions › Secrets):
//   FCM_SERVICE_ACCOUNT   Firebase 서비스 계정 키 JSON 전체
//   PUSH_WEBHOOK_SECRET   깨우는 쪽이 x-stepup-secret 헤더로 보낼 임의의 긴 문자열
// SUPABASE_URL · SUPABASE_SERVICE_ROLE_KEY 는 Supabase 가 넣어 준다.

import { createClient } from "jsr:@supabase/supabase-js@2";

type Row = {
  id: number;
  user_id: string;
  kind: string;
  args: Record<string, string>;
  link: string;
  tokens: { token: string; locale: string }[];
};

type ServiceAccount = { project_id: string; client_email: string; private_key: string };

// 아이템 이름(0106) — 운영자 지급 푸시가 받는 사람의 언어로 적는다. 종류를 더하면 여기도(모르는 종류는 '아이템')
const ITEM_NAME: Record<string, Record<string, string>> = {
  "ko": {
    "RUN_BOOSTER": "러닝 부스터",
    "REWARD_DOUBLE": "보상 2배권",
    "ENERGY_DRINK": "에너지 드링크",
    "DURABILITY_RESET": "내구도 초기화권",
    "ENERGY_RESET": "하루 할당량 초기화권"
  },
  "en": {
    "RUN_BOOSTER": "Run Booster",
    "REWARD_DOUBLE": "Double Reward Ticket",
    "ENERGY_DRINK": "Energy Drink",
    "DURABILITY_RESET": "Durability Reset",
    "ENERGY_RESET": "Daily Energy Reset"
  },
  "ja": {
    "RUN_BOOSTER": "ランブースター",
    "REWARD_DOUBLE": "報酬2倍券",
    "ENERGY_DRINK": "エナジードリンク",
    "DURABILITY_RESET": "耐久度リセット券",
    "ENERGY_RESET": "1日エナジーリセット券"
  },
  "zh": {
    "RUN_BOOSTER": "跑步加速器",
    "REWARD_DOUBLE": "奖励双倍券",
    "ENERGY_DRINK": "能量饮料",
    "DURABILITY_RESET": "耐久度重置券",
    "ENERGY_RESET": "每日能量重置券"
  }
};
const itemName = (lang: string, kind: string) => (ITEM_NAME[lang] ?? ITEM_NAME.ko)[kind] ?? ({ ko: "아이템", en: "an item", ja: "アイテム", zh: "道具" }[lang] ?? "아이템");

// 거래소 거두기 알림(0112 MARKET_RECLAIMED) — 매물만 · 입찰만 · 둘 다. a.listings · a.bids 는 개수, a.sup 은 돌려준 SUP(쉼표 넣은 글)
const reclaimed = (a: Record<string, string>, both: string, listings: string, bids: string) =>
  a.listings !== "0" && a.bids !== "0" ? both : a.listings !== "0" ? listings : bids;
const plural = (n: string, one: string, many: string) => (n === "1" ? one : many);

const TEXT: Record<string, Record<string, (a: Record<string, string>) => [string, string]>> = {
  ko: {
    MARKET_RECLAIMED: (a) => ["거래소를 잠시 쉬어요", reclaimed(a,
      `판매 중이던 신발 ${a.listings}켤레와 입찰에 묶였던 ${a.sup} SUP를 돌려드렸어요`,
      `판매 중이던 신발 ${a.listings}켤레를 내 신발로 돌려드렸어요`,
      `구매 입찰에 묶였던 ${a.sup} SUP를 돌려드렸어요`)],
    UPDATE_REWARD: (a) => ["업데이트 선물이 도착했어요 🎁", `신발 뽑기 ${a.free}회와 상급 뽑기 ${a.premium}회를 받았어요. 뽑기에서 확인해 보세요`],
    REWARD_RULES: (a) => ["러닝 상자 · 목표 보너스 기준이 바뀌었어요", `밖에서, 닳지 않은 신발로 달린 걸음으로 세요. 하루 목표는 4,000보부터, 바꾸면 다음 날부터예요(지금 목표 ${a.goal}보)`],
    ITEM_GRANTED: (a) => ["아이템이 도착했어요 🎁", `${itemName("ko", a.item)} ${a.qty}개를 받았어요. 아이템에서 확인해 보세요`],
    REWARD_EVENT: (a) => [`지금부터 ${a.hours}시간 보상 ${a.mult}배 🔥`, `${a.title} — 지금 달리면 적립이 ${a.mult}배예요`],
    WALLET_LINKED: (a) => ["지갑이 연결됐어요 🔐", `${a.address} 지갑이 계정에 연결됐어요. 본인이 아니라면 바로 고객센터에 알려 주세요`],
    COMMENT: (a) => ["새 댓글", `${a.name}님이 "${a.title}"에 댓글을 남겼어요`],
    REPLY: (a) => ["새 답글", `${a.name}님이 내 댓글에 답글을 남겼어요`],
    CREW_REQUEST: (a) => ["가입 신청", `${a.name}님이 ${a.crew} 크루에 가입을 신청했어요`],
    CREW_DECIDED: (a) => a.approved === "1"
      ? ["가입 승인 🎉", `${a.crew} 크루에 가입됐어요. 같이 달려요!`]
      : ["가입 결과", `${a.crew} 크루 가입이 이번엔 승인되지 않았어요`],
    // 출발(0117) — a.flash "1" 이면 번개러닝, a.title 은 크루 이름 · 번개 글 제목
    PARTY_STARTED: (a) => a.flash === "1"
      ? ["번개러닝이 출발했어요 🏃", `${a.title} — 지금 앱을 열면 함께 달려요`]
      : ["크루 러닝이 출발했어요 🏃", `${a.title} — 지금 앱을 열면 함께 달려요`],
    PARTY_MISSED: (a) => a.flash === "1"
      ? ["번개러닝이 출발했어요", `${a.title} — 대기실 화면을 보고 있지 않아 이번엔 함께 출발하지 못했어요`]
      : ["크루 러닝이 출발했어요", `${a.title} — 대기실 화면을 보고 있지 않아 이번엔 함께 출발하지 못했어요. 오늘 크루 러닝은 그대로 남아 있어요`],
    PARTY_OPEN: (a) => ["크루 러닝 대기실이 열렸어요 🏃", `${a.crew} 크루 러닝 대기실이 열렸어요. ${a.name}님과 같이 달려요!`],
    CREW_FLASH: (a) => ["크루 번개 ⚡", `${a.name}: ${a.title}`],
    CREW_CHAT: (a) => [a.crew, a.photo === "1" ? `${a.name}: 사진을 보냈어요` : `${a.name}: ${a.text}`],
  },
  en: {
    MARKET_RECLAIMED: (a) => ["The market is taking a short break", reclaimed(a,
      `We returned your ${a.listings} listed ${plural(a.listings, "shoe", "shoes")} and the ${a.sup} SUP held in your bids`,
      `We moved your ${a.listings} listed ${plural(a.listings, "shoe", "shoes")} back to My Shoes`,
      `We returned the ${a.sup} SUP held in your bids`)],
    UPDATE_REWARD: (a) => ["Your update gift arrived 🎁", `You got ${a.free} shoe ${plural(a.free, "draw", "draws")} and ${a.premium} premium ${plural(a.premium, "draw", "draws")}. Check them in Draw`],
    REWARD_RULES: (a) => ["Run boxes and the goal bonus now count differently", `Only steps from outdoor runs with a shoe that isn't worn out count. Daily goals start at 4,000 steps and changes apply from the next day (your goal: ${a.goal})`],
    ITEM_GRANTED: (a) => ["An item arrived 🎁", `You received ${a.qty} × ${itemName("en", a.item)}. Check it in Items`],
    REWARD_EVENT: (a) => [`${a.mult}x rewards for the next ${a.hours}h 🔥`, `${a.title} — run now and earn ${a.mult}x`],
    WALLET_LINKED: (a) => ["Wallet linked 🔐", `Wallet ${a.address} was linked to your account. If this wasn't you, contact support now`],
    COMMENT: (a) => ["New comment", `${a.name} commented on "${a.title}"`],
    REPLY: (a) => ["New reply", `${a.name} replied to your comment`],
    CREW_REQUEST: (a) => ["Join request", `${a.name} wants to join ${a.crew}`],
    CREW_DECIDED: (a) => a.approved === "1"
      ? ["Request approved 🎉", `You're now a member of ${a.crew}. Let's run together!`]
      : ["Join request update", `Your request to join ${a.crew} wasn't approved this time`],
    PARTY_STARTED: (a) => a.flash === "1"
      ? ["Your flash run has started 🏃", `${a.title} — open the app now to run together`]
      : ["Your crew run has started 🏃", `${a.title} — open the app now to run together`],
    PARTY_MISSED: (a) => a.flash === "1"
      ? ["Your flash run has started", `${a.title} — you weren't on the lobby screen, so you didn't start together this time`]
      : ["Your crew run has started", `${a.title} — you weren't on the lobby screen, so you didn't start together this time. Today's crew run is still yours`],
    PARTY_OPEN: (a) => ["Crew run lobby is open 🏃", `A ${a.crew} crew run lobby is open. Run with ${a.name}!`],
    CREW_FLASH: (a) => ["Crew flash run ⚡", `${a.name}: ${a.title}`],
    CREW_CHAT: (a) => [a.crew, a.photo === "1" ? `${a.name}: sent a photo` : `${a.name}: ${a.text}`],
  },
  ja: {
    MARKET_RECLAIMED: (a) => ["マーケットはしばらくお休みします", reclaimed(a,
      `出品中のシューズ${a.listings}足と入札で預かっていた${a.sup} SUPをお返ししました`,
      `出品中のシューズ${a.listings}足をマイシューズにお返ししました`,
      `購入入札で預かっていた${a.sup} SUPをお返ししました`)],
    UPDATE_REWARD: (a) => ["アップデートのプレゼントが届きました 🎁", `シューズガチャ${a.free}回と上級ガチャ${a.premium}回を受け取りました。ガチャで確認してください`],
    REWARD_RULES: (a) => ["ランボックスと目標ボーナスの数え方が変わりました", `屋外で、すり減っていないシューズで走った歩数で数えます。1日の目標は4,000歩から、変更は翌日から反映されます(今の目標 ${a.goal}歩)`],
    ITEM_GRANTED: (a) => ["アイテムが届きました 🎁", `${itemName("ja", a.item)}を${a.qty}個受け取りました。アイテムで確認してください`],
    REWARD_EVENT: (a) => [`今から${a.hours}時間、報酬${a.mult}倍 🔥`, `${a.title} — 今走ると獲得が${a.mult}倍です`],
    WALLET_LINKED: (a) => ["ウォレットを連携しました 🔐", `ウォレット ${a.address} がアカウントに連携されました。心当たりがない場合はすぐにサポートへご連絡ください`],
    COMMENT: (a) => ["新しいコメント", `${a.name}さんが「${a.title}」にコメントしました`],
    REPLY: (a) => ["新しい返信", `${a.name}さんがあなたのコメントに返信しました`],
    CREW_REQUEST: (a) => ["参加申請", `${a.name}さんが${a.crew}への参加を申請しました`],
    CREW_DECIDED: (a) => a.approved === "1"
      ? ["参加が承認されました 🎉", `${a.crew}のメンバーになりました。一緒に走りましょう！`]
      : ["参加申請の結果", `今回は${a.crew}への参加が承認されませんでした`],
    PARTY_STARTED: (a) => a.flash === "1"
      ? ["フラッシュランがスタートしました 🏃", `${a.title} — 今アプリを開くと一緒に走れます`]
      : ["クルーランがスタートしました 🏃", `${a.title} — 今アプリを開くと一緒に走れます`],
    PARTY_MISSED: (a) => a.flash === "1"
      ? ["フラッシュランがスタートしました", `${a.title} — 待機室の画面を見ていなかったため、今回は一緒にスタートできませんでした`]
      : ["クルーランがスタートしました", `${a.title} — 待機室の画面を見ていなかったため、今回は一緒にスタートできませんでした。今日のクルーランはそのまま残っています`],
    PARTY_OPEN: (a) => ["クルーランの待機室が開きました 🏃", `${a.crew}のクルーラン待機室が開きました。${a.name}さんと一緒に走りましょう！`],
    CREW_FLASH: (a) => ["クルーのフラッシュラン ⚡", `${a.name}: ${a.title}`],
    CREW_CHAT: (a) => [a.crew, a.photo === "1" ? `${a.name}: 写真を送りました` : `${a.name}: ${a.text}`],
  },
  zh: {
    MARKET_RECLAIMED: (a) => ["交易所暂时休息", reclaimed(a,
      `挂单中的 ${a.listings} 双鞋子和出价冻结的 ${a.sup} SUP 已退还给你`,
      `挂单中的 ${a.listings} 双鞋子已退回我的跑鞋`,
      `出价冻结的 ${a.sup} SUP 已退还给你`)],
    UPDATE_REWARD: (a) => ["更新礼物已到账 🎁", `你获得了 ${a.free} 次鞋子抽取和 ${a.premium} 次高级抽取，请在抽取中查看`],
    REWARD_RULES: (a) => ["跑步宝箱和目标奖励的计算方式已更改", `只计算在户外、穿着未磨损的鞋子跑步的步数。每日目标从 4,000 步起，修改后从第二天开始生效（当前目标 ${a.goal} 步）`],
    ITEM_GRANTED: (a) => ["道具已到账 🎁", `你收到了 ${a.qty} 个${itemName("zh", a.item)}，请在道具中查看`],
    REWARD_EVENT: (a) => [`接下来 ${a.hours} 小时奖励 ${a.mult} 倍 🔥`, `${a.title} — 现在跑步可获得 ${a.mult} 倍奖励`],
    WALLET_LINKED: (a) => ["钱包已绑定 🔐", `钱包 ${a.address} 已绑定到你的账户。如非本人操作，请立即联系客服`],
    COMMENT: (a) => ["新评论", `${a.name} 评论了「${a.title}」`],
    REPLY: (a) => ["新回复", `${a.name} 回复了你的评论`],
    CREW_REQUEST: (a) => ["加入申请", `${a.name} 申请加入 ${a.crew}`],
    CREW_DECIDED: (a) => a.approved === "1"
      ? ["加入申请已通过 🎉", `你已加入 ${a.crew}，一起跑吧！`]
      : ["加入申请结果", `你加入 ${a.crew} 的申请这次未通过`],
    PARTY_STARTED: (a) => a.flash === "1"
      ? ["快闪跑已出发 🏃", `${a.title} — 现在打开应用就能一起跑`]
      : ["跑团跑步已出发 🏃", `${a.title} — 现在打开应用就能一起跑`],
    PARTY_MISSED: (a) => a.flash === "1"
      ? ["快闪跑已出发", `${a.title} — 你当时没在看等候室页面，这次没能一起出发`]
      : ["跑团跑步已出发", `${a.title} — 你当时没在看等候室页面，这次没能一起出发。今天的跑团跑步机会仍然保留`],
    PARTY_OPEN: (a) => ["跑团跑步等候室已开启 🏃", `${a.crew} 的跑团跑步等候室已开启，和 ${a.name} 一起跑吧！`],
    CREW_FLASH: (a) => ["跑团闪电跑 ⚡", `${a.name}: ${a.title}`],
    CREW_CHAT: (a) => [a.crew, a.photo === "1" ? `${a.name}: 发送了一张照片` : `${a.name}: ${a.text}`],
  },
};

function compose(kind: string, locale: string, args: Record<string, string>): [string, string] | null {
  const lang = TEXT[locale.slice(0, 2)] ? locale.slice(0, 2) : "ko";
  const make = TEXT[lang][kind];
  return make ? make(args) : null;
}

// ── Google 접근 토큰(서비스 계정 JWT → OAuth) ────────────────────

let cached: { token: string; expires: number } | null = null;

function base64url(bytes: Uint8Array | string): string {
  const raw = typeof bytes === "string" ? new TextEncoder().encode(bytes) : bytes;
  let bin = "";
  raw.forEach((b) => (bin += String.fromCharCode(b)));
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function accessToken(sa: ServiceAccount): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (cached && cached.expires - 60 > now) return cached.token;

  const header = base64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = base64url(JSON.stringify({
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));
  const pem = sa.private_key.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey(
    "pkcs8",
    der,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = new Uint8Array(
    await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${header}.${claims}`)),
  );
  const jwt = `${header}.${claims}.${base64url(signature)}`;

  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: jwt }),
  });
  if (!res.ok) throw new Error(`google token ${res.status}: ${await res.text()}`);
  const body = await res.json();
  cached = { token: body.access_token, expires: now + (body.expires_in ?? 3600) };
  return cached.token;
}

// ── 보내기 ─────────────────────────────────────────────────────

type SendResult = "ok" | "gone" | "error";

async function send(
  sa: ServiceAccount,
  token: string,
  title: string,
  body: string,
  link: string,
  kind: string,
): Promise<[SendResult, string]> {
  const res = await fetch(`https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`, {
    method: "POST",
    headers: { Authorization: `Bearer ${await accessToken(sa)}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      message: {
        token,
        notification: { title, body },
        // 앱이 켜져 있을 때는 PushService 가 이 값으로 띄우고, 누르면 link 로 간다
        // kind — 앱이 앞에 있을 때 어떤 소식인지 가른다(0117: 번개 글 링크는 출발 소식과 댓글이 같이 쓴다). 예전 앱은 읽지 않는다
        data: { title, body, link, kind },
        // 크루 채팅은 방마다 알림 하나 — 같은 방의 새 알림이 앞의 것을 바꾼다
        android: {
          // 방 출발 알림(room/…, 0117)도 방마다 하나 — 앱이 띄운 "출발했어요" 알림(CrewStartAlert)과 같은 tag 로 겹치지 않게
          notification: { channel_id: "community", ...(link.startsWith("crew-chat/") || link.startsWith("room/") ? { tag: link } : {}) },
        },
      },
    }),
  });
  if (res.ok) return ["ok", ""];
  const text = await res.text();
  // 앱을 지웠거나 토큰이 바뀐 폰 — 다시 보내도 안 간다
  if (res.status === 404 || text.includes("UNREGISTERED")) return ["gone", text];
  return ["error", `${res.status} ${text}`];
}

Deno.serve(async (req: Request) => {
  const secret = Deno.env.get("PUSH_WEBHOOK_SECRET") ?? "";
  if (!secret || req.headers.get("x-stepup-secret") !== secret) {
    return new Response("forbidden", { status: 403 });
  }
  const raw = Deno.env.get("FCM_SERVICE_ACCOUNT");
  if (!raw) return new Response("FCM_SERVICE_ACCOUNT is not set", { status: 500 });
  const sa = JSON.parse(raw) as ServiceAccount;

  const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
    auth: { persistSession: false },
  });

  const { data, error } = await db.rpc("push_claim_batch", { p_limit: 100 });
  if (error) return new Response(`claim failed: ${error.message}`, { status: 500 });

  let sent = 0;
  for (const row of (data ?? []) as Row[]) {
    let delivered = false;
    let lastError = "";
    for (const t of row.tokens) {
      const text = compose(row.kind, t.locale ?? "ko", row.args ?? {});
      if (!text) {
        lastError = `unknown kind ${row.kind}`;
        continue;
      }
      try {
        const [result, detail] = await send(sa, t.token, text[0], text[1], row.link ?? "", row.kind);
        if (result === "ok") delivered = true;
        else if (result === "gone") await db.rpc("push_forget_token", { p_token: t.token });
        else lastError = detail;
      } catch (e) {
        lastError = String(e);
      }
    }
    // 폰이 하나도 없거나 모두 지워졌으면 더 보낼 곳이 없다 — 보낸 것으로 닫는다
    const done = delivered || row.tokens.length === 0 || lastError === "";
    await db.rpc("push_mark", { p_id: row.id, p_sent: done, p_error: lastError });
    if (delivered) sent++;
  }
  return Response.json({ picked: (data ?? []).length, sent });
});
