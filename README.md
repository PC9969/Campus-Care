# Campus Maintenance System

A centralized campus maintenance platform designed to streamline the reporting, prioritization, assignment, tracking, and resolution of campus maintenance issues.

## 📌 Overview

Campus environments regularly face issues such as water leakage, electrical faults, damaged infrastructure, broken equipment, network problems, and sanitation-related issues.

When these problems are reported through different channels such as calls, messages, or verbal communication, it becomes difficult to track complaints, prioritize urgent issues, assign the appropriate maintenance staff, and identify recurring problems.

The Campus Maintenance System aims to provide a single platform for managing the complete maintenance lifecycle.

## 🎯 Objectives

- Centralize all campus maintenance requests.
- Prioritize issues based on severity, urgency, and impact.
- Assign issues to the appropriate maintenance personnel or department.
- Track issues from reporting to resolution.
- Reduce response and resolution time.
- Identify recurring maintenance problems.
- Improve transparency and accountability.
- Provide useful maintenance insights to administrators.

## ✨ Planned Features

### 👤 Issue Reporting
- Report maintenance problems through a centralized platform.
- Add issue category, location, description, and images.
- Track previously reported issues.

### 🎯 Priority Management
- Classify issues based on severity and urgency.
- Prioritize critical problems over routine requests.
- Consider factors such as affected users and issue duration.

### 🔧 Staff Assignment
- Assign issues to the appropriate maintenance team.
- Track assigned and pending tasks.
- Monitor maintenance workload.

### 📍 Issue Tracking
- Track the complete issue lifecycle:
  - Reported
  - Categorized
  - Prioritized
  - Assigned
  - In Progress
  - Resolved
  - Verified
  - Closed

### 📊 Maintenance Analytics
- Monitor pending and resolved issues.
- Analyze resolution times.
- Identify frequently affected locations.
- Detect recurring maintenance problems.

## 👥 User Roles

### Student / Staff
- Report maintenance issues.
- Upload supporting information.
- Track issue status.
- Provide feedback after resolution.

### Administrator
- Manage reported issues.
- Prioritize and assign maintenance requests.
- Monitor maintenance activities.
- View analytics and recurring issues.

### Maintenance Personnel
- View assigned tasks.
- Update issue status.
- Add resolution details.
- Mark tasks as completed.

## 🛠️ Technology Stack

### Backend
- Java 25 (LTS)
- Spring Boot 4.x
- Spring Web
- Spring Data JPA

### Database
- PostgreSQL

### Frontend
- HTML
- CSS
- JavaScript

### Development Tools
- Visual Studio Code
- Maven
- Git
- GitHub
- Postman

### Deployment
- Vercel — Frontend
- Cloud-hosted Spring Boot backend
- PostgreSQL — Database

## 🏗️ High-Level Architecture

```text
                    Users
                      │
                      ▼
              ┌───────────────┐
              │   Frontend    │
              │ HTML/CSS/JS   │
              └───────┬───────┘
                      │
                   REST API
                      │
                      ▼
              ┌───────────────┐
              │ Spring Boot   │
              │    Backend    │
              └───────┬───────┘
                      │
              ┌───────┴───────┐
              ▼               ▼
        Business Logic   Authentication
              │
              ▼
        ┌───────────────┐
        │  PostgreSQL   │
        └───────────────┘
