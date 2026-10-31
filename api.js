/* Small API boundary: UI code calls CampusApi; switch config.js to enable REST. */
(() => {
  const config = window.CAMPUS_CONFIG;
  const delay = (value) => new Promise((resolve) => setTimeout(() => resolve(structuredClone(value)), 120));
  const request = async (path, options = {}) => {
    const response = await fetch(`${config.API_BASE_URL}${path}`, {
      ...options,
      headers: { "Content-Type": "application/json", ...(options.headers || {}) }
    });
    if (!response.ok) throw new Error(`Request failed (${response.status})`);
    return response.status === 204 ? null : response.json();
  };
  window.CampusApi = {
    getIssues: async () => config.USE_MOCK_DATA ? delay(window.MockStore.getIssues()) : request(config.ENDPOINTS.issues),
    createIssue: async (issue) => config.USE_MOCK_DATA ? delay(window.MockStore.createIssue(issue)) : request(config.ENDPOINTS.issues, { method: "POST", body: JSON.stringify(issue) }),
    updateIssue: async (id, patch) => config.USE_MOCK_DATA ? delay(window.MockStore.updateIssue(id, patch)) : request(`${config.ENDPOINTS.issues}/${encodeURIComponent(id)}`, { method: "PATCH", body: JSON.stringify(patch) }),
    getSummary: async () => config.USE_MOCK_DATA ? delay(window.MockStore.getSummary()) : request(config.ENDPOINTS.summary),
    getStaff: async () => config.USE_MOCK_DATA ? delay(window.MockStore.getStaff()) : request(config.ENDPOINTS.staff)
  };
})();
