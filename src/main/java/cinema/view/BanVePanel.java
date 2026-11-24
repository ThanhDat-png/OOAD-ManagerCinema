package cinema.view;

import cinema.view.ui.*;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.sales.*;
import cinema.dao.*;
import cinema.model.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterJob;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;

/** UC08: all database work runs off the event dispatch thread. */
public final class BanVePanel extends JPanel {
    static { Theme.install(); }

    private static final String[] STEP_LABELS = {
            "01 Suất chiếu", "02 Chọn ghế", "03 Thanh toán", "04 Phát hành"
    };
    private static final Locale VI_VN = Locale.forLanguageTag("vi-VN");

    private final BanVeBUS service;
    private final JTextField film = new JTextField(14), date = new JTextField(9), time = new JTextField(5);
    private final JComboBox<BanVeDAO.SuatBan> shows = new JComboBox<>();
    private final JPanel seats = new JPanel();
    private final JTextArea cartText = new JTextArea(), historyText = new JTextArea();
    private final SalesViews.Details summaryPanel = new SalesViews.Details(cartText, "Tóm tắt giao dịch");
    private final JLabel message = new JLabel(" "), identity = new JLabel();
    private final JLabel showBadge = Design.text("Chưa chọn suất chiếu", "subtitle");
    private final JLabel nextStepEyebrow = Design.text("BƯỚC 1/3 · CHỌN SUẤT CHIẾU", "eyebrow");
    private final JLabel nextStepBody = new Design.WrappingLabel("Chọn suất chiếu ở bảng bên trái rồi bấm «Chọn suất» (hoặc nhấp đúp).");
    private final JTextField code = new JTextField(22), from = new JTextField(10), to = new JTextField(10);
    private final DefaultListModel<GiaoDichDAO.Dong> historyModel = new DefaultListModel<>();
    private final JList<GiaoDichDAO.Dong> history = new JList<>(historyModel);
    private final JTabbedPane tabs = new JTabbedPane();
    private final Set<Integer> selected = new HashSet<>();
    private final Map<Integer, String> seatLabels = new LinkedHashMap<>();
    private final Map<Integer, Ghe> seatMeta = new LinkedHashMap<>();
    private Map<Integer, BigDecimal> seatPrices = Map.of();
    private final javax.swing.Timer refresh;
    private final JPanel stepsBar = buildStepsBar();
    private final JButton chooseBtn = new JButton("Chọn suất");
    private final JButton holdBtn = new JButton("Giữ ghế / đổi ghế");
    private final JButton quoteBtn = new JButton("Tính tiền");
    private final JButton payBtn = new JButton("Thanh toán / tiếp tục");
    private final JButton issueBtn = new JButton("Phát hành / in lại");
    private final JButton newTxBtn = new JButton("Giao dịch mới");
    private BanVeDAO.GioHang cart, inspected;
    private String id = BanVeBUS.newId();
    private Integer showId;
    private String activeShowSummary = "";
    private String lastSeatSignature = "";
    private boolean busy, silentBusy, paymentOpen, needsCheck, historyLoaded;
    private final Map<Component, Boolean> enabled = new IdentityHashMap<>();

    public BanVePanel(PhienDangNhap session) {
        this(session, new BanVeBUS(session, new BanVeDAO()));
    }

