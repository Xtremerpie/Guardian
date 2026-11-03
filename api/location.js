const { json, authorized, db, addEvent } = require("./_lib/common");
module.exports = async (req, res) => {
  if (req.method !== "POST") return json(res, 405, { error: "Method not allowed" });
  if (!authorized(req, "device")) return json(res, 401, { error: "Unauthorized" });
  const { latitude, longitude, accuracy } = req.body || {};
  if (![latitude, longitude].every(Number.isFinite) || Math.abs(latitude)>90 || Math.abs(longitude)>180)
    return json(res, 400, { error: "Invalid coordinates" });
  try {
    await db("guardian_state?id=eq.main", {
      method: "PATCH", body: JSON.stringify({
        latitude, longitude, accuracy: Number.isFinite(accuracy) ? accuracy : null,
        location_at: new Date().toISOString()
      })
    });
    return json(res, 200, { ok: true });
  } catch (e) { return json(res, 500, { error: "Location update failed", detail: e.message }); }
};