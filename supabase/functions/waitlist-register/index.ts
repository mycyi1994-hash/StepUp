// 공개 홈페이지의 사전 등록 요청. 브라우저에는 비밀 키나 DB 권한을 주지 않는다.
// Supabase Edge Function 설정: verify_jwt = false.
// Secrets: TURNSTILE_SECRET, WAITLIST_TURNSTILE_HOSTNAMES, RESEND_API_KEY,
//          WAITLIST_FROM_EMAIL, WAITLIST_ALLOWED_ORIGINS.
import { createClient } from "jsr:@supabase/supabase-js@2";

const allowedOrigins = new Set(
  (Deno.env.get("WAITLIST_ALLOWED_ORIGINS") || "https://stepupcrew.com,https://www.stepupcrew.com")
    .split(",").map((s) => s.trim()).filter(Boolean),
);
const allowedHostnames = new Set(
  (Deno.env.get("WAITLIST_TURNSTILE_HOSTNAMES") || "stepupcrew.com,www.stepupcrew.com")
    .split(",").map((s) => s.trim()).filter(Boolean),
);
const responseHeaders = (origin: string | null) => ({
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store",
  "Vary": "Origin",
  ...(origin && allowedOrigins.has(origin) ? { "Access-Control-Allow-Origin": origin } : {}),
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "content-type",
});
const json = (body: object, status: number, origin: string | null) =>
  new Response(JSON.stringify(body), { status, headers: responseHeaders(origin) });
const generic = (origin: string | null) => json({ accepted: true }, 200, origin);
const encoder = new TextEncoder();

function newToken(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  return btoa(String.fromCharCode(...bytes)).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}
async function hash(token: string): Promise<string> {
  const bytes = new Uint8Array(await crypto.subtle.digest("SHA-256", encoder.encode(token)));
  return [...bytes].map((b) => b.toString(16).padStart(2, "0")).join("");
}
function admin() {
  const url = Deno.env.get("SUPABASE_URL");
  const key = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !key) throw new Error("Supabase server configuration missing");
  return createClient(url, key, { auth: { autoRefreshToken: false, persistSession: false } });
}
async function verifyTurnstile(token: unknown): Promise<boolean> {
  const secret = Deno.env.get("TURNSTILE_SECRET");
  if (!secret || typeof token !== "string" || !token || token.length > 2048) return false;
  const response = await fetch("https://challenges.cloudflare.com/turnstile/v0/siteverify", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ secret, response: token }),
    signal: AbortSignal.timeout(10000),
  });
  if (!response.ok) return false;
  const result = await response.json();
  return result.success === true && result.action === "waitlist" && allowedHostnames.has(result.hostname);
}
async function sendConfirmation(email: string, token: string): Promise<boolean> {
  const apiKey = Deno.env.get("RESEND_API_KEY");
  const from = Deno.env.get("WAITLIST_FROM_EMAIL");
  if (!apiKey || !from) return false;
  const url = `https://stepupcrew.com/waitlist-confirm.html#confirm=${token}`;
  const response = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: { "Authorization": `Bearer ${apiKey}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      from,
      to: [email],
      subject: "StepUp 대기 명단 이메일 확인",
      text: `StepUp 대기 명단 등록을 마치려면 48시간 안에 아래 링크를 열어주세요.\n\n${url}\n\n신청한 적이 없다면 이 메일을 무시하세요. 문의: support@stepupcrew.com`,
      html: `<p>StepUp 대기 명단 등록을 마치려면 48시간 안에 링크를 열어주세요.</p><p><a href="${url}">이메일 확인하기</a></p><p>신청한 적이 없다면 이 메일을 무시하세요.</p>`,
    }),
    signal: AbortSignal.timeout(10000),
  });
  return response.ok;
}

Deno.serve(async (request) => {
  const origin = request.headers.get("Origin");
  if (!origin || !allowedOrigins.has(origin)) return json({ error: "forbidden" }, 403, origin);
  if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: responseHeaders(origin) });
  if (request.method !== "POST") return json({ error: "method" }, 405, origin);
  try {
    const raw = await request.text();
    if (raw.length > 4096) return json({ error: "invalid" }, 400, origin);
    const body = JSON.parse(raw);
    const db = admin();

    if (body.action === "confirm") {
      if (typeof body.token !== "string" || !/^[A-Za-z0-9_-]{32,128}$/.test(body.token))
        return json({ confirmed: false }, 400, origin);
      const tokenHash = await hash(body.token);
      const { data, error } = await db.from("waitlist_entries")
        .update({ status: "confirmed", confirmed_at: new Date().toISOString(), confirmation_token_hash: null, confirmation_expires_at: null })
        .eq("confirmation_token_hash", tokenHash).eq("status", "pending")
        .gt("confirmation_expires_at", new Date().toISOString()).select("id");
      if (error) throw error;
      return json({ confirmed: (data?.length || 0) === 1 }, data?.length ? 200 : 400, origin);
    }

    if (body.action !== "register") return json({ error: "invalid" }, 400, origin);
    if (body.consent !== true || typeof body.email !== "string") return json({ error: "invalid" }, 400, origin);
    const email = body.email.trim().toLowerCase();
    if (email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return json({ error: "invalid" }, 400, origin);
    if (!Deno.env.get("RESEND_API_KEY") || !Deno.env.get("WAITLIST_FROM_EMAIL"))
      return json({ error: "unavailable" }, 503, origin);
    if (!(await verifyTurnstile(body.turnstileToken))) return json({ error: "verification" }, 403, origin);

    const { data: existing, error: lookupError } = await db.from("waitlist_entries")
      .select("id,status,confirmation_sent_at").eq("email", email).maybeSingle();
    if (lookupError) throw lookupError;
    if (existing?.status === "confirmed" || existing?.status === "withdrawn") return generic(origin);
    if (existing?.confirmation_sent_at && Date.now() - Date.parse(existing.confirmation_sent_at) < 15 * 60 * 1000)
      return generic(origin);

    const token = newToken();
    const tokenHash = await hash(token);
    const now = new Date();
    const values = {
      confirmation_token_hash: tokenHash,
      confirmation_expires_at: new Date(now.getTime() + 48 * 60 * 60 * 1000).toISOString(),
      confirmation_sent_at: now.toISOString(),
      consented_at: now.toISOString(),
    };
    const { data: row, error: writeError } = existing
      ? await db.from("waitlist_entries").update(values).eq("id", existing.id).select("id").single()
      : await db.from("waitlist_entries").insert({ email, status: "pending", ...values }).select("id").single();
    if (writeError) {
      if (writeError.code === "23505") return generic(origin);
      throw writeError;
    }
    let delivered = false;
    try {
      delivered = await sendConfirmation(email, token);
    } catch (error) {
      console.error("waitlist email delivery failed", error instanceof Error ? error.message : "unknown");
    }
    if (!delivered) {
      await db.from("waitlist_entries").update({ confirmation_sent_at: null })
        .eq("id", row.id).eq("confirmation_token_hash", tokenHash);
      return json({ error: "delivery" }, 503, origin);
    }
    return generic(origin);
  } catch (error) {
    console.error("waitlist request failed", error instanceof Error ? error.message : "unknown");
    return json({ error: "unavailable" }, 503, origin);
  }
});
