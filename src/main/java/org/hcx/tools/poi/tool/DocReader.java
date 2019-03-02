package org.hcx.tools.poi.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.Colorref;
import org.apache.poi.hwpf.model.FontTable;
import org.apache.poi.hwpf.model.ListTables;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.model.StyleSheet;
import org.apache.poi.hwpf.model.TextPiece;
import org.apache.poi.hwpf.model.TextPieceTable;
import org.apache.poi.hwpf.usermodel.BorderCode;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.HWPFList;
import org.apache.poi.hwpf.usermodel.ObjectsPool;
import org.apache.poi.hwpf.usermodel.OfficeDrawing;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Section;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.hcx.tools.poi.tool.number.NumberFormatTool;

public class DocReader implements NameAndAttrs {

	private Map<Integer, int[]> listValues = new HashMap<Integer, int[]>();

	private int[] getListValue(int list) {
		int[] ret = null;
		if (listValues.containsKey(list)) {
			ret = listValues.get(list);
		} else {
			ret = new int[9];
			listValues.put(list, ret);
		}
		return ret;
	}

	boolean			appendAttr	= true;
	boolean			isDebug		= true;
	HWPFDocument	doc			= null;
	File			docfile		= null;
	private String	picOut		= null;

	public String getPicOut() {
		return picOut;
	}

	public void setPicOut(String picOut) {
		this.picOut = picOut;
	}

	private void open() {
		java.io.InputStream is = null;
		try {
			is = new FileInputStream(docfile);
			HWPFDocument doc = new HWPFDocument(is);
			this.doc = doc;

		} catch (Exception e) {
		}
	}

	private void close() {
		try {
			doc.close();
		} catch (IOException e) {
		}
	}

	public DocReader(File docFile) {

		this.docfile = docFile;
		open();
	}

	// public DocReader(HWPFDocument doc) {
	// this.doc = doc;
	// }

	Range						range			= null;
	java.util.Stack<Section>	currentSection	= new java.util.Stack<Section>();
	java.util.Stack<Table>		currentTable	= new java.util.Stack<Table>();

	private Table currentTable() {
		try {
			return this.currentTable.get(this.currentTable.size() - 1);
		} catch (Exception e) {
			return null;
		}
	}

	private void pushTable(Table table) {
		// System.out.printf("Table[%d] pushed\n", table.getTableLevel());
		this.currentTable.push(table);
	}

	private Table popTable() {
		Table table = this.currentTable.pop();
		// System.out.printf("Table[%d] Poped\n", table.getTableLevel());
		return table;
	}

	Map<Section, Integer> sectionParaIdx = new HashMap<Section, Integer>();

	private void pushSection(Section sec) {
		this.currentSection.push(sec);
		this.sectionParaIdx.put(sec, 0);
	}

	private void addParaCnt() {
		Section sce = this.currentSection();
		int cnt = this.sectionParaIdx.get(sce);
		this.sectionParaIdx.put(sce, cnt + 1);
	}

	private void addParaCnt(int i) {
		Section sce = this.currentSection();
		int cnt = this.sectionParaIdx.get(sce);
		this.sectionParaIdx.put(sce, cnt + i);
	}

	// private void subtractParaCnt() {
	// Section sce = this.currentSection();
	// int cnt = this.sectionParaIdx.get(sce);
	// this.sectionParaIdx.put(sce, cnt - 1);
	// }

	private PicturesTable pTable = null;

	private int sectionParaCnt() {
		Section sec = this.currentSection();
		return this.sectionParaIdx.get(sec);
	}

	private Section popSection() {
		return this.currentSection.pop();
	}

	private Section currentSection() {
		return this.currentSection.get(this.currentSection.size() - 1);
	}

	private void addMetaData(org.dom4j.Element e) {
		e.addCDATA("Authur: HCX");
	}

	FontTable		fontTable	= null;
	StyleSheet		ssh			= null;
	List<TextPiece>	txtpiecs	= null;

