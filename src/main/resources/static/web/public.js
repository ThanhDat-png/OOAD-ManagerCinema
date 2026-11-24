const booking = document.querySelector('[data-seat-booking]');

if (booking) {
    const seats = Array.from(booking.querySelectorAll('input[name="seats"]:enabled'));
    const selectedSeats = booking.querySelector('[data-selected-seats]');
    const selectedTotal = booking.querySelector('[data-selected-total]');
    const submit = booking.querySelector('button[type="submit"]');
    const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' });

    const updateSummary = () => {
        const selected = seats.filter(seat => seat.checked);
        const emptyMessage = seats.length ? 'Chưa chọn ghế' : 'Không còn ghế trống';
        selectedSeats.textContent = selected.length ? selected.map(seat => seat.dataset.name).join(', ') : emptyMessage;
        const total = selected.reduce((amount, seat) => amount + Number(seat.dataset.price), 0);
        selectedTotal.textContent = selected.length ? money.format(total) : '—';
        submit.disabled = selected.length === 0;
    };

    booking.addEventListener('change', updateSummary);
    window.addEventListener('pageshow', updateSummary);
    updateSummary();
}

const copy = document.querySelector('[data-copy-reference]');

if (copy && navigator.clipboard) {
    copy.hidden = false;
    copy.addEventListener('click', async () => {
        const reference = document.querySelector('[data-reference]');
        const status = document.querySelector('[data-copy-status]');
        try {
            await navigator.clipboard.writeText(reference.textContent.trim());
            status.textContent = 'Đã sao chép mã.';
        } catch {
            status.textContent = 'Vui lòng chọn và sao chép mã trên màn hình.';
        }
    });
}
