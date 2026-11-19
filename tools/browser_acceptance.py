"""Browser acceptance against a throwaway local Spring/SQLite fixture.

Build with mvn -DskipTests package, install playwright, then run this script.
Microsoft Edge is the default. Pass --channel chromium for Playwright Chromium.
"""

import argparse
from contextlib import closing
from datetime import datetime, timedelta, timezone
import json
import os
from pathlib import Path
import re
import sqlite3
import subprocess
from tempfile import TemporaryDirectory
from time import monotonic, sleep
from urllib.request import urlopen

from playwright.sync_api import expect, sync_playwright


ROOT = Path(__file__).resolve().parents[1]
PASSWORD = "Cinema@123"
WIDTHS = {375: 812, 768: 1024, 1440: 960}
expect.set_options(timeout=15_000)


def host_url(process, log_path):
    deadline = monotonic() + 150
    while monotonic() < deadline:
        log = log_path.read_text(encoding="utf-8", errors="replace")
        port = re.search(r"Tomcat started on port (\d+)", log)
        if port:
            base = f"http://127.0.0.1:{port[1]}"
            try:
                with urlopen(base + "/health", timeout=2) as response:
                    if response.status == 200:
                        # Warm the first rendered route and its session generator.
                        with urlopen(base + "/login", timeout=25) as login_response:
                            if login_response.status == 200:
                                return base
            except OSError:
                pass
        if process.poll() is not None:
            break
        sleep(0.5)
    raise RuntimeError(f"Spring failed to start. See {log_path}")


def login(page, role, password=PASSWORD):
    page.goto("/login")
    page.get_by_label("Tên đăng nhập", exact=True).fill(role + "_demo")
    page.get_by_label("Mật khẩu", exact=True).fill(password)
    page.get_by_role("button", name="Đăng nhập", exact=True).click()


def click_nav(page, path):
    toggle = page.locator(".menu-toggle")
    if toggle.is_visible() and toggle.get_attribute("aria-expanded") != "true":
        toggle.click()
    page.locator(f'#side nav a[href="{path}"]').click()
    expect(page.locator(f'#side nav a[href="{path}"]')).to_have_attribute("aria-current", "page")


def check_layout(page):
    expect(page.locator("main")).to_have_count(1)
    expect(page.locator("h1")).to_have_count(1)
    expect(page.locator("html")).to_have_attribute("lang", "vi")
    widths = page.evaluate("() => [document.documentElement.scrollWidth, innerWidth]")
    assert widths[0] <= widths[1], f"Horizontal page overflow at {page.url}: {widths}"


def capture(page, output, prefix):
    for width, height in WIDTHS.items():
        page.set_viewport_size({"width": width, "height": height})
        for theme in ("dark", "light"):
            if page.locator("html").get_attribute("data-theme") != theme:
                page.locator(".theme-toggle").click()
            expect(page.locator("html")).to_have_attribute("data-theme", theme)
            check_layout(page)
            page.screenshot(path=output / f"{prefix}-{width}-{theme}.png", animations="disabled")
    page.reload()
    expect(page.locator("html")).to_have_attribute("data-theme", "light")


def check_keyboard(page):
    page.set_viewport_size({"width": 375, "height": 812})
    page.reload()
    page.keyboard.press("Tab")
    expect(page.locator(".skip-link")).to_be_focused()
    assert page.locator(".skip-link").evaluate("(el) => getComputedStyle(el).outlineStyle") != "none"
    page.keyboard.press("Enter")
    expect(page.locator("#workspace-main")).to_be_focused()
    toggle = page.locator(".menu-toggle")
    toggle.focus()
    page.keyboard.press("Enter")
    expect(toggle).to_have_attribute("aria-expanded", "true")
    expect(page.locator("#side nav a").first).to_be_focused()
    page.keyboard.press("Tab")
    expect(page.locator("#side nav a").nth(1)).to_be_focused()
    page.keyboard.press("Shift+Tab")
    page.keyboard.press("Shift+Tab")
    expect(toggle).to_be_focused()
    page.keyboard.press("Shift+Tab")
    expect(page.locator(".logout button")).to_be_focused()
    page.keyboard.press("Escape")
    expect(toggle).to_have_attribute("aria-expanded", "false")
    expect(toggle).to_be_focused()
    page.set_viewport_size({"width": 1440, "height": 960})