	public org.dom4j.Document processDocument() {
		org.dom4j.Document document = DocumentHelper.createDocument();

		ssh = doc.getStyleSheet();
		fontTable = doc.getFontTable();
		range = doc.getRange();

		this.pTable = doc.getPicturesTable();
		TextPieceTable txtTable = doc.getTextTable();

		txtpiecs = txtTable.getTextPieces();

		// for (int i = 0; i < 1000; ++i) {
		// OfficeDrawing draw = drawings.getOfficeDrawingAt(i);
		// if (draw != null) {
		// System.out.println(i);
		// }
		// }
		// for (OfficeDrawing draw : doc.getOfficeDrawingsMain().getOfficeDrawings()) {
		// System.out.println(draw);
		// }
		org.dom4j.Element root = document.addElement(XMLNodeName.XMLROOTNAME);

		addMetaData(root);
		int secCnt = range.numSections();
		for (int i = 0; i < secCnt; ++i) {
			Section section = range.getSection(i);
			this.pushSection(section);
			processSection(section, root);
			this.popSection();
		}
		close();
		return document;

	}

	private void processSection(Section sec, org.dom4j.Element parent) {
		int paraCnt = sec.numParagraphs();
		org.dom4j.Element element = null;
		if (paraCnt <= 0) {
			return;
		}
		element = parent.addElement(XMLNodeName.XMLSECTIONNAME);
		if (appendAttr) {
			int marginLeft = sec.getMarginLeft(), marginRight = sec.getMarginRight(), marginTop = sec.getMarginTop(),
					marginButtom = sec.getMarginBottom(), columCnt = sec.getNumColumns(),
					columSpace = sec.getDistanceBetweenColumns(), pageHight = sec.getPageHeight(),
					pageWidth = sec.getPageWidth();
			boolean colEvenlySpaced = sec.isColumnsEvenlySpaced();
			element.addAttribute(Attribute.INDENT_LEFT, marginLeft + "");
			element.addAttribute(Attribute.INDENT_RIGHT, marginRight + "");
			element.addAttribute(Attribute.INDENT_TOP, marginTop + "");
			element.addAttribute(Attribute.INDENT_BOTTOM, marginButtom + "");
			element.addAttribute(Attribute.COLUMN_CNT, columCnt + "");
			element.addAttribute(Attribute.COLUMN_DIST, columSpace + "");
			element.addAttribute(Size.WIDTH, pageWidth + "");
			element.addAttribute(Size.HEIGHT, pageHight + "");
			element.addAttribute("colEvenlySpaced", colEvenlySpaced + "");
		}
		for (int i = 0; i < paraCnt; i = sectionParaCnt()) {
			Paragraph para = sec.getParagraph(i);
			processParagraph(para, element);
		}

	}

	private void processTable(Table table, org.dom4j.Element parent) {
		this.pushTable(table);
		int rowNum = table.numRows();
		org.dom4j.Element ele = parent.addElement(XMLNodeName.XMLTABLENNAME);

		for (int i = 0; i < rowNum; ++i) {
			TableRow row = table.getRow(i);
			org.dom4j.Element rowE = ele.addElement(XMLNodeName.XMLROWNNAME);
			int colCnt = row.numCells();
			StringBuilder colWidth = new StringBuilder();
			for (int j = 0; j < colCnt; ++j) {
				TableCell cell = row.getCell(j);
				if (this.appendAttr) {
					colWidth.append(cell.getWidth()).append(",");
					if (j == colCnt - 1) {
						rowE.addAttribute(Attribute.CELL_WIDTH, colWidth.toString());
					}
				}
				org.dom4j.Element cellE = rowE.addElement(XMLNodeName.XMLCOLNNAME);
				int secNum = cell.numSections();
				// 处理单元格内的段落
				for (int m = 0; m < secNum; ++m) {
					Section sec = cell.getSection(m);
					this.pushSection(sec);
					int paranum = sec.numParagraphs();
					for (int k = 0; k < paranum; k = sectionParaCnt()) {
						Paragraph para = sec.getParagraph(k);
						// System.out.printf("Table[%d] row[%d] col[%d] sec[%d] para[%d]
						// text[%s]\n",this.currentTable().getTableLevel(), i, j, m, k,
						// para.text().trim());
						if (para.isInList()) {
							processList(para, cellE);
						} else {
							this.processParagraph(para, cellE);
						}
					}
					int currIdx = sectionParaCnt();
					this.popSection();
					addParaCnt(currIdx);
				}
				appendCellAttrs(cell, cellE);
			}
			appendRowAttrs(row, rowE);
		}
		appendTableAttrs(table, ele);
		PoiUtil.createTableLayout(ele);
		this.popTable();
	}

