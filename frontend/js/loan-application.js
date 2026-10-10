const customerId = Number(new URLSearchParams(window.location.search).get("customerId"));
const loanForm = document.getElementById("loanForm");
const formMessage = document.getElementById("formMessage");
const loanTypeSelect = document.getElementById("loanTypeId");
const tenureInput = document.getElementById("loanTenure");

if (!Number.isInteger(customerId) || customerId < 1) {
    window.location.href = "registration.html";
} else {
    loadApplicationForm();
}

async function loadApplicationForm() {
    try {
        const [customer, products] = await Promise.all([
            window.apiRequest(`/customers/${customerId}`),
            window.apiRequest("/loan-products")
        ]);

        document.getElementById("customerName").textContent = customer.fullName;
        document.getElementById("customerId").textContent = customer.customerId;

        loanTypeSelect.replaceChildren(
            new Option("Select loan product", "")
        );

        products.forEach((product) => {
            const option = new Option(
                product.loanType,
                product.loanTypeId
            );

            option.dataset.maxTenure = product.maxTenureMonths;
            option.dataset.minAmount = product.minAmount;
            option.dataset.maxAmount = product.maxAmount;

            loanTypeSelect.add(option);
        });

        loanTypeSelect.addEventListener("change", updateProductLimits);
        loanForm.addEventListener("submit", submitApplication);

    } catch (error) {
        formMessage.textContent = error.message;
        loanForm.querySelector("button[type='submit']").disabled = true;
    }
}

function updateProductLimits() {
    const selected = loanTypeSelect.selectedOptions[0];
    const amountInput = document.getElementById("loanAmount");

    if (!selected || !selected.value) {
        return;
    }

    amountInput.min = selected.dataset.minAmount;
    amountInput.max = selected.dataset.maxAmount;
    tenureInput.max = selected.dataset.maxTenure;
}

async function submitApplication(event) {
    event.preventDefault();

    formMessage.textContent = "Submitting application...";

    const submitButton = loanForm.querySelector("button[type='submit']");
    submitButton.disabled = true;

    try {
        const application = await window.apiRequest("/applications", {
            method: "POST",
            body: JSON.stringify({
                customerId,
                loanTypeId: Number(loanTypeSelect.value),
                requestedAmount: Number(
                    document.getElementById("loanAmount").value
                ),
                tenureMonths: Number(tenureInput.value),
                purpose: document.getElementById("loanPurpose").value,
                existingEmi: Number(
                    document.getElementById("existingEmi").value
                ),
                creditScore: Number(
                    document.getElementById("creditScore").value
                )
            })
        });

        /*
         * Application has now been evaluated by the backend.
         * Send the user to the result page instead of directly
         * sending them to repayments.
         */
        window.location.href =
            `application-result.html?applicationId=${encodeURIComponent(
                application.applicationId
            )}`;

    } catch (error) {
        formMessage.textContent = error.message;
        submitButton.disabled = false;
    }
}