package org.hcx.tools.pdf.exec;

import java.io.FileInputStream;
import java.io.IOException;
import org.apache.pdfbox.examples.interactive.form.CreateSimpleFormWithEmbeddedFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class TestPdfBox {
	public static void main(String[] args) {
		try {
//			createTable();
			createImage();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void helloWordpdf() throws IOException {
		PDDocument document = new PDDocument();
		PDPage page = new PDPage();
		document.addPage(page);
		PDFont font = PDType1Font.HELVETICA_BOLD;
		PDPageContentStream contentStream = new PDPageContentStream(document, page);
		contentStream.beginText();
		font = PDType0Font.load(document, new FileInputStream("chinese zodiac tfb.ttf"), false);
		contentStream.setFont(font, 12);
		contentStream.newLineAtOffset(100, 700);
		contentStream.showText("Hello World");
		contentStream.endText();
		contentStream.close();
		document.save("Hello World.pdf");
		document.close();
	}
	private static void createImage() throws IOException {
			PDDocument document = new PDDocument();
		PDPage imagePage = new PDPage();
		document.addPage(imagePage);
		
		PDImageXObject pdImage = PDImageXObject.createFromFile("59d2f.png", document);
		PDPageContentStream contentStream = new PDPageContentStream(document, imagePage);
		contentStream.drawImage(pdImage, 70, 250);
		contentStream.close();
		document.save("image.pdf");
		document.close();
		
	}
	private static void createBlackPdf() throws IOException {
		PDDocument document = new PDDocument();
		PDPage blankPage = new PDPage();
		document.addPage(blankPage);
		document.save("BlankPage.pdf");
		document.close();
	}
	private static void  createTable() throws IOException {
PDDocument doc = new PDDocument();

		PDPage page = new PDPage();

		doc.addPage( page );

		PDPageContentStream contentStream =

		new PDPageContentStream(doc, page);

		String[][] content = {{"a","b","1"},

		{"c","d","2"},

		{"e","f","3"},

		{"g","h","4"},

		{"i","j","5"}} ;

		drawTable(page, contentStream, 700,100, content);

		contentStream.close();

		doc.save("test.pdf");
		
		
	}

	public static void drawTable(PDPage page, PDPageContentStream contentStream, float y, float margin,
			String[][] content) throws IOException {
		final int rows = content.length;
		final int cols = content[0].length;
		final float rowHeight = 20f;
		final float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
		final float tableHeight = rowHeight * rows;
		final float colWidth = tableWidth / (float) cols;
		final float cellMargin = 5f;
		float nexty = y;
		for (int i = 0; i <= rows; i++) {
			contentStream.drawLine(margin, nexty, margin + tableWidth, nexty);
			nexty -= rowHeight;
		}
		float nextx = margin;
		for (int i = 0; i <= cols; i++) {
			contentStream.drawLine(nextx, y, nextx, y - tableHeight);
			nextx += colWidth;
		}
		contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
		float textx = margin + cellMargin;
		float texty = y - 15;
		for (int i = 0; i < content.length; i++) {
			for (int j = 0; j < content[i].length; j++) {
				String text = content[i][j];
				contentStream.beginText();
				contentStream.newLineAtOffset(textx, texty);
				contentStream.showText(text);
				contentStream.endText();
				textx += colWidth;
			}
			texty -= rowHeight;
			textx = margin + cellMargin;
		}
	}
}