	private void appendCellAttrs(TableCell cell, org.dom4j.Element cellE) {
		if (this.appendAttr) {

			cellE.addAttribute(Size.WIDTH, cell.getWidth() + "");
			boolean isMerged = cell.isMerged();
			cellE.addAttribute("merged", isMerged + "");
			boolean isfirstMerged = cell.isFirstMerged();
			boolean isfirstVerticalMerged = cell.isFirstVerticallyMerged();
			cellE.addAttribute("firstMerge", isfirstMerged + "");
			cellE.addAttribute("firstVerticalMerged", isfirstVerticalMerged + "");
			cellE.addAttribute("verticalMerged", cell.isVerticallyMerged() + "");
			cellE.addAttribute("rotateFont", cell.isRotateFont() + "");
			cellE.addAttribute("vertical", cell.isVertical() + "");
			cellE.addAttribute("vAlign", cell.getVertAlign() + "");
			BorderCode topborder = cell.getBrcTop();
			BorderCode bottomborder = cell.getBrcBottom();
			BorderCode leftborder = cell.getBrcLeft();
			BorderCode rightborder = cell.getBrcRight();
			appendBorderInfo(Location.LOC_TOP, topborder, cellE);
			appendBorderInfo(Location.LOC_BOTTOM, bottomborder, cellE);
			appendBorderInfo(Location.LOC_LEFT, leftborder, cellE);
			appendBorderInfo(Location.LOC_RIGHT, rightborder, cellE);

		}
	}

	private void appendRowAttrs(TableRow row, org.dom4j.Element rowE) {
		if (this.appendAttr) {
			rowE.addAttribute("cols", row.numCells() + "");
			int justfication = row.getRowJustification();
			int gapHalf = row.getGapHalf();
			int rowHeight = row.getRowHeight();
			rowE.addAttribute("height", rowHeight + "");
			rowE.addAttribute("justfaction", justfication + "");
			rowE.addAttribute("gapHalf", gapHalf + "");
			BorderCode topborder = row.getTopBorder();
			BorderCode bottomborder = row.getBottomBorder();
			BorderCode leftborder = row.getLeftBorder();
			BorderCode rightborder = row.getRightBorder();
			appendBorderInfo(Location.LOC_TOP, topborder, rowE);
			appendBorderInfo(Location.LOC_BOTTOM, bottomborder, rowE);
			appendBorderInfo(Location.LOC_LEFT, leftborder, rowE);
			appendBorderInfo(Location.LOC_RIGHT, rightborder, rowE);

		}
	}

	private void appendBorderInfo(String location, BorderCode border, org.dom4j.Element row) {
		if (true)
			return;
		boolean empty = border.isEmpty(), shadow = border.isShadow(), frame = border.isFrame();
		int type = border.getBorderType(), space = border.getSpace(),
				color = Colorref.valueOfIco(border.getColor()).getValue();
		row.addAttribute("border-" + location + "-empty", empty + "");
		row.addAttribute("border-" + location + "-shadow", shadow + "");
		row.addAttribute("border-" + location + "-frame", frame + "");
		row.addAttribute("border-" + location + "-type", type + "");
		row.addAttribute("border-" + location + "-space", space + "");
		row.addAttribute("border-" + location + "-color", String.format("%06X", color));
	}

