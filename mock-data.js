/* Centralized mock data and browser-persisted mock store for Campus Care.
 * Designed for seamless replacement with Spring Boot + PostgreSQL REST API later.
 */
(() => {
  const USERS_STORAGE_KEY = "campus_care_users_v2";
  const ISSUES_STORAGE_KEY = "campus_care_issues_v2";
  const ACTIVE_USER_STORAGE_KEY = "campus_care_active_user_v2";

  const seedUsers = [
    {
      id: "USR-STU-01",
      name: "Rahul Sharma",
      email: "rahul.s@srmist.edu.in",
      password: "password123",
      role: "Student",
      contact: "+91 98765 43210",
      branch: "Computer Science & Engineering",
      registrationNo: "RA2211003010123"
    },
    {
      id: "USR-STU-02",
      name: "Amit Patel",
      email: "amit.patel@srmist.edu.in",
      password: "password123",
      role: "Student",
      contact: "+91 98765 12345",
      branch: "Information Technology",
      registrationNo: "RA2211003010456"
    },
    {
      id: "USR-PROF-01",
      name: "Dr. Priya Nair",
      email: "priya.nair@srmist.edu.in",
      password: "password123",
      role: "Professor",
      contact: "+91 98111 22334",
      department: "Department of Mechanical Engineering",
      designation: "Associate Professor",
      facultyId: "FAC-SRM-408"
    },
    {
      id: "USR-PROF-02",
      name: "Dr. Rajesh Sen",
      email: "rajesh.sen@srmist.edu.in",
      password: "password123",
      role: "Professor",
      contact: "+91 98222 33445",
      department: "Department of Electronics & Communication",
      designation: "HOD & Professor",
      facultyId: "FAC-SRM-102"
    },
    {
      id: "USR-STF-01",
      name: "Amit Kumar",
      email: "amit.k@srmist.edu.in",
      password: "password123",
      role: "Maintenance Staff",
      contact: "+91 97234 56789",
      field: "Electrical",
      staffId: "STF-SRM-019",
      issuesAssigned: 5,
      issuesSolved: 14
    },
    {
      id: "USR-STF-02",
      name: "Rajesh Verma",
      email: "rajesh.v@srmist.edu.in",
      password: "password123",
      role: "Maintenance Staff",
      contact: "+91 97123 45678",
      field: "Plumbing",
      staffId: "STF-SRM-022",
      issuesAssigned: 4,
      issuesSolved: 18
    },
    {
      id: "USR-ADM-01",
      name: "Dr. S. K. Gupta",
      email: "dean.admin@srmist.edu.in",
      password: "password123",
      role: "Admin",
      contact: "+91 98000 11223",
      designation: "Dean & Director of Campus Operations",
      department: "Office of Campus Administration",
      adminId: "ADM-SRM-001"
    }
  ];

  const seedStaff = [
    {
      id: "STF-01",
      name: "Rajesh Verma",
      field: "Plumbing",
      workload: "Moderate",
      activeIssues: 2,
      status: "Available",
      email: "rajesh.v@srmist.edu.in",
      phone: "+91 97123 45678"
    },
    {
      id: "STF-02",
      name: "Amit Kumar",
      field: "Electrical",
      workload: "High",
      activeIssues: 3,
      status: "On Task",
      email: "amit.k@srmist.edu.in",
      phone: "+91 97234 56789"
    },
    {
      id: "STF-03",
      name: "Sunita Rao",
      field: "HVAC",
      workload: "Light",
      activeIssues: 1,
      status: "Available",
      email: "sunita.r@srmist.edu.in",
      phone: "+91 97345 67890"
    },
    {
      id: "STF-04",
      name: "Harish Mehra",
      field: "Carpentry",
      workload: "Moderate",
      activeIssues: 2,
      status: "On Task",
      email: "harish.m@srmist.edu.in",
      phone: "+91 97456 78901"
    },
    {
      id: "STF-05",
      name: "Vikram Singh",
      field: "Networking",
      workload: "Light",
      activeIssues: 1,
      status: "Available",
      email: "vikram.s@srmist.edu.in",
      phone: "+91 97567 89012"
    },
    {
      id: "STF-06",
      name: "Manoj Tiwari",
      field: "General Maintenance",
      workload: "Moderate",
      activeIssues: 2,
      status: "Available",
      email: "manoj.t@srmist.edu.in",
      phone: "+91 97678 90123"
    }
  ];

  const seedIssues = [
    {
      id: "ISS-1048",
      title: "Water leakage near Fluids Lab",
      category: "Plumbing",
      priority: "High",
      status: "In Progress",
      location: "Mechanical Block · Ground Floor, Fluids Lab",
      assignee: "Rajesh Verma",
      reportedBy: "USR-STU-01",
      reportedByName: "Rahul Sharma",
      reportedByRole: "Student",
      createdAt: "2026-10-27T08:15:00",
      description: "Water is pooling continuously near the washroom entrance adjacent to the lab.",
      evidence: { type: "image", name: "water_pipe_leak.jpg", size: "1.8 MB" }
    },
    {
      id: "ISS-1047",
      title: "Flickering tube lights in Room 204",
      category: "Electrical",
      priority: "Medium",
      status: "Open",
      location: "Academic Block 1 · 2nd Floor, Room 204",
      assignee: "Amit Kumar",
      reportedBy: "USR-STU-01",
      reportedByName: "Rahul Sharma",
      reportedByRole: "Student",
      createdAt: "2026-10-26T14:35:00",
      description: "Three ceiling lights near the blackboard flicker continuously during lectures."
    },
    {
      id: "ISS-1046",
      title: "Broken study chair in Central Library",
      category: "Furniture",
      priority: "Low",
      status: "Reported",
      location: "Central Library · 2nd Floor Quiet Zone",
      assignee: "Harish Mehra",
      reportedBy: "USR-STU-01",
      reportedByName: "Rahul Sharma",
      reportedByRole: "Student",
      createdAt: "2026-10-26T11:20:00",
      description: "One wooden study chair has a loose leg and could topple if used."
    },
    {
      id: "ISS-1045",
      title: "Air conditioning not cooling in Audi 2",
      category: "HVAC",
      priority: "High",
      status: "In Progress",
      location: "Auditorium Complex · Seminar Hall 2",
      assignee: "Sunita Rao",
      reportedBy: "USR-PROF-01",
      reportedByName: "Dr. Priya Nair",
      reportedByRole: "Professor",
      createdAt: "2026-10-25T16:05:00",
      description: "Hall temperature is 29°C despite thermostat set to 20°C ahead of symposium.",
      evidence: { type: "image", name: "thermostat_reading.png", size: "2.1 MB" }
    },
    {
      id: "ISS-1044",
      title: "Projector HDMI cable damaged in Room 102",
      category: "Networking",
      priority: "Medium",
      status: "Resolved",
      location: "Academic Block 2 · Room 102",
      assignee: "Vikram Singh",
      reportedBy: "USR-PROF-01",
      reportedByName: "Dr. Priya Nair",
      reportedByRole: "Professor",
      createdAt: "2026-10-25T09:40:00",
      description: "HDMI output to the podium projector is throwing an artifacted signal."
    },
    {
      id: "ISS-1043",
      title: "Loose wooden door latch in Faculty Room",
      category: "Carpentry",
      priority: "Low",
      status: "Open",
      location: "Mechanical Block · Faculty Cabin M-14",
      assignee: "Harish Mehra",
      reportedBy: "USR-PROF-01",
      reportedByName: "Dr. Priya Nair",
      reportedByRole: "Professor",
      createdAt: "2026-10-24T10:10:00",
      description: "Door cannot be secured properly due to misaligned latch mechanism."
    },
    {
      id: "ISS-1042",
      title: "Power socket spark in Computer Lab 1",
      category: "Electrical",
      priority: "Critical",
      status: "In Progress",
      location: "Computer Center · Lab 1, Terminal Row 4",
      assignee: "Amit Kumar",
      reportedBy: "USR-STU-02",
      reportedByName: "Amit Patel",
      reportedByRole: "Student",
      createdAt: "2026-10-23T13:00:00",
      description: "Spark noticed while plugging in a laptop power brick. Breaker has not tripped."
    },
    {
      id: "ISS-1041",
      title: "Low water pressure in 1st Floor Restroom",
      category: "Plumbing",
      priority: "Medium",
      status: "Reported",
      location: "Main Academic Block · 1st Floor South Restroom",
      assignee: "Unassigned",
      reportedBy: "USR-STU-02",
      reportedByName: "Amit Patel",
      reportedByRole: "Student",
      createdAt: "2026-10-22T08:55:00",
      description: "Water taps running at very low trickle. Handwash faucets are unusable."
    },
    {
      id: "ISS-1040",
      title: "Staircase handrail loose in Admin Block",
      category: "Safety",
      priority: "High",
      status: "Resolved",
      location: "Administrative Block · West Stairwell",
      assignee: "Manoj Tiwari",
      reportedBy: "USR-PROF-02",
      reportedByName: "Dr. Rajesh Sen",
      reportedByRole: "Professor",
      createdAt: "2026-10-21T15:30:00",
      description: "Wall mounting bracket on the 2nd floor landing handrail came loose."
    }
  ];

  /* Storage accessors */
  const readStorage = (key, fallback) => {
    try {
      const data = localStorage.getItem(key);
      return data ? JSON.parse(data) : fallback;
    } catch {
      return fallback;
    }
  };

  const writeStorage = (key, data) => {
    try {
      localStorage.setItem(key, JSON.stringify(data));
    } catch (e) {
      console.warn("localStorage write failed:", e);
    }
    return data;
  };

  /* Initialize store */
  const getUsers = () => readStorage(USERS_STORAGE_KEY, seedUsers);
  const getIssues = () => readStorage(ISSUES_STORAGE_KEY, seedIssues);

  const getCurrentUser = () => {
    return readStorage(ACTIVE_USER_STORAGE_KEY, null);
  };

  const setCurrentUser = (user) => {
    writeStorage(ACTIVE_USER_STORAGE_KEY, user);
    return user;
  };

  const logout = () => {
    try {
      localStorage.removeItem(ACTIVE_USER_STORAGE_KEY);
    } catch {}
    return true;
  };

  const authenticate = (email, password, roleChoice) => {
    const users = getUsers();
    const cleanEmail = String(email || "").trim().toLowerCase();
    const cleanPassword = String(password || "").trim();

    const matchedUser = users.find(u => u.email.toLowerCase() === cleanEmail);
    if (!matchedUser) {
      throw new Error("No user registered with this email address.");
    }

    if (matchedUser.password !== cleanPassword) {
      throw new Error("Invalid password. Please check your credentials.");
    }

    // Role category validation if roleChoice provided
    if (roleChoice === "Student / Professor") {
      if (matchedUser.role !== "Student" && matchedUser.role !== "Professor") {
        throw new Error("This account is not registered as a Student or Professor.");
      }
    } else if (roleChoice === "Maintenance Staff") {
      if (matchedUser.role !== "Maintenance Staff") {
        throw new Error("This account is not registered as Maintenance Staff.");
      }
    }

    setCurrentUser(matchedUser);
    return matchedUser;
  };

  const getMyIssues = (userIdOrEmail) => {
    const all = getIssues();
    const currentUser = getCurrentUser();
    const target = userIdOrEmail || (currentUser ? currentUser.id : null);
    if (!target) return [];
    return all.filter(i => i.reportedBy === target || i.reportedBy === currentUser.email);
  };

  const createIssue = (payload) => {
    const all = getIssues();
    const highest = Math.max(1040, ...all.map(i => Number(String(i.id).replace("ISS-", "")) || 0));
    const currentUser = getCurrentUser();
    const newIssue = {
      ...payload,
      id: `ISS-${highest + 1}`,
      status: payload.status || "Reported",
      assignee: payload.assignee || "Unassigned",
      reportedBy: currentUser ? currentUser.id : "USR-GUEST",
      reportedByName: currentUser ? currentUser.name : "Anonymous",
      reportedByRole: currentUser ? currentUser.role : "Student",
      createdAt: new Date().toISOString()
    };
    writeStorage(ISSUES_STORAGE_KEY, [newIssue, ...all]);
    return newIssue;
  };

  const updateIssue = (id, patch) => {
    const all = getIssues().map(issue => (issue.id === id ? { ...issue, ...patch } : issue));
    writeStorage(ISSUES_STORAGE_KEY, all);
    return all.find(issue => issue.id === id);
  };

  const getCategories = () => {
    const all = getIssues();
    const counts = {};
    all.filter(i => i.status !== "Resolved" && i.status !== "Closed").forEach(i => {
      counts[i.category] = (counts[i.category] || 0) + 1;
    });

    // Ensure common campus maintenance categories are represented
    const defaultCategories = ["Electrical", "Plumbing", "HVAC", "Carpentry", "Networking", "Furniture", "Safety", "Cleaning", "General Maintenance"];
    const result = [];
    const seen = new Set();

    Object.entries(counts).forEach(([category, count]) => {
      result.push({ category, count });
      seen.add(category);
    });

    defaultCategories.forEach(cat => {
      if (!seen.has(cat)) {
        result.push({ category: cat, count: 0 });
      }
    });

    return result.sort((a, b) => b.count - a.count);
  };

  const getSummary = () => {
    const all = getIssues();
    const open = all.filter(i => i.status !== "Resolved" && i.status !== "Closed").length;
    const inProgress = all.filter(i => i.status === "In Progress").length;
    const resolved = all.filter(i => i.status === "Resolved" || i.status === "Closed").length;
    const categories = getCategories().filter(c => c.count > 0);

    return {
      open,
      inProgress,
      resolved,
      newThisWeek: all.filter(i => new Date(i.createdAt) >= new Date("2026-10-21")).length,
      avgResolution: "1.8 days",
      weekly: [
        { day: "Mon", reported: 5, resolved: 3 },
        { day: "Tue", reported: 8, resolved: 5 },
        { day: "Wed", reported: 6, resolved: 4 },
        { day: "Thu", reported: 11, resolved: 7 },
        { day: "Fri", reported: 7, resolved: 5 },
        { day: "Sat", reported: 3, resolved: 4 },
        { day: "Sun", reported: 5, resolved: 3 }
      ],
      categories
    };
  };

  window.MockStore = {
    getUsers,
    getCurrentUser,
    setCurrentUser,
    logout,
    authenticate,
    getIssues,
    getMyIssues,
    createIssue,
    updateIssue,
    getCategories,
    getSummary,
    getStaff: () => seedStaff
  };
})();
