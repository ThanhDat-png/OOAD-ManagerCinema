package cinema.view.ui;
import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
/** Shared renderer for actual seats; no availability or reservation decisions live here. */
public final class SeatVisual {
    private SeatVisual(){}
    public static void install(AbstractButton button,String state,String type,boolean own){button.putClientProperty("cinema.seatState",state);button.putClientProperty("cinema.seatType",type);button.putClientProperty("cinema.seatOwn",own);button.putClientProperty("cinema.noIcon",true);button.setUI(new SeatUI());button.setPreferredSize(new Dimension(Theme.s(80),Theme.s(76)));button.setBorder(BorderFactory.createEmptyBorder(Theme.s(Theme.XS),Theme.s(Theme.XS),Theme.s(Theme.XS),Theme.s(Theme.XS)));button.getAccessibleContext().setAccessibleName(button.getText()+" · "+type+" · "+state+(own?" · Giao dịch này":""));}
    public static void state(AbstractButton button,String state){button.putClientProperty("cinema.seatState",state);button.setUI(new SeatUI());button.repaint();}
    public static JPanel legend(){return legend("Trống","Đang giữ","Đã bán","Không khả dụng","Giao dịch này");}
    public static JPanel legend(String... states){JPanel panel=new JPanel(new Design.Wrap());for(String state:states){JLabel label=new JLabel(state,new Mark(state),JLabel.LEADING);label.setFont(Theme.font(12));panel.add(label);}return panel;}
    public static JComponent screen(){return new JComponent(){public Dimension getPreferredSize(){return new Dimension(Theme.s(200),Theme.s(40));}protected void paintComponent(Graphics graphics){Graphics2D g=Skin.graphics(graphics);try{g.setColor(Theme.border());g.setStroke(new BasicStroke(2));g.drawArc(16,8,getWidth()-32,32,10,160);g.setColor(Theme.muted());g.setFont(Theme.bold(11));String text="MÀN HÌNH";g.drawString(text,(getWidth()-g.getFontMetrics().stringWidth(text))/2,34);}finally{g.dispose();}}};}
    private static void symbol(Graphics2D g,String state,int x,int y){if(state.equals("Đã bán")){g.drawLine(x,y,x+8,y+8);g.drawLine(x+8,y,x,y+8);}else if(state.equals("Không khả dụng")){g.drawLine(x,y+4,x+8,y+4);}else if(state.equals("Đang giữ")){g.drawOval(x-1,y-1,10,10);g.drawLine(x+4,y+1,x+4,y+4);g.drawLine(x+4,y+4,x+7,y+5);}else if(state.equals("Giao dịch này")){g.drawLine(x,y+4,x+3,y+7);g.drawLine(x+3,y+7,x+9,y);}else g.drawOval(x,y,8,8);}
    private static final class Mark implements Icon{private final String state;Mark(String state){this.state=state;}public int getIconWidth(){return Theme.s(18);}public int getIconHeight(){return Theme.s(18);}public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=Skin.graphics(graphics);try{g.translate(x+3,y+3);g.setColor(state.equals("Giao dịch này")?Theme.accent():Theme.semantic(state));g.setStroke(new BasicStroke(1.6f));symbol(g,state,1,1);}finally{g.dispose();}}}
    public static final class SeatUI extends BasicButtonUI {
        @Override public void update(Graphics g,JComponent c){paint(g,c);}
        @Override public void paint(Graphics graphics,JComponent component){AbstractButton button=(AbstractButton)component;Graphics2D g=Skin.graphics(graphics);try{
            String state=String.valueOf(button.getClientProperty("cinema.seatState")),type=String.valueOf(button.getClientProperty("cinema.seatType"));boolean own=Boolean.TRUE.equals(button.getClientProperty("cinema.seatOwn")),selected=button.isSelected();boolean current=(own&&state.equals("Đang giữ"))||selected;
            Color color=current?Theme.accent():Theme.semantic(state);int width=component.getWidth(),height=component.getHeight();g.setColor(Theme.tone(color));g.fillRoundRect(5,5,width-10,height-10,16,16);g.setColor(color);g.setStroke(new BasicStroke(current||button.hasFocus()?2:1));g.drawRoundRect(5,5,width-11,height-11,16,16);
            if(type.toLowerCase().contains("vip")){g.drawLine(10,height-12,width-10,height-12);g.drawLine(10,height-16,width-10,height-16);}else if(type.toLowerCase().contains("đôi")){g.drawLine(width/2,height-18,width/2,height-9);}
            g.setStroke(new BasicStroke(1.6f));symbol(g,current?"Giao dịch này":state,width-22,11);
            String title=button.getText().split(" · ")[0];g.setFont(Theme.bold(14));g.setColor(Theme.text());g.drawString(title,(width-g.getFontMetrics().stringWidth(title))/2,32);
            String caption=current?"Đang chọn":state.equals("Không khả dụng")?"Khóa":state.equals("Đang giữ")?"Đang giữ":state.equals("Đã bán")?"Đã bán":"Trống";g.setFont(Theme.font(11));g.setColor(color);g.drawString(caption,(width-g.getFontMetrics().stringWidth(caption))/2,50);
            if(button.hasFocus()){g.setColor(Theme.accent());g.drawRoundRect(1,1,width-3,height-3,18,18);}
        }finally{g.dispose();}}
    }
}
