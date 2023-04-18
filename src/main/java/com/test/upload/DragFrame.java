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
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DragFrame extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(DragFrame.class);
    JPanel jp1;
    TextArea textArea;

    JComboBox<Station> jComboBox;

    private static final String url = "http://101.37.75.26:8080/hac_auto/uploadFile?param_name=upLoadFile";

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
        jComboBox = new JComboBox<>(new DefaultComboBoxModel<>(new Station[]{hm, sh}));
        jComboBox.setSelectedItem(sh);
        jComboBox.setRenderer(new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Station station = (Station) value;
                setText(station.getStationName());
                return this;
            }
        });
        textArea = new TextArea(30, 144);
        textArea.setBounds(20, 20, 500, 600);
        jp1.add(jComboBox);
        jp1.add(textArea, BorderLayout.CENTER);
        drag();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            log.error("error", e);
        }
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
        Station station = (Station) jComboBox.getSelectedItem();
        String stationCode = station.getStationCode();
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
        return sb.toString();
    }

}