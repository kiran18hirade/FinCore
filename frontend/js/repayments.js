const customerSearchForm = document.getElementById("customerSearchForm");
const customerIdInput = document.getElementById("customerId");
const repaymentMessage = document.getElementById("repaymentMessage");
const repaymentRows = document.getElementById("repaymentRows");
const customerIdFromUrl = Number(new URLSearchParams(window.location.search).get("customerId"));

if (Number.isInteger(customerIdFromUrl) && customerIdFromUrl > 0) {
    customerIdInput.value = customerIdFromUrl;
    loadRepayments(customerIdFromUrl);
}

customerSearchForm.addEventListener("submit", (event) => {
    event.preventDefault();
    loadRepayments(Number(customerIdInput.value));
});

async function loadRepayments(customerId) {
    repaymentMessage.textContent = "Loading repayment schedule...";
    repaymentRows.replaceChildren();

    try {
        const items = await window.apiRequest(`/customers/${customerId}/repayments`);
        repaymentMessage.textContent = items.length ? "" : "No repayment schedule was found for this customer.";
        if (!items.length) {
            showTableMessage("No installments to display.");
            return;
        }

        items.forEach((item) => repaymentRows.append(createRepaymentRow(item)));
    } catch (error) {
        repaymentMessage.textContent = error.message;
        showTableMessage("Repayment schedule could not be loaded.");
    }
}

function createRepaymentRow(item) {
    const row = document.createElement("tr");
    const values = [
        `${item.loanType} #${item.loanId}`,
        item.installmentNo,
        item.dueDate,
        `₹${Number(item.emiAmount).toLocaleString("en-IN", { maximumFractionDigits: 2 })}`,
        item.status
    ];

    values.forEach((value) => {
        const cell = document.createElement("td");
        cell.textContent = value;
        row.append(cell);
    });

    const actionCell = document.createElement("td");
    if (item.status === "PENDING") {
        const payButton = document.createElement("button");
        payButton.type = "button";
        payButton.className = "secondary-button";
        payButton.textContent = "Mark paid";
        payButton.addEventListener("click", () => payInstallment(item.scheduleId, payButton));
        actionCell.append(payButton);
    } else {
        actionCell.textContent = "-";
    }
    row.append(actionCell);
    return row;
}

async function payInstallment(scheduleId, button) {
    button.disabled = true;
    repaymentMessage.textContent = "Recording payment...";

    try {
        await window.apiRequest(`/repayments/${scheduleId}/pay`, {
            method: "POST",
            body: "{}"
        });
        repaymentMessage.textContent = "Payment recorded.";
        await loadRepayments(Number(customerIdInput.value));
    } catch (error) {
        repaymentMessage.textContent = error.message;
        button.disabled = false;
    }
}

function showTableMessage(message) {
    const row = document.createElement("tr");
    const cell = document.createElement("td");
    cell.colSpan = 6;
    cell.textContent = message;
    row.append(cell);
    repaymentRows.replaceChildren(row);
}
