document.addEventListener("DOMContentLoaded", () => {
  const mode = document.querySelector("#report-period");
  if (mode) {
    const updatePeriod = () => {
      document.querySelectorAll("[data-period]").forEach(field => {
        field.hidden = field.dataset.period !== mode.value;
        field.querySelector("input").disabled = field.hidden;
      });
    };
    mode.addEventListener("change", updatePeriod);
    updatePeriod();
  }
  const file = document.querySelector('input[name="upload"]');
  if (file) {
    const preview = document.querySelector(".upload-preview");
    let previous;
    file.addEventListener("change", () => {
      if (previous) URL.revokeObjectURL(previous);
      const image = file.files[0];
      preview.hidden = !image;
      if (image) {
        previous = URL.createObjectURL(image);
        preview.src = previous;
      }
    });
  }
});