    public BanVePanel(PhienDangNhap session, BanVeBUS customService) {
        super(new BorderLayout(8, 6));
        service = Objects.requireNonNull(customService);
        service.employeeId();
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        seats.setLayout(new BoxLayout(seats, BoxLayout.Y_AXIS));
        seats.setOpaque(false);

        JPanel selling = new JPanel(new BorderLayout(Theme.SM, Theme.SM));
        selling.putClientProperty("cinema.background", "canvas");

        JPanel top = new JPanel(new BorderLayout(Theme.SM, Theme.XS));
        top.putClientProperty("cinema.background", "canvas");

        JPanel idBadge = new JPanel(new BorderLayout(Theme.SM, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight(), arc = Theme.s(10);
                g2.setColor(Tokens.raised());
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.setColor(Tokens.border());
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.dispose();
            }
        };
        idBadge.setOpaque(false);
        idBadge.setBorder(BorderFactory.createEmptyBorder(Theme.s(5), Theme.s(Theme.MD), Theme.s(5), Theme.s(Theme.MD)));
        JLabel idIcon = new JLabel(new Icons("ticket", 16));
        idIcon.putClientProperty("cinema.statusRefresh", (Runnable) () -> idIcon.setForeground(Tokens.accent()));
        identity.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
            identity.setFont(Theme.number(13));
            identity.setForeground(Tokens.text());
        });
        idBadge.add(idIcon, BorderLayout.WEST);
        idBadge.add(identity, BorderLayout.CENTER);
        idBadge.putClientProperty("cinema.statusRefresh", (Runnable) () -> idBadge.setOpaque(false));

        JPanel headerRow = new JPanel(new BorderLayout(Theme.SM, 0));
        headerRow.setOpaque(false);
        headerRow.putClientProperty("cinema.statusRefresh", (Runnable) () -> headerRow.setOpaque(false));
        headerRow.add(idBadge, BorderLayout.CENTER);
        JButton claimBtn = new JButton("Nhận mã khách");
        claimBtn.addActionListener(event -> incomingDialog());
        headerRow.add(claimBtn, BorderLayout.EAST);

        top.add(headerRow, BorderLayout.NORTH);
        top.add(stepsBar, BorderLayout.SOUTH);
        selling.add(top, BorderLayout.NORTH);

        JPanel filters = new JPanel(new Design.Wrap());
        filters.setOpaque(false);
        filters.add(SalesViews.field("Tên phim", film));
        filters.add(SalesViews.field("Ngày chiếu", date));
        filters.add(SalesViews.field("Giờ chiếu", time));
        button(filters, "Tìm suất", this::search);

        JPanel schedule = new JPanel(new BorderLayout(Theme.SM, Theme.SM));
        schedule.setOpaque(false);
        schedule.add(new SalesViews.Shows(shows, this::choose), BorderLayout.CENTER);
        JPanel chooseAction = new JPanel(new BorderLayout());
        chooseAction.setOpaque(false);
        chooseBtn.addActionListener(event -> choose());
        chooseAction.add(chooseBtn, BorderLayout.NORTH);
        schedule.add(chooseAction, BorderLayout.EAST);

        Design.Card searchCard = new Design.Card(new BorderLayout(Theme.SM, Theme.XS));
        searchCard.setBorder(BorderFactory.createEmptyBorder(Theme.s(10), Theme.s(Theme.MD), Theme.s(10), Theme.s(Theme.MD)));
        searchCard.add(filters, BorderLayout.NORTH);
        searchCard.add(schedule, BorderLayout.CENTER);

        Design.Card seatCard = new Design.Card(new BorderLayout(Theme.SM, Theme.XS));
        seatCard.setBorder(BorderFactory.createEmptyBorder(Theme.s(8), Theme.s(Theme.MD), Theme.s(8), Theme.s(Theme.MD)));
        seatCard.setPreferredSize(new Dimension(0, Theme.s(220)));
        seatCard.setMinimumSize(new Dimension(0, Theme.s(180)));
        JPanel seatHead = new JPanel(new BorderLayout(Theme.SM, 0));
        seatHead.setOpaque(false);
        seatHead.putClientProperty("cinema.statusRefresh", (Runnable) () -> seatHead.setOpaque(false));
        JPanel seatTitleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.s(8), 0));
        seatTitleBox.setOpaque(false);
        seatTitleBox.putClientProperty("cinema.statusRefresh", (Runnable) () -> seatTitleBox.setOpaque(false));
        seatTitleBox.add(Design.text("Sơ đồ ghế", "heading"));
        seatTitleBox.add(showBadge);
        seatHead.add(seatTitleBox, BorderLayout.CENTER);
        JButton reloadBtn = new JButton("Cập nhật ghế");
        reloadBtn.addActionListener(event -> reload());
        seatHead.add(reloadBtn, BorderLayout.EAST);
        seatCard.add(seatHead, BorderLayout.NORTH);

        JScrollPane seatScroll = new JScrollPane(seats);
        seatScroll.setOpaque(false);
        seatScroll.getViewport().setOpaque(false);
        seatCard.add(seatScroll, BorderLayout.CENTER);
        renderEmptySeatPrompt();

        JPanel main = new JPanel(new BorderLayout(Theme.SM, Theme.SM));
        main.putClientProperty("cinema.background", "canvas");
        main.add(searchCard, BorderLayout.NORTH);
        main.add(seatCard, BorderLayout.CENTER);

        JPanel nextStepCard = new JPanel(new BorderLayout(Theme.SM, Theme.XS)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight(), arc = Theme.s(12);
                g2.setColor(Tokens.tone(Tokens.accent()));
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.setColor(Tokens.mix(Tokens.surface(), Tokens.accent(), Tokens.dark ? .55 : .40));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.dispose();
            }
        };
        nextStepCard.setOpaque(false);
        nextStepCard.setBorder(BorderFactory.createEmptyBorder(Theme.s(6), Theme.s(10), Theme.s(6), Theme.s(10)));
        nextStepCard.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
            nextStepCard.setOpaque(false);
            nextStepEyebrow.setForeground(Tokens.accent());
            nextStepEyebrow.setFont(Theme.bold(11));
            nextStepBody.setForeground(Tokens.text());
        });
        nextStepEyebrow.setForeground(Tokens.accent());
        nextStepBody.setPreferredSize(new Dimension(Theme.s(360), Theme.s(34)));
        nextStepBody.setMinimumSize(new Dimension(Theme.s(220), Theme.s(32)));
        JPanel nextText = new JPanel(new BorderLayout(0, Theme.s(2)));
        nextText.setOpaque(false);
        nextText.putClientProperty("cinema.statusRefresh", (Runnable) () -> nextText.setOpaque(false));
        nextText.add(nextStepEyebrow, BorderLayout.NORTH);
        nextText.add(nextStepBody, BorderLayout.CENTER);
        JLabel nextIcon = new JLabel(new Icons("arrow", 18));
        nextIcon.putClientProperty("cinema.statusRefresh", (Runnable) () -> nextIcon.setForeground(Tokens.accent()));
        nextStepCard.add(nextIcon, BorderLayout.WEST);
        nextStepCard.add(nextText, BorderLayout.CENTER);

        cartText.setEditable(false);
        cartText.setLineWrap(true);
        cartText.setWrapStyleWord(true);

        Design.Card actionsCard = new Design.Card(new BorderLayout(Theme.SM, Theme.XS));
        actionsCard.setBorder(BorderFactory.createEmptyBorder(Theme.s(8), Theme.s(10), Theme.s(8), Theme.s(10)));
        holdBtn.addActionListener(event -> hold());
        quoteBtn.addActionListener(event -> {
            if (savedSelection()) run(() -> service.quote(id), this::display);
        });
        quoteBtn.setVisible(false);
        payBtn.addActionListener(event -> payment());
        issueBtn.addActionListener(event -> run(() -> service.issue(id), tickets -> preview(tickets, false)));
        for (JButton b : List.of(payBtn, holdBtn, issueBtn)) {
            b.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
                b.setFont(Theme.bold(13));
                b.setMargin(new Insets(6, 10, 6, 10));
            });
        }

        JPanel secondaryRow = new JPanel(new GridLayout(1, 2, Theme.s(6), 0));
        secondaryRow.setOpaque(false);
        secondaryRow.putClientProperty("cinema.statusRefresh", (Runnable) () -> secondaryRow.setOpaque(false));
        secondaryRow.add(holdBtn);
        secondaryRow.add(issueBtn);

        JPanel mainActions = new JPanel(new BorderLayout(0, Theme.s(6)));
        mainActions.setOpaque(false);
        mainActions.putClientProperty("cinema.statusRefresh", (Runnable) () -> mainActions.setOpaque(false));
        mainActions.add(payBtn, BorderLayout.NORTH);
        mainActions.add(secondaryRow, BorderLayout.CENTER);
        mainActions.add(quoteBtn, BorderLayout.SOUTH);

        JPanel utilityRow = new JPanel(new GridBagLayout());
        utilityRow.setOpaque(false);
        utilityRow.putClientProperty("cinema.statusRefresh", (Runnable) () -> utilityRow.setOpaque(false));
        JButton recoverBtn = new JButton("Kiểm tra mã hiện tại");
        recoverBtn.addActionListener(event -> recover());
        JButton cancelBtn = new JButton("Hủy giao dịch");
        cancelBtn.putClientProperty("cinema.role", "danger");
        cancelBtn.addActionListener(event -> cancel());
        newTxBtn.addActionListener(event -> newTransaction());
        for (JButton b : List.of(recoverBtn, cancelBtn, newTxBtn)) {
            b.putClientProperty("cinema.statusRefresh", (Runnable) () -> {
                b.setFont(Theme.bold(12));
                b.setMargin(new Insets(5, 6, 5, 6));
            });
        }
        GridBagConstraints ug = new GridBagConstraints();
        ug.gridy = 0;
        ug.fill = GridBagConstraints.HORIZONTAL;
        ug.gridx = 0; ug.weightx = 0.44; ug.insets = new Insets(0, 0, 0, Theme.s(6));
        utilityRow.add(recoverBtn, ug);
        ug.gridx = 1; ug.weightx = 0.28; ug.insets = new Insets(0, 0, 0, Theme.s(6));
        utilityRow.add(cancelBtn, ug);
        ug.gridx = 2; ug.weightx = 0.28; ug.insets = new Insets(0, 0, 0, 0);
        utilityRow.add(newTxBtn, ug);

        actionsCard.add(mainActions, BorderLayout.NORTH);
        actionsCard.add(utilityRow, BorderLayout.SOUTH);

        JPanel rightRail = new JPanel(new BorderLayout(0, Theme.XS));
        rightRail.putClientProperty("cinema.background", "canvas");
        rightRail.setPreferredSize(new Dimension(Theme.s(452), Theme.s(360)));
        rightRail.setMinimumSize(new Dimension(Theme.s(395), Theme.s(300)));
        rightRail.add(nextStepCard, BorderLayout.NORTH);
        rightRail.add(summaryPanel, BorderLayout.CENTER);
        rightRail.add(actionsCard, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, main, rightRail);
        split.setResizeWeight(.56);
        split.setDividerLocation(.56);
        selling.add(split, BorderLayout.CENTER);

        for (JTextField field : List.of(film, date, time)) {
            field.addActionListener(event -> { if (!busy) search(); });
        }
        DateInputs.attach(date, false, true);
        DateInputs.attach(time, true, true);
        DateInputs.attach(from, false, true);
        DateInputs.attach(to, false, true);

        tabs.addTab("Bán vé", selling);
        tabs.addTab("Kiểm tra giao dịch", historyPanel());
        tabs.addChangeListener(event -> {
            if (tabs.getSelectedIndex() == 1) {
                SwingUtilities.invokeLater(() -> {
                    if (tabs.getSelectedIndex() == 1 && !busy && !historyLoaded
                            && code.getText().isBlank() && from.getText().isBlank() && to.getText().isBlank()) {
                        loadRecentHistory();
                    }
                });
            }
        });
        add(tabs, BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);

        refresh = new javax.swing.Timer(5000, event -> {
            if (!busy && !silentBusy && !paymentOpen && tabs.getSelectedIndex() == 0 && showId != null) silentRefresh();
        });
        identity();
        updateWorkflowGuidance();
        search();
    }

    private JPanel buildStepsBar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.X_AXIS));
        bar.setOpaque(false);
        bar.putClientProperty("cinema.statusRefresh", (Runnable) () -> bar.setOpaque(false));
        for (int i = 0; i < STEP_LABELS.length; i++) {
            final int stepIndex = i;
            JLabel pill = new JLabel(STEP_LABELS[i]) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int active = activeStep();
                    boolean isCurrent = stepIndex == active;
                    boolean isDone = stepIndex < active;
                    int w = getWidth(), h = getHeight();
                    Color fill = isCurrent ? Tokens.tone(Tokens.accent())
                            : isDone ? Tokens.mix(Tokens.surface(), Tokens.success(), .12) : Tokens.raised();
                    Color stroke = isCurrent ? Tokens.accent()
                            : isDone ? Tokens.mix(Tokens.surface(), Tokens.success(), .45) : Tokens.border();
                    g2.setColor(fill);
                    g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
                    g2.setColor(stroke);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            pill.setOpaque(false);
            pill.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            pill.putClientProperty("cinema.statusRefresh", (Runnable) () -> applyStepStyle(pill, stepIndex));
            applyStepStyle(pill, stepIndex);
            bar.add(pill);
            if (i < STEP_LABELS.length - 1) {
                bar.add(Box.createHorizontalStrut(6));
            }
        }
        bar.add(Box.createHorizontalGlue());
        return bar;
    }

    private void applyStepStyle(JLabel pill, int stepIndex) {
        int active = activeStep();
        pill.setFont(stepIndex == active ? Theme.bold(11) : Theme.font(11));
        pill.setForeground(stepIndex == active ? Tokens.accent()
                : stepIndex < active ? Tokens.success() : Tokens.muted());
    }

    private void refreshStepsBar() {
        int idx = 0;
        for (Component c : stepsBar.getComponents()) {
            if (c instanceof JLabel pill) {
                applyStepStyle(pill, idx++);
            }
        }
        stepsBar.revalidate();
        stepsBar.repaint();
        updateWorkflowGuidance();
    }

    private boolean selectionMatchesCart() {
        if (cart == null) return false;
        Set<Integer> held = new HashSet<>();
        cart.ghe().forEach(g -> held.add(g.ghe().maGhe()));
        return held.equals(selected);
    }

    private boolean isQuoted() {
        return cart != null && (cart.giaoDich().tongTien() != null && cart.giaoDich().tongTien().signum() > 0
                || cart.ghe().stream().anyMatch(g -> g.giaVeChot() != null));
    }

    private String selectedSeatText() {
        if (selected.isEmpty()) return "";
        List<String> names = new ArrayList<>();
        for (var entry : seatLabels.entrySet()) {
            if (selected.contains(entry.getKey())) names.add(entry.getValue());
        }
        if (names.isEmpty() && cart != null) {
            cart.ghe().forEach(g -> {
                if (selected.contains(g.ghe().maGhe())) names.add(g.ghe().hangGhe() + g.ghe().soGhe());
            });
        }
        return String.join(", ", names);
    }

    private void setPrimaryButton(JButton active) {
        for (JButton btn : List.of(chooseBtn, holdBtn, quoteBtn, payBtn, issueBtn, newTxBtn)) {
            boolean isPrimary = btn == active;
            btn.putClientProperty("cinema.role", isPrimary ? "primary" : "secondary");
            btn.setBackground(isPrimary ? Tokens.accent() : Tokens.raised());
            btn.setForeground(isPrimary ? Tokens.accentInk() : Tokens.text());
            btn.repaint();
        }
    }

    private static String formatVnd(BigDecimal amount) {
        if (amount == null) return "0 VND";
        NumberFormat fmt = NumberFormat.getIntegerInstance(VI_VN);
        return fmt.format(amount) + " VND";
    }

    private BigDecimal liveSelectedTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (int seatId : selected) {
            Ghe g = seatMeta.get(seatId);
            if (g != null) {
                BigDecimal unit = seatPrices.get(g.loaiGhe().maLoaiGhe());
                if (unit != null) sum = sum.add(unit);
            }
        }
        return sum;
    }

    private void updateLiveSummary() {
        if (cart != null && (!"Chờ thanh toán".equals(cart.giaoDich().trangThai()) || selectionMatchesCart())) {
            summaryPanel.clearDraftPreview();
            return;
        }
        if (selected.isEmpty()) {
            summaryPanel.clearDraftPreview();
            return;
        }
        List<String[]> lines = new ArrayList<>();
        lines.add(new String[]{"Giao dịch", id});
        if (!activeShowSummary.isBlank()) {
            lines.add(new String[]{"Suất chiếu", activeShowSummary});
        }
        BigDecimal total = BigDecimal.ZERO;
        boolean allPriced = true;
        for (var entry : seatMeta.entrySet()) {
            if (!selected.contains(entry.getKey())) continue;
            Ghe g = entry.getValue();
            BigDecimal unit = seatPrices.get(g.loaiGhe().maLoaiGhe());
            String priceText;
            if (unit != null) {
                total = total.add(unit);
                priceText = formatVnd(unit);
            } else {
                allPriced = false;
                priceText = "Chưa có giá";
            }
            lines.add(new String[]{"Ghế", g.hangGhe() + g.soGhe() + " · " + g.loaiGhe().tenLoaiGhe() + " · " + priceText});
        }
        String totalLabel = allPriced && total.signum() > 0 ? formatVnd(total) : "Chưa có bảng giá";
        summaryPanel.setDraftPreview("Đang chọn " + selected.size() + " ghế", lines, totalLabel);
    }

    private void updateWorkflowGuidance() {
        updateLiveSummary();
        if (showId == null && cart == null) {
            nextStepEyebrow.setText("BƯỚC 1/3 · CHỌN SUẤT CHIẾU");
            nextStepBody.setText("Chọn một suất chiếu ở bảng bên trái rồi bấm «Chọn suất» (hoặc nhấp đúp).");
            setPrimaryButton(chooseBtn);
            return;
        }
        if (cart != null) {
            String status = cart.giaoDich().trangThai();
            if ("Đã thanh toán".equals(status)) {
                nextStepEyebrow.setText("HOÀN TẤT · ĐÃ PHÁT HÀNH VÉ");
                nextStepBody.setText("Giao dịch đã hoàn tất. Bấm «Giao dịch mới» để bán cho khách tiếp theo hoặc «Phát hành / in lại».");
                setPrimaryButton(newTxBtn);
                return;
            }
            if ("Đang thanh toán".equals(status)) {
                nextStepEyebrow.setText("BƯỚC 3/3 · XÁC NHẬN THANH TOÁN");
                nextStepBody.setText("Đang chờ xác nhận thu tiền. Bấm «Thanh toán / tiếp tục» để hoàn tất.");
                setPrimaryButton(payBtn);
                return;
            }
            if (!"Chờ thanh toán".equals(status)) {
                nextStepEyebrow.setText("GIAO DỊCH ĐÃ ĐÓNG");
                nextStepBody.setText("Trạng thái: " + status + ". Bấm «Giao dịch mới» để bán cho khách tiếp theo.");
                setPrimaryButton(newTxBtn);
                return;
            }
            if (!selectionMatchesCart()) {
                String seatsLabel = selectedSeatText();
                BigDecimal liveTotal = liveSelectedTotal();
                nextStepEyebrow.setText("BƯỚC 3/3 · SẴN SÀNG THANH TOÁN");
                nextStepBody.setText(selected.isEmpty()
                        ? "Bạn vừa bỏ chọn hết ghế. Hãy chọn ghế mới trên sơ đồ bên trái."
                        : "Đã đổi sang " + selected.size() + " ghế (" + seatsLabel + " · " + formatVnd(liveTotal)
                                + "). Bấm «Thanh toán / tiếp tục» để chốt và thu tiền.");
                setPrimaryButton(selected.isEmpty() ? null : payBtn);
                return;
            }
            BigDecimal displayTotal = isQuoted() ? cart.giaoDich().tongTien() : liveSelectedTotal();
            nextStepEyebrow.setText("BƯỚC 3/3 · THU TIỀN TẠI QUẦY");
            nextStepBody.setText("Tổng " + formatVnd(displayTotal)
                    + " (" + cart.ghe().size() + " ghế). Bấm «Thanh toán / tiếp tục» để thu tiền và xuất vé.");
            setPrimaryButton(payBtn);
            return;
        }
        if (selected.isEmpty()) {
            nextStepEyebrow.setText("BƯỚC 2/3 · CHỌN GHẾ TRÊN SƠ ĐỒ");
            nextStepBody.setText("Bấm vào ghế trống màu sáng ở sơ đồ bên trái — tổng tiền tự động cập nhật ngay.");
            setPrimaryButton(null);
        } else {
            String seatsLabel = selectedSeatText();
            BigDecimal liveTotal = liveSelectedTotal();
            nextStepEyebrow.setText("BƯỚC 3/3 · THANH TOÁN NGAY");
            nextStepBody.setText("Đã chọn " + selected.size() + " ghế (" + seatsLabel + " · " + formatVnd(liveTotal)
                    + "). Bấm «Thanh toán / tiếp tục» để giữ ghế và thu tiền.");
            setPrimaryButton(payBtn);
        }
    }

    private void renderEmptySeatPrompt() {
        seats.removeAll();
        seatLabels.clear();
        seatMeta.clear();
        lastSeatSignature = "";
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setOpaque(false);
        JPanel box = Design.stack(
                Design.text("Chưa mở sơ đồ ghế", "heading"),
                Design.text("Bước 1: Chọn một suất chiếu ở danh sách phía trên rồi bấm «Chọn suất» (hoặc nhấp đúp dòng suất chiếu).", "subtitle"));
        box.setOpaque(false);
        empty.add(box);
        seats.add(empty);
        seats.revalidate();
        seats.repaint();
    }

    private int activeStep() {
        if (cart != null) {
            String status = cart.giaoDich().trangThai();
            if ("Đã thanh toán".equals(status)) return 3;
            if ("Đang thanh toán".equals(status) || "Chờ thanh toán".equals(status)) return 2;
            return 3;
        }
        if (showId == null) return 0;
        return selected.isEmpty() ? 1 : 2;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (refresh != null) refresh.start();
        if (getClientProperty("cinema.page") == null) {
            putClientProperty("cinema.page", true);
            Design.page(this, "Quầy bán vé", "Suất chiếu → Ghế → Giữ ghế → Tính tiền → Thanh toán → Phát hành vé", message);
            Component north = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.NORTH);
            if (north != null) remove(north);
            setBorder(BorderFactory.createEmptyBorder(Theme.s(8), Theme.s(16), Theme.s(6), Theme.s(16)));
            updateWorkflowGuidance();
        }
    }

    @Override
    public void removeNotify() {
        refresh.stop();
        super.removeNotify();
    }

    private JPanel historyPanel() {
        JPanel result = new JPanel(new BorderLayout(Theme.MD, Theme.MD));
        result.putClientProperty("cinema.background", "canvas");

        Design.Card filterCard = new Design.Card(new BorderLayout());
        JPanel filters = new JPanel(new Design.Wrap());
        filters.setOpaque(false);
        filters.add(SalesViews.field("Mã giao dịch / vé", code));
        filters.add(SalesViews.field("Từ ngày", from));
        filters.add(SalesViews.field("Đến ngày", to));
        button(filters, "Tra cứu", () -> {
            try {
                var a = day(from);
                var b = day(to);
                String input = code.getText();
                if ((input == null || input.isBlank()) && a == null && b == null) {
                    loadRecentHistory();
                    return;
                }
                run(() -> service.transactions(input, a, b), rows -> {
                    historyLoaded = true;
                    historyModel.clear();
                    inspected = null;
                    historyText.setText("");
                    rows.forEach(historyModel::addElement);
                    message.setText(rows.isEmpty() ? "Không tìm thấy giao dịch phù hợp" : "Tìm thấy " + rows.size() + " giao dịch");
                });
            } catch (RuntimeException e) {
                error(e);
            }
        });
        filterCard.add(filters, BorderLayout.CENTER);
        result.add(filterCard, BorderLayout.NORTH);

        history.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        history.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value instanceof GiaoDichDAO.Dong row) {
                    label.setText(row.giaoDich().maGiaoDich() + " · " + row.giaoDich().trangThai() + " · " + row.hoTenNhanVien());
                }
                return label;
            }
        });
        history.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !busy && history.getSelectedValue() != null) {
                String key = history.getSelectedValue().giaoDich().maGiaoDich();
                inspected = null;
                run(() -> new Detail(service.inspect(key), service.savedTickets(key)), detail -> {
                    inspected = detail.cart();
                    historyText.setText(description(inspected) + "\nVé đã lưu:\n"
                            + detail.tickets().stream()
                            .map(v -> v.ve().maVe() + " · " + v.ve().trangThai())
                            .reduce("", (a, b) -> a + b + "\n"));
                });
            }
        });

        historyText.setEditable(false);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new SalesViews.Transactions(history),
                new SalesViews.Details(historyText, "Chi tiết giao dịch"));
        split.setResizeWeight(.45);
        result.add(split, BorderLayout.CENTER);

        JPanel actionBar = new JPanel(new BorderLayout());
        actionBar.setOpaque(false);
        actionBar.putClientProperty("cinema.statusRefresh", (Runnable) () -> actionBar.setOpaque(false));
        JPanel actions = new JPanel(new GridLayout(1, 2, Theme.SM, 0));
        actions.setOpaque(false);
        actions.putClientProperty("cinema.statusRefresh", (Runnable) () -> actions.setOpaque(false));
        JButton resumeBtn = button(actions, "Tiếp tục giao dịch đã chọn", () -> {
            if (inspected == null) {
                error(new IllegalArgumentException("Vui lòng chọn giao dịch"));
                return;
            }
            if (!inspected.giaoDich().maNV().equalsIgnoreCase(service.employeeId())) {
                error(new IllegalArgumentException("Chỉ nhân viên tạo được tiếp tục giao dịch"));
                return;
            }
            if (!id.equals(inspected.giaoDich().maGiaoDich()) && !canLeave()) return;
            display(inspected);
            showId = cart.suat() == null ? null : cart.suat().maSuat();
            selected.clear();
            cart.ghe().forEach(g -> selected.add(g.ghe().maGhe()));
            tabs.setSelectedIndex(0);
            if (showId != null) reload();
        });
        resumeBtn.putClientProperty("cinema.role", "primary");
        button(actions, "In lại vé đã lưu", () -> {
            if (inspected != null) {
                String key = inspected.giaoDich().maGiaoDich();
                run(() -> service.printableTickets(key), tickets -> preview(tickets, false));
            }
        });
        actionBar.add(actions, BorderLayout.EAST);
        result.add(actionBar, BorderLayout.SOUTH);
        return result;
    }

    private void loadRecentHistory() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(30);
        run(() -> service.transactions(null, start, end), rows -> {
            historyLoaded = true;
            historyModel.clear();
            inspected = null;
            historyText.setText("");
            rows.forEach(historyModel::addElement);
            message.setText(rows.isEmpty()
                    ? "Chưa có giao dịch trong 30 ngày gần đây"
                    : "Hiển thị " + rows.size() + " giao dịch gần đây");
        });
    }

    private record Detail(BanVeDAO.GioHang cart, List<VeDAO.ThongTin> tickets) {}

    private record MapResult(List<GheSuatChieu> seats, BanVeDAO.GioHang cart, Map<Integer, BigDecimal> prices) {}

    private record ShowLoad(List<GheSuatChieu> seats, Map<Integer, BigDecimal> prices) {}

    private void recover() {
        String key = id;
        run(() -> {
            try {
                return service.inspect(key);
            } catch (IllegalArgumentException e) {
                if ("Không tìm thấy giao dịch".equals(e.getMessage())) return null;
                throw e;
            }
        }, value -> {
            needsCheck = false;
            if (value == null) {
                cart = null;
                cartText.setText("Chưa có giao dịch được lưu với mã " + key + ". Có thể chọn/giữ ghế lại.");
                message.setText("Đã kiểm tra: chưa có giao dịch với mã hiện tại");
                refreshStepsBar();
            } else {
                display(value);
                showId = value.suat() == null ? null : value.suat().maSuat();
                selected.clear();
                value.ghe().forEach(g -> selected.add(g.ghe().maGhe()));
                if (showId != null) reload();
            }
        });
    }

    private void search() {
        try {
            String name = film.getText();
            LocalDate d = day(date);
            LocalTime t = time.getText().isBlank() ? null : LocalTime.parse(time.getText().trim());
            run(() -> service.search(name, d, t), rows -> {
                shows.removeAllItems();
                rows.forEach(shows::addItem);
                message.setText(rows.isEmpty() ? "Không tìm thấy suất chiếu phù hợp" : "Tìm thấy " + rows.size() + " suất chiếu");
            });
        } catch (RuntimeException e) {
            error(e);
        }
    }

    private void choose() {
        var choice = (BanVeDAO.SuatBan) shows.getSelectedItem();
        if (choice == null) return;
        if (showId != null && showId == choice.suat().maSuat()) {
            reload();
            return;
        }
        if (!canLeave()) return;
        if (cart != null) {
            error(new IllegalArgumentException("Hủy giao dịch cũ hoặc tạo giao dịch mới trước khi đổi suất"));
            return;
        }
        int next = choice.suat().maSuat();
        String summary = "Suất #" + next + " · " + choice.tenPhim() + " · " + choice.tenPhong()
                + " · " + choice.suat().thoiGianBatDau().toLocalTime();
        run(() -> new ShowLoad(service.selectShow(next), service.seatPrices(next)), data -> {
            showId = next;
            activeShowSummary = summary;
            showBadge.setText(summary);
            seatPrices = data.prices();
            selected.clear();
            lastSeatSignature = "";
            renderSeats(data.seats());
            refreshStepsBar();
        });
    }

    private void reload() {
        if (showId == null) return;
        int show = showId;
        String key = id;
        boolean existing = cart != null;
        run(() -> new MapResult(service.seats(show), existing ? service.inspect(key) : null, service.seatPrices(show)), data -> {
            seatPrices = data.prices();
            if (data.cart() != null) display(data.cart());
            lastSeatSignature = "";
            renderSeats(data.seats());
        });
    }

    private void silentRefresh() {
        if (showId == null || busy || silentBusy || paymentOpen) return;
        int show = showId;
        String key = id;
        boolean existing = cart != null;
        silentBusy = true;
        new SwingWorker<MapResult, Void>() {
            @Override protected MapResult doInBackground() throws Exception {
                return new MapResult(service.seats(show), existing ? service.inspect(key) : null, seatPrices);
            }
            @Override protected void done() {
                silentBusy = false;
                if (busy || paymentOpen || !Objects.equals(showId, show) || !Objects.equals(id, key)) return;
                try {
                    MapResult data = get();
                    String nextSig = seatSignature(data.seats(), data.cart());
                    if (nextSig.equals(lastSeatSignature)) return;
                    if (data.cart() != null) display(data.cart());
                    renderSeats(data.seats());
                } catch (Exception ignored) {
                    // Silent background poll never interrupts active counter entry.
                }
            }
        }.execute();
    }

    private static String seatSignature(List<GheSuatChieu> rows, BanVeDAO.GioHang currentCart) {
        StringBuilder sb = new StringBuilder();
        if (currentCart != null) {
            sb.append(currentCart.giaoDich().maGiaoDich()).append(':')
                    .append(currentCart.giaoDich().trangThai()).append(':')
                    .append(currentCart.giaoDich().tongTien()).append(';');
        }
        for (var row : rows) {
            var g = row.ghe();
            sb.append(g.maGhe()).append('|')
                    .append(g.trangThai()).append('|')
                    .append(row.trangThai()).append('|')
                    .append(row.maGiaoDich()).append(';');
        }
        return sb.toString();
    }

    private void renderSeats(List<GheSuatChieu> rows) {
        seats.removeAll();
        seatLabels.clear();
        seatMeta.clear();
        boolean editable = cart == null || "Chờ thanh toán".equals(cart.giaoDich().trangThai());
        Set<Integer> selectable = new HashSet<>();
        JComponent screen = SeatVisual.screen();
        screen.setAlignmentX(Component.LEFT_ALIGNMENT);
        seats.add(screen);
        seats.add(Box.createVerticalStrut(4));
        JPanel legend = SeatVisual.legend();
        legend.setOpaque(false);
        legend.setAlignmentX(Component.LEFT_ALIGNMENT);
        seats.add(legend);
        seats.add(Box.createVerticalStrut(6));

        Map<String, JPanel> rowsByName = new LinkedHashMap<>();
        for (var row : rows) {
            var g = row.ghe();
            String codeLabel = g.hangGhe() + g.soGhe();
            seatLabels.put(g.maGhe(), codeLabel);
            seatMeta.put(g.maGhe(), g);
            boolean own = id.equals(row.maGiaoDich());
            boolean free = "Sẵn sàng sử dụng".equals(g.trangThai())
                    && ("Trống".equals(row.trangThai()) || own && "Đang giữ".equals(row.trangThai()));
            if (free) selectable.add(g.maGhe());
            String state = !"Sẵn sàng sử dụng".equals(g.trangThai()) ? "Không khả dụng" : row.trangThai();
            JPanel line = rowsByName.computeIfAbsent(g.hangGhe(), name -> {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
                panel.setOpaque(false);
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.s(92)));
                JLabel rowLbl = Design.text("Hàng " + name, "subtitle");
                rowLbl.setPreferredSize(new Dimension(Theme.s(64), Theme.s(36)));
                panel.add(rowLbl);
                seats.add(panel);
                return panel;
            });
            JToggleButton chair = new JToggleButton(codeLabel, selected.contains(g.maGhe()));
            chair.setPreferredSize(new Dimension(82, 42));
            SeatVisual.install(chair, state, g.loaiGhe().tenLoaiGhe(), own);
            chair.putClientProperty("seatId", g.maGhe());
            chair.setEnabled(editable && free);
            BigDecimal unit = seatPrices.get(g.loaiGhe().maLoaiGhe());
            String priceHint = unit == null ? "" : " · " + formatVnd(unit);
            chair.setToolTipText(g.loaiGhe().tenLoaiGhe() + priceHint + " · " + state + (own ? " · Giao dịch này" : ""));
            chair.addActionListener(event -> {
                if (chair.isSelected()) selected.add(g.maGhe());
                else selected.remove(g.maGhe());
                refreshStepsBar();
                message.setText(selected.isEmpty()
                        ? "Hãy chọn ít nhất 1 ghế trống trên sơ đồ"
                        : "Đã chọn " + selectedSeatText() + " (" + formatVnd(liveSelectedTotal())
                                + ") — Bấm «Thanh toán / tiếp tục» để giữ ghế và thu tiền");
            });
            line.add(chair);
        }
        selected.retainAll(selectable);
        lastSeatSignature = seatSignature(rows, cart);
        seats.revalidate();
        seats.repaint();
        refreshStepsBar();
        if (selectable.isEmpty()) message.setText("Suất chiếu đã hết chỗ hoặc không còn ghế khả dụng");
    }

    private boolean savedSelection() {
        if (cart == null || "Chờ thanh toán".equals(cart.giaoDich().trangThai())) {
            Set<Integer> held = new HashSet<>();
            if (cart != null) cart.ghe().forEach(g -> held.add(g.ghe().maGhe()));
            if (cart == null || !held.equals(selected)) {
                error(new IllegalArgumentException("Vui lòng bấm Giữ ghế / đổi ghế để xác nhận lựa chọn trước"));
                return false;
            }
        }
        return true;
    }

    private void hold() {
        if (showId == null) {
            error(new IllegalArgumentException("Vui lòng chọn suất chiếu"));
            return;
        }
        String key = id;
        int show = showId;
        Set<Integer> choice = Set.copyOf(selected);
        needsCheck = true;
        run(() -> {
            service.hold(key, show, choice);
            return service.quote(key);
        }, value -> {
            display(value);
            reload();
        });
    }

    private void payment() {
        if (cart != null && List.of("Đang thanh toán", "Đã thanh toán").contains(cart.giaoDich().trangThai())) {
            run(() -> service.beginPayment(id), value -> {
                display(value);
                if ("Đã thanh toán".equals(value.giaoDich().trangThai())) {
                    run(() -> service.issue(id), tickets -> preview(tickets, false));
                    return;
                }
                paymentDialog();
            });
            return;
        }
        if (showId == null) {
            error(new IllegalArgumentException("Vui lòng chọn suất chiếu"));
            return;
        }
        if (selected.isEmpty()) {
            error(new IllegalArgumentException("Vui lòng chọn ghế còn trống trên sơ đồ trước khi thanh toán"));
            return;
        }
        String key = id;
        int show = showId;
        Set<Integer> choice = Set.copyOf(selected);
        boolean needHold = cart == null || !selectionMatchesCart();
        needsCheck = true;
        run(() -> {
            if (needHold) service.hold(key, show, choice);
            service.quote(key);
            return service.beginPayment(key);
        }, value -> {
            display(value);
            if ("Đã thanh toán".equals(value.giaoDich().trangThai())) {
                run(() -> service.issue(key), tickets -> preview(tickets, false));
                return;
            }
            paymentDialog();
        });
    }

    private void paymentDialog() {
        paymentOpen = true;
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Xác nhận thanh toán tại quầy", Dialog.ModalityType.APPLICATION_MODAL);
        JComboBox<String> method = new JComboBox<>();
        method.addItem("Chọn phương thức");
        BanVeBUS.PHUONG_THUC.forEach(method::addItem);
        JTextField amount = new JTextField(cart.giaoDich().tongTien().toPlainString());
        JCheckBox received = new JCheckBox("Đã nhận đủ tiền hoặc đã kiểm tra thanh toán thành công tại quầy");
        JButton finish = new JButton("Hoàn tất giao dịch");
        finish.putClientProperty("cinema.role", "primary");
        finish.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.s(44)));
        JLabel note = Design.text("Đóng cửa sổ sẽ tiếp tục giữ ghế. Kiểm tra giao dịch nếu kết quả chưa rõ.", "subtitle");

        JPanel content = Design.stack(
                Design.text("Xác nhận khoản đã nhận tại quầy", "heading"),
                new JLabel("Giao dịch " + id),
                Design.text(formatVnd(cart.giaoDich().tongTien()), "title"),
                method,
                new JLabel("Số tiền đã nhận / xác nhận"),
                amount,
                received,
                note,
                finish
        );
        content.setBorder(BorderFactory.createEmptyBorder(Theme.XL, Theme.XL, Theme.XL, Theme.XL));
        finish.addActionListener(event -> {
            final BigDecimal money;
            try {
                money = new BigDecimal(amount.getText().trim());
            } catch (NumberFormatException e) {
                error(new IllegalArgumentException("Số tiền không hợp lệ"));
                return;
            }
            String key = id;
            String chosen = (String) method.getSelectedItem();
            boolean confirmed = received.isSelected();
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            finish.setEnabled(false);
            method.setEnabled(false);
            amount.setEnabled(false);
            received.setEnabled(false);
            run(() -> service.complete(key, chosen, money, confirmed), value -> {
                display(value);
                historyLoaded = false;
                dialog.dispose();
                message.setText("Thanh toán thành công. Đang phát hành vé...");
                run(() -> service.issue(key), tickets -> preview(tickets, true));
            }, () -> {
                if (dialog.isDisplayable()) {
                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    finish.setEnabled(true);
                    method.setEnabled(true);
                    amount.setEnabled(true);
                    received.setEnabled(true);
                }
            });
        });
        dialog.setContentPane(content);
        Theme.apply(content);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(Math.max(dialog.getWidth(), Theme.s(460)), dialog.getHeight() + Theme.s(12)));
        dialog.setSize(dialog.getMinimumSize());
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        paymentOpen = false;
    }

    private void cancel() {
        if (cart == null) {
            error(new IllegalArgumentException("Chưa có giao dịch để hủy; nếu lần giữ ghế bị lỗi, hãy tra cứu mã đang hiển thị"));
            return;
        }
        boolean processing = "Đang thanh toán".equals(cart.giaoDich().trangThai());
        String prompt = processing
                ? "Chỉ hủy khi chắc chắn CHƯA nhận tiền và thanh toán chưa thành công. Xác nhận hủy?"
                : "Hủy giao dịch và giải phóng ghế?";
        if (JOptionPane.showConfirmDialog(this, prompt, "Hủy giao dịch", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        String key = id;
        run(() -> {
            if (processing) service.cancelPaymentBeforeReceivingMoney(key);
            else service.cancel(key);
            return service.inspect(key);
        }, value -> {
            display(value);
            selected.clear();
            reload();
        });
    }

    private boolean canLeave() {
        if (needsCheck) {
            error(new IllegalArgumentException("Cần tra cứu mã " + id + " để xác định kết quả giữ ghế trước khi chuyển giao dịch"));
            return false;
        }
        if (cart != null && List.of("Chờ thanh toán", "Đang thanh toán").contains(cart.giaoDich().trangThai())) {
            error(new IllegalArgumentException("Hoàn tất hoặc hủy giao dịch hiện tại trước. Nếu chưa rõ kết quả, tra cứu mã " + id));
            return false;
        }
        return true;
    }

    private void incomingDialog() {
        if (!canLeave()) return;
        run(service::yeuCauChonGheKhach, initial -> {
            paymentOpen = true;
            JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                    "Nhận mã chọn ghế của khách", Dialog.ModalityType.APPLICATION_MODAL);
            JPanel root = new JPanel(new BorderLayout(Theme.MD, Theme.MD));
            root.setBorder(BorderFactory.createEmptyBorder(Theme.LG, Theme.LG, Theme.LG, Theme.LG));
            JLabel status = new JLabel(initial.isEmpty()
                    ? "Chưa có yêu cầu chọn ghế đang chờ."
                    : "Đang có " + initial.size() + " yêu cầu chọn ghế chờ tiếp nhận.");
            root.add(Design.stack(Design.text("Nhận mã chọn ghế của khách", "heading"), status), BorderLayout.NORTH);

            DefaultListModel<ChonGheKhach.YeuCauCho> listModel = new DefaultListModel<>();
            initial.forEach(listModel::addElement);
            JList<ChonGheKhach.YeuCauCho> requestList = new JList<>(listModel);
            requestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            if (!initial.isEmpty()) requestList.setSelectedIndex(0);
            JScrollPane scroll = new JScrollPane(requestList);
            scroll.setPreferredSize(new Dimension(620, 210));
            root.add(scroll, BorderLayout.CENTER);

            JTextField manualCode = new JTextField(26);
            JButton refreshBtn = new JButton("Cập nhật danh sách");
            JButton claimSelected = new JButton("Nhận yêu cầu");
            claimSelected.putClientProperty("cinema.role", "primary");
            JButton claimManual = new JButton("Nhận lựa chọn");

            Consumer<String> claimCode = raw -> {
                String currentOrder = cart == null ? null : id;
                dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                refreshBtn.setEnabled(false);
                claimSelected.setEnabled(false);
                claimManual.setEnabled(false);
                manualCode.setEnabled(false);
                run(() -> service.nhanMaChonGhe(raw, currentOrder), claimed -> {
                    dialog.dispose();
                    display(claimed.gioHang());
                    showId = claimed.maSuat();
                    selected.clear();
                    claimed.gioHang().ghe().forEach(g -> selected.add(g.ghe().maGhe()));
                    message.setText(claimed.daThanhToan()
                            ? "Mã đặt vé này đã thanh toán hoàn tất"
                            : "Đã tiếp nhận lựa chọn và giữ ghế tại quầy");
                    if (showId != null) reload();
                }, () -> {
                    if (dialog.isDisplayable()) {
                        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                        refreshBtn.setEnabled(true);
                        claimSelected.setEnabled(true);
                        claimManual.setEnabled(true);
                        manualCode.setEnabled(true);
                        status.setText(message.getText());
                    }
                });
            };

            refreshBtn.addActionListener(e -> run(service::yeuCauChonGheKhach, rows -> {
                listModel.clear();
                rows.forEach(listModel::addElement);
                if (!rows.isEmpty()) requestList.setSelectedIndex(0);
                status.setText(rows.isEmpty()
                        ? "Chưa có yêu cầu chọn ghế đang chờ."
                        : "Đang có " + rows.size() + " yêu cầu chọn ghế chờ tiếp nhận.");
            }));
            claimSelected.addActionListener(e -> {
                var chosen = requestList.getSelectedValue();
                if (chosen == null) {
                    status.setText("Vui lòng chọn một yêu cầu trong danh sách hoặc nhập mã");
                    return;
                }
                claimCode.accept(chosen.maChonGhe());
            });
            claimManual.addActionListener(e -> claimCode.accept(manualCode.getText()));
            manualCode.addActionListener(e -> claimCode.accept(manualCode.getText()));

            JPanel listActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            listActions.add(refreshBtn);
            listActions.add(claimSelected);
            JPanel manualRow = new JPanel(new BorderLayout(8, 0));
            manualRow.add(new JLabel("Mã chọn ghế:"), BorderLayout.WEST);
            manualRow.add(manualCode, BorderLayout.CENTER);
            manualRow.add(claimManual, BorderLayout.EAST);
            root.add(Design.stack(listActions, manualRow), BorderLayout.SOUTH);

            dialog.setContentPane(root);
            Theme.apply(root);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            paymentOpen = false;
        });
    }

    private void newTransaction() {
        if (!canLeave()) return;
        resetForNextCustomer("Đã tạo giao dịch mới. Chọn ghế để bán cho khách tiếp theo.");
    }

    private void resetForNextCustomer(String statusText) {
        cart = null;
        id = BanVeBUS.newId();
        selected.clear();
        summaryPanel.clearDraftPreview();
        cartText.setText("");
        identity();
        refreshStepsBar();
        message.setText(statusText);
        if (showId != null) reload();
    }

    public boolean requestClose() {
        if (busy || paymentOpen) return false;
        if (needsCheck || cart != null && List.of("Chờ thanh toán", "Đang thanh toán").contains(cart.giaoDich().trangThai())) {
            return JOptionPane.showConfirmDialog(
                    this,
                    "Giao dịch " + id + " vẫn đang xử lý. Ghế đang thanh toán không tự giải phóng; dùng Kiểm tra giao dịch để tiếp tục. Đóng cửa sổ?",
                    "Giao dịch chưa hoàn tất",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        }
        return true;
    }

    private void display(BanVeDAO.GioHang value) {
        cart = value;
        needsCheck = false;
        id = value.giaoDich().maGiaoDich();
        if (value.suat() != null) {
            activeShowSummary = "Suất #" + value.suat().maSuat() + " · " + value.tenPhim()
                    + " · " + value.phong().tenPhong() + " · " + value.suat().thoiGianBatDau().toLocalTime();
            showBadge.setText(activeShowSummary);
        }
        identity();
        cartText.setText(description(value));
        refreshStepsBar();
        if (!"Chờ thanh toán".equals(value.giaoDich().trangThai())) {
            for (Component row : seats.getComponents()) {
                if (row instanceof Container container) {
                    for (Component chair : container.getComponents()) {
                        if (chair instanceof JToggleButton button) {
                            button.setEnabled(false);
                            Object key = button.getClientProperty("seatId");
                            if (value.ghe().stream().anyMatch(g -> Objects.equals(key, g.ghe().maGhe()) && "Đã bán".equals(g.trangThai()))) {
                                button.setSelected(false);
                                SeatVisual.state(button, "Đã bán");
                                button.setToolTipText("Đã bán · Giao dịch này");
                            }
                        }
                    }
                }
            }
        }
    }

    private void identity() {
        identity.setText("Mã giao dịch hiện tại: " + id + " · Lưu mã này để tra cứu nếu mất kết nối.");
    }

    private static String description(BanVeDAO.GioHang value) {
        var g = value.giaoDich();
        StringBuilder text = new StringBuilder("Giao dịch: " + g.maGiaoDich()
                + "\nTạo lúc: " + g.thoiGianTao()
                + "\nNhân viên: " + g.maNV()
                + "\nTrạng thái: " + g.trangThai()
                + "\nPhim: " + value.tenPhim() + "\n");
        if (value.suat() != null) {
            text.append("Suất: ").append(value.suat().maSuat()).append(" · ").append(value.suat().thoiGianBatDau())
                    .append("\nPhòng: ").append(value.phong().tenPhong()).append('\n');
        }
        for (var row : value.ghe()) {
            text.append("Ghế ").append(row.ghe().hangGhe()).append(row.ghe().soGhe())
                    .append(" · ").append(row.trangThai())
                    .append(" · ").append(row.giaVeChot() == null ? "Chưa tính tiền" : row.giaVeChot().toPlainString() + " VND")
                    .append(row.thoiGianHetHanGiu() == null ? "" : " · Giữ đến " + row.thoiGianHetHanGiu())
                    .append('\n');
        }
        text.append("Tổng tiền: ").append(g.tongTien().toPlainString()).append(" VND\n");
        if (value.thanhToan() != null) {
            text.append("Thanh toán: ").append(value.thanhToan().phuongThuc())
                    .append(" · ").append(value.thanhToan().soTien())
                    .append(" · ").append(value.thanhToan().thoiGianThanhToan());
        }
        return text.toString();
    }

    private void preview(List<VeDAO.ThongTin> tickets, boolean fromCompletedSale) {
        if (tickets.isEmpty()) {
            message.setText("Chưa có vé được phát hành");
            return;
        }
        BigDecimal grandTotal = tickets.stream()
                .map(t -> t.ve().giaVe())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String seatSummary = tickets.stream()
                .map(t -> t.ve().hangGheLucPhatHanh() + t.ve().soGheLucPhatHanh())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        JPanel root = new JPanel(new BorderLayout(Theme.SM, Theme.SM));
        root.setBorder(BorderFactory.createEmptyBorder(Theme.MD, Theme.MD, Theme.MD, Theme.MD));
        root.putClientProperty("cinema.background", "canvas");

        Design.Card banner = new Design.Card(new BorderLayout(Theme.SM, Theme.XS));
        banner.setBorder(BorderFactory.createEmptyBorder(Theme.s(10), Theme.s(14), Theme.s(10), Theme.s(14)));
        JLabel title = Design.text("Đã phát hành " + tickets.size() + " vé (Ghế " + seatSummary
                + ") · Tổng: " + formatVnd(grandTotal), "heading");
        JLabel sub = Design.text(tickets.size() > 1
                ? "Mỗi ghế tương ứng 1 vé riêng — bấm từng tab ghế bên dưới để xem hoặc sao chép mã vé."
                : "Sao chép mã vé bên dưới để dùng tại cổng soát vé hoặc tra cứu giao dịch.", "subtitle");
        JPanel bannerText = Design.stack(title, sub);
        bannerText.setOpaque(false);
        banner.add(bannerText, BorderLayout.NORTH);

        JTextField codeField = new JTextField(tickets.get(0).ve().maVe());
        codeField.setEditable(false);
        codeField.setFont(Theme.number(14));
        JLabel copyStatus = Design.text(" ", "subtitle");
        JButton copyOne = new JButton("Sao chép mã vé");
        copyOne.addActionListener(e -> {
            String value = codeField.getText();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
            copyStatus.setText("Đã sao chép mã vé: " + value);
            copyStatus.setForeground(Tokens.success());
        });

        JPanel copyButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.s(6), 0));
        copyButtons.setOpaque(false);
        copyButtons.add(copyOne);
        if (tickets.size() > 1) {
            JButton copyAll = new JButton("Sao chép tất cả (" + tickets.size() + " mã)");
            copyAll.addActionListener(e -> {
                String all = tickets.stream()
                        .map(t -> t.ve().hangGheLucPhatHanh() + t.ve().soGheLucPhatHanh() + ": " + t.ve().maVe())
                        .reduce((a, b) -> a + System.lineSeparator() + b)
                        .orElse("");
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(all), null);
                copyStatus.setText("Đã sao chép toàn bộ " + tickets.size() + " mã vé vào clipboard");
                copyStatus.setForeground(Tokens.success());
            });
            copyButtons.add(copyAll);
        }

        JPanel codeBar = new JPanel(new BorderLayout(Theme.s(8), Theme.s(4)));
        codeBar.setOpaque(false);
        codeBar.add(Design.text("Mã vé:", "subtitle"), BorderLayout.WEST);
        codeBar.add(codeField, BorderLayout.CENTER);
        codeBar.add(copyButtons, BorderLayout.EAST);
        codeBar.add(copyStatus, BorderLayout.SOUTH);
        banner.add(codeBar, BorderLayout.SOUTH);
        root.add(banner, BorderLayout.NORTH);

        JTabbedPane pages = new JTabbedPane();
        for (int i = 0; i < tickets.size(); i++) {
            var ticket = tickets.get(i);
            String tabTitle = "Ghế " + ticket.ve().hangGheLucPhatHanh() + ticket.ve().soGheLucPhatHanh()
                    + " (" + (i + 1) + "/" + tickets.size() + ")";
            JScrollPane imgScroll = new JScrollPane(new JLabel(new ImageIcon(InVe.preview(ticket))));
            pages.addTab(tabTitle, imgScroll);
        }
        pages.addChangeListener(e -> {
            int idx = pages.getSelectedIndex();
            if (idx >= 0 && idx < tickets.size()) {
                codeField.setText(tickets.get(idx).ve().maVe());
                copyStatus.setText(" ");
            }
        });
        root.add(pages, BorderLayout.CENTER);

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Vé đã lưu", Dialog.ModalityType.MODELESS);
        JButton print = new JButton("In vé đã lưu");
        print.addActionListener(event -> {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(new InVe(tickets));
            if (job.printDialog()) {
                print.setEnabled(false);
                run(() -> {
                    job.print();
                    return true;
                }, done -> message.setText("Đã gửi vé tới máy in"), () -> print.setEnabled(true));
            }
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.s(8), 0));
        footer.setOpaque(false);
        footer.add(print);
        if (fromCompletedSale) {
            JButton nextSale = new JButton("Hoàn tất & bán đơn mới");
            nextSale.putClientProperty("cinema.role", "primary");
            nextSale.addActionListener(e -> {
                dialog.dispose();
                if (cart != null && "Đã thanh toán".equals(cart.giaoDich().trangThai())) {
                    resetForNextCustomer("Giao dịch trước đã hoàn tất. Sẵn sàng bán đơn mới.");
                }
            });
            footer.add(nextSale);
            dialog.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) {
                    if (cart != null && "Đã thanh toán".equals(cart.giaoDich().trangThai())) {
                        SwingUtilities.invokeLater(() ->
                                resetForNextCustomer("Giao dịch trước đã hoàn tất. Sẵn sàng bán đơn mới."));
                    }
                }
            });
        }
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        Theme.apply(root);
        dialog.setSize(640, 780);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private static LocalDate day(JTextField field) {
        return field.getText().isBlank() ? null : LocalDate.parse(field.getText().trim());
    }

    private JButton button(JPanel target, String label, Runnable action) {
        JButton button = new JButton(label);
        button.addActionListener(event -> action.run());
        target.add(button);
        return button;
    }

    private void error(Throwable e) {
        message.setText(e instanceof IllegalArgumentException || e instanceof java.time.format.DateTimeParseException
                ? e.getMessage()
                : "Không hoàn tất thao tác. Hãy tra cứu mã " + id + " trước khi thực hiện lại.");
    }

    private void disable(Component component) {
        enabled.put(component, component.isEnabled());
        component.setEnabled(false);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) disable(child);
        }
    }

    private <T> void run(Callable<T> work, Consumer<T> done) {
        run(work, done, () -> {});
    }

    private <T> void run(Callable<T> work, Consumer<T> done, Runnable always) {
        if (busy) return;
        busy = true;
        enabled.clear();
        disable(this);
        message.setText("Đang xử lý...");
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                busy = false;
                enabled.forEach(Component::setEnabled);
                enabled.clear();
                try {
                    T value = get();
                    message.setText("Đã hoàn tất");
                    done.accept(value);
                } catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    error(cause);
                } finally {
                    always.run();
                }
            }
        }.execute();
    }
}
