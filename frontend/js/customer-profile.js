const params =
    new URLSearchParams(window.location.search);

const customerId =
    Number(params.get("customerId"));

const container =
    document.getElementById("customerDetails");


if (!Number.isInteger(customerId) || customerId < 1) {

    container.innerHTML = `
        <div class="error-card">
            Invalid customer ID.
        </div>
    `;

} else {

    loadCustomer();

}


async function loadCustomer() {

    try {

        const customer =
            await window.apiRequest(
                `/customers/${customerId}`
            );


        container.innerHTML = `

            <div class="profile-card">

                <div class="profile-header">

                    <div class="profile-avatar">
                        ${customer.fullName
                            .charAt(0)
                            .toUpperCase()}
                    </div>

                    <div>

                        <h2>
                            ${customer.fullName}
                        </h2>

                        <p>
                            Customer ID:
                            ${customer.customerId}
                        </p>

                    </div>

                </div>


                <div class="profile-grid">

                    <div>
                        <span>Email</span>
                        <strong>
                            ${customer.email}
                        </strong>
                    </div>

                    <div>
                        <span>Phone</span>
                        <strong>
                            ${customer.phone}
                        </strong>
                    </div>

                    <div>
                        <span>Employment</span>
                        <strong>
                            ${customer.employmentType}
                        </strong>
                    </div>

                    <div>
                        <span>Monthly Income</span>
                        <strong>
                            ₹${Number(customer.monthlyIncome)
                                .toLocaleString("en-IN")}
                        </strong>
                    </div>

                </div>

            </div>

        `;

    } catch (error) {

        container.innerHTML = `
            <div class="error-card">
                ${error.message}
            </div>
        `;

    }
}