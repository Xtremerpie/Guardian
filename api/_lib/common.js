const BASE = process.env.SUPABASE_URL;
const KEY = process.env.SUPABASE_SERVICE_ROLE_KEY;
function json(res, status, body) {
  res.status(status).setHeader("Content-Type", "application/json").send(JSON.stringify(body));
}
function requireEnv() {
  if (!BASE || !KEY || !process.env.ADMIN_KEY || !process.env.DEVICE_KEY) {
    throw new Error("Server environment variables are not configured");
  }
}
function authorized(req, kind) {
  const supplied = req.headers[kind === "admin" ? "x-admin-key" : "x-device-key"];
  const expected = process.env[kind === "admin" ? "ADMIN_KEY" : "DEVICE_KEY"];
  return typeof supplied === "string" && supplied.length > 0 && supplied === expected;
}
async function db(path, options = {}) {
  requireEnv();
  const response = await fetch(`${BASE}/rest/v1/${path}`, {
    ...options,
    headers: {
      apikey: KEY,
      Authorization: `Bearer ${KEY}`,
      "Content-Type": "application/json",
      Prefer: options.prefer || "return=representation",
      ...(options.headers || {})
    }
  });
  const body = await response.text();
  if (!response.ok) throw new Error(`Database request failed (${response.status}): ${body.slice(0,300)}`);
  return body ? JSON.parse(body) : [];
}
async function addEvent(type, detail = "") {
  await db("guardian_events", {
    method: "POST",
    body: JSON.stringify({ event_type: type, detail })
  });
}
module.exports = { json, authorized, db, addEvent };
