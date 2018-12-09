package org.hcx.tools.poi.exec;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.poi.hwpf.converter.WordToHtmlConverter;


/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
//    	try {
//			WordToHtmlConverter.main(new String[] {"src/格式2.doc","src/out.html"});
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (ParserConfigurationException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (TransformerException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//    	if(true)return ;
    	Locale.setDefault(Locale.CHINA);
        System.out.println( "Hello World!" );
        try {
//			new TrueTypeFont("simfang.ttf", null, 0, true);
//			new TrueTypeFont("simfang.ttf", null, 0, true);
//        	TrueTypeFont font=	new TrueTypeFont("SIMSUN.TTC", null, 0, true);
        	TrueTypeFont font=	new TrueTypeFont("src/chinese zodiac tfb.ttf");
//        	if(font.directoryCount>1) {
//        		for( int i =1;i<font.directoryCount;++i) {
//        			font.init(i);
//        		}
//        	}
//			new TrueTypeFont("仿宋.ttf", null, 0, true);
//        	if(true)return;
        	File dir=new File(".");
        	File[] ttcs=dir.listFiles(new FilenameFilter() {

				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".ttc");
				}});
        	
        	for(File f:ttcs) {
        		new TrueTypeFont(f.getAbsolutePath());
        		
        	}
        	
		} catch (FontFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
