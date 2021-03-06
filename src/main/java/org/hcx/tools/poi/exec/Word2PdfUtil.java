package org.hcx.tools.poi.exec;


import java.io.File;

import java.io.FileOutputStream;

import java.io.InputStream;

 


 

import com.aspose.words.Document;

import com.aspose.words.License;

import com.aspose.words.SaveFormat;

 

 

/**

 * @author Administrator

 * @version $Id$

 * @since

 * @see

 */

public class Word2PdfUtil {

 

    public static void main(String[] args) {

        //doc2pdf("C:/Users/lss/Desktop/test.doc");
    	doc2pdf("src/格式2.doc","src/out");
    }

 


 

    public static void doc2pdf(String inPath, String outPath) {

  

        try {

            long old = System.currentTimeMillis();

            File file = new File(outPath); // 新建一个空白pdf文档

            FileOutputStream os = new FileOutputStream(file);

            Document doc = new Document(inPath); // Address是将要被转化的word文档

            doc.save(os, SaveFormat.HTML);// 全面支持DOC, DOCX, OOXML, RTF HTML, OpenDocument, PDF,

                                         // EPUB, XPS, SWF 相互转换

            long now = System.currentTimeMillis();

            System.out.println("共耗时：" + ((now - old) / 1000.0) + "秒"); // 转化用时

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

}
