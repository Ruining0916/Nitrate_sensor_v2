package com.example.bluetooth_transmission;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

/**
 * Created by MEI on 2016/10/25.
 */

public class SaveData {
    private String Path;
    public static final String FILE_NAME = "NITRATE_SENSOR.txt";
    private String excel_path;
    List<String> list_data;
    private WritableWorkbook wwb;
    private Context context;
    public void saveData(List<String> data,Context context){
        Date current = Calendar.getInstance().getTime();

        Path = getSDPath() + File.separator + current.toString() +"-"+ FILE_NAME;
        File YFile = new File(Path);
        if(!YFile.exists()){
            try {
                YFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.context = context;
        excel_path = getSDPath() + File.separator + current.toString() +"-"+ "NITRATE_SENSOR_DATA" + ".xls";
        File file = new File(excel_path);
        if (file.exists()) {
            file.delete();
        }
        deilData(Path, data);
        ReadTxtFile(Path);
        createExcel();
    }


    public void createExcel() {
        WritableSheet ws = null;
        try {
            // 创建表
            if (list_data.size() == 0) {
                Toast.makeText(context, "no data available", Toast.LENGTH_SHORT).show();
            } else {
                wwb = Workbook.createWorkbook(new File(excel_path));
                // 创建表单,其中sheet表示该表格的名字,0表示第一个表格,
                ws = wwb.createSheet("zz", 0);
                // 在指定单元格插入数据
                Label lbl1 = new Label(0, 0, "impedance");// 第一个参数表示,0表示第一列,第二个参数表示行,同样0表示第一行,第三个参数表示想要添加到单元格里的数据.
                Label bll2 = new Label(1, 0, "temperature");

                Label bll9 = new Label(2, 0, "test condition");


                for (int i = 0; i < list_data.size(); i++) {
                    String s = "";

                    s = list_data.get(i);

                    Label b = new Label(i % 3, i / 3 + 1, s);

                    ws.addCell(b);
                }

                // 添加到指定表格里.
                ws.addCell(lbl1);
                ws.addCell(bll2);
//                ws.addCell(bll5);
//                ws.addCell(bll6);
                ws.addCell(bll9);

                // 从内存中写入文件中
                wwb.write();
                wwb.close();
                Toast.makeText(context, "data has been saved to root directory", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getSDPath() {
        boolean hasSDCard = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (hasSDCard) {
            return Environment.getExternalStorageDirectory().toString();
        } else {
            return Environment.getDownloadCacheDirectory().toString();
        }
    }

    private void deilData(String filePath, List<String> str_list) {
        String str = "";
        for (int i = 0; i <str_list.size() ; i++) {
            str+=str_list.get(i);
        }
        FileOutputStream fos = null;
        try {
            System.out.println("writing "+str);
            File file = new File(filePath);
            fos = new FileOutputStream(file, true);
            fos.write(str.getBytes());
            System.out.println("ok");
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fos)
                    fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void ReadTxtFile(String strFilePath) {
        String path = strFilePath;
        list_data = new ArrayList<String>(); //文件内容字符串
        //打开文件
        File file = new File(path);
        //如果path是传递过来的参数，可以做一个非目录的判断
        if (file.isDirectory()) {
            Log.d("TestFile", "The File doesn't not exist.");
        } else {
            try {
                InputStream instream = new FileInputStream(file);

                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);
                String line;
                //分行读取
                while ((line = buffreader.readLine()) != null) {
                    if (!line.trim().equals("")) {
                        System.out.println("reading "+line);
                        String line_txt = line.replace("impedance:","").replace("佑蜜值:","").replace("temperature:","")
                                .replace("test condition:","");
                        for (int i = 0; i <line_txt.split("\\|").length ; i++) {
                            list_data.add(line_txt.split("\\|")[i]);
                        }

                    }
                }
                instream.close();

            } catch (FileNotFoundException e) {
                Log.d("TestFile", "The File doesn't not exist.");
            } catch (IOException e) {
                Log.d("TestFile", e.getMessage());
            }
        }

    }
}