	private void appendTableAttrs(Table table, org.dom4j.Element ele) {
		if (this.appendAttr) {
			ele.addAttribute("rows", table.numRows() + "");
		}
	}

	private void processList(Paragraph para, org.dom4j.Element parent) {
		this.addParaCnt();

		org.dom4j.Element paraE = parent.addElement(XMLNodeName.XMLPARANNAME);
		this.appendParaAttribute(para, paraE);
		parseListNumberPart(para, paraE);
		int secNum = para.numCharacterRuns();
		for (int i = 0; i < secNum; ++i) {
			i += this.processCharacterRun(para.getCharacterRun(i), paraE);
		}

	};

	private void parseListNumberPart(Paragraph para, org.dom4j.Element parent) {

		HWPFList list = para.getList();
		int ilvl = para.getIlvl();
		int ilfo = para.getIlfo();
		int format = list.getNumberFormat((char) ilvl);
		String numText = list.getNumberText((char) ilvl);
		org.dom4j.Element ele = parent.addElement(XMLNodeName.XMLCHARANNAME);
		this.appendCharacterRunAttributes(para.getCharacterRun(0), ele);
		int[] values = this.getListValue(ilfo);
		values[ilvl]++;
		if (isDebug) {
			ele.addAttribute("format", String.format("%02X", format));
			StringBuilder sb = new StringBuilder();
			for (char i : numText.toCharArray()) {
				sb.append(String.format("U%04X", Integer.valueOf(i)));
			}
			ele.addAttribute("str_unix", sb.toString());
		}
		ele.setText(this.formatNumberText(numText, values, format));
	}

	private String getFormatedNumber(int format, int value, String numText) {
		return NumberFormatTool.formatNumber(format, value, numText);
	}

	private String formatNumberText(String numTxt, int[] values, int format) {

		Pattern p = Pattern.compile("[\\x00-\\x07\\uF06C\\uF075\\uF06E\\uF075\\uF06C\\uF0FC\\uF0D8\\uF0B2]");
		Matcher m = p.matcher(numTxt);
		int count = 0;
		ArrayList<String> valuestr = new ArrayList<String>();
		while (m.find()) {
			valuestr.add(getFormatedNumber(format, values[count++], m.group()));
		}
		String ret = m.replaceAll("%s");
		return String.format(ret, valuestr.toArray());
	}

	private void processParagraph(Paragraph para, org.dom4j.Element parent) {

		if (para.pageBreakBefore()) {
			parent.addElement(XMLNodeName.XMLPAGEBREAK);
		}
		org.dom4j.Element element = null;
		if (para.isInTable()) {

			this.addParaCnt();
			try {
				Table table = this.currentSection().getTable(para);
				element = parent.addElement(XMLNodeName.XMLPARANNAME);
				if (this.currentTable() != null) {
					if (table.getTableLevel() != this.currentTable().getTableLevel()) {// 子表
						this.processTable(table, element);
					} else {
						int numCharaRun = para.numCharacterRuns();
						for (int i = 0; i < numCharaRun; ++i) {
							CharacterRun cr = para.getCharacterRun(i);
							i += processCharacterRun(cr, element);
						}
					}
				} else {
					processTable(table, element);
				}

			} catch (Exception e) {
				if (para.isInList()) {
					this.processList(para, parent);
				} else {
					element = parent.addElement(XMLNodeName.XMLPARANNAME);
					int numCharaRun = para.numCharacterRuns();
					for (int i = 0; i < numCharaRun; ++i) {
						CharacterRun cr = para.getCharacterRun(i);
						i += processCharacterRun(cr, element);
					}
				}
			}

		} else if (para.isInList()) {
			processList(para, parent);
		} else {
			this.addParaCnt();
			element = parent.addElement(XMLNodeName.XMLPARANNAME);
			int numCharaRun = para.numCharacterRuns();
			for (int i = 0; i < numCharaRun; ++i) {
				CharacterRun cr = para.getCharacterRun(i);
				// if (!cr.isSpecialCharacter()) {
				i += processCharacterRun(cr, element);
				// }
			}
		}
		if (element != null) {
			// if (!element.hasContent()) {
			//// element.getParent().remove(element);
			// } else {
			appendParaAttribute(para, element);
			// }
		}
		if (isCreateNewPage) {
			parent.addElement(XMLNodeName.XMLPAGEBREAK);
			this.isCreateNewPage = false;
		}
	}

