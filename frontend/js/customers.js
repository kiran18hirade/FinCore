let customers = [];

const table =
    document.getElementById("customerTable");

const search =
    document.getElementById("customerSearch");


loadCustomers();


async function loadCustomers() {

    try {

        customers =
            await window.apiRequest("/customers");

        renderCustomers(customers);

    } catch (error) {

        table.innerHTML = `
            <tr>
                <td colspan="7">
                    ${error.message}
                </td>
            </tr>
        `;

    }
}


function renderCustomers(list) {

    if (!list.length) {

        table.innerHTML = `
            <tr>
                <td colspan="7">
                    No customers found.
                </td>
            </tr>
        `;

        return;
    }


    table.innerHTML = list.map(customer => `

        <tr>

            <td>
                ${customer.customerId}
            </td>

            <td>
                <strong>
                    ${customer.fullName}
                </strong>
            </td>

            <td>
                ${customer.email}
            </td>

            <td>
                ${customer.phone}
            </td>

            <td>
                ${customer.employmentType || "—"}
            </td>

            <td>
                ₹${Number(customer.monthlyIncome)
                    .toLocaleString("en-IN")}
            </td>

            <td>

                <a
                    class="view-button"
                    href="customer-profile.html?customerId=${customer.customerId}">
                    View
                </a>

            </td>

        </tr>

    `).join("");
}


search.addEventListener("input", () => {

    const query =
        search.value.toLowerCase().trim();


    const filtered =
        customers.filter(customer =>

            customer.fullName
                .toLowerCase()
                .includes(query)

            ||

            customer.email
                .toLowerCase()
                .includes(query)

            ||

            customer.phone
                .includes(query)

        );


    renderCustomers(filtered);

});