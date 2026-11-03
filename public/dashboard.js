const $ = id => document.getElementById(id);
let adminKey = "", interval = null, notified = false;
async function api(path, method="GET", body) {
  const r = await fetch(path, { method, headers: { "Content-Type":"application/json", "X-Admin-Key":adminKey }, body: body ? JSON.stringify(body) : undefined });
  const data = await r.json().catch(()=>({}));
  if (!r.ok) throw new Error(data.error || `Request failed (${r.status})`);
  return data;
}
$("connect").onclick = async () => {
  adminKey = $("adminKey").value.trim();
  try {
    if ("Notification" in window && Notification.permission === "default") await Notification.requestPermission();
    await refresh(); $("login").hidden = true; $("dashboard").hidden = false;
    interval = setInterval(() => refresh().catch(showError), 10000);
  } catch(e) { $("loginError").textContent = e.message; }
};
function showError(e) { $("alert").hidden = false; $("alert").textContent = e.message; }
async function command(active) { try { await api("/api/command","POST",{active}); await refresh(); } catch(e){showError(e);} }
$("on").onclick = () => command(true);
$("off").onclick = () => command(false);
async function refresh() {
  const d = await api("/api/state");
  const s = d.state || {};
  $("active").textContent = s.active ? "Active" : "Inactive";
  $("active").className = s.active ? "good" : "";
  $("summary").textContent = s.active ? "The phone should check in while its service is running." : "Monitoring is currently disabled.";
  const age = s.last_seen ? Math.max(0, Math.floor((Date.now()-new Date(s.last_seen).getTime())/1000)) : Infinity;
  const online = age < 120;
  $("connection").textContent = online ? "Recently connected" : s.last_seen ? "Check-in overdue" : "Not connected yet";
  $("connection").className = online ? "good" : "warn";
  $("lastSeen").textContent = s.last_seen ? `Last check-in: ${new Date(s.last_seen).toLocaleString()} (${age < 60 ? age+" sec" : Math.floor(age/60)+" min"} ago)` : "No check-in received";
  $("battery").textContent = s.battery == null ? "—" : `${s.battery}%`;
  if (Number.isFinite(s.latitude) && Number.isFinite(s.longitude)) {
    $("location").textContent = `${s.latitude.toFixed(5)}, ${s.longitude.toFixed(5)}`;
    $("locationAt").textContent = s.location_at ? new Date(s.location_at).toLocaleString() : "";
    $("map").href = `https://www.openstreetmap.org/?mlat=${s.latitude}&mlon=${s.longitude}#map=16/${s.latitude}/${s.longitude}`;
    $("map").hidden = false;
  } else { $("location").textContent = "Not shared"; $("map").hidden = true; }
  const overdue = s.active && !online;
  $("alert").hidden = !overdue;
  $("alert").textContent = overdue ? "ALERT: No recent phone check-in. Check the phone and its internet connection; the cause cannot be determined remotely." : "";
  if (overdue && !notified && "Notification" in window && Notification.permission === "granted") new Notification("Xtremerpie Guardian alert", {body:"Phone check-in overdue. Check connectivity and the phone."});
  notified = overdue;
  $("events").replaceChildren();
  (d.events || []).forEach(e => {
    const item = document.createElement("div"); item.className = "event";
    const title = document.createElement("b"); title.textContent = e.event_type;
    const time = document.createElement("small"); time.textContent = `${new Date(e.created_at).toLocaleString()}${e.detail ? " · "+e.detail : ""}`;
    item.append(title,time); $("events").append(item);
  });
  if (!(d.events || []).length) $("events").textContent = "No activity recorded yet.";
  $("updated").textContent = `Updated ${new Date().toLocaleTimeString()}`;
}