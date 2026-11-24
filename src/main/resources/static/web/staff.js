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
if (admissionCheck) {
    const roomSelect = admissionCheck.elements.namedItem("room");
    const showSelect = admissionCheck.elements.namedItem("show");
    const codeInput = admissionCheck.elements.namedItem("code");
    const confirmForm = document.querySelector('form[action$="/staff/admission/confirm"]');
    function syncShows() {
        if (!roomSelect || !showSelect) return;
        const room = roomSelect.value;
        let validSelection = false;
        for (const option of showSelect.options) {
            if (!option.value) continue;
            const matches = !room || option.dataset.room === room;
            option.hidden = !matches;
            option.disabled = !matches;
            if (matches && option.selected) validSelection = true;
        }
        if (showSelect.value && !validSelection) showSelect.value = "";
    }
    function invalidateConfirm() {
        if (confirmForm) confirmForm.hidden = true;
    }
    if (roomSelect) {
        syncShows();
        roomSelect.addEventListener("change", () => {
            syncShows();
            invalidateConfirm();
        });
    }
    if (showSelect) showSelect.addEventListener("change", invalidateConfirm);
    if (codeInput) codeInput.addEventListener("input", invalidateConfirm);
}

const incoming = document.querySelector("[data-incoming-url]");
if (incoming) {
    const refresh = incoming.querySelector("[data-refresh-incoming]");
    const status = incoming.querySelector("[data-incoming-status]");
    const count = incoming.querySelector("[data-incoming-count]");
    refresh.hidden = false;

    async function refreshIncoming(manual = false) {
        if (refresh.disabled) return;
        refresh.disabled = true;
        status.textContent = "";
        try {
            const response = await fetch(incoming.dataset.incomingUrl, {cache: "no-store"});
            if (!response.ok || response.redirected) throw new Error();
            const fragment = new DOMParser().parseFromString(await response.text(), "text/html")
                .querySelector("[data-incoming-list]");
            if (!fragment) throw new Error();
            incoming.querySelector("[data-incoming-list]").replaceWith(fragment);
            count.textContent = fragment.dataset.requestCount;
            count.hidden = fragment.dataset.requestCount === "0";
            if (manual) status.textContent = "Đã cập nhật danh sách.";
        } catch {
            status.textContent = "Không thể cập nhật danh sách. Vui lòng thử lại.";
        } finally {
            refresh.disabled = false;
        }
    }

    incoming.addEventListener("toggle", () => {
        if (incoming.open) refreshIncoming();
    });
    refresh.addEventListener("click", () => refreshIncoming(true));
}