def prepare_show(database):
    start = datetime.now(timezone(timedelta(hours=7))) + timedelta(minutes=8)
    date_format = "%Y-%m-%d %H:%M:%S"
    with closing(sqlite3.connect(database)) as conn, conn:
        show, room = conn.execute(
            "SELECT MaSuat, MaPhong FROM SUATCHIEU ORDER BY MaSuat LIMIT 1"
        ).fetchone()
        conn.execute(
            "UPDATE SUATCHIEU SET ThoiGianBatDau=?, ThoiGianTraPhong=? WHERE MaSuat=?",
            (start.strftime(date_format), (start + timedelta(hours=3)).strftime(date_format), show),
        )
    return str(show), room, start.date().isoformat()


def counter_workflow(page, database, output):
    show, room, day = prepare_show(database)
    page.locator('input[name="day"]').fill(day)
    page.get_by_label("Tên phim").fill("no-matching-film-browser-check")
    page.get_by_role("button", name="Tìm kiếm").click()
    expect(page.get_by_text("Không tìm thấy suất chiếu phù hợp")).to_be_visible()
    page.get_by_label("Tên phim").clear()
    page.get_by_role("button", name="Tìm kiếm").click()
    page.locator(f'a[href="/staff/sales?show={show}"]').click()
    capture(page, output, "staff-seats")
    seat = page.locator('input[name="seats"]:enabled').first
    seat.focus()
    page.keyboard.press("Space")
    expect(seat).to_be_checked()
    for action in ("Giữ ghế đã chọn", "Tính tiền", "Thanh toán"):
        page.get_by_role("button", name=action, exact=True).click()
    amount = page.locator(".st-total b").inner_text().split()[0]
    complete = page.get_by_role("button", name="Hoàn tất giao dịch")
    complete.click()
    assert page.locator('select[name="method"]').evaluate("(el) => el.validity.valueMissing")
    page.get_by_label("Phương thức thanh toán").select_option("Tiền mặt")
    page.get_by_label("Đã xác nhận nhận đủ tiền tại quầy").check()
    complete.click()
    expect(page.get_by_role("heading", name="Vé đã phát hành")).to_be_visible()
    ticket = page.locator(".st-ticket small").inner_text()
    order = page.locator(".counter-title p").inner_text()
    expect(page.locator(".st-ticket img")).to_have_js_property("complete", True)
    qr = page.request.get(page.locator(".st-ticket img").get_attribute("src"))
    assert qr.ok and qr.headers["content-type"] == "image/png"
    page.evaluate("() => { window.print = () => { window.printInvoked = true; }; }")
    page.get_by_role("button", name="In vé").click()
    assert page.evaluate("() => window.printInvoked")
    page.emulate_media(media="print")
    assert page.locator(".app-top").evaluate("(el) => getComputedStyle(el).display") == "none"
    assert page.locator(".st-ticket").evaluate("(el) => getComputedStyle(el).visibility") == "visible"
    page.pdf(path=output / "sample-ticket.pdf")
    page.emulate_media(media="screen")
    click_nav(page, "/staff/transactions")
    page.get_by_label("Mã giao dịch hoặc mã vé").fill(ticket)
    page.get_by_role("button", name="Tìm kiếm").click()
    expect(page.get_by_role("link", name=order)).to_be_visible()

    click_nav(page, "/staff/admission")
    page.get_by_label("Ngày chiếu").fill(day)
    page.get_by_role("button", name="Tải suất chiếu").click()
    page.get_by_label("Phòng đang kiểm soát").select_option(room)
    page.get_by_label("Suất chiếu đang kiểm soát").select_option(show)
    page.get_by_text("Quét mã QR từ ảnh", exact=True).click()
    page.get_by_label("Ảnh mã QR").set_input_files(
        {"name": "ticket.png", "mimeType": "image/png", "buffer": qr.body()}
    )
    page.get_by_role("button", name="Đọc mã QR").click()
    expect(page.get_by_text(ticket, exact=True)).to_be_visible()
    expect(page.get_by_label("Phòng đang kiểm soát")).to_have_value(room)
    expect(page.get_by_label("Suất chiếu đang kiểm soát")).to_have_value(show)
    check = page.get_by_role("button", name="Kiểm tra tính hợp lệ")
    check.click()
    expect(page.get_by_role("status")).to_have_text("Vé hợp lệ")
    page.get_by_role("button", name="Xác nhận", exact=True).click()
    expect(page.get_by_role("status")).to_have_text("Xác nhận vé thành công")
    check.click()
    expect(page.get_by_role("alert")).to_have_text("Vé đã được sử dụng")
    expect(page.get_by_role("button", name="Xác nhận", exact=True)).to_have_count(0)
    with closing(sqlite3.connect(database)) as conn:
        payment_day, = conn.execute(
            "SELECT date(ThoiGianThanhToan) FROM THANHTOAN WHERE MaGiaoDich=?", (order,)
        ).fetchone()
    return payment_day, amount


