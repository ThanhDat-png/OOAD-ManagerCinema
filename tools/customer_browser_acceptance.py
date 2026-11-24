"""Exercise the Vietnamese customer site and counter handoff in a real browser.

Build the application JAR first. Run with a Python environment that has
Playwright installed. Microsoft Edge is the default browser channel.
"""

import argparse
from contextlib import closing
import json
import os
from pathlib import Path
import re
import sqlite3
import subprocess
from tempfile import TemporaryDirectory

from playwright.sync_api import expect, sync_playwright

from browser_acceptance import ROOT, PASSWORD, WIDTHS, check_layout, host_url, login, prepare_show


def inspect_page(page, output, label):
    for width, height in WIDTHS.items():
        page.set_viewport_size({"width": width, "height": height})
        check_layout(page)
        expect(page.get_by_role("navigation").first).to_be_visible()
        page.screenshot(path=output / f"{label}-{width}.png", animations="disabled")
    broken = page.locator("img").evaluate_all(
        "(images) => images.filter(img => img.complete && !img.naturalWidth).map(img => img.src)"
    )
    assert not broken, f"Broken images at {page.url}: {broken}"


def button(page, label):
    return page.get_by_role("button", name=label, exact=True)


def purchase_counts(database):
    with closing(sqlite3.connect(database)) as connection:
        return {
            table: connection.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
            for table in ("GIAODICH", "THANHTOAN", "VE")
        }


def verify_invalid_selection(browser, base, output, show):
    page = browser.new_page(base_url=base, java_script_enabled=False,
                            viewport={"width": 375, "height": 812})
    try:
        assert page.goto(f"/cinema/showtimes/{show}").ok
        with page.expect_navigation() as navigation:
            button(page, "Lấy mã đặt vé").click()
        assert navigation.value.status == 400
        expect(page.get_by_role("alert")).to_contain_text("Vui lòng chọn")
        page.screenshot(path=output / "seats-validation-375.png")
    finally:
        page.close()


def public_selection(page, database, output, show, day):
    response = page.goto("/cinema")
    assert response and response.ok
    inspect_page(page, output, "home")
    page.set_viewport_size({"width": 375, "height": 812})
    page.reload()
    page.keyboard.press("Tab")
    skip = page.get_by_role("link", name=re.compile("Chuyển đến nội dung", re.I))
    expect(skip).to_be_focused()
    expect(skip).to_be_in_viewport()
    assert skip.evaluate("(link) => getComputedStyle(link).outlineStyle") != "none"
    page.keyboard.press("Enter")
    expect(page.get_by_role("main")).to_be_focused()

    page.locator('a[href="/cinema/films/PH001"]').first.click()
    expect(page.get_by_role("heading", level=1)).to_contain_text("Mắt biếc")
    another_day = page.locator(".date-choice:not([aria-current])").first
    another_day.click()
    expect(page.locator(".date-choice[aria-current=date]")).to_have_count(1)
    expect(page.locator(f'a[href="/cinema/showtimes/{show}"]')).to_have_count(0)
    page.locator(f'.date-choice[href$="date={day}"]').click()
    inspect_page(page, output, "film")
    page.locator(f'a[href="/cinema/showtimes/{show}"]').first.click()
    inspect_page(page, output, "seats")
    page.set_viewport_size({"width": 375, "height": 812})
    form = page.locator(f'form[action="/cinema/showtimes/{show}"]')
    expect(form.locator('input[name="_csrf"]')).to_have_count(1)
    expect(form.locator('input[name="seats"]:disabled').first).to_be_disabled()
    submit = button(form, "Lấy mã đặt vé")
    expect(submit).to_be_disabled()
    seat = form.get_by_role("checkbox", name=re.compile(r"^Ghế A1,"))
    seat_id = seat.get_attribute("value")
    assert seat_id
    seat.focus()
    expect(seat).to_be_focused()
    page.keyboard.press("Space")
    expect(seat).to_be_checked()
    expect(form.locator("[data-selected-seats]")).to_have_text("A1")
    expect(form.locator("[data-selected-total]")).to_contain_text("₫")
    page.screenshot(path=output / "seats-selected-375.png", animations="disabled")
    submit.click()
    expect(page).to_have_url(re.compile(r"/cinema/selection/[^/?]+$"))
    reference = page.url.rsplit("/", 1)[-1]
    expect(page.get_by_text(reference, exact=True).first).to_be_visible()
    assert purchase_counts(database) == {"GIAODICH": 0, "THANHTOAN": 0, "VE": 0}
    expect(page.locator('img[alt*="QR"], img[src*="/qr"]')).to_have_count(0)
    inspect_page(page, output, "selection")
    copy = button(page, "Sao chép mã")
    copy.focus()
    page.keyboard.press("Enter")
    expect(page.locator("[data-copy-status]")).to_have_text("Đã sao chép mã.")
    assert page.evaluate("navigator.clipboard.readText()") == reference
    page.reload()
    expect(page.get_by_text(reference, exact=True).first).to_be_visible()
    return reference, seat_id


