const { json, authorized, db } = require("./_lib/common");
module.exports = async (req, res) => {
  if (req.method !== "GET") return json(res, 405, { error: "Method not allowed" });
  if (!authorized(req, "admin")) return json(res, 401, { error: "Unauthorized" });
  try {
    const rows = await db("guardian_state?select=*&id=eq.main");
    const events = await db("guardian_events?select=id,event_type,detail,created_at&order=created_at.desc&limit=100");
    return json(res, 200, { state: rows[0] || null, events });
  } catch (e) { return json(res, 500, { error: "Could not load state", detail: e.message }); }
};