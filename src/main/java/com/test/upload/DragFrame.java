package com.test.upload;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DragFrame extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(DragFrame.class);
    private static final String url = "http://101.37.75.26:8080/hac_auto/uploadFile?param_name=upLoadFile";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    JPanel jp1;
    TextArea textArea;
    TextField valueField;

    TextField textField;
    JComboBox<Station> jComboBox1;
    JComboBox<Device> jComboBox2;

    JPopupMenu pop;

    JMenuItem paste = new JMenuItem("粘贴");

    public DragFrame() {

        jp1 = new JPanel();
        getContentPane().add(jp1, BorderLayout.CENTER);
        jp1.setSize(500, 400);
        setSize(1200, 600);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocation(200, 200);
        setTitle("拖拽文件进入进行上传");
        Station hm = new Station("3301010001", "浙江环茂测试站点");
        Station sh = new Station("3301010502", "无人运维站点-申昊");
        Device cod = new Device("002", "COD分析仪");
        Device an = new Device("006", "氨氮分析仪");
        Device p = new Device("010", "总磷分析仪");
        Device n = new Device("014", "总氮分析仪");
        jComboBox1 = new JComboBox<>(new DefaultComboBoxModel<>(new Station[]{hm, sh}));
        jComboBox1.setSelectedItem(sh);
        jComboBox1.setRenderer(new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Station station = (Station) value;
                setText(station.getStationName());
                return this;
            }
        });
        jComboBox2 = new JComboBox<>(new DefaultComboBoxModel<>(new Device[]{cod, an, p, n}));
        jComboBox2.setSelectedItem(cod);
        jComboBox2.setRenderer(new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Device device = (Device) value;
                setText(device.getDeviceName());
                return this;
            }
        });
        textField = new TextField(20);
        valueField = new TextField(6);
        jp1.add(valueField);
        jp1.add(textField);
        MouseListener mouseListener = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    paste.setEnabled(true);
                    pop.show(textField, e.getX(), e.getY());
                }

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        };
        textField.addMouseListener(mouseListener);
        pop = new JPopupMenu();
        pop.add(paste);
        paste.setAccelerator(KeyStroke.getKeyStroke('V', InputEvent.CTRL_MASK));
        paste.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    textField.setText(Toolkit.getDefaultToolkit()
                            .getSystemClipboard().getContents(null)
                            .getTransferData(DataFlavor.stringFlavor).toString());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        textArea = new TextArea(30, 144);
        textArea.setBounds(20, 20, 500, 600);
        jp1.add(jComboBox1);
        jp1.add(jComboBox2);
//        Container parent = textArea.getParent();
//        parent.setComponentZOrder(parent,0);
        jp1.add(textArea, BorderLayout.CENTER);
        drag();
    }

    public static void main(String[] args) {
        new DragFrame().setVisible(true);

    }


    public void drag() {
        new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                        List<File> list = (List<File>) (dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor));
                        List<UploadResult> results = new ArrayList<>();
                        for (File file : list) {
                            String result = HttpRequest.post(url)
                                    .form("file", file)
                                    .execute()
                                    .body();
                            log.info("上传文件结果：{}", result);
                            results.add(JSONObject.parseObject(result, UploadResult.class));
                        }
                        StringBuilder sb = new StringBuilder();
                        for (UploadResult result : results) {
                            sb.append("originFileName:[ ");
                            sb.append(result.getUploadFile().get(0).getOldFileName());
                            sb.append(" ], newFileName:[ ");
                            sb.append(result.getUploadFile().get(0).getNewFileName());
                            sb.append("]\n");
                        }
                        String text = sb.toString();
                        textArea.setText(text);
                        String sql = buildSql(results);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(new StringSelection(sql), null);
                    } else {
                        dtde.rejectDrop();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }


        });


    }

    private String buildSql(List<UploadResult> results) {
        Station station = (Station) jComboBox1.getSelectedItem();
        String text = textField.getText();
        String value = valueField.getText();
        LocalDateTime time = LocalDateTime.parse(text, DTF);
        String stationCode = station.getStationCode();
        Device device = (Device) jComboBox2.getSelectedItem();
        String deviceCode = device.getDeviceCode();
        StringBuilder sb = new StringBuilder("insert into t_sys_file (old_file_name,new_file_name,login_id,file_att) values");
        int size = results.size();
        for (int i = 0; i < size; i++) {
            UploadResult uploadResult = results.get(i);
            UploadResult.UploadFile uploadFile = uploadResult.getUploadFile().get(0);
            String oldFileName = uploadFile.getOldFileName();
            String newFileName = uploadFile.getNewFileName();
            sb.append("('");
            sb.append(oldFileName);
            sb.append("','");
            sb.append(newFileName);
            sb.append("','");
            sb.append(stationCode);
            sb.append("','JPG')");
            if (i == size - 1) {
                sb.append(";");
            } else {
                sb.append(",");
            }
        }
        time = time.plusMinutes(59L);
        sb.append("insert into b_task_result (task_code, get_time, result_value, result_desc, station_code, device_code, s_desc)\n")
                .append(String.format("values ('A060601', '%s', 1, '%s', '%s', '%s', '')",
                        DTF.format(time), value, stationCode, deviceCode));
        return sb.toString();
    }

}