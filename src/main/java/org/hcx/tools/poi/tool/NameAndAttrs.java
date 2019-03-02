package org.hcx.tools.poi.tool;

public interface NameAndAttrs {
	interface XMLNodeName {
		static final String	XMLROOTNAME			= "root";
		static final String	XMLSECTIONNAME		= "section";
		static final String	XMLPARANNAME		= "para";
		static final String	XMLTABLENNAME		= "table";
		static final String	XMLROWNNAME			= "row";
		static final String	XMLCOLNNAME			= "col";
		static final String	XMLCHARANNAME		= "char";
		static final String	XMLPICANNAME		= "pic";
		static final String	XMLSHSAPENAME		= "shape";
		static final String	XMLCONTAINERNAME	= "container";
		static final String	XMLPAGEBREAK		= "pagebreak";
		static final String	XMLLINK				= "link";

	}

	interface Location {
		static final String	LOC_TOP		= "top";
		static final String	LOC_BOTTOM	= "bottom";
		static final String	LOC_LEFT	= "left";
		static final String	LOC_RIGHT	= "right";
	}

	interface Size {
		static final String	WIDTH	= "width";
		static final String	HEIGHT	= "height";
	}

	interface Attribute {
		static final String	ALIGN					= "align";
		static final String	V_ALIGN					= "valign";
		static final String	FONT_ALIGN				= "fontAlign";
		static final String	FIRST_LINE_INDENT		= "firstLineIndent";
		static final String	INDENT_LEFT				= "indentLeft";
		static final String	INDENT_RIGHT			= "indentRight";
		static final String	INDENT_TOP				= "indentTop";
		static final String	INDENT_BOTTOM			= "indentBottom";
		static final String	SPACE_BETWEEN_LINES		= "lineSpace";

		static final String	COLUMN_CNT				= "colcnt";
		static final String	COLUMN_DIST				= "colDist";

		static final String	CELL_WIDTH				= "cellSizes";

		static final String	BOLD					= "bold";
		static final String	CAPITALIZED				= "capitalized";
		static final String	DOUBLE_STRIKE_THROUGH	= "doubleStrikeThrogh";
		static final String	EMBOSSED				= "embossed";
		static final String	HIGHLIGHTED				= "highlited";
		static final String	COLOR					= "color";
		static final String	ChARACTERSPACING		= "charspace";
		static final String	FONTFAMILY				= "fontfamily";
		static final String	FONTSIZE				= "fontsize";
		static final String	SUBSCRIPT				= "subscript";
		static final String	TEXTPOSITION			= "textPosition";
		static final String	UNDERLINE				= "underline";
		static final String	SMALLCAPS				= "smallCaps";
		static final String	STRIKETHROUGH			= "strikethrough";
		static final String	IMPRINTED				= "imprinted";
		static final String	ITALIC					= "italic";
		static final String	SHADOWED				= "shadowed";
		static final String	TEXT_DIRECTION			= "textDirection";

	}

}
