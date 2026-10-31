/* Switch USE_MOCK_DATA to false after configuring your Spring Boot API. */
window.CAMPUS_CONFIG = {
  USE_MOCK_DATA: true,
  API_BASE_URL: "http://localhost:8080/api",
  ENDPOINTS: {
    issues: "/issues",
    summary: "/issues/summary",
    staff: "/staff"
  }
};
