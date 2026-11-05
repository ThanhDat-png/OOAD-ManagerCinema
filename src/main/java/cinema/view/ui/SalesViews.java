package cinema.view.ui;
import cinema.dao.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;

/** Presentation adapters retain original selection models and text sources. */
public final class SalesViews {
    private SalesViews(){}
    private static DefaultTableModel model(String... columns){return new DefaultTableModel(columns,0){public boolean isCellEditable(int r,int c){return false;}};}
    public static final class Shows extends JPanel {
        private boolean syncing;private final JTable table;
        public Shows(JComboBox<BanVeDAO.SuatBan> source,Runnable confirm){super(new BorderLayout());DefaultTableModel rows=model("Suất","Phim","Phòng","Bắt đầu");table=new JTable(rows);table.putClientProperty("cinema.compactTable",true);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.setRowHeight(32);JScrollPane scroll=new JScrollPane(table);scroll.setPreferredSize(new Dimension(0,Theme.s(96)));add(scroll);source.setVisible(false);add(source,BorderLayout.SOUTH);
            Runnable sync=()->{if(syncing)return;syncing=true;try{rows.setRowCount(0);for(int i=0;i<source.getItemCount();i++){var show=source.getItemAt(i);rows.addRow(new Object[]{show.suat().maSuat(),show.tenPhim(),show.tenPhong(),show.suat().thoiGianBatDau()});}int index=source.getSelectedIndex();if(index>=0&&index<rows.getRowCount())table.setRowSelectionInterval(index,index);}finally{syncing=false;}};
            source.getModel().addListDataListener(new ListDataListener(){public void intervalAdded(ListDataEvent e){sync.run();}public void intervalRemoved(ListDataEvent e){sync.run();}public void contentsChanged(ListDataEvent e){sync.run();}});
            table.getSelectionModel().addListSelectionListener(e->{if(!syncing&&!e.getValueIsAdjusting()&&table.getSelectedRow()>=0)source.setSelectedIndex(table.getSelectedRow());});
            table.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"cinema.confirmShow");table.getActionMap().put("cinema.confirmShow",new AbstractAction(){public void actionPerformed(ActionEvent e){confirm.run();}});sync.run();Theme.apply(this);
        }
    }
    public static final class Transactions extends JPanel {
        private boolean syncing;
        public Transactions(JList<GiaoDichDAO.Dong> source){super(new BorderLayout());DefaultTableModel rows=model("Mã giao dịch","Thời gian","Nhân viên","Trạng thái","Tổng tiền (VND)");JTable table=new JTable(rows);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);add(new JScrollPane(table));source.setVisible(false);add(source,BorderLayout.SOUTH);
            Runnable sync=()->{if(syncing)return;syncing=true;try{rows.setRowCount(0);for(int i=0;i<source.getModel().getSize();i++){var row=source.getModel().getElementAt(i);var sale=row.giaoDich();rows.addRow(new Object[]{sale.maGiaoDich(),sale.thoiGianTao(),row.hoTenNhanVien(),sale.trangThai(),sale.tongTien()});}if(source.getSelectedIndex()>=0&&source.getSelectedIndex()<rows.getRowCount())table.setRowSelectionInterval(source.getSelectedIndex(),source.getSelectedIndex());}finally{syncing=false;}};
            source.getModel().addListDataListener(new ListDataListener(){public void intervalAdded(ListDataEvent e){sync.run();}public void intervalRemoved(ListDataEvent e){sync.run();}public void contentsChanged(ListDataEvent e){sync.run();}});source.addListSelectionListener(e->{if(!e.getValueIsAdjusting())sync.run();});table.getSelectionModel().addListSelectionListener(e->{if(!syncing&&!e.getValueIsAdjusting()&&table.getSelectedRow()>=0)source.setSelectedIndex(table.getSelectedRow());});sync.run();Theme.apply(this);
        }
    }
    public static final class Details extends JPanel {
        private final JPanel rows=new Rows();private final JLabel total=Design.text("Chưa tính tiền","title"),state=new JLabel("Chưa có giao dịch");private final JTextArea source;
        public Details(JTextArea source,String title){super(new BorderLayout(8,12));this.source=source;rows.addComponentListener(new ComponentAdapter(){public void componentResized(ComponentEvent e){update();}});setBorder(BorderFactory.createEmptyBorder(16,16,16,16));total.putClientProperty("cinema.text","total");add(Design.stack(Design.text(title,"heading"),state,total),BorderLayout.NORTH);add(new JScrollPane(rows));source.setVisible(false);add(source,BorderLayout.SOUTH);source.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){update();}public void removeUpdate(DocumentEvent e){update();}public void changedUpdate(DocumentEvent e){update();}});update();}
        private void update(){rows.removeAll();total.setText("Chưa tính tiền");state.setText("Chọn suất và ghế để bắt đầu");int row=0;for(String line:source.getText().split("\\R")){if(line.isBlank())continue;if(line.startsWith("Tổng tiền:"))total.setText(line.substring("Tổng tiền:".length()).trim());if(line.startsWith("Trạng thái:"))state.setText(line.substring("Trạng thái:".length()).trim());String key="",value=line;int colon=line.indexOf(':');if(colon>=0&&!line.startsWith("Ghế ")){key=line.substring(0,colon);value=line.substring(colon+1).trim();}JLabel name=Design.text(key,"subtitle"),content=new JLabel("<html><div style='width:"+Math.max(120,(int)(rows.getWidth()*.68)-12)+"px'>"+escape(value)+"</div></html>");GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=row;c.weightx=.25;c.anchor=GridBagConstraints.NORTHWEST;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(6,0,6,12);rows.add(name,c);c.gridx=1;c.weightx=.75;c.insets=new Insets(6,0,6,0);rows.add(content,c);row++;}if(row==0){GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=0;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;rows.add(Design.text("Thông tin giao dịch sẽ xuất hiện tại đây.","subtitle"),c);}GridBagConstraints end=new GridBagConstraints();end.gridy=row+1;end.weighty=1;rows.add(Box.createVerticalGlue(),end);Theme.apply(this);state.setForeground(Theme.semantic(state.getText()));rows.revalidate();rows.repaint();}
    }
    private static final class Rows extends JPanel implements Scrollable {
        Rows(){super(new GridBagLayout());}public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}public int getScrollableUnitIncrement(Rectangle r,int o,int d){return Theme.s(24);}public int getScrollableBlockIncrement(Rectangle r,int o,int d){return Theme.s(120);}public boolean getScrollableTracksViewportWidth(){return true;}public boolean getScrollableTracksViewportHeight(){return false;}
    }
    public static JPanel field(String label,JComponent input){JPanel panel=new JPanel(new BorderLayout(0,4));JLabel name=Design.text(label,"subtitle");name.setLabelFor(input);panel.add(name,BorderLayout.NORTH);panel.add(input);return panel;}
    private static String escape(String text){return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
}
