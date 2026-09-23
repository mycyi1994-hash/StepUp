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

const TEXT: Record<string, Record<string, (a: Record<string, string>) => [string, string]>> = {
  ko: {
    COMMENT: (a) => ["새 댓글", `${a.name}님이 "${a.title}"에 댓글을 남겼어요`],
    REPLY: (a) => ["새 답글", `${a.name}님이 내 댓글에 답글을 남겼어요`],
    CREW_REQUEST: (a) => ["가입 신청", `${a.name}님이 ${a.crew} 크루에 가입을 신청했어요`],
    PARTY_OPEN: (a) => ["파티런 로비가 열렸어요 🏃", `${a.name}님이 ${a.crew} 파티런을 열었어요. 같이 달려요!`],
    CREW_FLASH: (a) => ["크루 번개 ⚡", `${a.name}: ${a.title}`],
  },
  en: {
    COMMENT: (a) => ["New comment", `${a.name} commented on "${a.title}"`],
    REPLY: (a) => ["New reply", `${a.name} replied to your comment`],
    CREW_REQUEST: (a) => ["Join request", `${a.name} wants to join ${a.crew}`],
    PARTY_OPEN: (a) => ["Party run lobby is open 🏃", `${a.name} opened a ${a.crew} party run. Join in!`],
    CREW_FLASH: (a) => ["Crew flash run ⚡", `${a.name}: ${a.title}`],
  },
  ja: {
    COMMENT: (a) => ["新しいコメント", `${a.name}さんが「${a.title}」にコメントしました`],
    REPLY: (a) => ["新しい返信", `${a.name}さんがあなたのコメントに返信しました`],
    CREW_REQUEST: (a) => ["参加申請", `${a.name}さんが${a.crew}への参加を申請しました`],
    PARTY_OPEN: (a) => ["パーティーランのロビーが開きました 🏃", `${a.name}さんが${a.crew}のパーティーランを開きました`],
    CREW_FLASH: (a) => ["クルーのフラッシュラン ⚡", `${a.name}: ${a.title}`],
  },
  zh: {
    COMMENT: (a) => ["新评论", `${a.name} 评论了「${a.title}」`],
    REPLY: (a) => ["新回复", `${a.name} 回复了你的评论`],
    CREW_REQUEST: (a) => ["加入申请", `${a.name} 申请加入 ${a.crew}`],
    PARTY_OPEN: (a) => ["组队跑大厅已开启 🏃", `${a.name} 开启了 ${a.crew} 组队跑，一起来吧！`],
    CREW_FLASH: (a) => ["跑团闪电跑 ⚡", `${a.name}: ${a.title}`],
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
): Promise<[SendResult, string]> {
  const res = await fetch(`https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`, {
    method: "POST",
    headers: { Authorization: `Bearer ${await accessToken(sa)}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      message: {
        token,
        notification: { title, body },
        // 앱이 켜져 있을 때는 PushService 가 이 값으로 띄우고, 누르면 link 로 간다
        data: { title, body, link },
        android: { notification: { channel_id: "community" } },
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
        const [result, detail] = await send(sa, t.token, text[0], text[1], row.link ?? "");
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
