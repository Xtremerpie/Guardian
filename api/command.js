const { json, authorized, db, addEvent } = require("./_lib/common");
module.exports = async (req, res) => {
  if (req.method !== "POST") return json(res, 405, { error: "Method not allowed" });
  if (!authorized(req, "admin")) return json(res, 401, { error: "Unauthorized" });
  if (typeof req.body?.active !== "boolean") return json(res, 400, { error: "active must be boolean" });
  try {
    await db("guardian_state?id=eq.main", {
      method: "PATCH", body: JSON.stringify({ active: req.body.active, updated_at: new Date().toISOString() })
    });
    await addEvent(req.body.active ? "Monitoring activated" : "Monitoring deactivated");
    return json(res, 200, { ok: true, active: req.body.active });
  } catch (e) { return json(res, 500, { error: "Could not update monitoring", detail: e.message }); }
};