/* Configuration for Campus Care frontend.
 * Switch USE_MOCK_DATA to false after connecting your Spring Boot + PostgreSQL backend.
 */
window.CAMPUS_CONFIG = {
  USE_MOCK_DATA: false,
  API_BASE_URL: "https://campus-care-5jev.onrender.com/api",
  ENDPOINTS: {
    auth: "/auth/login",
    currentUser: "/users/me",
    issues: "/issues",
    myIssues: "/issues/my",
    summary: "/issues/summary",
    categories: "/issues/categories",
    staff: "/staff",
    assignments: "/assignments"
  }
};
