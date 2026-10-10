const applicationId = Number(new URLSearchParams(window.location.search).get("applicationId"));
const profileMessage = document.getElementById("profileMessage");

if (!Number.isInteger(applicationId) || applicationId < 1) {
    profileMessage.textContent = "Application not found. Submit a loan application to view its status.";
} else {
    loadApplicationProfile();
}

async function loadApplicationProfile() {
    try {
        const application = await window.apiRequest(`/applications/${applicationId}`);
        const setText = (id, value) => {
            document.getElementById(id).textContent = value ?? "-";
        };
        const formatAmount = (value) => value == null
            ? "-"
            : Number(value).toLocaleString("en-IN", { maximumFractionDigits: 2 });

        setText("customerId", application.customerId);
        setText("name", application.fullName);
        setText("email", application.email);
        setText("phone", application.phone);
        setText("employment", application.employmentType.replaceAll("_", " "));
        setText("income", formatAmount(application.monthlyIncome));
        setText("applicationId", application.applicationId);
        setText("loanType", application.loanType);
        setText("loanAmount", formatAmount(application.requestedAmount));
        setText("loanTenure", application.tenureMonths);
        setText("loanPurpose", application.purpose);
        setText("existingEmi", formatAmount(application.existingEmi));
        setText("applicationStatus", application.status);
        setText("decisionReason", application.rejectionReason || "No rejection reason");
        setText("monthlyEmi", formatAmount(application.monthlyEmi));
        setText("creditScore", application.creditScore);
        document.getElementById("repaymentLink").href =
            `repayments.html?customerId=${encodeURIComponent(application.customerId)}`;
    } catch (error) {
        profileMessage.textContent = error.message;
    }
}
