const { json, authorized, db } = require("./_lib/common");
module.exports = async (req, res) => {
  if (req.method !== "POST") return json(res, 405, { error: "Method not allowed" });
  if (!authorized(req, "device")) return json(res, 401, { error: "Unauthorized" });
  const battery = req.body?.battery;
  if (battery !== null && battery !== undefined && (!Number.isInteger(battery) || battery < 0 || battery > 100))
    return json(res, 400, { error: "battery must be 0-100" });
  try {
    await db("guardian_state?id=eq.main", {
      method: "PATCH", body: JSON.stringify({ last_seen: new Date().toISOString(), battery: battery ?? null })
    });
    return json(res, 200, { ok: true });
  } catch (e) { return json(res, 500, { error: "Heartbeat failed", detail: e.message }); }
};