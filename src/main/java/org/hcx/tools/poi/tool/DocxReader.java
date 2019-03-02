package org.hcx.tools.poi.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ICell;
import org.apache.poi.xwpf.usermodel.IRunElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFieldRun;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFSDT;
import org.apache.poi.xwpf.usermodel.XWPFSDTCell;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTrPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVerticalJc;

public class DocxReader implements NameAndAttrs {
	private File docxfile = null;

	public DocxReader(File file) {
		docxfile = file;
		open();
	}

	XWPFDocument doc = null;

	private void open() {
		FileInputStream inputStream = null;//
		try {
			inputStream = new FileInputStream(docxfile);
			doc = new XWPFDocument(inputStream);
		} catch (Exception e) {
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
			}
		}

	}

	public org.dom4j.Document processDocument() {
		org.dom4j.Document document = DocumentHelper.createDocument();
		org.dom4j.Element root = document.addElement(XMLNodeName.XMLROOTNAME);
		List<IBodyElement> bodyElements = doc.getBodyElements();
		System.out.println("w:" + doc.getDocument().getBody().getSectPr().getPgSz().getW() + " h:"
				+ doc.getDocument().getBody().getSectPr().getPgSz().getH());
		for (IBodyElement element : bodyElements) {
			// XWPFParagraph, XWPFSDT, XWPFTable
			if (element instanceof XWPFParagraph) {
				processParagraph((XWPFParagraph) element, root);
			} else if (element instanceof XWPFTable) {
				XWPFTable table = (XWPFTable) element;
				processTable(table, root);
			} else if (element instanceof XWPFSDT) {
				XWPFSDT sdt = (XWPFSDT) element;
				processSDT(sdt, root);
			}
		}
		return document;
	}

	private void processSDT(XWPFSDT sdt, Element parent) {

	}

	private void processTable(XWPFTable table, Element parent) {
		org.dom4j.Element te = parent.addElement(XMLNodeName.XMLTABLENNAME);
		int rowcnt = table.getNumberOfRows();
		for (int i = 0; i < rowcnt; i++) {
			XWPFTableRow row = table.getRow(i);
			System.out.println("Row" + i);
			processTableRow(row, te);
		}
	}

	private void processTableRow(XWPFTableRow row, org.dom4j.Element parent) {
		org.dom4j.Element ce = parent.addElement(XMLNodeName.XMLROWNNAME);
		List<ICell> cells = row.getTableICells();
		appendRowAttrs(row, ce);
		ce.addAttribute("cols", cells.size() + "");
		for (int i = 0; i < cells.size(); i++) {
			ICell cell = cells.get(i);
			System.out.println(" Cell" + i);
			processCell(cell, ce);
		}
	}

	private void appendRowAttrs(XWPFTableRow row, org.dom4j.Element e) {
		e.addAttribute(Size.HEIGHT, row.getHeight() + "");
		// CTRow ctRow=row.getCtRow();
		// CTTrPr cttrpr = ctRow.getTrPr();
		// System.out.println(cttrpr);
	}

	private void processCell(ICell cell, org.dom4j.Element parent) {
		parent = parent.addElement(XMLNodeName.XMLCOLNNAME);
		if (cell instanceof XWPFSDTCell) {
			processSDTCell((XWPFSDTCell) cell, parent);
		} else {
			processTableCell((XWPFTableCell) cell, parent);
		}
	}

	private void appendCellAttr(XWPFTableCell cell, Element cellE) {
		CTTc cttc = cell.getCTTc();

		CTTcPr cttcpr = cttc.getTcPr();
		cellE.addAttribute(Size.WIDTH, cttcpr.getTcW().getW() + "");
		CTTcBorders border = cttcpr.getTcBorders();
		if (border != null) {
			appendBorderInfo(border, cellE);
		}
		if (cttcpr.getTextDirection() != null) {
			cellE.addAttribute(Attribute.TEXT_DIRECTION, cttcpr.getTextDirection().getVal().toString());
		}
		CTHMerge hmerge = cttcpr.getHMerge();
		if (hmerge != null) {
			if (hmerge.isSetVal()) {
				cellE.addAttribute("hmerge", hmerge.getVal().toString());
			} else {
				cellE.addAttribute("hmerge", "");
			}
		}
		CTVMerge vmerge = cttcpr.getVMerge();
		if (vmerge != null) {
			if (vmerge.isSetVal()) {
				cellE.addAttribute("vmerge", vmerge.getVal().toString());
			} else {
				cellE.addAttribute("vmerge", "");
			}
		}
		CTVerticalJc valign = cttcpr.getVAlign();

		if (valign != null) {
			cellE.addAttribute(Attribute.V_ALIGN, valign.getVal().toString());
		}
	}

	private void appendBorderInfo(CTTcBorders border, Element cellE) {
		CTBorder top = border.getTop();
		System.out.println(top.getColor());
		System.out.println(top);

	}

	private void processTableCell(XWPFTableCell cell, Element cellE) {
		appendCellAttr(cell, cellE);
		List<IBodyElement> bodyElements = cell.getBodyElements();
		for (IBodyElement element : bodyElements) {
			if (element instanceof XWPFParagraph) {
				processParagraph((XWPFParagraph) element, cellE);
			} else if (element instanceof XWPFTable) {
				XWPFTable table = (XWPFTable) element;
				processTable(table, cellE);
			} else if (element instanceof XWPFSDT) {
				XWPFSDT sdt = (XWPFSDT) element;
				processSDT(sdt, cellE);
			}
		}
	}

	private void processSDTCell(XWPFSDTCell cell, org.dom4j.Element parent) {

	}

	private void appendParaAttrs(XWPFParagraph para, org.dom4j.Element e) {
		this.appendBorderInfo(para.getBorderTop(), Location.LOC_TOP, e);
		this.appendBorderInfo(para.getBorderBottom(), Location.LOC_BOTTOM, e);
		this.appendBorderInfo(para.getBorderLeft(), Location.LOC_LEFT, e);
		this.appendBorderInfo(para.getBorderRight(), Location.LOC_RIGHT, e);
		e.addAttribute(Attribute.ALIGN, "" + para.getAlignment());
		e.addAttribute(Attribute.V_ALIGN, "" + para.getVerticalAlignment());
		e.addAttribute(Attribute.FIRST_LINE_INDENT, "" + para.getFirstLineIndent());
		e.addAttribute(Attribute.FONT_ALIGN, "" + para.getFontAlignment());
		e.addAttribute(Attribute.INDENT_LEFT, "" + para.getIndentationLeft());
		e.addAttribute(Attribute.INDENT_RIGHT, "" + para.getIndentationRight());
		e.addAttribute(Attribute.INDENT_TOP, "" + para.getSpacingBefore());
		e.addAttribute(Attribute.INDENT_BOTTOM, "" + para.getSpacingAfter());
		e.addAttribute(Attribute.SPACE_BETWEEN_LINES, "" + para.getSpacingBetween());
		
		CTPPr pr = para.getCTP().getPPr();
		if(pr!=null) {
		}

	}

	private void appendBorderInfo(Borders border, String location, org.dom4j.Element e) {
		e.addAttribute("border-" + location + "-name", border.toString());
		e.addAttribute("border-" + location + "-value", "" + border.getValue());
	}

	private void processParagraph(XWPFParagraph para, org.dom4j.Element parent) {
		CTSectPr ctSectPr = null;
		if (para.getCTP().getPPr() != null) {
			try {
				if (para.getCTP().getPPr().isSetSectPr())
					ctSectPr = para.getCTP().getPPr().getSectPr();
				if (ctSectPr != null) {
					CTPageSz pgsz = ctSectPr.getPgSz();
					System.out.println("w:" + pgsz.getW() + " h:" + pgsz.getH());
					org.dom4j.Element sec = parent.addElement(XMLNodeName.XMLSECTIONNAME);
					sec.addAttribute(Size.WIDTH, pgsz.getW() + "");
					sec.addAttribute(Size.HEIGHT, pgsz.getH() + "");
				}
			} catch (Exception e) {
			}

		}

		List<IRunElement> runs = para.getIRuns();
		org.dom4j.Element parae = parent.addElement(XMLNodeName.XMLPARANNAME);
		appendParaAttrs(para, parae);
		for (IRunElement run : runs) {
			// XWPFFieldRun, XWPFHyperlinkRun, XWPFRun, XWPFSDT
			if (run instanceof XWPFFieldRun) {
				XWPFFieldRun fieldRun = (XWPFFieldRun) run;
				processFieldRun(fieldRun, parae);
			} else if (run instanceof XWPFHyperlinkRun) {
				XWPFHyperlinkRun linkRun = (XWPFHyperlinkRun) run;
				processLinkRun(linkRun, parae);
			} else if (run instanceof XWPFRun) {
				XWPFRun xwpfRun = (XWPFRun) run;
				processRun(xwpfRun, parae);
			} else if (run instanceof XWPFSDT) {
				XWPFSDT sdt = (XWPFSDT) run;
				processSDT(sdt, parae);
			}

		}
	}

	private void processFieldRun(XWPFFieldRun fieldRun, Element parent) {
		// TODO Auto-generated method stub

	}

	private void processLinkRun(XWPFHyperlinkRun linkRun, Element parent) {
		// TODO Auto-generated method stub
		org.dom4j.Element rune = parent.addElement(XMLNodeName.XMLLINK);
		appendRunAttr(linkRun, rune);
		rune.addAttribute("href", linkRun.getHyperlink(doc).getURL());
	}

	private void processRun(XWPFRun xwpfRun, Element parent) {
		org.dom4j.Element rune = parent.addElement(XMLNodeName.XMLCHARANNAME);
		appendRunAttr(xwpfRun, rune);
	}

	private void appendRunAttr(XWPFRun run, Element e) {
		e.addAttribute(Attribute.BOLD, run.isBold() + "");
		e.addAttribute(Attribute.CAPITALIZED, run.isCapitalized() + "");
		e.addAttribute(Attribute.DOUBLE_STRIKE_THROUGH, run.isDoubleStrikeThrough() + "");
		e.addAttribute(Attribute.EMBOSSED, run.isEmbossed() + "");
		e.addAttribute(Attribute.HIGHLIGHTED, run.isHighlighted() + "");
		e.addAttribute(Attribute.COLOR, run.getColor() + "");
		e.addAttribute(Attribute.ChARACTERSPACING, run.getCharacterSpacing() + "");
		e.addAttribute(Attribute.FONTFAMILY, run.getFontFamily() + "");
		e.addAttribute(Attribute.FONTSIZE, run.getFontSize() + "");
		e.addAttribute(Attribute.SUBSCRIPT, run.getSubscript() + "");
		e.addAttribute(Attribute.TEXTPOSITION, run.getTextPosition() + "");
		e.addAttribute(Attribute.UNDERLINE, run.getUnderline().name() + "");
		e.addAttribute(Attribute.IMPRINTED, run.isImprinted() + "");
		e.addAttribute(Attribute.ITALIC, run.isItalic() + "");
		e.addAttribute(Attribute.SHADOWED, run.isShadowed() + "");
		e.addAttribute(Attribute.SMALLCAPS, run.isSmallCaps() + "");
		e.addAttribute(Attribute.STRIKETHROUGH, run.isStrikeThrough() + "");
		e.setText(run.text());

	}

	public static void main(String[] args) throws Exception {
		File f = new File("./src/word2.docx");
		org.dom4j.Document document = new DocxReader(f).processDocument();
		org.dom4j.io.OutputFormat format = org.dom4j.io.OutputFormat.createPrettyPrint();
		format.setEncoding("utf-8");
		java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
		org.dom4j.io.XMLWriter writer = new org.dom4j.io.XMLWriter(os, format);
		writer.write(document);
		writer.flush();
		System.out.println(os.toString("utf-8"));
	}
}
