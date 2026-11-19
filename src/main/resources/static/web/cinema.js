(() => {
  const root = document.documentElement;
  try { root.dataset.theme = localStorage.getItem("cinema-theme") || "dark"; } catch { root.dataset.theme = "dark"; }
  document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".theme-toggle").forEach(btn => btn.addEventListener("click", () => {
      root.dataset.theme = root.dataset.theme === "light" ? "dark" : "light";
      try { localStorage.setItem("cinema-theme", root.dataset.theme); } catch {}
    }));
    const peek = document.querySelector(".peek");
    if (peek) peek.addEventListener("click", () => {
      const input = document.getElementById("password");
      const show = input.type === "password";
      input.type = show ? "text" : "password";
      peek.setAttribute("aria-pressed", String(show));
      peek.setAttribute("aria-label", show ? "Ẩn mật khẩu" : "Hiện mật khẩu");
    });
    const toggle = document.querySelector(".menu-toggle");
    const scrim = document.querySelector(".sidebar-scrim");
    const sidebar = document.getElementById("side");
    if (!toggle || !scrim || !sidebar) return;
    const compact = matchMedia("(max-width:900px)");
    const links = [...sidebar.querySelectorAll("nav a, .logout button")];
    function setMenu(open) {
      toggle.setAttribute("aria-expanded", String(open));
      document.body.classList.toggle("menu-open", open);
      scrim.hidden = !open;
      if (open) links[0]?.focus();
      else if (compact.matches) toggle.focus();
    }
    toggle.addEventListener("click", () => setMenu(toggle.getAttribute("aria-expanded") === "false"));
    scrim.addEventListener("click", () => setMenu(false));
    compact.addEventListener("change", event => { if (!event.matches && !scrim.hidden) setMenu(false); });
    document.addEventListener("keydown", event => {
      if (scrim.hidden) return;
      if (event.key === "Escape") setMenu(false);
      if (event.key !== "Tab") return;
      const items = [toggle, ...links];
      const index = items.indexOf(document.activeElement);
      if (index === -1 || (event.shiftKey ? index === 0 : index === items.length - 1)) {
        event.preventDefault();
        items[event.shiftKey ? items.length - 1 : 0].focus();
      }
    });
  });
})();
