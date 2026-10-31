/* Seed data and browser-persisted mock store. */
(() => {
  const seed = [
    { id: "ISS-1048", title: "Leaking faucet in restroom", category: "Plumbing", priority: "High", status: "In Progress", location: "Science Building · 2nd floor", assignee: "Jordan Lee", createdAt: "2026-10-27T08:15:00", description: "The faucet in the north restroom is leaking continuously." },
    { id: "ISS-1047", title: "Flickering lights in lecture hall", category: "Electrical", priority: "Medium", status: "Open", location: "Lecture Hall B · Room 204", assignee: "Sam Rivera", createdAt: "2026-10-26T14:35:00", description: "Three ceiling lights near the front row flicker during class." },
    { id: "ISS-1046", title: "Broken chair in classroom", category: "Furniture", priority: "Low", status: "Open", location: "Arts Building · Room 118", assignee: "Taylor Kim", createdAt: "2026-10-26T11:20:00", description: "One chair has a loose leg and should be replaced." },
    { id: "ISS-1045", title: "Air conditioning not cooling", category: "HVAC", priority: "High", status: "In Progress", location: "Library · 3rd floor", assignee: "Morgan Chen", createdAt: "2026-10-25T16:05:00", description: "The study area is unusually warm even though the thermostat is set to 21°C." },
    { id: "ISS-1044", title: "Overflowing recycling bin", category: "Cleaning", priority: "Low", status: "Resolved", location: "Student Center · East entrance", assignee: "Jamie Patel", createdAt: "2026-10-25T09:40:00", description: "Recycling bin was overflowing during lunch hour." },
    { id: "ISS-1043", title: "Pothole near main walkway", category: "Grounds", priority: "Medium", status: "Open", location: "Main quad · West path", assignee: "Jordan Lee", createdAt: "2026-10-24T10:10:00", description: "A small pothole is forming on the path between the quad and library." },
    { id: "ISS-1042", title: "Exit sign needs replacement", category: "Safety", priority: "High", status: "Resolved", location: "Engineering Building · Level 1", assignee: "Sam Rivera", createdAt: "2026-10-23T13:00:00", description: "The illuminated exit sign near the south stairwell was out." },
    { id: "ISS-1041", title: "Water fountain running slowly", category: "Plumbing", priority: "Medium", status: "Open", location: "Gymnasium · Main lobby", assignee: "Unassigned", createdAt: "2026-10-22T08:55:00", description: "The fountain has very low water pressure." }
  ];
  const staff = [{ name: "Jordan Lee", role: "Plumbing & general" }, { name: "Sam Rivera", role: "Electrical" }, { name: "Taylor Kim", role: "Facilities" }, { name: "Morgan Chen", role: "HVAC" }, { name: "Jamie Patel", role: "Custodial" }];
  const read = () => { try { const saved = localStorage.getItem("campuscare-issues"); return saved ? JSON.parse(saved) : seed; } catch { return seed; } };
  const write = (issues) => { localStorage.setItem("campuscare-issues", JSON.stringify(issues)); return issues; };
  window.MockStore = {
    getIssues: () => read(), getStaff: () => staff,
    createIssue: (payload) => { const all = read(); const highest = Math.max(1040, ...all.map(i => Number(String(i.id).replace("ISS-", "")) || 0)); const issue = { ...payload, id: `ISS-${highest + 1}`, status: "Open", assignee: "Unassigned", createdAt: new Date().toISOString() }; write([issue, ...all]); return issue; },
    updateIssue: (id, patch) => { const all = read().map(issue => issue.id === id ? { ...issue, ...patch } : issue); write(all); return all.find(issue => issue.id === id); },
    getSummary: () => { const all = read(), open = all.filter(i => i.status !== "Resolved").length, inProgress = all.filter(i => i.status === "In Progress").length, resolved = all.filter(i => i.status === "Resolved").length; const byCategory = {}; all.filter(i => i.status !== "Resolved").forEach(i => byCategory[i.category] = (byCategory[i.category] || 0) + 1); return { open, inProgress, resolved, newThisWeek: all.filter(i => new Date(i.createdAt) >= new Date("2026-10-26")).length, avgResolution: "1.8 days", weekly: [{day:"Mon",reported:5,resolved:3},{day:"Tue",reported:8,resolved:5},{day:"Wed",reported:6,resolved:4},{day:"Thu",reported:11,resolved:7},{day:"Fri",reported:7,resolved:5},{day:"Sat",reported:3,resolved:4},{day:"Sun",reported:5,resolved:3}], categories: Object.entries(byCategory).sort((a,b) => b[1]-a[1]).map(([name,count]) => ({name,count})) }; }
  };
})();