def claim(page, reference):
    form = page.locator('form[action="/staff/sales/customer-selection"]',
                        has=page.locator('input[name="reference"]:not([type="hidden"])'))
    details = form.locator("xpath=ancestor::details")
    if details.count() and details.get_attribute("open") is None:
        details.locator("summary").click()
    field = form.locator('input[name="reference"]')
    field.fill(reference)
    form.get_by_role("button").click()


def counter_handoff(staff, retry, database, page, output, show, room, day, reference, seat_id):
    handoff = staff.locator("details", has=staff.locator('form[action="/staff/sales/customer-selection"]'))
    expect(handoff).to_have_count(1)
    staff.set_viewport_size({"width": 1440, "height": 960})
    field = handoff.get_by_role("textbox", name="Mã chọn ghế")
    expect(field).to_have_value("Bản nháp")
    refresh = button(handoff, "Cập nhật danh sách")
    refresh.click()
    expect(handoff.locator("[data-incoming-status]")).to_have_text("Đã cập nhật danh sách.")
    incoming = handoff.get_by_role("list", name="Yêu cầu đang chờ tại quầy")
    request = incoming.get_by_role("listitem").filter(has_text=reference)
    expect(request.get_by_text(reference, exact=True)).to_be_visible()
    expect(request).to_contain_text("Mắt biếc")
    expect(request).to_contain_text("A1")
    expect(field).to_have_value("Bản nháp")
    field.clear()
    staff.screenshot(path=output / "counter-incoming.png", animations="disabled")
    button(request, "Nhận yêu cầu").click()
    expect(staff).to_have_url(re.compile(rf"/staff/sales\?show={show}(?:&|$)"))
    expect(staff.locator(f'input[name="seats"][value="{seat_id}"]')).to_be_checked()
    assert purchase_counts(database) == {"GIAODICH": 1, "THANHTOAN": 0, "VE": 0}
    staff.screenshot(path=output / "counter-claim.png", animations="disabled")

    claim(retry, reference)
    expect(retry.get_by_role("alert")).to_contain_text("nhân viên khác tiếp nhận")
    assert purchase_counts(database) == {"GIAODICH": 1, "THANHTOAN": 0, "VE": 0}
    retry.get_by_role("button", name="Đăng xuất").click()
    login(retry, "staff")
    claim(retry, reference)
    expect(retry.locator(f'input[name="seats"][value="{seat_id}"]')).to_be_checked()
    assert purchase_counts(database) == {"GIAODICH": 1, "THANHTOAN": 0, "VE": 0}
    page.reload()
    expect(page.get_by_role("heading", level=1)).to_contain_text("Quầy đã nhận")
    page.goto(f"/cinema/showtimes/{show}")
    expect(page.locator(f'input[name="seats"][value="{seat_id}"]')).to_be_disabled()

    button(staff, "Thanh toán").click()
    staff.get_by_label("Phương thức thanh toán").select_option("Tiền mặt")
    staff.get_by_label("Đã xác nhận nhận đủ tiền tại quầy").check()
    button(staff, "Hoàn tất giao dịch").click()
    expect(staff.get_by_role("heading", name="Vé đã phát hành")).to_be_visible()
    expect(staff.locator(".st-ticket")).to_have_count(1)
    ticket = staff.locator(".st-ticket small").inner_text()
    qr = staff.request.get(staff.locator(".st-ticket img").get_attribute("src"))
    assert qr.ok and qr.headers["content-type"] == "image/png"
    assert purchase_counts(database) == {"GIAODICH": 1, "THANHTOAN": 1, "VE": 1}
    staff.screenshot(path=output / "counter-ticket.png", animations="disabled")

    staff.goto("/staff/admission")
    staff.get_by_label("Ngày chiếu").fill(day)
    button(staff, "Tải suất chiếu").click()
    staff.get_by_label("Phòng đang kiểm soát").select_option(room)
    staff.get_by_label("Suất chiếu đang kiểm soát").select_option(show)
    staff.get_by_text("Quét mã QR từ ảnh", exact=True).click()
    staff.get_by_label("Ảnh mã QR").set_input_files(
        {"name": "ticket.png", "mimeType": "image/png", "buffer": qr.body()}
    )
    button(staff, "Đọc mã QR").click()
    expect(staff.get_by_text(ticket, exact=True)).to_be_visible()
    button(staff, "Kiểm tra tính hợp lệ").click()
    expect(staff.get_by_role("status")).to_have_text("Vé hợp lệ")
    button(staff, "Xác nhận").click()
    expect(staff.get_by_role("status")).to_have_text("Xác nhận vé thành công")
    button(staff, "Kiểm tra tính hợp lệ").click()
    expect(staff.get_by_role("alert")).to_have_text("Vé đã được sử dụng")
    staff.screenshot(path=output / "counter-admission.png", animations="disabled")


