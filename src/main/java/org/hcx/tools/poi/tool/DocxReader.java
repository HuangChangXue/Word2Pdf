package org.hcx.tools.poi.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.dom4j.DocumentHelper;

public class DocxReader {
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
		Iterator<XWPFParagraph> paraits = doc.getParagraphsIterator();
		while (paraits.hasNext()) {
			XWPFParagraph para = paraits.next();
			
			System.out.println(para.getText());
		}
		return document;
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
	}
}
