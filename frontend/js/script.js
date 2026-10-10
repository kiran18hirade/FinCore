async function loadDashboardStats() {
	const message = document.getElementById("dashboardMessage");
	try {
		const stats = await window.apiRequest("/dashboard");
		document.getElementById("customerCount").textContent = stats.customerCount;
		document.getElementById("applicationCount").textContent = stats.applicationCount;
		document.getElementById("approvedLoanCount").textContent = stats.approvedLoanCount;
		message.textContent = "";
	} catch (error) {
		document.getElementById("customerCount").textContent = "-";
		document.getElementById("applicationCount").textContent = "-";
		document.getElementById("approvedLoanCount").textContent = "-";
		message.textContent = error.message;
	}
}

loadDashboardStats();