	boolean isCreateNewPage = false;

	private void appendPictureAttributes(Picture pic, org.dom4j.Element e) {
		if (this.appendAttr) {
			int w = pic.getWidth(), h = pic.getHeight();
			e.addAttribute(Size.WIDTH, w + "");
			e.addAttribute(Size.HEIGHT, h + "");
			e.addAttribute("hScale", pic.getHorizontalScalingFactor() + "");
			e.addAttribute("vScale", pic.getVerticalScalingFactor() + "");
		}
	}

	private int processCharacterRun(CharacterRun crun, org.dom4j.Element parent) {
		// System.out.println(crun.getStartOffset() + " crun ");
		int ret = 0;
		if (pTable.hasPicture(crun)) {
			Picture pic = pTable.extractPicture(crun, false);
			if (!"image/unknown".equalsIgnoreCase(pic.getMimeType())) {
				String afileName = pic.suggestFullFileName();
				File picOutputDir = new File(this.picOut == null ? "." : picOut);
				java.io.OutputStream os = null;
				try {
					File outFile = new File(picOutputDir, afileName);
					os = new java.io.FileOutputStream(outFile);
					pic.writeImageContent(os);
					os.close();
					org.dom4j.Element element = parent.addElement(XMLNodeName.XMLPICANNAME);
					element.addAttribute("src", outFile.getAbsolutePath());
					appendPictureAttributes(pic, element);
					appendCharacterRunAttributes(crun, element);
				} catch (Exception e) {
				} finally {
					try {
						os.close();
					} catch (IOException e) {
					}

				}
			}
			// System.out.println(crun.getStartOffset() + " pic ");
		}

		String s = getString(crun);

		if (!crun.isSpecialCharacter()) {
			// System.out.println(crun.getStartOffset() + " !spec ");
			// if (!s.trim().isEmpty()) {
			String text = crun.text();
			if (text != null && text.charAt(0) == '\f') {
				this.isCreateNewPage = true;
			} else {
				org.dom4j.Element element = parent.addElement(XMLNodeName.XMLCHARANNAME);
				appendCharacterRunAttributes(crun, element);
				element.setText(crun.text());
			}
			// }
		} else {

			org.dom4j.Element element = null;
			// System.out.println(crun.getStartOffset() + " spec ");
			if (s.charAt(0) == 0x08) {// 画图

				OfficeDrawing drawing = doc.getOfficeDrawingsMain().getOfficeDrawingAt(crun.getStartOffset());
				// System.out.println(crun.getStartOffset() + " " + (drawing == null ? "NULL" :
				// "OK"));

				if (drawing != null) {
					System.out.println(drawing);
					org.apache.poi.ddf.EscherContainerRecord containerRecord = drawing.getOfficeArtSpContainer();
					element = parent.addElement(XMLNodeName.XMLCONTAINERNAME);
					String containeXml = containerRecord.toXml("");
					try {
						org.dom4j.Element tmp = DocumentHelper.parseText(containeXml).getRootElement();
						tmp.detach();
						element.add(tmp);
					} catch (DocumentException e) {
						e.printStackTrace();
					}
					appendDrawingAttributes(drawing, element);
					// System.out.println(containerRecord);
				} else {

				}
			} else if (s.charAt(0) == 0x13) {// FieldStart
				element = parent.addElement(XMLNodeName.XMLCHARANNAME);
				element.addAttribute("type", "FieldStart");
				element.addAttribute("skiped", "FieldNameCharacterRun");
				ret++;
			} else if (s.charAt(0) == 0x15) {// FieldEnd
				element = parent.addElement(XMLNodeName.XMLCHARANNAME);
				element.addAttribute("type", "FieldEnd");
			} else if (s.charAt(0) == 0x14) {// FieldEnd
				element = parent.addElement(XMLNodeName.XMLCHARANNAME);
				element.addAttribute("type", "FIELD_SEPARATOR_MARK");
			} else {
				element = parent.addElement(XMLNodeName.XMLCHARANNAME);
				element.addAttribute("type", "unknown");
			}
			appendCharacterRunAttributes(crun, element);
			StringBuilder sb = new StringBuilder();
			for (char a : s.toCharArray()) {
				sb.append(String.format("\\u%02X", Integer.valueOf(a)));
			}
			if (element != null) {
				element.addAttribute("TEXT_HEX", sb.toString());
				element.addAttribute("startOffset", String.format("%d", crun.getStartOffset()));
				element.addAttribute("endOffset", String.format("%d", crun.getEndOffset()));
			}
		}
		return ret;
	}

