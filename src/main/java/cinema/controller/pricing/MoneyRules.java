package cinema.controller.pricing;

import java.math.BigDecimal;

public final class MoneyRules {
    private static final BigDecimal MAX=new BigDecimal("999999999999");
    private MoneyRules() { }
    public static BigDecimal positive(BigDecimal value,String message) {
        if(value==null || value.signum()<=0 || value.compareTo(MAX)>0 || value.stripTrailingZeros().scale()>0)
            throw new IllegalArgumentException(message+" (VND nguyên, tối đa 12 chữ số)");
        return value.setScale(0);
    }
}
