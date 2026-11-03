/* Campus Care — Frontend Application Logic
 * Supports role-based access for Student, Professor, Maintenance Staff, and Admin.
 * Prepared for Spring Boot + PostgreSQL REST API integration.
 */
(() => {
  const CampusApi = window.CampusApi;
  const MockStore = window.MockStore;
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const content = $("#app-content");
  const appShell = $("#app-shell");

  const state = {
    page: "overview",
    currentUser: null,
    issues: [],
    search: "",
    status: "All issues",
    category: "All categories",
    priority: "All priorities",
    evidenceFile: null
  };

  const pageTitles = {
    login: "Login",
    overview: "Overview",
    "report-issue": "Report an Issue",
    "issues-reported": "Issue Reported",
    "issue-management": "Issue Management",
    "staff-workspace": "Staff Workspace",
    profile: "Profile"
  };

  // Route alias mapping for backwards-compatibility
  const routeAliases = {
    dashboard: "overview",
    report: "report-issue",
    issues: "issue-management",
    staff: "staff-workspace"
  };

  /* Helper utilities */
  const escapeHtml = (value = "") =>
    String(value).replace(/[&<>"']/g, (char) => ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#39;"
    })[char]);

  const initials = (name = "U") =>
    name
      .split(/\s+/)
      .map((part) => part[0])
      .slice(0, 2)
      .join("")
      .toUpperCase();

  const shortId = (id = "") => `#${String(id).replace(/^ISS-/, "")}`;

  const dateLabel = (value) => {
    try {
      return new Intl.DateTimeFormat("en", { month: "short", day: "numeric", year: "numeric" }).format(new Date(value));
    } catch {
      return "Recent";
    }
  };

  const statusClass = (status = "") => status.toLowerCase().replaceAll(" ", "-");
  const priorityClass = (priority = "") => priority.toLowerCase();

  const statusPill = (status = "Reported") =>
    `<span class="status-pill ${escapeHtml(statusClass(status))}"><i></i>${escapeHtml(status)}</span>`;

  const priorityPill = (priority = "Medium") =>
    `<span class="priority ${escapeHtml(priorityClass(priority))}"><span class="priority-dot"></span>${escapeHtml(priority)}</span>`;

  const iconFor = (category = "") =>
    ({
      Plumbing: "◉",
      Electrical: "ϟ",
      Cleaning: "✳",
      HVAC: "⌁",
      Grounds: "❋",
      Furniture: "▦",
      Safety: "◇",
      Networking: "⌗",
      Carpentry: "⚒",
      "General Maintenance": "⚙"
    })[category] || "◈";

  function shellHeader(eyebrow, title, subtitle, action = "") {
    return `<div class="page-heading">
      <div>
        <div class="eyebrow">${escapeHtml(eyebrow)}</div>
        <h1>${escapeHtml(title)}</h1>
        <p>${escapeHtml(subtitle)}</p>
      </div>
      ${action}
    </div>`;
  }

  function issueRow(issue, showActions = true) {
    const evidenceTag = issue.evidence
      ? `<span class="evidence-tag" title="${escapeHtml(issue.evidence.name || issue.evidence.fileName || 'Evidence')}">${(issue.evidence.type === "video" || (issue.evidence.fileType && issue.evidence.fileType.includes("video"))) ? "🎥 Video" : "📷 Photo"}</span>`
      : "";

    const user = state.currentUser;
    const isAdmin = user && user.role === "Admin";
    const actionLabel = isAdmin && (!issue.assignee || issue.assignee === "Unassigned") ? "Assign" : "Details";
    const actionBtnClass = actionLabel === "Assign" ? "button button-primary button-sm" : "button button-outline button-sm";

    return `<tr>
      <td><span class="issue-id">${escapeHtml(shortId(issue.id))}</span></td>
      <td>
        <div class="issue-title-cell">
          <span class="category-icon ${escapeHtml(statusClass(issue.category))}">${iconFor(issue.category)}</span>
          <span>
            <b>${escapeHtml(issue.title)}</b>
            <small>${escapeHtml(issue.location)}</small>
          </span>
          ${evidenceTag}
        </div>
      </td>
      <td>${priorityPill(issue.priority)}</td>
      <td>${statusPill(issue.status)}</td>
      <td>
        <span class="assigned-cell">
          <span class="avatar tiny-avatar">${initials(issue.assignee || "Unassigned")}</span>
          ${escapeHtml(issue.assignee || "Unassigned")}
        </span>
      </td>
      <td class="date-cell">${dateLabel(issue.createdAt)}</td>
      ${showActions ? `<td><button class="${actionBtnClass}" data-open-issue="${escapeHtml(issue.id)}">${actionLabel}</button></td>` : "<td></td>"}
    </tr>`;
  }

  /* Role-based navigation rendering */
  function updateNavigation() {
    const nav = $("#main-nav");
    if (!nav) return;

    const user = state.currentUser;
    if (!user) return;

    let links = [];
    const role = user.role;

    if (role === "Student" || role === "Professor") {
      links = [
        { id: "overview", label: "Overview", icon: "▦", count: null },
        { id: "report-issue", label: "Report an Issue", icon: "＋", count: null },
        {
          id: "issues-reported",
          label: "Issue Reported",
          icon: "📋",
          count: state.issues.filter(i => (i.reportedBy === user.id || i.reportedByName === user.name) && i.status !== "Resolved" && i.status !== "Closed").length
        },
        { id: "profile", label: "Profile", icon: "👤", count: null }
      ];
    } else if (role === "Maintenance Staff") {
      links = [
        { id: "overview", label: "Overview", icon: "▦", count: null },
        { id: "report-issue", label: "Report an Issue", icon: "＋", count: null },
        { id: "issues-reported", label: "Issue Reported", icon: "📋", count: null },
        {
          id: "issue-management",
          label: "Issue Management",
          icon: "⚙",
          count: state.issues.filter(i => i.status !== "Resolved" && i.status !== "Closed").length
        },
        { id: "staff-workspace", label: "Staff Workspace", icon: "🛠", count: null },
        { id: "profile", label: "Profile", icon: "👤", count: null }
      ];
    } else if (role === "Admin") {
      links = [
        { id: "overview", label: "Overview", icon: "▦", count: null },
        { id: "report-issue", label: "Report an Issue", icon: "＋", count: null },
        { id: "issues-reported", label: "Issue Reported", icon: "📋", count: null },
        {
          id: "issue-management",
          label: "Issue Management",
          icon: "⚙",
          count: state.issues.filter(i => i.status !== "Resolved" && i.status !== "Closed").length
        },
        { id: "staff-workspace", label: "Staff Workspace", icon: "🛠", count: null },
        { id: "profile", label: "Profile", icon: "👤", count: null }
      ];
    }

    nav.innerHTML = links
      .map(
        (link) => `<a href="#${link.id}" data-page="${link.id}" class="nav-link ${state.page === link.id ? "active" : ""}">
          <span class="nav-icon">${link.icon}</span>
          ${escapeHtml(link.label)}
          ${link.count ? `<span class="nav-count">${link.count}</span>` : ""}
        </a>`
      )
      .join("");

    // Update profile cards and topbar badges
    const userInitials = initials(user.name);
    $("#sidebar-user-avatar").textContent = userInitials;
    $("#sidebar-user-name").textContent = user.name;
    $("#sidebar-user-role").textContent = user.role;
    $("#topbar-user-avatar").textContent = userInitials;
    $("#topbar-role-badge").textContent = user.role;
  }

  /* ==========================================================================
     Screen 1: Login Page
     ========================================================================== */
  function renderLogin() {
    appShell.classList.add("auth-mode");

    content.innerHTML = `
      <div class="login-wrapper">
        <div class="login-card">
          <div class="login-branding">
            <span class="login-mark">C</span>
            <h1>Campus Care</h1>
            <p>Campus Maintenance Management System</p>
            <span class="login-campus-tag">SRMIST, Delhi-NCR Campus</span>
          </div>

          <div class="login-error-banner" id="login-error-banner" role="alert"></div>

          <form class="login-form" id="login-form">
            <div>
              <label class="field-label" for="login-role">Login as <em>*</em></label>
              <select class="text-input" id="login-role" name="roleChoice" required>
                <option value="Student / Professor" selected>Student / Professor</option>
                <option value="Maintenance Staff">Maintenance Staff</option>
              </select>
            </div>

            <div>
              <label class="field-label" for="login-email">Official College Email <em>*</em></label>
              <input
                class="text-input"
                id="login-email"
                name="email"
                type="email"
                placeholder="name@srmist.edu.in"
                required
                autocomplete="email"
              />
              <div class="input-helper-text">Please enter your official SRMIST institutional email.</div>
            </div>

            <div>
              <label class="field-label" for="login-password">Password <em>*</em></label>
              <div class="password-input-wrap">
                <input
                  class="text-input"
                  id="login-password"
                  name="password"
                  type="password"
                  placeholder="Enter your password"
                  required
                  autocomplete="current-password"
                />
                <button type="button" class="password-toggle-btn" id="toggle-password-btn" aria-label="Toggle password visibility">Show</button>
              </div>
            </div>

            <button type="submit" class="button button-primary" style="width: 100%; height: 42px; margin-top: 8px;">
              Login
            </button>
          </form>

          ${window.CAMPUS_CONFIG && window.CAMPUS_CONFIG.USE_MOCK_DATA ? `
          <div class="login-demo-helper">
            <p>Quick Demo Sign-In (Select Role)</p>
            <div class="demo-pills">
              <button type="button" class="demo-pill" data-demo="student">Student (Rahul)</button>
              <button type="button" class="demo-pill" data-demo="professor">Professor (Dr. Priya)</button>
              <button type="button" class="demo-pill" data-demo="staff">Maintenance Staff (Amit)</button>
            </div>
          </div>
          ` : ""}
        </div>
      </div>
    `;

    // Show / hide password
    const toggleBtn = $("#toggle-password-btn");
    const pwdInput = $("#login-password");
    toggleBtn.addEventListener("click", () => {
      const isPwd = pwdInput.type === "password";
      pwdInput.type = isPwd ? "text" : "password";
      toggleBtn.textContent = isPwd ? "Hide" : "Show";
    });

    // Quick demo buttons (active only in mock mode)
    if (window.CAMPUS_CONFIG && window.CAMPUS_CONFIG.USE_MOCK_DATA) {
      $$(".demo-pill").forEach((btn) => {
        btn.addEventListener("click", () => {
          const type = btn.dataset.demo;
          if (type === "student") {
            $("#login-role").value = "Student / Professor";
            $("#login-email").value = "rahul.s@srmist.edu.in";
            $("#login-password").value = "password123";
          } else if (type === "professor") {
            $("#login-role").value = "Student / Professor";
            $("#login-email").value = "priya.nair@srmist.edu.in";
            $("#login-password").value = "password123";
          } else if (type === "staff") {
            $("#login-role").value = "Maintenance Staff";
            $("#login-email").value = "amit.k@srmist.edu.in";
            $("#login-password").value = "password123";
          }
        });
      });
    }

    // Form submission
    $("#login-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const errBanner = $("#login-error-banner");
      errBanner.style.display = "none";

      const roleChoice = $("#login-role").value;
      const email = $("#login-email").value.trim();
      const password = $("#login-password").value;

      // Basic validation
      if (!email || !email.includes("@")) {
        errBanner.textContent = "Please provide a valid college email address.";
        errBanner.style.display = "block";
        return;
      }
      if (!password) {
        errBanner.textContent = "Please enter your password.";
        errBanner.style.display = "block";
        return;
      }

      try {
        const user = await CampusApi.login(email, password, roleChoice);
        state.currentUser = user;
        toast("Login Successful", `Welcome to Campus Care, ${user.name}!`);
        appShell.classList.remove("auth-mode");
        location.hash = "#overview";
      } catch (err) {
        errBanner.textContent = err.message || "Invalid credentials. Please verify and retry.";
        errBanner.style.display = "block";
      }
    });
  }

  /* ==========================================================================
     Screen 2: Overview Page
     ========================================================================== */
  function renderOverview(summary) {
    const rawName = state.currentUser ? state.currentUser.name : "User";
    const cleanName = rawName.replace(/^(Dr\.|Prof\.|Mr\.|Ms\.|Mrs\.)\s+/i, "");
    const firstName = cleanName.split(/\s+/)[0] || "User";
    const today = new Intl.DateTimeFormat("en", {
      weekday: "long",
      month: "long",
      day: "numeric",
      year: "numeric"
    }).format(new Date()).toUpperCase();

    // Category chart palette
    const colors = ["#1d4ed8", "#0284c7", "#0d9488", "#ca8a04", "#7c3aed", "#e11d48", "#475569"];

    // Build dynamic donut conic gradient from centralized summary.categories
    const categories = summary.categories || [];
    const totalCategoryCount = categories.reduce((sum, item) => sum + (item.count || 0), 0) || 1;

    let gradientSegments = [];
    let currentPct = 0;
    categories.forEach((cat, idx) => {
      const slicePct = Math.round((cat.count / totalCategoryCount) * 100);
      const nextPct = currentPct + slicePct;
      const color = colors[idx % colors.length];
      gradientSegments.push(`${color} ${currentPct}% ${nextPct}%`);
      currentPct = nextPct;
    });

    const conicGradientStyle = gradientSegments.length > 0
      ? `conic-gradient(${gradientSegments.join(", ")})`
      : `conic-gradient(#1d4ed8 0% 100%)`;

    const recent = [...state.issues]
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      .slice(0, 5);

    content.innerHTML = `
      ${shellHeader(
        today,
        `Hello, ${firstName}`,
        "Here’s what’s happening across your campus today.",
        `<a class="button button-primary" href="#report-issue"><span>＋</span> Report an issue</a>`
      )}

      <section class="stat-grid">
        <article class="stat-card">
          <div class="stat-top">
            <span>Open issues</span>
            <span class="stat-icon blue-tint">☷</span>
          </div>
          <div class="stat-value">${summary.open}<span class="stat-context">Across campus</span></div>
          <div class="stat-foot">
            <span class="trend up">↗ ${summary.newThisWeek} new</span>
            <span>this week</span>
          </div>
        </article>

        <article class="stat-card">
          <div class="stat-top">
            <span>In progress</span>
            <span class="stat-icon violet-tint">◷</span>
          </div>
          <div class="stat-value">${summary.inProgress}<span class="stat-context">Being resolved</span></div>
          <div class="stat-foot">
            <span class="neutral-dot">●</span>
            <span>Assigned to staff</span>
          </div>
        </article>

        <article class="stat-card">
          <div class="stat-top">
            <span>Resolved this month</span>
            <span class="stat-icon green-tint">✓</span>
          </div>
          <div class="stat-value">${summary.resolved}<span class="stat-context">Resolved issues</span></div>
          <div class="stat-foot">
            <span class="trend up">↗ 12%</span>
            <span>vs. previous period</span>
          </div>
        </article>

        <article class="stat-card">
          <div class="stat-top">
            <span>Avg. resolution time</span>
            <span class="stat-icon amber-tint">◷</span>
          </div>
          <div class="stat-value">${summary.avgResolution}<span class="stat-context">Campus average</span></div>
          <div class="stat-foot">
            <span class="trend up">↘ 8%</span>
            <span>faster response</span>
          </div>
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="panel activity-panel">
          <div class="panel-heading">
            <div>
              <h2>Issue activity</h2>
              <p>Incoming reports vs. resolutions over the last 7 days</p>
            </div>
            <span class="today-label">Current week</span>
          </div>
          <div class="chart-legend">
            <span><i class="legend-blue"></i>Reported</span>
            <span><i class="legend-light"></i>Resolved</span>
          </div>
          <div class="chart-wrap">
            <div class="chart-y">
              <span>20</span><span>15</span><span>10</span><span>5</span><span>0</span>
            </div>
            <div class="chart" aria-label="Bar chart showing weekly reported and resolved issues">
              <div class="chart-gridlines"><i></i><i></i><i></i><i></i><i></i></div>
              ${summary.weekly
                .map(
                  (day) => `<div class="chart-day">
                    <div class="bar-pair">
                      <span class="bar reported" style="height:${Math.min(day.reported * 5, 100)}%"></span>
                      <span class="bar resolved" style="height:${Math.min(day.resolved * 5, 100)}%"></span>
                    </div>
                    <small>${escapeHtml(day.day)}</small>
                  </div>`
                )
                .join("")}
            </div>
          </div>
        </article>

        <article class="panel category-panel">
          <div class="panel-heading">
            <div>
              <h2>Issues by category</h2>
              <p>Active issues breakdown by specialized domain</p>
            </div>
          </div>
          <div class="donut-wrap">
            <div class="donut" style="background: ${conicGradientStyle}">
              <div>
                <strong>${summary.open}</strong>
                <small>Open issues</small>
              </div>
            </div>
            <div class="category-legend">
              ${categories
                .map(
                  (item, i) => `<div>
                    <span><i style="background: ${colors[i % colors.length]}"></i>${escapeHtml(item.category || item.name)}</span>
                    <b>${escapeHtml(item.count)}</b>
                  </div>`
                )
                .join("")}
            </div>
          </div>
        </article>
      </section>

      <section class="panel recent-panel">
        <div class="panel-heading">
          <div>
            <h2>Recent campus reports</h2>
            <p>Latest maintenance requests submitted across SRMIST</p>
          </div>
          <a href="#issues-reported" class="button button-outline button-sm">View all reports <span>→</span></a>
        </div>
        ${
          recent.length
            ? `<div class="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>ISSUE</th>
                      <th>DESCRIPTION &amp; LOCATION</th>
                      <th>PRIORITY</th>
                      <th>STATUS</th>
                      <th>ASSIGNED TO</th>
                      <th>REPORTED</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>${recent.map((issue) => issueRow(issue)).join("")}</tbody>
                </table>
              </div>`
            : `<div class="empty-state">No maintenance issues reported yet.</div>`
        }
      </section>
    `;
  }

  /* ==========================================================================
     Screen 3: Report an Issue Page
     ========================================================================== */
  async function renderReport() {
    state.evidenceFile = null;

    const fallbackCategories = [
      "Plumbing", "Electrical", "HVAC", "Carpentry", "Networking",
      "Cleaning", "Furniture", "Grounds", "Safety", "General Maintenance"
    ];
    let categories = fallbackCategories;
    try {
      const fetched = await CampusApi.getCategories();
      if (Array.isArray(fetched) && fetched.length > 0) {
        categories = fetched
          .map((c) => (typeof c === "string" ? c : (c.name || c.category)))
          .filter(Boolean);
      }
    } catch (err) {
      console.warn("Failed to fetch backend categories, using fallback:", err);
    }

    content.innerHTML = `
      ${shellHeader(
        "CAMPUS CARE",
        "Report a maintenance issue",
        "Submit a request to the campus facilities and maintenance team."
      )}

      <div class="report-layout">
        <form class="panel report-form" id="report-form">
          <div class="form-section-title">
            <span class="step-number">1</span>
            <div>
              <h2>Issue details</h2>
              <p>Provide specific details to help the maintenance team respond quickly.</p>
            </div>
          </div>

          <label class="field-label" for="issue-title">What needs attention? <em>*</em></label>
          <input
            class="text-input"
            id="issue-title"
            name="title"
            placeholder="e.g. Water cooler leaking near computer lab"
            required
            maxlength="90"
          />

          <div class="field-row">
            <div>
              <label class="field-label" for="issue-category">Category <em>*</em></label>
              <select class="text-input" id="issue-category" name="category" required>
                <option value="">Select a category</option>
                ${categories
                  .map((v) => `<option value="${escapeHtml(v)}">${escapeHtml(v)}</option>`)
                  .join("")}
              </select>
            </div>
            <div>
              <label class="field-label" for="issue-priority">Priority <em>*</em></label>
              <select class="text-input" id="issue-priority" name="priority" required>
                <option value="">Select priority</option>
                <option value="Low">Low</option>
                <option value="Medium" selected>Medium</option>
                <option value="High">High</option>
                <option value="Critical">Critical</option>
              </select>
            </div>
          </div>

          <label class="field-label" for="issue-location">Where is the issue located? <em>*</em></label>
          <div class="location-input">
            <span>⌖</span>
            <input
              class="text-input"
              id="issue-location"
              name="location"
              placeholder="e.g. Academic Block 1 · 2nd Floor, Room 204"
              required
              maxlength="100"
            />
          </div>

          <label class="field-label" for="issue-description">Description <span class="optional">Optional</span></label>
          <textarea
            class="text-input"
            id="issue-description"
            name="description"
            rows="4"
            placeholder="Describe the issue, hazards, or any details to help our staff locate and fix it..."
          ></textarea>

          <!-- Evidence Attachment Section -->
          <div class="evidence-section">
            <div class="evidence-header">
              <h3>Attach Evidence (Photo / Video)</h3>
              <p>Attach a photo or short video of the issue (Max 10 MB). Supported: JPG, JPEG, PNG, WEBP, MP4, MOV.</p>
            </div>

            <div class="evidence-actions">
              <label class="evidence-upload-btn" for="evidence-camera-input">
                <span>📷</span> Take Photo / Video
              </label>
              <input
                type="file"
                id="evidence-camera-input"
                accept="image/*,video/*"
                capture="environment"
                style="display: none;"
              />

              <label class="evidence-upload-btn" for="evidence-file-input">
                <span>📁</span> Choose from Gallery / Files
              </label>
              <input
                type="file"
                id="evidence-file-input"
                accept="image/jpeg,image/png,image/webp,video/mp4,video/quicktime,.jpg,.jpeg,.png,.webp,.mp4,.mov"
                style="display: none;"
              />
            </div>

            <div id="evidence-preview-container"></div>
            <div id="evidence-error-message" class="evidence-error"></div>
          </div>

          <div class="form-footer">
            <span><em>*</em> Required fields</span>
            <button class="button button-primary" type="submit">Submit issue <span>→</span></button>
          </div>
        </form>

        <aside class="report-aside">
          <h3>Campus Care Process</h3>
          <p>Every report is logged into the SRMIST campus maintenance queue and assigned to specialized technicians based on category and priority.</p>

          <div class="response-note">
            <span>◷</span>
            <div>
              <b>Target Response Time</b>
              <small>Critical: 2-4 hrs · High: 24 hrs · Normal: 1-2 days</small>
            </div>
          </div>

          <div class="response-note">
            <span>🏛</span>
            <div>
              <b>SRMIST Coverage</b>
              <small>All academic blocks, hostels, labs, and sports arenas.</small>
            </div>
          </div>

          <div class="secure-note">
            ◇ Your report is visible to authorized maintenance personnel.
          </div>
        </aside>
      </div>
    `;

    // File validation and preview handler
    const handleFile = (file) => {
      const errBox = $("#evidence-error-message");
      const previewBox = $("#evidence-preview-container");
      errBox.textContent = "";

      if (!file) return;

      const maxSize = 10 * 1024 * 1024; // 10 MB
      if (file.size > maxSize) {
        errBox.textContent = `File size (${(file.size / (1024 * 1024)).toFixed(1)} MB) exceeds the 10 MB limit. Please select a smaller file.`;
        state.evidenceFile = null;
        previewBox.innerHTML = "";
        return;
      }

      const validMimes = [
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/jpg",
        "video/mp4",
        "video/quicktime",
        "video/x-m4v"
      ];

      const isImage = file.type.startsWith("image/");
      const isVideo = file.type.startsWith("video/") || file.name.endsWith(".mov") || file.name.endsWith(".mp4");

      if (!isImage && !isVideo) {
        errBox.textContent = "Unsupported file type. Please upload a JPG, PNG, WEBP, MP4, or MOV file.";
        state.evidenceFile = null;
        previewBox.innerHTML = "";
        return;
      }

      const formattedSize = file.size > 1024 * 1024
        ? `${(file.size / (1024 * 1024)).toFixed(1)} MB`
        : `${Math.round(file.size / 1024)} KB`;

      state.evidenceFile = {
        name: file.name,
        size: formattedSize,
        type: isVideo ? "video" : "image",
        dataUrl: null,
        file: file
      };

      if (isImage) {
        const reader = new FileReader();
        reader.onload = (e) => {
          state.evidenceFile.dataUrl = e.target.result;
          renderEvidencePreview();
        };
        reader.readAsDataURL(file);
      } else {
        renderEvidencePreview();
      }
    };

    const renderEvidencePreview = () => {
      const previewBox = $("#evidence-preview-container");
      if (!state.evidenceFile) {
        previewBox.innerHTML = "";
        return;
      }

      const thumb = state.evidenceFile.type === "image" && state.evidenceFile.dataUrl
        ? `<img class="evidence-thumb" src="${state.evidenceFile.dataUrl}" alt="Preview" />`
        : `<div class="evidence-video-icon">🎥</div>`;

      previewBox.innerHTML = `
        <div class="evidence-preview-card">
          ${thumb}
          <div class="evidence-info">
            <b>${escapeHtml(state.evidenceFile.name)}</b>
            <small>${escapeHtml(state.evidenceFile.size)} · ${state.evidenceFile.type === "video" ? "Video recording" : "Photo attached"}</small>
          </div>
          <button type="button" class="evidence-remove" id="evidence-remove-btn">✕ Remove</button>
        </div>
      `;

      $("#evidence-remove-btn").addEventListener("click", () => {
        state.evidenceFile = null;
        previewBox.innerHTML = "";
        $("#evidence-camera-input").value = "";
        $("#evidence-file-input").value = "";
      });
    };

    $("#evidence-camera-input").addEventListener("change", (e) => handleFile(e.target.files[0]));
    $("#evidence-file-input").addEventListener("change", (e) => handleFile(e.target.files[0]));
    $("#report-form").addEventListener("submit", submitIssue);
  }

  async function submitIssue(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);

    const payload = {
      title: form.get("title").trim(),
      category: form.get("category"),
      priority: form.get("priority"),
      location: form.get("location").trim(),
      description: form.get("description").trim(),
      evidence: state.evidenceFile
        ? {
            name: state.evidenceFile.name,
            size: state.evidenceFile.size,
            type: state.evidenceFile.type
          }
        : null,
      evidenceFile: state.evidenceFile ? state.evidenceFile.file : null
    };

    try {
      const created = await CampusApi.createIssue(payload);
      state.issues = await CampusApi.getIssues();
      if (created && created.evidenceUploadFailed) {
        toast(
          "Issue Created (Evidence Failed)",
          `Ticket ${shortId(created.id)} was registered, but evidence upload failed: ${created.evidenceUploadError}. You can attach evidence later.`,
          true
        );
      } else {
        toast("Issue Reported", `Ticket ${shortId(created.id)} registered and forwarded to facilities team.`);
      }
      location.hash = "#issues-reported";
    } catch (error) {
      toast("Submission Failed", error.message || "Please check your inputs and try again.", true);
    }
  }

  /* ==========================================================================
     Screen 4: Issue Reported (User-Specific for Student/Prof, Campus for Staff)
     ========================================================================== */
  function renderIssuesReported() {
    const user = state.currentUser;
    const isStudentOrProf = user.role === "Student" || user.role === "Professor";

    // Filter issues: Students & Professors ONLY see their own issues
    const displayIssues = isStudentOrProf
      ? state.issues.filter((i) => i.reportedBy === user.id || i.reportedByName === user.name)
      : state.issues;

    const totalCount = displayIssues.length;
    const openCount = displayIssues.filter((i) => i.status === "Reported" || i.status === "Open").length;
    const inProgressCount = displayIssues.filter((i) => i.status === "In Progress").length;
    const resolvedCount = displayIssues.filter((i) => i.status === "Resolved" || i.status === "Verified" || i.status === "Closed").length;

    content.innerHTML = `
      ${shellHeader(
        "CAMPUS CARE",
        "Issue Reported",
        isStudentOrProf
          ? "View and follow up on the status of maintenance requests reported by you."
          : "Overview of maintenance requests submitted by campus students and faculty.",
        `<a class="button button-primary" href="#report-issue"><span>＋</span> Report an issue</a>`
      )}

      <section class="stat-strip">
        <div>
          <span class="strip-icon strip-blue">📋</span>
          <span>
            <b>${totalCount}</b>
            <small>${isStudentOrProf ? "My reports" : "Total reports"}</small>
          </span>
        </div>
        <div>
          <span class="strip-icon strip-amber">☷</span>
          <span>
            <b>${openCount}</b>
            <small>Open / Pending</small>
          </span>
        </div>
        <div>
          <span class="strip-icon strip-violet">◷</span>
          <span>
            <b>${inProgressCount}</b>
            <small>In progress</small>
          </span>
        </div>
        <div>
          <span class="strip-icon strip-green">✓</span>
          <span>
            <b>${resolvedCount}</b>
            <small>Resolved</small>
          </span>
        </div>
      </section>

      <section class="panel issues-panel">
        <div class="panel-heading" style="padding: 16px 20px; border-bottom: 1px solid var(--line); margin-bottom: 0;">
          <div>
            <h2>${isStudentOrProf ? "Your submitted issues" : "Reported issues register"}</h2>
            <p>${isStudentOrProf ? "Track progress and staff responses in real time" : "Review and triage incoming campus tickets"}</p>
          </div>
          <span class="today-label">${displayIssues.length} records</span>
        </div>

        <div class="table-scroll">
          <table>
            <thead>
              <tr>
                <th>ISSUE ID</th>
                <th>DESCRIPTION &amp; LOCATION</th>
                <th>CATEGORY</th>
                <th>PRIORITY</th>
                <th>STATUS</th>
                <th>ASSIGNED STAFF</th>
                <th>REPORTED ON</th>
                <th>ACTIONS</th>
              </tr>
            </thead>
            <tbody>
              ${
                displayIssues.length
                  ? displayIssues.map((issue) => issueRow(issue, true)).join("")
                  : `<tr><td colspan="8"><div class="empty-state">${
                      isStudentOrProf
                        ? "You have not reported any issues yet. Click 'Report an issue' to submit one."
                        : "No issues have been reported."
                    }</div></td></tr>`
              }
            </tbody>
          </table>
        </div>
      </section>
    `;
  }

  /* ==========================================================================
     Screen 5: Issue Management (Staff & Admin Only)
     ========================================================================== */
  function filteredIssues() {
    return state.issues.filter((issue) => {
      const matchStatus = state.status === "All issues" || issue.status === state.status;
      const matchCategory = state.category === "All categories" || issue.category === state.category;
      const matchPriority = state.priority === "All priorities" || issue.priority === state.priority;
      const matchSearch =
        !state.search ||
        `${issue.title} ${issue.location} ${issue.id} ${issue.assignee} ${issue.category}`
          .toLowerCase()
          .includes(state.search.toLowerCase());
      return matchStatus && matchCategory && matchPriority && matchSearch;
    });
  }

  function renderIssueManagement() {
    const rows = filteredIssues();
    const categories = [...new Set(state.issues.map((i) => i.category))].sort();

    content.innerHTML = `
      ${shellHeader(
        "FACILITIES &amp; MAINTENANCE",
        "Issue management",
        "Administrative triage queue to assign, update, and resolve campus maintenance tickets.",
        `<a class="button button-primary" href="#report-issue"><span>＋</span> New issue</a>`
      )}

      <section class="stat-strip">
        <div>
          <span class="strip-icon strip-blue">☷</span>
          <span>
            <b>${state.issues.filter((i) => i.status !== "Resolved" && i.status !== "Closed").length}</b>
            <small>Active issues</small>
          </span>
        </div>
        <div>
          <span class="strip-icon strip-violet">◷</span>
          <span>
            <b>${state.issues.filter((i) => i.status === "In Progress").length}</b>
            <small>In progress</small>
          </span>
        </div>
        <div>
          <span class="strip-icon strip-amber">⚑</span>
          <span>
            <b>${state.issues.filter((i) => (i.priority === "High" || i.priority === "Critical") && i.status !== "Resolved").length}</b>
            <small>High / Critical</small>
          </span>
        </div>
        <div>
          <span class="strip-icon strip-green">✓</span>
          <span>
            <b>${state.issues.filter((i) => i.status === "Resolved" || i.status === "Closed").length}</b>
            <small>Resolved</small>
          </span>
        </div>
      </section>

      <section class="panel issues-panel">
        <div class="issue-toolbar">
          <div class="search-box">
            <span>⌕</span>
            <input
              id="issue-search"
              type="search"
              placeholder="Search by ID, keyword, location, assignee..."
              value="${escapeHtml(state.search)}"
              aria-label="Search issues"
            />
          </div>

          <select id="status-filter" aria-label="Filter by status">
            <option>All issues</option>
            ${["Reported", "Open", "In Progress", "Resolved", "Verified", "Closed"]
              .map((v) => `<option ${state.status === v ? "selected" : ""}>${v}</option>`)
              .join("")}
          </select>

          <select id="category-filter" aria-label="Filter by category">
            <option>All categories</option>
            ${categories.map((v) => `<option ${state.category === v ? "selected" : ""}>${escapeHtml(v)}</option>`).join("")}
          </select>

          <select id="priority-filter" aria-label="Filter by priority">
            <option>All priorities</option>
            ${["Low", "Medium", "High", "Critical"]
              .map((v) => `<option ${state.priority === v ? "selected" : ""}>${v}</option>`)
              .join("")}
          </select>

          <button class="filter-button" id="clear-filters">
            ↻ <span>Reset</span>
          </button>
        </div>

        <div class="table-scroll">
          <table>
            <thead>
              <tr>
                <th>ISSUE</th>
                <th>DESCRIPTION &amp; LOCATION</th>
                <th>PRIORITY</th>
                <th>STATUS</th>
                <th>ASSIGNED TO</th>
                <th>REPORTED</th>
                <th>ACTIONS</th>
              </tr>
            </thead>
            <tbody>
              ${
                rows.length
                  ? rows.map((issue) => issueRow(issue, true)).join("")
                  : `<tr><td colspan="7"><div class="empty-state">No issues match the selected filters.</div></td></tr>`
              }
            </tbody>
          </table>
        </div>

        <div class="table-footer">
          <span>Showing <b>${rows.length}</b> of <b>${state.issues.length}</b> total issues</span>
          <span>SRMIST Maintenance Register</span>
        </div>
      </section>
    `;

    // Filter event listeners
    $("#issue-search").addEventListener("input", (e) => {
      state.search = e.target.value;
      const pos = e.target.selectionStart;
      renderIssueManagement();
      const input = $("#issue-search");
      if (input) {
        input.focus();
        input.setSelectionRange(pos, pos);
      }
    });

    $("#status-filter").addEventListener("change", (e) => {
      state.status = e.target.value;
      renderIssueManagement();
    });

    $("#category-filter").addEventListener("change", (e) => {
      state.category = e.target.value;
      renderIssueManagement();
    });

    $("#priority-filter").addEventListener("change", (e) => {
      state.priority = e.target.value;
      renderIssueManagement();
    });

    $("#clear-filters").addEventListener("click", () => {
      state.search = "";
      state.status = "All issues";
      state.category = "All categories";
      state.priority = "All priorities";
      renderIssueManagement();
    });
  }

  /* ==========================================================================
     Screen 6: Staff Workspace (Staff & Admin Only)
     ========================================================================== */
  async function renderStaffWorkspace() {
    let staffList = [];
    try {
      staffList = await CampusApi.getStaff();
    } catch {
      staffList = [];
    }

    const user = state.currentUser;
    const isMaintenance = user && user.role === "Maintenance Staff";
    const isAdmin = user && user.role === "Admin";

    const isAssignedToUser = (i) => {
      if (!user) return false;
      if (i.assigneeId && user.id && String(i.assigneeId).toLowerCase() === String(user.id).toLowerCase()) return true;
      if (i.assignee && user.name && i.assignee.trim().toLowerCase() === user.name.trim().toLowerCase()) return true;
      return false;
    };

    // For maintenance staff: display ONLY issues assigned to the logged-in technician
    // For admin: display the campus-wide active queue
    const activeIssues = isMaintenance
      ? state.issues.filter((i) => isAssignedToUser(i) && i.status !== "Resolved" && i.status !== "Closed")
      : state.issues.filter((i) => i.status !== "Resolved" && i.status !== "Closed");

    const resolvedCount = isMaintenance
      ? state.issues.filter((i) => isAssignedToUser(i) && (i.status === "Resolved" || i.status === "Closed")).length
      : state.issues.filter((i) => i.status === "Resolved" || i.status === "Closed").length;

    const displayName = user && user.name ? user.name.replace(/^(Dr\.|Prof\.|Mr\.|Ms\.|Mrs\.)\s+/i, "").split(/\s+/)[0] : "Facilities";

    content.innerHTML = `
      ${shellHeader(
        "CAMPUS FACILITIES TEAM",
        "Staff workspace",
        isMaintenance
          ? "Your personal maintenance work queue and campus facilities coverage."
          : "Your team’s work queue and campus coverage at a glance.",
        `<button class="button button-outline" id="staff-export">↓ <span>Export queue (CSV)</span></button>`
      )}

      <section class="staff-welcome">
        <div>
          <span class="staff-status-dot"></span> TEAM STATUS: ON CAMPUS
          <h2>Hello, ${escapeHtml(displayName)}</h2>
          <p>${activeIssues.length} active maintenance ${activeIssues.length === 1 ? "request" : "requests"} ${isMaintenance ? "assigned to your queue today." : "need your team’s attention today."}</p>
        </div>
        <div class="staff-avatars">
          ${staffList
            .slice(0, 4)
            .map((person, i) => `<span class="avatar staff-avatar staff-${i}" title="${escapeHtml(person.name)}">${initials(person.name)}</span>`)
            .join("")}
          <span class="avatar staff-more">+${Math.max(staffList.length - 4, 0)}</span>
        </div>
      </section>

      <div class="staff-stats">
        <article class="panel staff-stat">
          <span class="staff-stat-icon strip-blue">☷</span>
          <div>
            <b>${activeIssues.length}</b>
            <small>${isMaintenance ? "In your queue" : "Active in queue"}</small>
          </div>
          <span class="staff-stat-note">${isMaintenance ? "Assigned to you" : "Campus coverage"}</span>
        </article>

        <article class="panel staff-stat">
          <span class="staff-stat-icon strip-amber">⚑</span>
          <div>
            <b>${activeIssues.filter((i) => i.priority === "High" || i.priority === "Critical").length}</b>
            <small>High / Critical</small>
          </div>
          <span class="staff-stat-note urgent-text">Urgent attention</span>
        </article>

        <article class="panel staff-stat">
          <span class="staff-stat-icon strip-green">✓</span>
          <div>
            <b>${resolvedCount}</b>
            <small>Resolved tickets</small>
          </div>
          <span class="staff-stat-note positive-text">${isMaintenance ? "Completed by you" : "Closed successfully"}</span>
        </article>
      </div>

      <!-- Work Queue Panel -->
      <section class="panel queue-panel" style="margin-bottom: 24px;">
        <div class="panel-heading">
          <div>
            <h2>${isMaintenance ? "My active work queue" : "Campus active work queue"}</h2>
            <p>${isMaintenance ? "Update ticket status inline for issues assigned to you." : "Update ticket status inline as your team conducts field repairs."}</p>
          </div>
          <a href="#issue-management" class="button button-outline button-sm">Full issue list <span>→</span></a>
        </div>

        <div class="table-scroll">
          <table>
            <thead>
              <tr>
                <th>ISSUE</th>
                <th>LOCATION</th>
                <th>PRIORITY</th>
                <th>STATUS</th>
                <th>ASSIGNED TO</th>
                <th>ACTIONS</th>
              </tr>
            </thead>
            <tbody>
              ${
                activeIssues.length
                  ? activeIssues
                      .map(
                        (issue) => `<tr>
                        <td>
                          <div class="issue-title-cell">
                            <span class="category-icon ${escapeHtml(statusClass(issue.category))}">${iconFor(issue.category)}</span>
                            <span>
                              <b>${escapeHtml(issue.title)}</b>
                              <small>${escapeHtml(shortId(issue.id))} · ${dateLabel(issue.createdAt)}</small>
                            </span>
                          </div>
                        </td>
                        <td>${escapeHtml(issue.location)}</td>
                        <td>${priorityPill(issue.priority)}</td>
                        <td>
                          <select class="status-select" data-status-id="${escapeHtml(issue.id)}" aria-label="Update status for ${escapeHtml(issue.title)}">
                            ${["Reported", "Open", "In Progress", "Resolved", "Verified", "Closed"]
                              .map((v) => `<option value="${v}" ${v === issue.status ? "selected" : ""}>${v}</option>`)
                              .join("")}
                          </select>
                        </td>
                        <td>
                          <span class="assigned-cell">
                            <span class="avatar tiny-avatar">${initials(issue.assignee || "Unassigned")}</span>
                            ${escapeHtml(issue.assignee || "Unassigned")}
                          </span>
                        </td>
                        <td>
                          <button class="button button-outline button-sm" data-open-issue="${escapeHtml(issue.id)}">Details</button>
                        </td>
                      </tr>`
                      )
                      .join("")
                  : `<tr><td colspan="6"><div class="empty-state">${isMaintenance ? "Your active queue is completely clear. Excellent work!" : "The active campus queue is completely clear."}</div></td></tr>`
              }
            </tbody>
          </table>
        </div>
      </section>

      <!-- All Staff Section -->
      <section class="panel all-staff-section">
        <div class="panel-heading">
          <div>
            <h2>All maintenance staff</h2>
            <p>Campus technician directory, specialized domains, and active workloads.</p>
          </div>
          <span class="today-label">${staffList.length} Technicians</span>
        </div>

        <div class="staff-grid">
          ${staffList
            .map(
              (member) => `
            <div class="panel staff-card">
              <div class="staff-card-header">
                <span class="avatar avatar-blue">${initials(member.name)}</span>
                <div>
                  <b>${escapeHtml(member.name)}</b>
                  <small>${escapeHtml(member.field)} Specialist</small>
                </div>
              </div>
              <div class="staff-tags">
                <span class="workload-pill ${escapeHtml((member.workload || "moderate").toLowerCase())}">
                  Workload: ${escapeHtml(member.workload || "Moderate")}
                </span>
                <span class="availability-pill ${escapeHtml((member.status || "available").toLowerCase().replace(" ", "-"))}">
                  ● ${escapeHtml(member.status || "Available")}
                </span>
              </div>
              <div class="staff-card-footer">
                <span>Active tasks: <b>${member.activeIssues || 0}</b></span>
                <span>${escapeHtml(member.phone || "")}</span>
              </div>
            </div>`
            )
            .join("")}
        </div>
      </section>
    `;

    // Queue inline status change
    $(".queue-panel").addEventListener("change", async (e) => {
      const id = e.target.dataset.statusId;
      if (!id) return;
      try {
        await CampusApi.updateIssue(id, { status: e.target.value });
        state.issues = await CampusApi.getIssues();
        toast("Status Updated", `${shortId(id)} updated to ${e.target.value}.`);
        await renderStaffWorkspace();
      } catch (err) {
        toast("Update Failed", err.message || "Could not update status. Try again.", true);
      }
    });

    $("#staff-export").addEventListener("click", exportQueue);
  }

  function exportQueue() {
    const columns = ["id", "title", "category", "priority", "status", "location", "assignee", "createdAt"];
    const quote = (v) => `"${String(v ?? "").replaceAll('"', '""')}"`;
    const csv = [columns.join(","), ...state.issues.map((issue) => columns.map((key) => quote(issue[key])).join(","))].join("\n");

    const link = document.createElement("a");
    link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv" }));
    link.download = "campus-care-issue-queue.csv";
    link.click();
    URL.revokeObjectURL(link.href);
    toast("Queue Exported", "The issue queue was downloaded as a CSV file.");
  }

  /* ==========================================================================
     Screen 7: Profile Page (Role-Aware)
     ========================================================================== */
  function renderProfile() {
    const user = state.currentUser;
    if (!user) return;

    let roleSpecificHtml = "";

    if (user.role === "Student") {
      const myReported = state.issues.filter((i) => i.reportedBy === user.id || i.reportedByName === user.name).length;
      const myResolved = state.issues.filter(
        (i) => (i.reportedBy === user.id || i.reportedByName === user.name) && (i.status === "Resolved" || i.status === "Closed")
      ).length;

      roleSpecificHtml = `
        <div class="info-item">
          <span>Branch / Program</span>
          <b>${escapeHtml(user.branch || "Computer Science & Engineering")}</b>
        </div>
        <div class="info-item">
          <span>Registration Number</span>
          <b>${escapeHtml(user.registrationNo || "RA2211003010123")}</b>
        </div>
        <div class="info-item">
          <span>Issues Reported</span>
          <b>${myReported}</b>
        </div>
        <div class="info-item">
          <span>Issues Resolved</span>
          <b>${myResolved}</b>
        </div>
      `;
    } else if (user.role === "Professor") {
      const myReported = state.issues.filter((i) => i.reportedBy === user.id || i.reportedByName === user.name).length;

      roleSpecificHtml = `
        <div class="info-item">
          <span>Designation</span>
          <b>${escapeHtml(user.designation || "Associate Professor")}</b>
        </div>
        <div class="info-item">
          <span>Department</span>
          <b>${escapeHtml(user.department || "Department of Mechanical Engineering")}</b>
        </div>
        <div class="info-item">
          <span>Faculty ID</span>
          <b>${escapeHtml(user.facultyId || "FAC-SRM-408")}</b>
        </div>
        <div class="info-item">
          <span>Issues Reported</span>
          <b>${myReported}</b>
        </div>
      `;
    } else if (user.role === "Maintenance Staff") {
      const isAssigned = (i) =>
        (i.assigneeId && user.id && String(i.assigneeId).toLowerCase() === String(user.id).toLowerCase()) ||
        (i.assignee && user.name && i.assignee.trim().toLowerCase() === user.name.trim().toLowerCase());
      const assignedCount = state.issues.filter(isAssigned).length;
      const solvedCount = state.issues.filter((i) => isAssigned(i) && (i.status === "Resolved" || i.status === "Closed")).length;

      roleSpecificHtml = `
        <div class="info-item">
          <span>Field / Domain</span>
          <b>${escapeHtml(user.field || "Electrical")}</b>
        </div>
        <div class="info-item">
          <span>Staff ID</span>
          <b>${escapeHtml(user.staffId || "STF-SRM-019")}</b>
        </div>
        <div class="info-item">
          <span>Assigned Issues</span>
          <b>${assignedCount}</b>
        </div>
        <div class="info-item">
          <span>Solved Issues</span>
          <b>${solvedCount}</b>
        </div>
      `;
    } else if (user.role === "Admin") {
      roleSpecificHtml = `
        <div class="info-item">
          <span>Administrative Designation</span>
          <b>${escapeHtml(user.designation || "Dean & Director of Campus Operations")}</b>
        </div>
        <div class="info-item">
          <span>Office</span>
          <b>${escapeHtml(user.department || "Office of Campus Administration")}</b>
        </div>
        <div class="info-item">
          <span>Access Level</span>
          <b>Full Administrative Access</b>
        </div>
        <div class="info-item">
          <span>Total Campus Tickets</span>
          <b>${state.issues.length}</b>
        </div>
      `;
    }

    content.innerHTML = `
      ${shellHeader(
        "CAMPUS CARE ACCOUNT",
        "User profile",
        "Your official SRMIST registration and maintenance management credentials."
      )}

      <div class="profile-container">
        <section class="panel" style="padding: 0; overflow: hidden;">
          <div class="profile-card-header" style="background: #f8fafc; border-bottom: 1px solid var(--line);">
            <div class="profile-avatar-lg">${initials(user.name)}</div>
            <div class="profile-title-block">
              <h2>${escapeHtml(user.name)}</h2>
              <div class="profile-meta-pills">
                <span class="role-pill">${escapeHtml(user.role)}</span>
                <span class="campus-badge">🏛 SRMIST, Delhi-NCR Campus</span>
              </div>
            </div>
          </div>

          <div class="profile-info-grid">
            <div class="info-item">
              <span>Full Name</span>
              <b>${escapeHtml(user.name)}</b>
            </div>
            <div class="info-item">
              <span>Official College Email</span>
              <b>${escapeHtml(user.email)}</b>
            </div>
            <div class="info-item">
              <span>Contact Number</span>
              <b>${escapeHtml(user.contact || "+91 98765 43210")}</b>
            </div>
            <div class="info-item">
              <span>Account Role</span>
              <b>${escapeHtml(user.role)}</b>
            </div>

            ${roleSpecificHtml}
          </div>

          <div class="profile-actions-panel" style="background: #f8fafc; border-top: 1px solid var(--line);">
            ${window.CAMPUS_CONFIG && window.CAMPUS_CONFIG.USE_MOCK_DATA ? `
            <div class="role-switcher-box">
              <span>Switch Demo Account:</span>
              <button class="button button-outline button-sm" data-switch-user="USR-STU-01">Student (Rahul)</button>
              <button class="button button-outline button-sm" data-switch-user="USR-PROF-01">Professor (Dr. Priya)</button>
              <button class="button button-outline button-sm" data-switch-user="USR-STF-01">Staff (Amit)</button>
            </div>
            ` : ""}
            <button class="button button-danger button-sm" id="profile-signout-btn">Sign out</button>
          </div>
        </section>
      </div>
    `;

    // Role switcher for testing (active in mock mode only)
    if (window.CAMPUS_CONFIG && window.CAMPUS_CONFIG.USE_MOCK_DATA) {
      $$("[data-switch-user]").forEach((btn) => {
        btn.addEventListener("click", async () => {
          const users = await CampusApi.getUsers();
          const target = users.find((u) => u.id === btn.dataset.switchUser);
          if (target && window.MockStore) {
            state.currentUser = window.MockStore.setCurrentUser(target);
            toast("Role Switched", `Logged in as ${target.name} (${target.role})`);
            render();
          }
        });
      });
    }

    $("#profile-signout-btn").addEventListener("click", handleLogout);
  }

  async function handleLogout() {
    await CampusApi.logout();
    state.currentUser = null;
    toast("Signed Out", "You have been signed out of Campus Care.");
    location.hash = "#login";
  }

  /* ==========================================================================
     Issue Detail Modal Dialog
     ========================================================================== */
  async function showIssue(id) {
    const cleanId = String(id).replace(/^ISS-/, "").replace(/^#/, "");
    const issue = state.issues.find(
      (item) => String(item.id) === String(id) ||
                String(item.numericId) === String(cleanId) ||
                String(item.id).replace(/^ISS-/, "").replace(/^#/, "") === cleanId
    );
    if (!issue) return;

    const existing = $("#issue-dialog");
    if (existing) existing.remove();

    const dialog = document.createElement("dialog");
    dialog.id = "issue-dialog";
    dialog.className = "issue-dialog";

    const user = state.currentUser;
    const isAdmin = user && user.role === "Admin";
    const isAssignedStaff = user && (
      (issue.assigneeId && user.id && String(issue.assigneeId).toLowerCase() === String(user.id).toLowerCase()) ||
      (issue.assignee && user.name && issue.assignee.trim().toLowerCase() === user.name.trim().toLowerCase())
    );
    const canUpdateStatus = isAdmin || isAssignedStaff;

    // Fetch history, evidence, and maintenance staff (for admin) in parallel
    let history = [];
    let evidenceList = [];
    let staffList = [];

    try {
      const issueNumericId = issue.numericId || issue.id;
      const promises = [
        CampusApi.getIssueHistory(issueNumericId).catch(() => []),
        CampusApi.getIssueEvidence(issueNumericId).catch(() => [])
      ];
      if (isAdmin) {
        promises.push(CampusApi.getStaff().catch(() => []));
      }
      const results = await Promise.all(promises);
      history = results[0] || [];
      evidenceList = results[1] || [];
      if (isAdmin) {
        staffList = results[2] || [];
      }
    } catch {}

    if (evidenceList.length === 0 && issue.evidence) {
      evidenceList = [issue.evidence];
    }

    const evidenceHtml = evidenceList.length > 0
      ? `<div class="dialog-evidence">
          <b>Attached Evidence:</b>
          <div style="display: grid; gap: 10px; margin-top: 6px;">
            ${evidenceList.map((ev) => {
              const isVideo = ev.type === "video" || (ev.fileType && ev.fileType.includes("video"));
              const displayName = escapeHtml(ev.fileName || ev.name || "Evidence file");
              const sizeLabel = ev.size ? `(${escapeHtml(ev.size)})` : "";
              const mediaPreview = ev.fileUrl ? (
                isVideo
                  ? `<video controls style="max-width: 100%; max-height: 200px; border-radius: 6px; margin-top: 6px; display: block; background: #000;">
                       <source src="${escapeHtml(ev.fileUrl)}" type="${escapeHtml(ev.fileType || 'video/mp4')}">
                       Your browser does not support video playback.
                     </video>`
                  : `<a href="${escapeHtml(ev.fileUrl)}" target="_blank" rel="noopener noreferrer" style="display: block; margin-top: 6px;" title="Click to view full image">
                       <img src="${escapeHtml(ev.fileUrl)}" alt="${displayName}" style="max-width: 100%; max-height: 180px; border-radius: 6px; border: 1px solid var(--line); object-fit: contain; background: #f8fafc;" />
                     </a>`
              ) : "";

              return `<div style="background: #f8fafc; border: 1px solid var(--line); border-radius: 6px; padding: 10px;">
                <div style="display: flex; align-items: center; justify-content: space-between; gap: 8px;">
                  <span class="evidence-tag">${isVideo ? "🎥 Video" : "📷 Photo"}</span>
                  <span style="font-size: 12px; color: var(--navy); font-weight: 600; flex: 1; margin-left: 6px;">${displayName} <small style="color: var(--text-muted); font-weight: 400;">${sizeLabel}</small></span>
                  ${ev.fileUrl ? `<a href="${escapeHtml(ev.fileUrl)}" target="_blank" rel="noopener noreferrer" class="button button-outline button-sm" style="font-size: 11px; padding: 2px 8px; height: 26px;">Open</a>` : ""}
                </div>
                ${mediaPreview}
              </div>`;
            }).join("")}
          </div>
        </div>`
      : "";

    const assignmentHtml = isAdmin
      ? `<div class="dialog-assignment" style="padding: 14px 0; border-top: 1px solid var(--line);">
          <b style="display: block; font-size: 11px; color: var(--text-muted); text-transform: uppercase; margin-bottom: 8px;">Assign Maintenance Staff (Admin)</b>
          <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
            <select id="dialog-assign-staff-select" class="text-input" style="flex: 1; min-width: 220px; height: 38px;" aria-label="Select maintenance staff">
              <option value="">Select Technician...</option>
              ${staffList.map((s) => {
                const sId = s.userId || s.id;
                const isSelected = (issue.assigneeId && String(issue.assigneeId).toLowerCase() === String(sId).toLowerCase()) ||
                                   (issue.assignee && issue.assignee.toLowerCase() === s.name.toLowerCase());
                return `<option value="${escapeHtml(sId)}" ${isSelected ? "selected" : ""}>
                  ${escapeHtml(s.name)} (${escapeHtml(s.field || "General")} · Workload: ${escapeHtml(s.workload || "Available")})
                </option>`;
              }).join("")}
            </select>
            <button type="button" class="button button-primary button-sm" id="dialog-assign-submit-btn" style="height: 38px;">Assign</button>
          </div>
        </div>`
      : "";

    const statusUpdateHtml = canUpdateStatus
      ? `<div class="dialog-status-update" style="padding: 14px 0; border-top: 1px solid var(--line);">
          <b style="display: block; font-size: 11px; color: var(--text-muted); text-transform: uppercase; margin-bottom: 8px;">Update Ticket Status</b>
          <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
            <select id="dialog-status-select" class="text-input" style="flex: 1; min-width: 180px; height: 38px;" aria-label="Update status">
              ${["Reported", "Open", "In Progress", "Resolved", "Verified", "Closed"]
                .map((v) => `<option value="${v}" ${v === issue.status ? "selected" : ""}>${v}</option>`)
                .join("")}
            </select>
            <button type="button" class="button button-outline button-sm" id="dialog-status-submit-btn" style="height: 38px;">Update</button>
          </div>
        </div>`
      : "";

    const historyHtml = history.length > 0
      ? `<div class="dialog-history" style="padding: 14px 0; border-top: 1px solid var(--line);">
          <b style="display: block; font-size: 11px; color: var(--text-muted); text-transform: uppercase; margin-bottom: 8px;">Status History &amp; Timeline</b>
          <div style="display: grid; gap: 8px; margin-top: 6px;">
            ${history.map((h) => `
              <div style="display: flex; gap: 10px; font-size: 12px; align-items: flex-start; background: #f8fafc; border: 1px solid var(--line); border-radius: 6px; padding: 8px 10px;">
                <span class="status-pill status-${(h.status || '').toLowerCase().replace(' ', '-')}" style="font-size: 10px; padding: 2px 6px;">${escapeHtml(h.status)}</span>
                <div style="flex: 1;">
                  <div style="display: flex; justify-content: space-between; gap: 4px;">
                    <b>${escapeHtml(h.changedByName || "System")}</b>
                    <small style="color: var(--text-muted);">${dateLabel(h.changedAt)}</small>
                  </div>
                  ${h.comment ? `<div style="color: var(--text-secondary); margin-top: 2px; font-size: 11px;">${escapeHtml(h.comment)}</div>` : ""}
                </div>
              </div>
            `).join("")}
          </div>
        </div>`
      : "";

    dialog.innerHTML = `
      <div class="dialog-top">
        <span>${escapeHtml(shortId(issue.id))} · Reported ${dateLabel(issue.createdAt)}</span>
        <button class="dialog-close" aria-label="Close dialog">✕</button>
      </div>
      <h2>${escapeHtml(issue.title)}</h2>
      <p class="dialog-location">⌖ ${escapeHtml(issue.location)}</p>
      <div class="dialog-pills">
        ${priorityPill(issue.priority)}
        ${statusPill(issue.status)}
      </div>
      <div class="dialog-description">
        ${escapeHtml(issue.description || "No additional description was provided for this issue.")}
      </div>
      ${evidenceHtml}
      ${assignmentHtml}
      ${statusUpdateHtml}
      ${historyHtml}
      <div class="dialog-meta">
        <div>
          <span>Category</span>
          <b>${escapeHtml(issue.category)}</b>
        </div>
        <div>
          <span>Assigned Technician</span>
          <b>${escapeHtml(issue.assignee || "Unassigned")}</b>
        </div>
        <div>
          <span>Reported By</span>
          <b>${escapeHtml(issue.reportedByName || "Anonymous")}</b>
        </div>
        <div>
          <span>Reporter Role</span>
          <b>${escapeHtml(issue.reportedByRole || "Student")}</b>
        </div>
      </div>
    `;

    document.body.append(dialog);
    dialog.showModal();

    $(".dialog-close", dialog).addEventListener("click", () => dialog.close());
    dialog.addEventListener("click", (e) => {
      if (e.target === dialog) dialog.close();
    });
    dialog.addEventListener("close", () => dialog.remove());

    // Admin assignment submission
    if (isAdmin && $("#dialog-assign-submit-btn", dialog)) {
      $("#dialog-assign-submit-btn", dialog).addEventListener("click", async () => {
        const staffSelect = $("#dialog-assign-staff-select", dialog);
        const staffId = staffSelect ? staffSelect.value : null;
        if (!staffId) {
          toast("Assignment Warning", "Please select a technician to assign.", true);
          return;
        }
        try {
          const res = await CampusApi.assignStaff(issue.numericId || issue.id, staffId);
          state.issues = await CampusApi.getIssues();
          toast("Staff Assigned", `Assigned ${shortId(issue.id)} to ${res.staffName || "technician"}.`);
          dialog.close();
          render();
        } catch (err) {
          toast("Assignment Failed", err.message || "Could not assign staff.", true);
        }
      });
    }

    // Status update submission (for Admin or assigned Maintenance Staff)
    if (canUpdateStatus && $("#dialog-status-submit-btn", dialog)) {
      $("#dialog-status-submit-btn", dialog).addEventListener("click", async () => {
        const statusSelect = $("#dialog-status-select", dialog);
        const newStatus = statusSelect ? statusSelect.value : null;
        if (!newStatus || newStatus === issue.status) return;
        try {
          await CampusApi.updateIssue(issue.numericId || issue.id, { status: newStatus });
          state.issues = await CampusApi.getIssues();
          toast("Status Updated", `Updated ${shortId(issue.id)} to ${newStatus}.`);
          dialog.close();
          render();
        } catch (err) {
          toast("Update Failed", err.message || "Could not update status.", true);
        }
      });
    }
  }

  /* Toast Notification */
  function toast(title, message, error = false) {
    const node = document.createElement("div");
    node.className = `toast ${error ? "toast-error" : ""}`;
    node.innerHTML = `
      <span class="toast-check">${error ? "!" : "✓"}</span>
      <span>
        <b>${escapeHtml(title)}</b>
        <small>${escapeHtml(message)}</small>
      </span>
      <button aria-label="Dismiss">×</button>
    `;
    $("#toast-region").append(node);
    $("button", node).addEventListener("click", () => node.remove());
    setTimeout(() => node.remove(), 4500);
  }

  /* ==========================================================================
     Router and App Rendering
     ========================================================================== */
  async function render() {
    let hash = location.hash.replace("#", "") || "overview";

    // Handle aliases
    if (routeAliases[hash]) {
      hash = routeAliases[hash];
    }

    try {
      if (window.CAMPUS_CONFIG && window.CAMPUS_CONFIG.USE_MOCK_DATA && window.MockStore) {
        state.currentUser = window.MockStore.getCurrentUser();
        state.issues = window.MockStore.getIssues();
      } else {
        state.currentUser = await CampusApi.getCurrentUser();
        state.issues = await CampusApi.getIssues();
      }
    } catch {}

    // Auth verification
    if (!state.currentUser && hash !== "login") {
      location.hash = "#login";
      return;
    }

    if (state.currentUser && hash === "login") {
      location.hash = "#overview";
      return;
    }

    // Role-based route guard
    const userRole = state.currentUser ? state.currentUser.role : null;
    if (userRole === "Student" || userRole === "Professor") {
      if (hash === "issue-management" || hash === "staff-workspace") {
        toast("Access Restricted", "This section is restricted to Maintenance Staff and Administrators.", true);
        location.hash = "#overview";
        return;
      }
    }

    state.page = pageTitles[hash] ? hash : "overview";

    // Update document title and topbar
    const currentTitle = pageTitles[state.page] || "Campus Care";
    document.title = `${currentTitle} — Campus Care`;

    if (state.page === "login") {
      renderLogin();
      return;
    }

    appShell.classList.remove("auth-mode");
    $("#topbar-title").textContent = currentTitle;
    document.body.classList.remove("nav-open");

    updateNavigation();

    // Route dispatch
    if (state.page === "overview") {
      const summary = await CampusApi.getSummary();
      renderOverview(summary);
    } else if (state.page === "report-issue") {
      await renderReport();
    } else if (state.page === "issues-reported") {
      renderIssuesReported();
    } else if (state.page === "issue-management") {
      renderIssueManagement();
    } else if (state.page === "staff-workspace") {
      await renderStaffWorkspace();
    } else if (state.page === "profile") {
      renderProfile();
    }
  }

  /* Global Events */
  document.addEventListener("click", (event) => {
    const issueBtn = event.target.closest("[data-open-issue]");
    if (issueBtn) {
      showIssue(issueBtn.dataset.openIssue);
    }
  });

  $("#mobile-menu").addEventListener("click", () => {
    document.body.classList.toggle("nav-open");
  });

  $("#sidebar-backdrop").addEventListener("click", () => {
    document.body.classList.remove("nav-open");
  });

  $("#sidebar-profile-btn").addEventListener("click", (e) => {
    if (e.target.closest("#sidebar-logout-btn")) {
      e.stopPropagation();
      handleLogout();
    } else {
      location.hash = "#profile";
    }
  });

  $("#today-label").textContent = new Intl.DateTimeFormat("en", {
    weekday: "short",
    month: "short",
    day: "numeric"
  }).format(new Date());

  window.addEventListener("hashchange", render);

  // Initial bootstrap
  (async () => {
    try {
      state.currentUser = await CampusApi.getCurrentUser();
      if (!state.currentUser && !location.hash.includes("login") && window.CAMPUS_CONFIG && window.CAMPUS_CONFIG.USE_MOCK_DATA) {
        const users = await CampusApi.getUsers();
        if (users && users.length && window.MockStore) {
          state.currentUser = window.MockStore.setCurrentUser(users[0]);
        }
      }
      state.issues = await CampusApi.getIssues();
    } catch (e) {
      console.warn("Bootstrap load notice:", e);
    }
    render();
  })();
})();
