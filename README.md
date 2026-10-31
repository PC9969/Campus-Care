# CampusCare — Campus Maintenance System

A responsive, plain HTML/CSS/JavaScript frontend for reporting and managing campus maintenance requests. It includes an overview dashboard, issue reporting form, searchable issue management list, and a staff work queue.

## Run in VS Code

1. Unzip the project and open the `campus-maintenance-frontend` folder in VS Code (`File → Open Folder…`).
2. Open `index.html` in a local web server. The easiest option is the **Live Server** extension: right-click `index.html` and choose **Open with Live Server**.
3. Alternatively, use Python if installed: from the project folder run `python -m http.server 5500`, then visit `http://localhost:5500`.

No build step, package install, or backend is required for the mock-data experience.

## Included screens and behavior

- **Overview:** issue counts, activity chart, category breakdown, and recent reports.
- **Report an issue:** validated issue form with category, priority, location, and description.
- **Issue management:** searchable issue table with status/category filters and issue detail dialogs.
- **Staff workspace:** active work queue, status updates, team summary, and CSV export.
- **Mock data:** eight sample requests. New issues and status changes persist in browser `localStorage`.
- **Spring Boot adapter:** centralized in `api.js`; the UI calls `CampusApi` rather than `fetch` directly.

## Connect a Spring Boot REST API

1. Set `USE_MOCK_DATA` to `false` in `config.js`.
2. Set `API_BASE_URL` to your backend origin and edit `ENDPOINTS` to match your controller routes.
3. The starter adapter expects:
   - `GET /issues` → JSON array of issue objects
   - `POST /issues` → created issue JSON
   - `PATCH /issues/{id}` → updated issue JSON
   - `GET /issues/summary` → `{ open, inProgress, resolved, newThisWeek, avgResolution, weekly, categories }`
   - `GET /staff` → JSON array such as `[{ "name": "Jordan Lee", "role": "Plumbing" }]`
4. Issue objects use `id`, `title`, `category`, `priority`, `status`, `location`, `assignee`, `createdAt`, and `description`. Use `Open`, `In Progress`, and `Resolved` status values; `Low`, `Medium`, and `High` priorities.
5. Enable CORS on Spring Boot for the local frontend origin. Adjust request/response mapping in `api.js` if your DTO shape differs.

If your API uses only `PUT` for status changes, change the `PATCH` request in `api.js` to `PUT`. Authentication can be added in the same adapter, for example through a shared authorization header.

## Project files

- `index.html` — page shell and asset loading
- `styles.css` — responsive visual system
- `app.js` — navigation, rendering, filtering, forms, queue actions, and export
- `api.js` — mock/REST adapter boundary
- `mock-data.js` — local sample records and persistence
- `config.js` — API base URL, endpoint paths, and mode switch
