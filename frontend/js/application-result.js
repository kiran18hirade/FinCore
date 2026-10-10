const params =
    new URLSearchParams(window.location.search);

const applicationId =
    Number(params.get("applicationId"));

const resultContainer =
    document.getElementById("resultContainer");

const repaymentButton =
    document.getElementById("repaymentButton");


if (!Number.isInteger(applicationId) ||
    applicationId < 1) {

    resultContainer.innerHTML = `
        <div class="result-card rejected">

            <h2>
                Invalid Application
            </h2>

            <p>
                No valid application was provided.
            </p>

        </div>
    `;

} else {

    loadApplicationResult();

}


async function loadApplicationResult() {

    try {

        const application =
            await window.apiRequest(
                `/applications/${applicationId}`
            );


        const approved =
            application.status === "APPROVED";


        resultContainer.innerHTML = `

            <div class="
                result-card
                ${approved ? "approved" : "rejected"}
            ">

                <div class="result-icon">

                    ${approved ? "✓" : "✕"}

                </div>


                <h2>

                    ${
                        approved
                        ? "Loan Application Approved"
                        : "Loan Application Rejected"
                    }

                </h2>


                <p class="result-subtitle">

                    Application #
                    ${application.applicationId}

                </p>


                <div class="decision-status">

                    ${application.status}

                </div>


                <div class="application-summary">

                    <div>
                        <span>Customer</span>
                        <strong>
                            ${application.fullName}
                        </strong>
                    </div>


                    <div>
                        <span>Loan Product</span>
                        <strong>
                            ${application.loanType}
                        </strong>
                    </div>


                    <div>
                        <span>Loan Amount</span>
                        <strong>
                            ₹${Number(
                                application.requestedAmount
                            ).toLocaleString("en-IN")}
                        </strong>
                    </div>


                    <div>
                        <span>Tenure</span>
                        <strong>
                            ${application.tenureMonths}
                            months
                        </strong>
                    </div>


                    <div>
                        <span>Credit Score</span>
                        <strong>
                            ${application.creditScore}
                        </strong>
                    </div>


                    <div>
                        <span>Monthly EMI</span>
                        <strong>

                            ${
                                application.monthlyEmi
                                ? "₹" +
                                  Number(
                                      application.monthlyEmi
                                  ).toLocaleString("en-IN")
                                : "—"
                            }

                        </strong>
                    </div>

                </div>


                ${
                    approved

                    ?

                    `
                    <div class="
                        decision-message
                        success-message
                    ">

                        Your application satisfies
                        the required eligibility criteria
                        and has been approved.

                    </div>
                    `

                    :

                    `
                    <div class="
                        decision-message
                        error-message
                    ">

                        <strong>
                            Reason for rejection:
                        </strong>

                        <br>

                        ${
                            application.rejectionReason ||
                            "Eligibility requirements were not satisfied."
                        }

                    </div>
                    `

                }

            </div>

        `;


        if (approved) {

            repaymentButton.style.display =
                "inline-block";

            repaymentButton.href =
                `repayments.html?customerId=${
                    encodeURIComponent(
                        application.customerId
                    )
                }`;

        }

    } catch (error) {

        resultContainer.innerHTML = `

            <div class="result-card rejected">

                <h2>
                    Unable to Load Application
                </h2>

                <p>
                    ${error.message}
                </p>

            </div>

        `;

    }

}