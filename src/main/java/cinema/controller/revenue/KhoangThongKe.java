package cinema.controller.revenue;

import java.time.*;

/** Inclusive calendar dates; reporting SQL uses DATETIME's stored cinema dates. */
public record KhoangThongKe(LocalDate tuNgay,LocalDate denNgay) {
    public KhoangThongKe {
        if(tuNgay==null || denNgay==null) throw new IllegalArgumentException("Vui lòng chọn đầy đủ thời gian thống kê");
        if(tuNgay.isAfter(denNgay) || tuNgay.getYear()<1000 || denNgay.getYear()>9999)
            throw new IllegalArgumentException("Khoảng thời gian không hợp lệ");
    }
    public static KhoangThongKe ngay(LocalDate date) {return new KhoangThongKe(date,date);}
    public static KhoangThongKe thang(YearMonth month) {
        if(month==null)throw new IllegalArgumentException("Vui lòng chọn tháng thống kê");
        return new KhoangThongKe(month.atDay(1),month.atEndOfMonth());
    }
    public static KhoangThongKe nam(int year) {
        if(year<1000 || year>9999)throw new IllegalArgumentException("Năm thống kê không hợp lệ");
        return new KhoangThongKe(LocalDate.of(year,1,1),LocalDate.of(year,12,31));
    }
    @Override public String toString(){return tuNgay+" đến "+denNgay;}
}