def exercise(browser, base, database, output):
    errors = []
    page, staff, retry = (
        browser.new_page(base_url=base, viewport={"width": 1440, "height": 960})
        for _ in range(3)
    )
    for tab in (page, staff, retry):
        tab.on("pageerror", lambda error: errors.append(str(error)))
        tab.on("response", lambda response: errors.append(f"{response.status} {response.url}")
               if response.status >= 500 else None)
    show, room, day = prepare_show(database)
    with closing(sqlite3.connect(database)) as connection, connection:
        connection.execute("UPDATE TAIKHOAN SET TrangThai='Đang hoạt động' WHERE TenDangNhap='locked_demo'")
    page.context.grant_permissions(["clipboard-read", "clipboard-write"])
    page.goto("/staff/sales")
    expect(page).to_have_url(re.compile(r"/login$"))
    for counter, username in ((staff, "staff"), (retry, "locked")):
        login(counter, username)
        expect(counter).to_have_url(re.compile(r"/staff/sales$"))
    handoff = staff.locator(".customer-claim")
    handoff.locator("summary").click()
    expect(handoff.locator("[data-incoming-list]")).to_contain_text("Chưa có yêu cầu")
    handoff.get_by_role("textbox", name="Mã chọn ghế").fill("Bản nháp")
    verify_invalid_selection(browser, base, output, show)
    reference, seat_id = public_selection(page, database, output, show, day)
    counter_handoff(staff, retry, database, page, output, show, room, day, reference, seat_id)
    assert not errors, errors
    report = {
        "status": "passed",
        "viewports": [f"{width}x{height}" for width, height in WIDTHS.items()],
        "checks": [
            "Public navigation, Vietnamese headings, keyboard skip link and seat selection",
            "Empty selection rejected and valid selection creates a reference without payment or tickets",
            "Counter refresh finds the request and preserves input, then claim is applied once",
            "Staff takes payment and issues exactly one ticket",
            "QR upload, admission confirmation and duplicate scan rejection",
        ],
    }
    (output / "results.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", type=Path, default=ROOT / "target/manager-cinema-1.0-SNAPSHOT.jar")
    parser.add_argument("--output", type=Path, default=ROOT / "target/customer-web-qa")
    parser.add_argument("--channel", default="msedge")
    args = parser.parse_args()
    if not args.jar.is_file():
        parser.error(f"Build the application JAR first: {args.jar}")
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    host_log = output / "spring.log"
    with TemporaryDirectory(prefix="cinema-customer-browser-") as folder, host_log.open("wb") as log:
        database = Path(folder) / "browser.sqlite"
        env = {**os.environ, "PORT": "0", "CINEMA_SQLITE_PATH": str(database),
               "CINEMA_DEMO_DATA": "true", "CINEMA_SECURE_COOKIES": "false",
               "CINEMA_ADMIN_PASSWORD": PASSWORD, "CINEMA_STAFF_PASSWORD": PASSWORD,
               "TURSO_DATABASE_URL": "", "TURSO_AUTH_TOKEN": ""}
        java = "java"
        if os.name == "nt":
            details = subprocess.run([java, "-XshowSettings:properties", "-version"],
                                     capture_output=True, text=True, check=True)
            home = re.search(r"^\s*java.home = (.+)$", details.stderr, re.MULTILINE)[1]
            java = str(Path(home) / "bin/java.exe")
        process = subprocess.Popen([java, "-jar", str(args.jar.resolve())], cwd=folder,
                                   env=env, stdout=log, stderr=subprocess.STDOUT)
        try:
            base = host_url(process, host_log)
            with sync_playwright() as playwright:
                browser = playwright.chromium.launch(channel=args.channel, args=["--no-proxy-server"])
                exercise(browser, base, database, output)
                browser.close()
        finally:
            process.terminate()
            process.wait(timeout=20)


if __name__ == "__main__":
    main()