def exercise(browser, base, database, output):
    errors = []
    timings = {}
    admin = browser.new_page(base_url=base, viewport={"width": 1440, "height": 960})
    staff = browser.new_page(base_url=base, viewport={"width": 1440, "height": 960})
    for page in (admin, staff):
        page.set_default_timeout(25_000)
        page.set_default_navigation_timeout(60_000)
        page.on("pageerror", lambda error: errors.append(str(error)))
        page.on("response", lambda response: errors.append(f"{response.status} {response.url}")
                if response.status >= 500 else None)

    start = monotonic()
    staff.goto("/staff/sales")
    expect(staff).to_have_url(re.compile(r"/login$"))
    capture(staff, output, "login")
    login(staff, "staff", "wrong-test-password")
    expect(staff.get_by_role("alert")).to_be_visible()
    assert staff.locator(".login-form").evaluate(
        '(form) => !!form.querySelector("input[name=_csrf]")'
    ), "The login form must include a CSRF token"
    login(admin, "admin")
    expect(admin).to_have_url(re.compile(r"/admin/showtimes$"))
    login(staff, "staff")
    expect(staff).to_have_url(re.compile(r"/staff/sales$"))
    assert staff.goto("/admin/reports").status == 403
    expect(staff.locator("h1")).to_have_text("Không thể thực hiện yêu cầu")
    assert admin.goto("/staff/sales").status == 403
    assert admin.goto("/browser-acceptance-missing").status == 404
    expect(admin.locator("h1")).to_have_text("Không tìm thấy trang")
    timings["login_and_error_statuses_seconds"] = round(monotonic() - start, 2)

    start = monotonic()
    counts = {}
    for role, page, landing in (("admin", admin, "/admin/showtimes"), ("staff", staff, "/staff/sales")):
        page.goto(landing)
        check_keyboard(page)
        links = page.locator("#side nav a").evaluate_all("(items) => items.map(a => a.pathname)")
        for path in links:
            click_nav(page, path)
            for width, height in WIDTHS.items():
                page.set_viewport_size({"width": width, "height": height})
                check_layout(page)
            page.set_viewport_size({"width": 1440, "height": 960})
        counts[role] = len(links)
    capture(admin, output, "admin-reports")
    timings["navigation_and_responsive_seconds"] = round(monotonic() - start, 2)

    start = monotonic()
    click_nav(staff, "/staff/sales")
    day, amount = counter_workflow(staff, database, output)
    admin.get_by_label("Ngày thanh toán", exact=True).fill(day)
    admin.get_by_role("button", name="Thống kê").click()
    expect(admin.locator("tbody td")).to_have_text(["1", "1", "1", "0", amount])
    for page in (admin, staff):
        page.get_by_role("button", name="Đăng xuất").click()
        expect(page).to_have_url(re.compile(r"/login$"))
    assert not errors, errors
    timings["sale_print_admission_and_revenue_seconds"] = round(monotonic() - start, 2)
    return {"navigation_links": counts, "screenshots": 18, "revenue_vnd": amount, **timings}


