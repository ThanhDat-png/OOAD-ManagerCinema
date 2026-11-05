package cinema.view.ui;
import java.awt.*;
import javax.swing.*;
/** Original, stroke-based icon family. No external icon or image dependency. */
public final class Icons implements Icon {
    private final String kind;private final int size;
    public Icons(String kind){this(kind,18);}public Icons(String kind,int size){this.kind=kind;this.size=size;}
    public int getIconWidth(){return Theme.s(size);}public int getIconHeight(){return Theme.s(size);}
    public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();try{g.translate(x,y);g.scale(getIconWidth()/20.0,getIconHeight()/20.0);g.setColor(c.isEnabled()?c.getForeground():Tokens.muted());g.setStroke(new BasicStroke(1.6f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));switch(kind){
        case "search"->{g.drawOval(3,3,10,10);g.drawLine(12,12,17,17);}
        case "film"->{g.drawRoundRect(2,3,16,14,2,2);g.drawLine(6,3,6,17);g.drawLine(14,3,14,17);g.drawLine(2,8,6,8);g.drawLine(14,8,18,8);g.drawLine(2,12,6,12);g.drawLine(14,12,18,12);}
        case "people"->{g.drawOval(7,2,6,6);g.drawArc(3,9,14,12,0,180);g.drawOval(1,5,4,4);g.drawOval(15,5,4,4);}
        case "calendar"->{g.drawRoundRect(2,4,16,14,3,3);g.drawLine(2,8,18,8);g.drawLine(6,2,6,6);g.drawLine(14,2,14,6);g.drawLine(6,12,8,12);g.drawLine(12,12,14,12);}
        case "seat"->{g.drawRoundRect(5,2,10,11,3,3);g.drawRoundRect(2,9,16,7,2,2);g.drawLine(4,16,4,18);g.drawLine(16,16,16,18);}
        case "chart"->{g.drawLine(2,2,2,18);g.drawLine(2,18,18,18);g.drawLine(6,14,6,10);g.drawLine(11,14,11,5);g.drawLine(16,14,16,8);}
        case "ticket"->{g.drawRoundRect(2,4,16,12,3,3);g.drawLine(13,5,13,7);g.drawLine(13,10,13,12);g.drawLine(13,14,13,15);}
        case "check"->{g.drawLine(3,10,8,15);g.drawLine(8,15,17,5);}
        case "sun"->{g.drawOval(6,6,8,8);for(int i=0;i<8;i++){double a=i*Math.PI/4;g.drawLine((int)(10+7*Math.cos(a)),(int)(10+7*Math.sin(a)),(int)(10+9*Math.cos(a)),(int)(10+9*Math.sin(a)));}}
        case "exit"->{g.drawRoundRect(2,2,10,16,2,2);g.drawLine(8,10,18,10);g.drawLine(14,6,18,10);g.drawLine(14,14,18,10);}
        case "plus"->{g.drawLine(10,3,10,17);g.drawLine(3,10,17,10);}
        case "alert"->{g.drawRoundRect(2,2,16,16,5,5);g.drawLine(10,6,10,11);g.drawLine(10,14,10,14);}
        case "box"->{g.drawRoundRect(3,3,14,14,3,3);g.drawLine(3,8,17,8);g.drawLine(8,3,8,8);}
        default->{g.drawRoundRect(3,3,5,5,1,1);g.drawRoundRect(12,3,5,5,1,1);g.drawRoundRect(3,12,5,5,1,1);g.drawRoundRect(12,12,5,5,1,1);}
    }}finally{g.dispose();}}
    public static String forTitle(String text){String s=text==null?"":text.toLowerCase();if(s.contains("phim")||s.contains("poster"))return "film";if(s.contains("nhân viên")||s.contains("cá nhân"))return "people";if(s.contains("lịch")||s.contains("suất"))return "calendar";if(s.contains("phòng")||s.contains("ghế"))return "seat";if(s.contains("báo cáo")||s.contains("thống kê"))return "chart";if(s.contains("vé")||s.contains("giao dịch"))return "ticket";if(s.contains("tìm")||s.contains("tra cứu"))return "search";if(s.contains("thêm")||s.contains("tạo"))return "plus";return "box";}
}
