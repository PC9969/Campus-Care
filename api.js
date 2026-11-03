/* API Boundary for Campus Care.
 * UI components interact exclusively with CampusApi.
 * Switch CAMPUS_CONFIG.USE_MOCK_DATA to false in config.js to connect to Spring Boot backend.
 */
(() => {
  const config = window.CAMPUS_CONFIG;
  const delay = (value) => new Promise((resolve) => setTimeout(() => resolve(structuredClone(value)), 100));

  const request = async (path, options = {}) => {
    const token = localStorage.getItem("campus_care_token");
    const headers = {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {})
    };

    const response = await fetch(`${config.API_BASE_URL}${path}`, {
      ...options,
      headers
    });

    if (!response.ok) {
      let errorMessage = `Request failed (${response.status})`;
      try {
        const errorData = await response.json();
        if (errorData && errorData.message) errorMessage = errorData.message;
      } catch {}
      throw new Error(errorMessage);
    }
    return response.status === 204 ? null : response.json();
  };

  window.CampusApi = {
    login: async (email, password, roleChoice) => {
      if (config.USE_MOCK_DATA) {
        return delay(window.MockStore.authenticate(email, password, roleChoice));
      }
      const data = await request(config.ENDPOINTS.auth, {
        method: "POST",
        body: JSON.stringify({ email, password, roleChoice })
      });
      if (data && data.token) {
        localStorage.setItem("campus_care_token", data.token);
      }
      return data && data.user ? data.user : data;
    },

    logout: async () => {
      if (config.USE_MOCK_DATA) {
        return delay(window.MockStore.logout());
      }
      try {
        await request("/auth/logout", { method: "POST" });
      } catch {}
      localStorage.removeItem("campus_care_token");
      return Promise.resolve(true);
    },

    getCurrentUser: async () => {
      if (config.USE_MOCK_DATA) {
        return delay(window.MockStore.getCurrentUser());
      }
      return request(config.ENDPOINTS.currentUser);
    },

    getUsers: async () => {
      if (config.USE_MOCK_DATA) {
        return delay(window.MockStore.getUsers());
      }
      return request("/users");
    },

    getIssues: async () => {
      if (config.USE_MOCK_DATA) {
        return delay(window.MockStore.getIssues());
      }
      return request(config.ENDPOINTS.issues);
    },

    getMyIssues: async (userId) => {
      if (config.USE_MOCK_DATA) {
        return delay(window.MockStore.getMyIssues(userId));
      }
      return request(config.ENDPOINTS.myIssues);
    },

    createIssue: async (issue) => {
      if (config.USE_MOCK_DATA) {
        return delay(window.MockStore.createIssue(issue));
      }
      const evidenceFile = issue.evidenceFile;
      const payload = { ...issue };
      delete payload.evidenceFile;
      // In live mode, remove client-provided metadata so it is never recorded without the actual uploaded file
      delete payload.evidence;

      const created = await request(config.ENDPOINTS.issues, {
        method: "POST",
        body: JSON.stringify(payload)
      });

      if (evidenceFile && created && (created.id || created.numericId)) {
        try {
          const uploaded = await window.CampusApi.uploadEvidence(created.numericId || created.id, evidenceFile);
          created.evidence = uploaded;
        } catch (uploadErr) {
          console.warn("Evidence upload failed:", uploadErr);
          created.evidenceUploadFailed = true;
          created.evidenceUploadError = uploadErr.message || "Failed to upload file to storage";
        }
      }
      return created;
    },

    uploadEvidence: async (issueId, file) => {
      if (config.USE_MOCK_DATA) {
        return delay({ success: true });
      }
      const token = localStorage.getItem("campus_care_token");
      const formData = new FormData();
      formData.append("file", file);
      const cleanId = String(issueId).replace(/^ISS-/, "").replace(/^#/, "");

      const response = await fetch(`${config.API_BASE_URL}${config.ENDPOINTS.issues}/${encodeURIComponent(cleanId)}/evidence`, {
        method: "POST",
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: formData
      });

      if (!response.ok) {
        let msg = `Upload failed (${response.status})`;
        try {
          const err = await response.json();
          if (err && err.message) msg = err.message;
        } catch {}
        throw new Error(msg);
      }
      return response.json();
    },

    updateIssue: async (id, patch) => {
      if (config.USE_MOCK_DATA) {
        return delay(window.MockStore.updateIssue(id, patch));
      }
      const cleanId = String(id).replace(/^ISS-/, "");
      return request(`${config.ENDPOINTS.issues}/${encodeURIComponent(cleanId)}`, {
        method: "PATCH",
        body: JSON.stringify(patch)
      });
    },

    assignStaff: async (issueId, staffId) => {
      const cleanIssueId = parseInt(String(issueId).replace(/^ISS-/, "").replace(/^#/, ""), 10);
      if (config.USE_MOCK_DATA) {
        const staffList = window.MockStore ? window.MockStore.getStaff() : [];
        const staff = staffList.find((s) => s.id === staffId || s.staffId === staffId || s.userId === staffId);
        const staffName = staff ? staff.name : staffId;
        const formattedId = typeof issueId === "number" ? `ISS-${issueId}` : String(issueId);
        const updated = window.MockStore
          ? window.MockStore.updateIssue(formattedId, {
              assignee: staffName,
              assigneeId: staffId,
              status: "In Progress"
            })
          : null;
        return delay({
          id: Date.now(),
          issueId: cleanIssueId,
          issueTitle: updated ? updated.title : "",
          staffId,
          staffName,
          assignedAt: new Date().toISOString()
        });
      }
      return request(config.ENDPOINTS.assignments || "/assignments", {
        method: "POST",
        body: JSON.stringify({ issueId: cleanIssueId, staffId })
      });
    },

    getAssignmentsByIssue: async (issueId) => {
      const cleanIssueId = parseInt(String(issueId).replace(/^ISS-/, "").replace(/^#/, ""), 10);
      if (config.USE_MOCK_DATA) {
        return delay([]);
      }
      return request(`${config.ENDPOINTS.assignments || "/assignments"}/issue/${encodeURIComponent(cleanIssueId)}`);
    },

    getAssignmentsByStaff: async (staffId) => {
      if (config.USE_MOCK_DATA) {
        return delay([]);
      }
      return request(`${config.ENDPOINTS.assignments || "/assignments"}/staff/${encodeURIComponent(staffId)}`);
    },

    getIssueHistory: async (issueId) => {
      const cleanId = String(issueId).replace(/^ISS-/, "").replace(/^#/, "");
      if (config.USE_MOCK_DATA) {
        const issue = (window.MockStore ? window.MockStore.getIssues() : []).find((i) => String(i.id).includes(cleanId));
        return delay([
          {
            id: 1,
            issueId: cleanId,
            status: issue ? issue.status : "Reported",
            changedByName: issue ? (issue.reportedByName || "Reporter") : "System",
            comment: "Issue reported",
            changedAt: issue ? issue.createdAt : new Date().toISOString()
          }
        ]);
      }
      return request(`${config.ENDPOINTS.issues}/${encodeURIComponent(cleanId)}/history`);
    },

    getIssueEvidence: async (issueId) => {
      const cleanId = String(issueId).replace(/^ISS-/, "").replace(/^#/, "");
      if (config.USE_MOCK_DATA) {
        const issue = (window.MockStore ? window.MockStore.getIssues() : []).find((i) => String(i.id).includes(cleanId));
        return delay(issue && issue.evidence ? [issue.evidence] : []);
      }
      return request(`${config.ENDPOINTS.issues}/${encodeURIComponent(cleanId)}/evidence`);
    },

    getSummary: async () => {
      if (config.USE_MOCK_DATA) {
        return delay(window.MockStore.getSummary());
      }
      return request(config.ENDPOINTS.summary);
    },

    getCategories: async () => {
      if (config.USE_MOCK_DATA) {
        return delay(window.MockStore.getCategories());
      }
      return request(config.ENDPOINTS.categories);
    },

    getStaff: async () => {
      if (config.USE_MOCK_DATA) {
        return delay(window.MockStore.getStaff());
      }
      return request(config.ENDPOINTS.staff);
    }
  };
})();
