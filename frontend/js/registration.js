const registrationForm = document.getElementById("registrationForm");
const formMessage = document.getElementById("formMessage");

registrationForm.addEventListener("submit", async function (event) {

    event.preventDefault();

    const submitButton =
        registrationForm.querySelector("button[type='submit']");

    submitButton.disabled = true;
    formMessage.textContent = "Registering customer...";

    try {

        const customer = await window.apiRequest("/customers", {
            method: "POST",

            body: JSON.stringify({

                fullName: document.getElementById("name").value,

                email: document.getElementById("email").value,

                phone: document.getElementById("phone").value,

                password: document.getElementById("password").value,

                dateOfBirth: document.getElementById("dob").value,

                address: document.getElementById("address").value,

                city: document.getElementById("city").value,

                employmentType:
                    document.getElementById("employment").value,

                companyName:
                    document.getElementById("employer").value,

                monthlyIncome:
                    Number(document.getElementById("income").value),

                experienceYears:
                    Number(document.getElementById("experience").value),

                panNumber:
                    document.getElementById("panNumber").value,

                aadhaarLast4:
                    document.getElementById("aadhaarLast4").value

            })
        });

        formMessage.textContent =
            `Customer registered successfully. Customer ID: ${customer.customerId}`;

        // Continue to loan application
        setTimeout(() => {
            window.location.href =
                `loan-application.html?customerId=${encodeURIComponent(customer.customerId)}`;
        }, 800);

    } catch (error) {

        formMessage.textContent = error.message;

        submitButton.disabled = false;
    }
});