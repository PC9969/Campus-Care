(() => {
  const $ = (selector, root = document) => root.querySelector(selector);
  const content = $("#app-content");
  const state = { page: "dashboard", issues: [], search: "", status: "All issues", category: "All categories" };
  const pageTitles = { dashboard: "Overview", report: "Report an issue", issues: "Issue management", staff: "Staff workspace" };
  const escapeHtml = (value = "") => String(value).replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
  const dateLabel = (value) => new Intl.DateTimeFormat("en", { month: "short", day: "numeric" }).format(new Date(value));
  const statusClass = (status) => status.toLowerCase().replaceAll(" ", "-");
  const priorityClass = (priority) => priority.toLowerCase();
  const statusPill = (status) => `<span class="status-pill ${escapeHtml(statusClass(status))}"><i></i>${escapeHtml(status)}</span>`;
  const priorityPill = (priority) => `<span class="priority ${priorityClass(priority)}"><span class="priority-dot"></span>${escapeHtml(priority)}</span>`;
  const iconFor = (category) => ({ Plumbing: "◉", Electrical: "ϟ", Cleaning: "✳", HVAC: "⌁", Grounds: "❋", Furniture: "▦", Safety: "◇" })[category] || "◈";
  const initials = (name) => name.split(/\s+/).map((part) => part[0]).slice(0, 2).join("").toUpperCase();
  const shortId = (id) => `#${String(id).replace(/^ISS-/, "")}`;

  function shellHeader(eyebrow, title, subtitle, action = "") {
    return `<div class="page-heading"><div><div class="eyebrow">${eyebrow}</div><h1>${title}</h1><p>${subtitle}</p></div>${action}</div>`;
  }
  function issueRow(issue) {
    return `<tr><td><span class="issue-id">${escapeHtml(shortId(issue.id))}</span></td><td><div class="issue-title-cell"><span class="category-icon ${escapeHtml(statusClass(issue.category))}">${iconFor(issue.category)}</span><span><b>${escapeHtml(issue.title)}</b><small>${escapeHtml(issue.location)}</small></span></div></td><td>${priorityPill(issue.priority)}</td><td>${statusPill(issue.status)}</td><td><span class="assigned-cell"><span class="avatar tiny-avatar">${initials(issue.assignee || "Unassigned")}</span>${escapeHtml(issue.assignee || "Unassigned")}</span></td><td class="date-cell">${dateLabel(issue.createdAt)}</td><td><button class="row-menu" aria-label="Issue actions" data-open-issue="${escapeHtml(issue.id)}">···</button></td></tr>`;
  }
  function renderDashboard(summary) {
    const recent = [...state.issues].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0, 5);
    content.innerHTML = `${shellHeader("TUESDAY, OCTOBER 27, 2026", 'Good morning, Alex <span class="wave">✦</span>', "Here’s what’s happening across your campus today.", `<a class="button button-primary" href="#report"><span>＋</span> Report an issue</a>`)}
      <section class="stat-grid">
        <article class="stat-card"><div class="stat-top"><span>Open issues</span><span class="stat-icon blue-tint">☷</span></div><div class="stat-value">${summary.open}<span class="stat-context">Across campus</span></div><div class="stat-foot"><span class="trend up">↗ ${summary.newThisWeek} new</span><span>this week</span></div></article>
        <article class="stat-card"><div class="stat-top"><span>In progress</span><span class="stat-icon violet-tint">◷</span></div><div class="stat-value">${summary.inProgress}<span class="stat-context">Being worked on</span></div><div class="stat-foot"><span class="neutral-dot">●</span><span>by facilities team</span></div></article>
        <article class="stat-card"><div class="stat-top"><span>Resolved this month</span><span class="stat-icon green-tint">✓</span></div><div class="stat-value">${summary.resolved}<span class="stat-context">Great work, team!</span></div><div class="stat-foot"><span class="trend up">↗ 12%</span><span>vs. last month</span></div></article>
        <article class="stat-card"><div class="stat-top"><span>Avg. resolution time</span><span class="stat-icon amber-tint">◷</span></div><div class="stat-value">${summary.avgResolution}<span class="stat-context">Across all issues</span></div><div class="stat-foot"><span class="trend up">↘ 8%</span><span>faster than last month</span></div></article>
      </section>
      <section class="dashboard-grid"><article class="panel activity-panel"><div class="panel-heading"><div><h2>Issue activity</h2><p>Incoming requests over the last 7 days</p></div><button class="select-button">This week⌄</button></div><div class="chart-legend"><span><i class="legend-blue"></i>Reported</span><span><i class="legend-light"></i>Resolved</span></div><div class="chart-wrap"><div class="chart-y"><span>20</span><span>15</span><span>10</span><span>5</span><span>0</span></div><div class="chart" aria-label="Bar chart of issues reported and resolved this week"><div class="chart-gridlines"><i></i><i></i><i></i><i></i><i></i></div>${summary.weekly.map((day) => `<div class="chart-day"><div class="bar-pair"><span class="bar reported" style="height:${day.reported * 5}%"></span><span class="bar resolved" style="height:${day.resolved * 5}%"></span></div><small>${day.day}</small></div>`).join("")}</div></div></article>
      <article class="panel category-panel"><div class="panel-heading"><div><h2>Issues by category</h2><p>Open requests breakdown</p></div><button class="more-button" aria-label="More category options">···</button></div><div class="donut-wrap"><div class="donut"><div><strong>${summary.open}</strong><small>Open issues</small></div></div><div class="category-legend">${summary.categories.map((item, i) => `<div><span><i class="cat-${i % 5}"></i>${escapeHtml(item.name)}</span><b>${escapeHtml(item.count)}</b></div>`).join("")}</div></div></article></section>
      <section class="panel recent-panel"><div class="panel-heading"><div><h2>Recent issues</h2><p>Latest reports from across campus</p></div><a href="#issues" class="text-link">View all issues <span>→</span></a></div>${recent.length ? `<div class="table-scroll"><table><thead><tr><th>ISSUE</th><th>DESCRIPTION</th><th>PRIORITY</th><th>STATUS</th><th>ASSIGNED TO</th><th>REPORTED</th><th></th></tr></thead><tbody>${recent.map(issueRow).join("")}</tbody></table></div>` : `<div class="empty-state">No issues yet. Report the first campus issue.</div>`}</section>`;
  }
  function filteredIssues() {
    return state.issues.filter((issue) => (state.status === "All issues" || issue.status === state.status) && (state.category === "All categories" || issue.category === state.category) && (!state.search || `${issue.title} ${issue.location} ${issue.id} ${issue.assignee}`.toLowerCase().includes(state.search.toLowerCase())));
  }
  function renderIssues() {
    const rows = filteredIssues();
    content.innerHTML = `${shellHeader("FACILITIES", "Issue management", "Track, assign, and resolve maintenance requests across campus.", `<a class="button button-primary" href="#report"><span>＋</span> New issue</a>`)}
      <section class="stat-strip"><div><span class="strip-icon strip-blue">☷</span><span><b>${state.issues.filter(i => i.status !== "Resolved").length}</b><small>Total open</small></span></div><div><span class="strip-icon strip-violet">◷</span><span><b>${state.issues.filter(i => i.status === "In Progress").length}</b><small>In progress</small></span></div><div><span class="strip-icon strip-amber">⚑</span><span><b>${state.issues.filter(i => i.priority === "High" && i.status !== "Resolved").length}</b><small>High priority</small></span></div><div><span class="strip-icon strip-green">✓</span><span><b>${state.issues.filter(i => i.status === "Resolved").length}</b><small>Resolved</small></span></div></section>
      <section class="panel issues-panel"><div class="issue-toolbar"><div class="search-box"><span>⌕</span><input id="issue-search" type="search" placeholder="Search issues..." value="${escapeHtml(state.search)}" aria-label="Search issues"></div><select id="status-filter" aria-label="Filter by status"><option>All issues</option>${["Open", "In Progress", "Resolved"].map(v => `<option ${state.status === v ? "selected" : ""}>${v}</option>`).join("")}</select><select id="category-filter" aria-label="Filter by category"><option>All categories</option>${[...new Set(state.issues.map(i => i.category))].sort().map(v => `<option ${state.category === v ? "selected" : ""}>${escapeHtml(v)}</option>`).join("")}</select><button class="filter-button" id="clear-filters">↻ <span>Reset</span></button></div><div class="table-scroll"><table><thead><tr><th>ISSUE</th><th>DESCRIPTION</th><th>PRIORITY</th><th>STATUS</th><th>ASSIGNED TO</th><th>REPORTED</th><th></th></tr></thead><tbody>${rows.map(issueRow).join("")}</tbody></table>${rows.length ? "" : `<div class="empty-state">No issues match these filters.</div>`}</div><div class="table-footer"><span>Showing <b>${rows.length}</b> of <b>${state.issues.length}</b> issues</span><div><button disabled>←</button><button class="page-current">1</button><button disabled>→</button></div></div></section>`;
    $("#issue-search").addEventListener("input", (event) => { state.search = event.target.value; const pos = event.target.selectionStart; renderIssues(); const input = $("#issue-search"); input.focus(); input.setSelectionRange(pos, pos); });
    $("#status-filter").addEventListener("change", (event) => { state.status = event.target.value; renderIssues(); });
    $("#category-filter").addEventListener("change", (event) => { state.category = event.target.value; renderIssues(); });
    $("#clear-filters").addEventListener("click", () => { state.search = ""; state.status = "All issues"; state.category = "All categories"; renderIssues(); });
  }
  function renderReport() {
    content.innerHTML = `${shellHeader("CAMPUSCARE", "Report a maintenance issue", "Tell us what needs attention and our facilities team will take it from here.")}
      <div class="report-layout"><form class="panel report-form" id="report-form"><div class="form-section-title"><span class="step-number">1</span><div><h2>Issue details</h2><p>Give us a few details so we can help quickly.</p></div></div><label class="field-label" for="issue-title">What needs attention? <em>*</em></label><input class="text-input" id="issue-title" name="title" placeholder="e.g. Leaking tap in the restroom" required maxlength="90"><div class="field-row"><div><label class="field-label" for="issue-category">Category <em>*</em></label><select class="text-input" id="issue-category" name="category" required><option value="">Select a category</option>${["Plumbing", "Electrical", "HVAC", "Cleaning", "Furniture", "Grounds", "Safety"].map(v => `<option>${v}</option>`).join("")}</select></div><div><label class="field-label" for="issue-priority">Priority <em>*</em></label><select class="text-input" id="issue-priority" name="priority" required><option value="">Select priority</option><option>Low</option><option selected>Medium</option><option>High</option></select></div></div><label class="field-label" for="issue-location">Where is the issue? <em>*</em></label><div class="location-input"><span>⌖</span><input class="text-input" id="issue-location" name="location" placeholder="Building, floor, room number" required maxlength="100"></div><label class="field-label" for="issue-description">Description <span class="optional">Optional</span></label><textarea class="text-input" id="issue-description" name="description" rows="4" placeholder="Add any details that could help our team..."></textarea><div class="form-footer"><span><em>*</em> Required fields</span><button class="button button-primary" type="submit">Submit issue <span>→</span></button></div></form><aside class="report-aside"><div class="aside-illustration"><div class="illustration-circle">✦</div><div class="illustration-building"><span></span><span></span><span></span><span></span></div><div class="illustration-plant">♧</div></div><h3>We’ll take it from here</h3><p>Your report goes straight to the campus facilities team. We’ll keep you updated as work progresses.</p><div class="response-note"><span>◷</span><div><b>Typical response time</b><small>Within 1–2 business days</small></div></div><div class="secure-note">◇&nbsp; Your report is only shared with campus staff.</div></aside></div>`;
    $("#report-form").addEventListener("submit", submitIssue);
  }
  async function submitIssue(event) {
    event.preventDefault(); const form = new FormData(event.currentTarget);
    const payload = { title: form.get("title").trim(), category: form.get("category"), priority: form.get("priority"), location: form.get("location").trim(), description: form.get("description").trim() };
    try { await CampusApi.createIssue(payload); state.issues = await CampusApi.getIssues(); toast("Issue reported", "Your request has been sent to the facilities team."); location.hash = "#issues"; }
    catch (error) { toast("Couldn’t submit issue", error.message || "Please try again.", true); }
  }
  async function renderStaff() {
    let staff;
    try { staff = await CampusApi.getStaff(); } catch { staff = []; }
    const active = state.issues.filter(i => i.status !== "Resolved");
    content.innerHTML = `${shellHeader("FACILITIES TEAM", "Staff workspace", "Your team’s work queue and campus coverage at a glance.", `<button class="button button-outline" id="staff-export">↓ <span>Export queue</span></button>`)}
      <section class="staff-welcome"><div><span class="staff-status-dot"></span> TEAM STATUS: ON CAMPUS <h2>Good morning, Facilities team</h2><p>${active.length} active ${active.length === 1 ? "request" : "requests"} need your attention today.</p></div><div class="staff-avatars">${staff.slice(0, 4).map((person, i) => `<span class="avatar staff-avatar staff-${i}">${initials(person.name)}</span>`).join("")}<span class="avatar staff-more">+${Math.max(staff.length - 4, 0)}</span></div></section>
      <div class="staff-stats"><article class="panel staff-stat"><span class="staff-stat-icon strip-blue">☷</span><div><b>${active.length}</b><small>Assigned to team</small></div><span class="staff-stat-note">Active queue</span></article><article class="panel staff-stat"><span class="staff-stat-icon strip-amber">⚑</span><div><b>${active.filter(i => i.priority === "High").length}</b><small>High priority</small></div><span class="staff-stat-note urgent-text">Needs attention</span></article><article class="panel staff-stat"><span class="staff-stat-icon strip-green">✓</span><div><b>${state.issues.filter(i => i.status === "Resolved").length}</b><small>Resolved this month</small></div><span class="staff-stat-note positive-text">Keep it up!</span></article></div>
      <section class="panel queue-panel"><div class="panel-heading"><div><h2>My work queue</h2><p>Update the status as you make progress.</p></div><a href="#issues" class="text-link">Full issue list <span>→</span></a></div><div class="table-scroll"><table><thead><tr><th>ISSUE</th><th>LOCATION</th><th>PRIORITY</th><th>STATUS</th><th>ASSIGNED TO</th><th></th></tr></thead><tbody>${active.map(issue => `<tr><td><div class="issue-title-cell"><span class="category-icon ${escapeHtml(statusClass(issue.category))}">${iconFor(issue.category)}</span><span><b>${escapeHtml(issue.title)}</b><small>${escapeHtml(shortId(issue.id))} · ${dateLabel(issue.createdAt)}</small></span></div></td><td>${escapeHtml(issue.location)}</td><td>${priorityPill(issue.priority)}</td><td><select class="status-select" data-status-id="${escapeHtml(issue.id)}" aria-label="Update status for ${escapeHtml(issue.title)}">${["Open", "In Progress", "Resolved"].map(v => `<option ${v === issue.status ? "selected" : ""}>${v}</option>`).join("")}</select></td><td><span class="assigned-cell"><span class="avatar tiny-avatar">${initials(issue.assignee || "Unassigned")}</span>${escapeHtml(issue.assignee || "Unassigned")}</span></td><td><button class="row-menu">···</button></td></tr>`).join("")}</tbody></table>${active.length ? "" : `<div class="empty-state">Your queue is clear. Nice work!</div>`}</div></section>`;
    $(".queue-panel").addEventListener("change", async (event) => { const id = event.target.dataset.statusId; if (!id) return; try { await CampusApi.updateIssue(id, { status: event.target.value }); state.issues = await CampusApi.getIssues(); toast("Status updated", `${shortId(id)} moved to ${event.target.value}.`); renderStaff(); } catch { toast("Update failed", "Please try again.", true); } });
    $("#staff-export").addEventListener("click", exportQueue);
  }
  function exportQueue() {
    const columns = ["id", "title", "category", "priority", "status", "location", "assignee", "createdAt"];
    const quote = (v) => `"${String(v ?? "").replaceAll('"', '""')}"`;
    const csv = [columns.join(","), ...state.issues.map(issue => columns.map(key => quote(issue[key])).join(","))].join("\n");
    const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv" })); link.download = "campus-issue-queue.csv"; link.click(); URL.revokeObjectURL(link.href); toast("Queue exported", "Your issue list downloaded as a CSV file.");
  }
  function toast(title, message, error = false) {
    const node = document.createElement("div"); node.className = `toast ${error ? "toast-error" : ""}`; node.innerHTML = `<span class="toast-check">${error ? "!" : "✓"}</span><span><b>${escapeHtml(title)}</b><small>${escapeHtml(message)}</small></span><button aria-label="Dismiss">×</button>`; $("#toast-region").append(node); $("button", node).addEventListener("click", () => node.remove()); setTimeout(() => node.remove(), 4800);
  }
  async function showIssue(id) {
    const issue = state.issues.find(item => item.id === id); if (!issue) return;
    const existing = $("#issue-dialog"); if (existing) existing.remove();
    const dialog = document.createElement("dialog"); dialog.id = "issue-dialog"; dialog.className = "issue-dialog";
    dialog.innerHTML = `<div class="dialog-top"><span>${shortId(issue.id)} · Reported ${dateLabel(issue.createdAt)}</span><button class="dialog-close" aria-label="Close dialog">×</button></div><h2>${escapeHtml(issue.title)}</h2><p class="dialog-location">⌖ ${escapeHtml(issue.location)}</p><div class="dialog-pills">${priorityPill(issue.priority)} ${statusPill(issue.status)}</div><div class="dialog-description">${escapeHtml(issue.description || "No additional details provided.")}</div><div class="dialog-meta"><span>Category</span><b>${escapeHtml(issue.category)}</b><span>Assigned to</span><b>${escapeHtml(issue.assignee || "Unassigned")}</b></div>`;
    document.body.append(dialog); dialog.showModal(); $(".dialog-close", dialog).addEventListener("click", () => dialog.close()); dialog.addEventListener("click", e => { if (e.target === dialog) dialog.close(); }); dialog.addEventListener("close", () => dialog.remove());
  }
  async function render() {
    const hash = location.hash.replace("#", "") || "dashboard"; state.page = pageTitles[hash] ? hash : "dashboard";
    document.querySelectorAll(".nav-link").forEach(link => link.classList.toggle("active", link.dataset.page === state.page));
    $("#topbar-title").textContent = pageTitles[state.page]; document.title = `${pageTitles[state.page]} — CampusCare`;
    document.body.classList.remove("nav-open");
    $("#nav-open-count").textContent = state.issues.filter(i => i.status !== "Resolved").length;
    if (state.page === "dashboard") { const summary = await CampusApi.getSummary(); renderDashboard(summary); }
    if (state.page === "report") renderReport();
    if (state.page === "issues") renderIssues();
    if (state.page === "staff") await renderStaff();
  }
  document.addEventListener("click", (event) => { const button = event.target.closest("[data-open-issue]"); if (button) showIssue(button.dataset.openIssue); });
  $("#mobile-menu").addEventListener("click", () => document.body.classList.toggle("nav-open"));
  $("#today-label").textContent = new Intl.DateTimeFormat("en", { weekday: "short", month: "short", day: "numeric" }).format(new Date());
  window.addEventListener("hashchange", render);
  (async () => { try { state.issues = await CampusApi.getIssues(); } catch (error) { toast("Unable to load issues", "Check your API connection and try again.", true); } render(); })();
})();
