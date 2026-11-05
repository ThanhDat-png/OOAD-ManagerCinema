package cinema.view.ui;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
public final class Design {
    private Design(){}
    public static JLabel text(String value,String type){JLabel label=new JLabel(value);label.putClientProperty("cinema.text",type);return label;}
    public static JPanel stack(Component... children){JPanel panel=new JPanel();panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));for(Component child:children){if(child instanceof JComponent c)c.setAlignmentX(Component.LEFT_ALIGNMENT);panel.add(child);panel.add(Box.createVerticalStrut(8));}return panel;}
    public static final class WrappingLabel extends JLabel {
        public WrappingLabel(String text){super(text);setPreferredSize(new Dimension(360,56));}
        @Override protected void paintComponent(Graphics graphics){Graphics2D g=Skin.graphics(graphics);try{g.setColor(getForeground());g.setFont(Theme.font(12));int x=getIcon()==null?0:28,y=16;if(getIcon()!=null)getIcon().paintIcon(this,g,0,2);String line="";for(String word:getText().split(" ")){if(g.getFontMetrics().stringWidth(line+word)>getWidth()-x-8&&!line.isEmpty()){g.drawString(line,x,y);y+=18;line="";}line+=word+" ";}g.drawString(line,x,y);}finally{g.dispose();}}
    }
    public static final class SearchField extends JTextField {
        private final String hint;public SearchField(int columns,String hint){super(columns);this.hint=hint;}
        @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);if(getText().isEmpty()&&!hasFocus()){Graphics2D g=Skin.graphics(graphics);try{g.setColor(Tokens.muted());g.setFont(Tokens.font(12));g.drawString(hint,14,(getHeight()+g.getFontMetrics().getAscent()-g.getFontMetrics().getDescent())/2);}finally{g.dispose();}}}
    }
    public static void status(JLabel label){Runnable update=()->{String value=label.getText()==null?"":label.getText();boolean error=value.startsWith("Không thể")||value.contains("không hợp lệ")||value.contains("không chính xác")||value.contains("không được phép");boolean hint=value.startsWith("Vui lòng");boolean loading=value.startsWith("Đang");label.setForeground(error?Tokens.danger():hint?Tokens.warning():Tokens.muted());label.setIcon(new Icons(error||hint?"alert":loading?"calendar":value.startsWith("Đã")?"check":"box"));label.setIconTextGap(8);label.setFont(Tokens.font(12));};label.addPropertyChangeListener("text",e->update.run());update.run();}
    public static final class Card extends JPanel {
        public Card(LayoutManager layout){super(layout);setOpaque(false);setBorder(BorderFactory.createEmptyBorder(16,16,16,16));}
        @Override protected void paintComponent(Graphics graphics){Graphics2D g=Skin.graphics(graphics);try{g.setColor(Tokens.surface());g.fillRoundRect(0,0,getWidth(),getHeight(),16,16);g.setColor(Tokens.border());g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);}finally{g.dispose();}super.paintComponent(graphics);}
    }
    public static final class Wrap extends FlowLayout {
        public Wrap(){super(FlowLayout.LEFT,8,8);}
        @Override public Dimension preferredLayoutSize(Container target){return size(target);}@Override public Dimension minimumLayoutSize(Container target){return size(target);}
        private Dimension size(Container target){synchronized(target.getTreeLock()){int width=target.getWidth();Container p=target;while(width<=0&&p.getParent()!=null){p=p.getParent();width=p.getWidth();}if(width<=0)width=1024;Insets in=target.getInsets();int max=width-in.left-in.right-16,x=0,y=8,h=0,best=0;for(Component c:target.getComponents())if(c.isVisible()){Dimension d=c.getPreferredSize();if(x>0&&x+d.width>max){best=Math.max(best,x);y+=h+8;x=0;h=0;}x+=d.width+8;h=Math.max(h,d.height);}return new Dimension(Math.min(width,Math.max(best,x)+in.left+in.right+8),y+h+in.top+in.bottom+8);}}
    }
    public static void page(JPanel root,String title,String subtitle,JLabel status){
        if(!(root.getLayout() instanceof BorderLayout layout))return;
        Component top=layout.getLayoutComponent(BorderLayout.NORTH);if(top!=null)root.remove(top);
        JPanel head=stack(text(title,"title"),text(subtitle,"subtitle"));
        if(top instanceof JPanel panel&&panel.getComponentCount()==0)top=null;
        if(top!=null){Card filters=new Card(new BorderLayout());filters.setAlignmentX(Component.LEFT_ALIGNMENT);filters.add(top);head.add(filters);}
        root.add(head,BorderLayout.NORTH);root.setBorder(BorderFactory.createEmptyBorder(24,24,16,24));
        if(status!=null){status(status);status.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0,Tokens.border()),BorderFactory.createEmptyBorder(12,4,4,4)));status.getAccessibleContext().setAccessibleName("Thông báo trạng thái");for(Component c:all(root))if(c instanceof JTable table){if(table.getClientProperty("cinema.stateLabel")==null)table.putClientProperty("cinema.stateLabel",status);status.addPropertyChangeListener("text",e->table.repaint());}}
        for(Component c:all(root))if(c instanceof JPanel panel&&panel.getLayout() instanceof FlowLayout)panel.setLayout(new Wrap());
        root.addComponentListener(new ComponentAdapter(){public void componentResized(ComponentEvent e){root.revalidate();for(Component c:all(root))if(c instanceof JSplitPane split){int direction=root.getWidth()<900?JSplitPane.VERTICAL_SPLIT:JSplitPane.HORIZONTAL_SPLIT;if(split.getOrientation()!=direction){split.setOrientation(direction);split.setDividerLocation(.55);}}}});
        root.putClientProperty("cinema.background","canvas");head.putClientProperty("cinema.background","canvas");
        Component center=layout.getLayoutComponent(BorderLayout.CENTER);if(center instanceof JScrollPane scroll&&scroll.getViewport().getView() instanceof JTable){root.remove(center);Card card=new Card(new BorderLayout());card.add(center);root.add(card,BorderLayout.CENTER);}
        Theme.apply(root);root.setBackground(Tokens.bg());head.setBackground(Tokens.bg());
    }
    public static java.util.List<Component> all(Container root){java.util.List<Component> result=new java.util.ArrayList<>();for(Component c:root.getComponents()){result.add(c);if(c instanceof Container container)result.addAll(all(container));}return result;}
}
