package org.hcx.tools.poi.tool.formater;

import org.hcx.tools.poi.tool.number.NumberFormater;

public class RomaNumberFormater implements  NumberFormater{


	private String [] digit={"","Ⅰ","Ⅱ","Ⅲ","Ⅳ","Ⅴ","Ⅵ","Ⅶ","Ⅷ","Ⅸ"};
	private String [] tens={"","Ⅹ","ⅩⅩ","ⅩⅩⅩ","ⅩⅬ","Ⅼ","ⅬⅩ","ⅬⅩⅩ","ⅬⅩⅩⅩ","ⅩⅭ"};
	private String [] hundr={"","Ⅽ","ⅭⅭ","ⅭⅭⅭ","ⅭⅮ","Ⅾ","ⅮⅭ","ⅮⅭⅭ","ⅮⅭⅭⅭ","ⅭⅯ"};
	private String [] thoou={"","Ⅿ","ⅯⅯ","ⅯⅯⅯ"};
	public String getChar(int number) {
		if(number>0&&number<4000){
			String res= thoou[number/1000]+hundr[number%1000/100]+tens[number%100/10]+digit[number%10];
			return res;
		}
		return number+"";
	}

	public static void  main(String [] args) {
		System.out.println(new RomaNumberFormater().getChar(10));
	}
}
