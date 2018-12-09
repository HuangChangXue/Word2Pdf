package org.hcx.tools.poi.tool.formater;

import org.hcx.tools.poi.tool.number.NumberFormater;

public class BulletFormater implements NumberFormater {
	private String numTxt = null;

	public BulletFormater(String numText) {
		this.numTxt = numText;
	}

	public String getChar(int number) {
		String ret = "";
		switch (numTxt.charAt(0)) {
			case '\uF06E':
				ret = "\u25A0";
				break;
			case '\uF075':
				ret = "\u25C6";
				break;
			case '\uF06C':
				ret = "\u25CF";
				break;
			case '\uF0FC':
				ret = "\u221A";
				break;
			case '\uF0D8':
				break;
			case '\uF0B2':
				break;

		}
		return ret;
	}

}