	private void appendDrawingAttributes(OfficeDrawing draw, org.dom4j.Element element) {
		if (element != null && draw != null) {

			element.addAttribute("shapeId", String.format("%08X", draw.getShapeId()));// (draw.getShapeId()>>1)-0x1ca));
			int top = draw.getRectangleTop(), left = draw.getRectangleLeft(), right = draw.getRectangleRight(),
					buttom = draw.getRectangleBottom();
			element.addAttribute("top", top + "");
			element.addAttribute("left", left + "");
			element.addAttribute("buttom", buttom + "");
			element.addAttribute("right", right + "");
			OfficeDrawing.HorizontalPositioning hposition = draw.getHorizontalPositioning();
			OfficeDrawing.VerticalPositioning vposition = draw.getVerticalPositioning();
			OfficeDrawing.HorizontalRelativeElement hrelative = draw.getHorizontalRelative();
			OfficeDrawing.VerticalRelativeElement vrelative = draw.getVerticalRelativeElement();
			element.addAttribute("hposition", hposition.name());
			element.addAttribute("vposition", vposition.name());
			element.addAttribute("hrelative", hrelative.name());
			element.addAttribute("vrelative", vrelative.name());
		}
	}

	private void appendCharacterRunAttributes(CharacterRun crun, org.dom4j.Element element) {
		if (appendAttr && element != null) {
			element.addAttribute("colorIdx", String.format("%X", crun.getColor()));
			element.addAttribute("color", String.format("%06X", crun.getIco24()));
			element.addAttribute("underline", crun.getUnderlineCode() + "");
			element.addAttribute("strike", crun.isStrikeThrough() + "");
			element.addAttribute("hightlighted", crun.isHighlighted() + "");
			element.addAttribute("highlight-color",
					String.format("%06X", Colorref.valueOfIco(crun.getHighlightedColor()).getValue()));
			element.addAttribute("isItalic", crun.isItalic() + "");
			element.addAttribute("isBold", crun.isBold() + "");
			element.addAttribute("font-name", crun.getFontName());
			element.addAttribute("font-size", crun.getFontSize() + "");
			element.addAttribute("character-spacing", crun.getCharacterSpacing() + "");
			element.addAttribute("subsuperIdx", crun.getSubSuperScriptIndex() + "");
			element.addAttribute("hidden", crun.isVanished() + "");
		}
	}

