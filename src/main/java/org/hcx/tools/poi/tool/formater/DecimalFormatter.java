package org.hcx.tools.poi.tool.formater;

import org.hcx.tools.poi.tool.number.NumberFormater;

public class DecimalFormatter implements NumberFormater {

	public String getChar(int number) {
		return String.format("%d", number);
	}

}
