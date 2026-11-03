const { json, authorized, db } = require("./_lib/common");
module.exports = async (req, res) => {
  if (req.method !== "GET") return json(res, 405, { error: "Method not allowed" });
  if (!authorized(req, "device")) return json(res, 401, { error: "Unauthorized" });
  try {
    const rows = await db("guardian_state?select=active&id=eq.main");
    return json(res, 200, { active: !!rows[0]?.active });
  } catch (e) { return json(res, 500, { error: "Could not read command", detail: e.message }); }
};