	private void appendParaAttribute(Paragraph para, org.dom4j.Element element) {
		if (appendAttr) {
			int firstLineInddent = para.getFirstLineIndent();
			int fontAlign = para.getFontAlignment();
			int indentLeft = para.getIndentFromLeft();
			int indentRight = para.getIndentFromRight();
			int justification = para.getJustification();
			element.addAttribute("first-indent", firstLineInddent + "");
			element.addAttribute("font-align", fontAlign + "");
			element.addAttribute("indent-left", indentLeft + "");
			element.addAttribute("indent-right", indentRight + "");
			element.addAttribute("justification", justification + "");
			element.addAttribute("space-before", para.getSpacingBefore() + "");
			element.addAttribute("space-after", para.getSpacingAfter() + "");
			element.addAttribute("lineSpace", para.getLineSpacing().toString() + "");

		}
	}

	public static void main(String args[]) throws Exception {
		String path = "src/格式.doc";
		// path="F:\\兴业相关业务数据\\兴业-交易银行部\\附加文件_附件1：兴业银行特约商户条码支付服务协议（以此为准）.doc";
		// path = "src/shape.doc";
		File file = new File(path);
		DocReader reader = new DocReader(file);
		// reader.open();
		org.dom4j.Document document = reader.processDocument();
		reader.close();
		org.dom4j.io.OutputFormat format = org.dom4j.io.OutputFormat.createPrettyPrint();

		format.setEncoding("utf-8");
		java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
		org.dom4j.io.XMLWriter writer = new org.dom4j.io.XMLWriter(os, format);

		writer.write(document);

		writer.flush();

		new java.io.FileOutputStream("out.xml").write(os.toByteArray());
	}

	public static void maina(String args[]) throws Exception {

		File file = new File("src/格式.doc");
		HWPFDocument doc = new HWPFDocument(new FileInputStream(file));
		ListTables listtable = doc.getListTables();
		PicturesTable pictable = doc.getPicturesTable();
		StyleSheet sst = doc.getStyleSheet();
		Range range = doc.getRange();
		// System.out.println("Sections:" + range.numSections());
		// System.out.println("Paragraphics:" + range.getSection(0).numParagraphs());
		ObjectsPool objpool = doc.getObjectsPool();
		for (int i = 0; i < range.numParagraphs(); ++i) {
			Paragraph para = range.getParagraph(i);
			int align = para.getJustification();

			System.out.println("Align:" + align);
			System.out.println(String.format("段落前间距： %s  后间距: %s 行间距:%s 首行缩进：%d 表格中： %b  列表中:%b 缩进：%d ",
					para.getSpacingBefore(), para.getSpacingAfter(), para.getLineSpacing(), para.getFirstLineIndent(),
					para.isInTable(), para.isInList(), para.getIndentFromLeft()));
			if (para.isInList()) {
				int ilfo = para.getIlfo();
				;
			}
			for (int j = 0; j < para.numCharacterRuns(); ++j) {
				CharacterRun crun = para.getCharacterRun(j);
				if (!crun.isSpecialCharacter()) {
					String s = getString(crun);
					if (!s.trim().isEmpty()) {
						System.out.println("颜色:" + crun.getColor() + " " + String.format("%06X", crun.getIco24())
								+ "  下划线:" + crun.getUnderlineCode() + " 删除线:" + crun.isStrikeThrough() + " 强调:"
								+ crun.isHighlighted() + "  强调色："
								+ String.format("%06X", Colorref.valueOfIco(crun.getHighlightedColor()).getValue())
								+ " 倾斜:" + crun.isItalic() + "  加粗:" + crun.isBold() + " 底纹:" + crun.isShadowed());
						System.out.println("字体" + crun.getFontName() + " 大小:" + crun.getFontSize() + "  字符间距"
								+ crun.getCharacterSpacing() + " 下标" + crun.getSubSuperScriptIndex() + " 隐藏："
								+ crun.isVanished());
						System.out.println(s);
					}
				} else {
				}
			}
		}

	}

	public static String getString(CharacterRun crun) {
		return crun.text();
	}
}
