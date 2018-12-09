package org.hcx.tools.poi.tool.number;

import org.hcx.tools.poi.tool.formater.BulletFormater;
import org.hcx.tools.poi.tool.formater.ChineseLegalNumberFormater;
import org.hcx.tools.poi.tool.formater.DecimalFormatter;
import org.hcx.tools.poi.tool.formater.HEXNumberFormater;
import org.hcx.tools.poi.tool.formater.RomaNumberFormater;

public class NumberFormatTool {
	public static String formatNumber(int format, int num,String numText) {
		int luInfo = 0;
		String ret = null;
		NumberFormater formater = null;
		switch (format) {
			case NumFormat.MSONFCARABIC:
				formater = new DecimalFormatter();
				break;
			case NumFormat.MSONFCLCROMAN:
				luInfo = 1;
			case NumFormat.MSONFCUCROMAN:
				luInfo = 2;
				formater = new RomaNumberFormater();
				break;
			case NumFormat.MSONFCHEX:
				formater=new HEXNumberFormater();
				break;
			case NumFormat.MSONFCBULLET:
				formater=new BulletFormater(numText);
				break;
			case NumFormat.MSONFCCHNDBNUM2:
				formater= new ChineseLegalNumberFormater();
				break;
			default:
				formater = new DecimalFormatter();
				break;

		}
		ret = formater.getChar(num);
		return luInfo == 0 ? ret : (luInfo == 1 ? ret.toLowerCase() : ret.toUpperCase());
	}
}
