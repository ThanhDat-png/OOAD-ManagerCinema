document.addEventListener("click", function (event) {
    if (event.target.closest("[data-print-tickets]")) {
        window.print();
    }
});

const admissionCheck = document.getElementById("admission-check");
const qrUpload = document.querySelector('form[action="/staff/admission/read"]');
if (admissionCheck && qrUpload) {
    qrUpload.addEventListener("formdata", ({formData}) => {
        for (const field of ["day", "room", "show"]) {
            formData.set(field, admissionCheck.elements.namedItem(field).value);
        }
    });
}