def write_evidence(output):
    def passed(observation):
        return {"status": "pass", "evidence": observation}

    sizes = {width: f"{width}x{height}" for width, height in WIDTHS.items()}
    observations = {
        "login": "Login heading and labeled credentials rendered.",
        "admin-reports": "Administrator report tabs and period controls rendered.",
        "staff-seats": "Seat map and ticket sale controls rendered.",
    }
    record = {
        "contract": "docs/WEB_USE_CASES.md. Role-specific operations, dark and light themes, and local ticket lifecycle.",
        "targetViewports": list(sizes.values()),
        "accessibility": passed("Keyboard skip link and visible focus, drawer focus and Escape, labeled fields, and Space to select a seat."),
        "responsive": passed("Both workspaces' navigation pages and the photographed views fit all three viewport widths."),
        "contentStates": {
            "loading": {"status": "not-applicable", "reason": "Checked routes use full server-rendered navigation with no in-page loading state."},
            "empty": passed("An unmatched film search displays a no-results message."),
            "error": passed("Failed login, missing required payment method, 404, and duplicate admission show their error states."),
            "partial": passed("The held seat progresses through quote and payment before ticket issuance."),
            "success": passed("Payment issues the ticket, the QR image identifies it, and admission and revenue reconciliation succeed."),
            "permission": passed("Anonymous staff access redirects, and staff and admin receive 403 on one another's protected routes."),
        },
        "renderedEvidence": [
            {"viewport": viewport, "artifact": f"{prefix}-{width}-{theme}.png",
             "observation": f"{observation} {theme.title()} theme."}
            for width, viewport in sizes.items()
            for theme in ("dark", "light")
            for prefix, observation in observations.items()
        ],
    }
    (output / "evidence.json").write_text(json.dumps(record, indent=2) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", type=Path, default=ROOT / "target/manager-cinema-1.0-SNAPSHOT.jar")
    parser.add_argument("--output", type=Path, default=ROOT / "target/web-qa")
    parser.add_argument("--channel", default="msedge")
    args = parser.parse_args()
    if not args.jar.is_file():
        parser.error(f"Build the application JAR first: {args.jar}")
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    host_log = output / "spring.log"
    with TemporaryDirectory(prefix="cinema-browser-") as folder, host_log.open("wb") as log:
        database = Path(folder) / "browser-acceptance.sqlite"
        env = {**os.environ, "PORT": "0", "CINEMA_SQLITE_PATH": str(database),
               "CINEMA_DEMO_DATA": "true", "CINEMA_SECURE_COOKIES": "false",
               "CINEMA_ADMIN_PASSWORD": PASSWORD, "CINEMA_STAFF_PASSWORD": PASSWORD,
               "TURSO_DATABASE_URL": "", "TURSO_AUTH_TOKEN": ""}
        java = "java"
        if os.name == "nt":
            # Oracle's javapath launcher spawns a second process on Windows.
            details = subprocess.run([java, "-XshowSettings:properties", "-version"],
                                     capture_output=True, text=True, check=True)
            home = re.search(r"^\s*java.home = (.+)$", details.stderr, re.MULTILINE)[1]
            java = str(Path(home) / "bin/java.exe")
        process = subprocess.Popen([java, "-jar", str(args.jar.resolve())], cwd=folder,
                                   env=env, stdout=log, stderr=subprocess.STDOUT)
        try:
            start = monotonic()
            base = host_url(process, host_log)
            startup = round(monotonic() - start, 2)
            with sync_playwright() as playwright:
                browser = playwright.chromium.launch(channel=args.channel, args=["--no-proxy-server"])
                result = exercise(browser, base, database, output)
                browser.close()
            report = json.dumps({"startup_seconds": startup, **result}, indent=2)
            (output / "results.json").write_text(report + "\n", encoding="utf-8")
            write_evidence(output)
            print(report)
        finally:
            process.terminate()
            process.wait(timeout=20)


if __name__ == "__main__":
    main()
