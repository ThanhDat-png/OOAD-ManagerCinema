package cinema.view.ui;
import cinema.controller.film.PosterStore;
import cinema.model.Phim;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
public final class PosterCells implements TableCellRenderer {
    private final JTable table;private final PosterStore store;private final Map<String,Phim> films=new HashMap<>();private final Map<String,ImageIcon> images=new HashMap<>();
    public PosterCells(JTable table,PosterStore store){this.table=table;this.store=store;table.putClientProperty("cinema.posterTable",true);table.getColumnModel().getColumn(1).setCellRenderer(this);table.getColumnModel().getColumn(1).setPreferredWidth(300);}
    public void update(List<Phim> data){films.clear();for(Phim film:data){films.put(film.maPhim(),film);String key=film.poster();if(key!=null&&!images.containsKey(key)){images.put(key,null);new SwingWorker<BufferedImage,Void>(){protected BufferedImage doInBackground(){try{return store.load(key);}catch(RuntimeException e){return null;}}protected void done(){try{BufferedImage image=get();if(image!=null)images.put(key,new ImageIcon(image.getScaledInstance(60,84,Image.SCALE_SMOOTH)));}catch(Exception ignored){}table.repaint();}}.execute();}}table.repaint();}
    @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focused,int row,int column){Phim film=films.get(String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(row),0)));JPanel cell=new JPanel(new BorderLayout(12,4));cell.setBorder(BorderFactory.createEmptyBorder(Theme.s(Theme.SM),Theme.s(Theme.SM),Theme.s(Theme.SM),Theme.s(Theme.SM)));cell.setBackground(selected?Theme.tone(Theme.accent()):Theme.surface());JLabel art=new JLabel();art.setPreferredSize(new Dimension(60,84));ImageIcon icon=film==null?null:images.get(film.poster());art.setIcon(icon==null?new Placeholder():icon);cell.add(art,BorderLayout.WEST);String title=value==null?"":value.toString();JPanel text=Design.stack(Design.text(title,"heading"),Design.text(film==null?"":film.dinhDangChieu()+" · "+film.thoiLuong()+" phút","subtitle"));Theme.apply(text);text.setBackground(cell.getBackground());cell.add(text);cell.setToolTipText(title);return cell;}
    private static final class Placeholder implements Icon {public int getIconWidth(){return 60;}public int getIconHeight(){return 84;}public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=Skin.graphics(graphics);try{g.setColor(Theme.tone(Theme.accent()));g.fillRoundRect(x,y,60,84,10,10);g.setColor(Theme.accent());new Icons("film",28).paintIcon(c,g,x+16,y+20);g.setFont(Theme.font(11));g.drawString("POSTER",x+8,y+65);}finally{g.dispose();}}}
}
