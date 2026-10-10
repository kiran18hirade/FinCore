let applications = [];

const table =
    document.getElementById("applicationTable");

const filterButtons =
    document.querySelectorAll(".filter-button");


loadApplications();


async function loadApplications() {

    try {

        applications =
            await window.apiRequest("/applications");

        renderApplications(applications);

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


function renderApplications(list) {

    if (!list.length) {

        table.innerHTML = `
            <tr>
                <td colspan="7">
                    No applications found.
                </td>
            </tr>
        `;

        return;
    }


    table.innerHTML =
        list.map(application => {

            const status =
                application.status || "PENDING";


            return `

                <tr>

                    <td>
                        #${application.applicationId}
                    </td>

                    <td>
                        <strong>
                            ${application.fullName}
                        </strong>
                    </td>

                    <td>
                        ${application.loanType}
                    </td>

                    <td>
                        ₹${Number(
                            application.requestedAmount
                        ).toLocaleString("en-IN")}
                    </td>

                    <td>
                        ${application.creditScore}
                    </td>

                    <td>

                        <span class="
                            status-badge
                            status-${status.toLowerCase()}
                        ">
                            ${status}
                        </span>

                    </td>

                    <td>

                        <a
                            class="view-button"
                            href="application-result.html?applicationId=${application.applicationId}">
                            View
                        </a>

                    </td>

                </tr>

            `;

        }).join("");
}


filterButtons.forEach(button => {

    button.addEventListener("click", () => {

        filterButtons.forEach(btn =>
            btn.classList.remove("active")
        );

        button.classList.add("active");


        const filter =
            button.dataset.filter;


        if (filter === "ALL") {

            renderApplications(applications);

        } else {

            renderApplications(
                applications.filter(
                    application =>
                        application.status === filter
                )
            );

        }

    });

});