const { json, authorized, addEvent } = require("./_lib/common");
const ALLOWED = ["Phone service started", "Phone service stopped", "Location sharing enabled", "Location sharing disabled"];
module.exports = async (req, res) => {
  if (req.method !== "POST") return json(res, 405, { error: "Method not allowed" });
  if (!authorized(req, "device")) return json(res, 401, { error: "Unauthorized" });
  if (!ALLOWED.includes(req.body?.type)) return json(res, 400, { error: "Unsupported event" });
  try { await addEvent(req.body.type); return json(res, 200, { ok: true }); }
  catch (e) { return json(res, 500, { error: "Event failed", detail: e.message }); }
};