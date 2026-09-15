// Review Server — secure gateway for the Review app.
// GROQ_API_KEY exists only in Render environment variables. Never put it in the APK.
import express from 'express';

const app = express();
const PORT = Number(process.env.PORT || 3000);
const GROQ_URL = 'https://api.groq.com/openai/v1/chat/completions';
const GROQ_KEY = String(process.env.GROQ_API_KEY || '').trim();
const MODEL = 'openai/gpt-oss-120b';
const ONESIGNAL_APP_ID = String(process.env.ONESIGNAL_APP_ID || '').trim();
const ONESIGNAL_REST_API_KEY = String(process.env.ONESIGNAL_REST_API_KEY || '').trim();
const NOTIFICATION_ADMIN_KEY = String(process.env.NOTIFICATION_ADMIN_KEY || '').trim();
const ONESIGNAL_URL = 'https://api.onesignal.com/notifications';

app.disable('x-powered-by');
app.use(express.json({ limit: '256kb' }));
app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  res.setHeader('Vary', 'Origin');
  if (req.method === 'OPTIONS') return res.sendStatus(204);
  next();
});

const buckets = new Map();
function rateLimit(req, res, next) {
  const ip = String(req.headers['x-forwarded-for'] || req.socket.remoteAddress || 'unknown').split(',')[0].trim();
  const now = Date.now();
  const row = buckets.get(ip) || { start: now, count: 0 };
  if (now - row.start >= 60_000) { row.start = now; row.count = 0; }
  row.count += 1;
  buckets.set(ip, row);
  if (row.count > 60) return res.status(429).json({ success: false, error: { code: 'rate_limit_exceeded', message: 'Too many requests' } });
  next();
}

app.get('/', (_req, res) => res.json({ app: 'Review Server', status: 'online', model: MODEL, notificationsConfigured: !!(ONESIGNAL_APP_ID && ONESIGNAL_REST_API_KEY) }));
app.get('/health', (_req, res) => res.json({ ok: true, app: 'Review Server', model: MODEL, groqConfigured: !!GROQ_KEY, notificationsConfigured: !!(ONESIGNAL_APP_ID && ONESIGNAL_REST_API_KEY) }));


function requireNotificationAdmin(req, res, next) {
  if (!NOTIFICATION_ADMIN_KEY) return res.status(503).json({ success: false, error: { code: 'notification_service_not_configured', message: 'Notification service is not configured' } });
  const auth = String(req.headers.authorization || '');
  if (auth !== `Bearer ${NOTIFICATION_ADMIN_KEY}`) return res.status(401).json({ success: false, error: { code: 'unauthorized', message: 'Unauthorized' } });
  next();
}

// POST /api/notifications
// { "title": "...", "body": "...", "data": { ... } }
// Sends a push notification through OneSignal to all subscribed Review devices.
app.post('/api/notifications', requireNotificationAdmin, async (req, res) => {
  if (!ONESIGNAL_APP_ID || !ONESIGNAL_REST_API_KEY) {
    return res.status(503).json({ success: false, error: { code: 'onesignal_not_configured', message: 'OneSignal is not configured on Render' } });
  }
  const title = typeof req.body?.title === 'string' ? req.body.title.trim() : '';
  const body = typeof req.body?.body === 'string' ? req.body.body.trim() : '';
  const data = (req.body?.data && typeof req.body.data === 'object') ? req.body.data : {};
  if (!title || !body) return res.status(400).json({ success: false, error: { code: 'notification_content_required', message: 'title and body are required' } });
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 20_000);
  try {
    const upstream = await fetch(ONESIGNAL_URL, {
      method: 'POST',
      headers: { 'Authorization': `Key ${ONESIGNAL_REST_API_KEY}`, 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify({ app_id: ONESIGNAL_APP_ID, included_segments: ['Subscribed Users'], headings: { en: title, ar: title }, contents: { en: body, ar: body }, data }),
      signal: controller.signal
    });
    const raw = await upstream.text();
    let result = null; try { result = JSON.parse(raw); } catch (_) {}
    if (!upstream.ok) {
      console.error(`OneSignal HTTP ${upstream.status}`);
      return res.status(502).json({ success: false, error: { code: 'onesignal_upstream_error', message: 'Push provider request failed' } });
    }
    return res.status(200).json({ success: true, id: result?.id || null, recipients: result?.recipients ?? null });
  } catch (err) {
    return res.status(502).json({ success: false, error: { code: 'onesignal_unreachable', message: 'Could not reach push provider' } });
  } finally { clearTimeout(timer); }
});

// Official Review AI contract:
// POST /api/ai
// { "message": "نص المستخدم" }
// -> { "success": true, "reply": "..." }
app.post('/api/ai', rateLimit, async (req, res) => {
  res.setHeader('Cache-Control', 'no-store');
  res.setHeader('X-Content-Type-Options', 'nosniff');

  if (!GROQ_KEY) {
    return res.status(503).json({ success: false, error: { code: 'server_ai_key_missing', message: 'AI service is not configured on Render' } });
  }

  const message = typeof req.body?.message === 'string' ? req.body.message.trim() : '';
  if (!message) {
    return res.status(400).json({ success: false, error: { code: 'message_required', message: 'message must be a non-empty string' } });
  }
  if (message.length > 20000) {
    return res.status(413).json({ success: false, error: { code: 'message_too_large', message: 'message is too large' } });
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 60_000);
  try {
    const upstream = await fetch(GROQ_URL, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${GROQ_KEY}`,
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify({
        model: MODEL,
        messages: [
          { role: 'system', content: 'أنت Review Agent، مساعد تعليمي ذكي داخل تطبيق Review. أجب بالعربية بوضوح ودقة. التزم بحالة التطبيق والتعليمات الموجودة داخل رسالة المستخدم ولا تخترع بيانات خارجها.' },
          { role: 'user', content: message }
        ],
        stream: false,
        temperature: 0.3
      }),
      signal: controller.signal
    });

    const raw = await upstream.text();
    let data = null;
    try { data = JSON.parse(raw); } catch (_) {}

    if (!upstream.ok) {
      const upstreamMessage = String(data?.error?.message || '').trim();
      console.error(`Groq HTTP ${upstream.status}${upstreamMessage ? `: ${upstreamMessage}` : ''}`);
      return res.status(502).json({ success: false, error: { code: 'ai_upstream_error', message: 'AI provider request failed' } });
    }

    const reply = data?.choices?.[0]?.message?.content;
    if (typeof reply !== 'string' || !reply.trim()) {
      return res.status(502).json({ success: false, error: { code: 'empty_ai_reply', message: 'AI provider returned an empty reply' } });
    }

    return res.status(200).json({ success: true, reply: reply.trim() });
  } catch (err) {
    const message = err?.name === 'AbortError' ? 'AI request timed out' : 'Could not reach AI provider';
    console.error(message);
    return res.status(502).json({ success: false, error: { code: 'upstream_unreachable', message } });
  } finally {
    clearTimeout(timer);
  }
});

app.use((_req, res) => res.status(404).json({ success: false, error: { code: 'not_found', message: 'Not found' } }));

app.listen(PORT, () => {
  console.log(`Review Server listening on ${PORT}`);
  console.log(`Groq model: ${MODEL}`);
  console.log(`Groq key configured: ${!!GROQ_KEY}`);
});
