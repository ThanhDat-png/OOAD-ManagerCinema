package cinema.view.ui;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableModel;
/** All charts and KPIs are projections of the original report table, never new queries. */
public final class ReportVisuals extends JPanel {
    private final JTable table;private final JLabel summary;private final String kind;
    private final JLabel[] captions=new JLabel[3],values=new JLabel[3];private final Bars chart=new Bars();
    private record Point(String name,BigDecimal value){}
    public ReportVisuals(JTable table,JLabel summary,String kind){super(new BorderLayout(12,12));this.table=table;this.summary=summary;this.kind=kind;table.putClientProperty("cinema.stateLabel",summary);summary.addPropertyChangeListener("text",e->{update();table.repaint();});JPanel cards=new JPanel(new GridLayout(1,3,12,12));for(int i=0;i<3;i++){Design.Card card=new Design.Card(new BorderLayout(8,8));captions[i]=Design.text(" ","subtitle");values[i]=Design.text("—","metric");card.add(captions[i],BorderLayout.NORTH);card.add(values[i]);cards.add(card);}Design.Card graph=new Design.Card(new BorderLayout());graph.add(chart);JPanel top=new JPanel(new BorderLayout(12,12));top.add(cards,BorderLayout.NORTH);top.add(graph);add(top,BorderLayout.NORTH);add(new JScrollPane(table));table.getModel().addTableModelListener(e->update());update();}
    private static BigDecimal number(Object value){return value instanceof BigDecimal amount?amount:value instanceof Number number?BigDecimal.valueOf(number.longValue()):BigDecimal.ZERO;}
    private static String format(BigDecimal value){return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value);}
    private void metrics(String[] names,BigDecimal... data){for(int i=0;i<3;i++){captions[i].setText(names[i]);values[i].setText(format(data[i]));}}
    private void update(){TableModel data=table.getModel();String state=summary.getText()==null?"":summary.getText();if(data.getColumnCount()==0||state.startsWith("Tiêu chí")||state.startsWith("Chưa có")||state.startsWith("Đang")||state.startsWith("Không tải")){for(int i=0;i<3;i++){captions[i].setText(new String[]{"Kết quả theo bộ lọc","Số lượng","Giá trị"}[i]);values[i].setText("—");}chart.data(List.of(),"Chọn tiêu chí rồi bấm Thống kê.","");return;}BigDecimal zero=BigDecimal.ZERO;List<Point> points=new ArrayList<>();
        switch(kind){
            case "time"->{if(data.getRowCount()==0)return;metrics(new String[]{"Giao dịch thành công","Vé đã bán","Doanh thu · VND"},number(data.getValueAt(0,0)),number(data.getValueAt(0,1)),number(data.getValueAt(0,4)));points.add(new Point("Đã phát hành",number(data.getValueAt(0,2))));points.add(new Point("Chờ phát hành",number(data.getValueAt(0,3))));chart.data(points,"Phân bổ vé đã thanh toán","vé");}
            case "film","staff"->{BigDecimal count=zero,total=zero;int countCol=kind.equals("film")?2:2,moneyCol=kind.equals("film")?3:6;for(int i=0;i<data.getRowCount();i++){count=count.add(number(data.getValueAt(i,countCol)));total=total.add(number(data.getValueAt(i,moneyCol)));if(i<6)points.add(new Point(String.valueOf(data.getValueAt(i,1)),number(data.getValueAt(i,moneyCol))));}metrics(kind.equals("film")?new String[]{"Phim theo bộ lọc","Vé đã phát hành","Doanh thu · VND"}:new String[]{"Nhân viên theo bộ lọc","Giao dịch thành công","Doanh thu · VND"},BigDecimal.valueOf(data.getRowCount()),count,total);chart.data(points,"Doanh thu theo "+(kind.equals("film")?"phim":"nhân viên")+(data.getRowCount()>6?" · 6 dòng đầu, bảng bên dưới đầy đủ":""),"VND");}
            case "tickets"->{if(data.getRowCount()==0)return;metrics(new String[]{"Vé phát hành","Chưa sử dụng","Đã sử dụng"},number(data.getValueAt(0,0)),number(data.getValueAt(0,1)),number(data.getValueAt(0,2)));points.add(new Point("Chưa sử dụng",number(data.getValueAt(0,1))));points.add(new Point("Đã sử dụng",number(data.getValueAt(0,2))));chart.data(points,"Trạng thái vé hiện tại","vé");}
            case "stock"->{long available=0,empty=0;for(int i=0;i<data.getRowCount();i++){if(number(data.getValueAt(i,4)).signum()>0)available++;else empty++;}metrics(new String[]{"Sản phẩm theo bộ lọc","Còn hàng","Hết hàng"},BigDecimal.valueOf(data.getRowCount()),BigDecimal.valueOf(available),BigDecimal.valueOf(empty));points.add(new Point("Còn hàng",BigDecimal.valueOf(available)));points.add(new Point("Hết hàng",BigDecimal.valueOf(empty)));chart.data(points,"Tình trạng tồn kho · Không cộng lẫn đơn vị sản phẩm","sản phẩm");}
            default->{}
        }revalidate();repaint();
    }
    private static final class Bars extends JComponent {
        private List<Point> points=List.of();private String title="",unit="";
        Bars(){setPreferredSize(new Dimension(0,96));setToolTipText("");getAccessibleContext();}
        void data(List<Point> points,String title,String unit){this.points=List.copyOf(points);this.title=title;this.unit=unit;setPreferredSize(new Dimension(0,points.size()>3?144:96));revalidate();repaint();}
        @Override protected void paintComponent(Graphics graphics){Graphics2D g=Skin.graphics(graphics);try{g.setFont(Theme.bold(12));g.setColor(Theme.text());g.drawString(title,0,14);if(points.isEmpty()){g.setColor(Theme.muted());g.setFont(Theme.font(14));g.drawString("Chưa có số liệu để vẽ biểu đồ.",0,72);return;}int labelWidth=Math.min(190,getWidth()/4),right=130,x=labelWidth,plot=Math.max(80,getWidth()-labelWidth-right),top=32,barHeight=Math.min(22,Math.max(12,(getHeight()-60)/points.size()-6));double max=points.stream().mapToDouble(p->p.value().doubleValue()).max().orElse(0);if(max<=0)max=1;
            g.setColor(Theme.border());g.setStroke(new BasicStroke(1));for(int i=0;i<=4;i++){int gx=x+plot*i/4;g.drawLine(gx,top,gx,getHeight()-22);}int spacing=Math.max(18,(getHeight()-56)/points.size());for(int i=0;i<points.size();i++){Point point=points.get(i);int y=top+i*spacing;g.setColor(Theme.muted());g.setFont(Theme.font(12));String name=point.name();while(name.length()>3&&g.getFontMetrics().stringWidth(name)>labelWidth-12)name=name.substring(0,name.length()-2)+"…";g.drawString(name,0,y+barHeight-3);int width=(int)(plot*point.value().doubleValue()/max);g.setColor(Theme.accent());g.fillRoundRect(x,y,Math.max(0,width),barHeight,6,6);g.setFont(Theme.number());g.setColor(Theme.text());g.drawString(format(point.value()),x+plot+12,y+barHeight-3);}g.setFont(Theme.font(11));g.setColor(Theme.muted());g.drawString("0",x,getHeight()-4);String end=format(BigDecimal.valueOf(max))+" "+unit;g.drawString(end,x+plot-g.getFontMetrics().stringWidth(end),getHeight()-4);
        }finally{g.dispose();}}
        @Override public String getToolTipText(MouseEvent event){if(points.isEmpty())return title;int index=(event.getY()-32)/Math.max(18,(getHeight()-56)/points.size());if(index<0||index>=points.size())return title;Point point=points.get(index);return point.name()+": "+format(point.value())+" "+unit;}
    }
}
