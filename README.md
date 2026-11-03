# Campus Care — Campus Maintenance Management System
**SRMIST, Delhi-NCR Campus**

A modern, institutional, role-aware HTML/CSS/JavaScript frontend portal for reporting, tracking, and managing campus maintenance requests across SRMIST, Delhi-NCR Campus.

---

## Key Features & Role-Based Workflows

### 1. Application Roles
- **Student**:
  - View Overview dashboard with real-time campus statistics and category breakdown
  - Report an Issue with multi-format evidence upload (camera capture / gallery, <= 10 MB)
  - View "Issue Reported" page containing strictly their own reported issues
  - View their Student Profile (Branch, Registration number, personal ticket counts)
- **Professor**:
  - View Overview dashboard
  - Report an Issue with evidence attachment
  - View "Issue Reported" page containing strictly their own reported issues
  - View their Faculty Profile (Department, Designation, Faculty ID, ticket counts)
- **Maintenance Staff**:
  - View Overview dashboard
  - Report an Issue
  - View all campus issues under "Issue Reported"
  - Access Issue Management (advanced search, multi-filter triage by status, category, priority)
  - Access Staff Workspace (active work queue with inline status updates, CSV export, and "All Staff" technician directory)
  - View their Staff Profile (Specialized field/domain, assigned/solved ticket metrics)
- **Admin**:
  - Supported conceptually in frontend data model and routes
  - Complete access to all sections

### 2. Available Routes
- `/#login` — Institutional login portal with role selection, college email verification, and password toggle
- `/#overview` (or `/#dashboard`) — Campus dashboard with dynamic category breakdown and recent issues
- `/#report-issue` (or `/#report`) — Validated reporting form with evidence attachment (camera/gallery, <= 10 MB)
- `/#issues-reported` — Dedicated role-aware tracking page for submitted requests
- `/#issue-management` (or `/#issues`) — Maintenance staff ticket triage and assignment queue
- `/#staff-workspace` (or `/#staff`) — Active technician queue, status updates, and "All Staff" campus coverage
- `/#profile` — Role-aware user profile with quick demo account switching

---

## Running the Application Locally

1. Open this directory in VS Code or any terminal.
2. Serve `index.html` using any local HTTP server:
   - **Python 3**:
     ```bash
     python -m http.server 5500
     ```
     Then open `http://localhost:5500` in your browser.
   - **VS Code Live Server**: Right-click `index.html` and choose **Open with Live Server**.
   - **Node.js `npx serve`**:
     ```bash
     npx serve .
     ```

No build step, npm packages, or external database is required for the frontend demonstration.

---

## Centralized Mock Data & Sample Credentials

All mock data is centralized in `mock-data.js` and persists across sessions via browser `localStorage`.

### Sample Login Credentials
| Role | Name | Email | Password |
|---|---|---|---|
| **Student** | Rahul Sharma | `rahul.s@srmist.edu.in` | `password123` |
| **Professor** | Dr. Priya Nair | `priya.nair@srmist.edu.in` | `password123` |
| **Maintenance Staff** | Amit Kumar | `amit.k@srmist.edu.in` | `password123` |

Quick demo pills are also available directly on the login page and profile page for instant 1-click role testing.

---

## Connect to Spring Boot + PostgreSQL REST API Later

1. Set `USE_MOCK_DATA: false` in `config.js`.
2. Update `API_BASE_URL` in `config.js` to point to your Spring Boot server (e.g. `http://localhost:8080/api`).
3. The UI interacts solely with `CampusApi` in `api.js`, which handles standard REST endpoints:
   - `POST /api/auth/login` → `{ token, user }`
   - `GET /api/users/me` → User profile object
   - `GET /api/issues` → Array of issues
   - `GET /api/issues/my` → Array of user's reported issues
   - `POST /api/issues` → Created issue object
   - `PATCH /api/issues/{id}` → Updated issue object
   - `GET /api/issues/summary` → Dashboard statistics and category breakdown
   - `GET /api/staff` → Array of maintenance technicians
