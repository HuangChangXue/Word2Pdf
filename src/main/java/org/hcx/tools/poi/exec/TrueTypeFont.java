/*
 * Copyright (c) 2003, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.hcx.tools.poi.exec;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Locale;

import sun.font.FontUtilities;

/**
 * TrueTypeFont is not called SFntFont because it is not expected to handle all
 * types that may be housed in a such a font file. If additional types are
 * supported later, it may make sense to create an SFnt superclass. Eg to handle
 * sfnt-housed postscript fonts. OpenType fonts are handled by this class, and
 * possibly should be represented by a subclass. An instance stores some
 * information from the font file to faciliate faster access. File size, the
 * table directory and the names of the font are the most important of these. It
 * amounts to approx 400 bytes for a typical font. Systems with mutiple locales
 * sometimes have up to 400 font files, and an app which loads all font files
 * would need around 160Kbytes. So storing any more info than this would be
 * expensive.
 */
public class TrueTypeFont {
	protected boolean useJavaRasterizer = true;
	protected int fileSize = 0, style;
	private String platName = null, fullName, familyName;
	/* -- Tags for required TrueType tables */
	public static final int cmapTag = 0x636D6170; // 'cmap'
	public static final int glyfTag = 0x676C7966; // 'glyf'
	public static final int headTag = 0x68656164; // 'head'
	public static final int hheaTag = 0x68686561; // 'hhea'
	public static final int hmtxTag = 0x686D7478; // 'hmtx'
	public static final int locaTag = 0x6C6F6361; // 'loca'
	public static final int maxpTag = 0x6D617870; // 'maxp'
	public static final int nameTag = 0x6E616D65; // 'name'
	public static final int postTag = 0x706F7374; // 'post'
	public static final int os_2Tag = 0x4F532F32; // 'OS/2'

	/* -- Tags for opentype related tables */
	public static final int GDEFTag = 0x47444546; // 'GDEF'
	public static final int GPOSTag = 0x47504F53; // 'GPOS'
	public static final int GSUBTag = 0x47535542; // 'GSUB'
	public static final int mortTag = 0x6D6F7274; // 'mort'

	/* -- Tags for non-standard tables */
	public static final int fdscTag = 0x66647363; // 'fdsc' - gxFont descriptor
	public static final int fvarTag = 0x66766172; // 'fvar' - gxFont variations
	public static final int featTag = 0x66656174; // 'feat' - layout features
	public static final int EBLCTag = 0x45424C43; // 'EBLC' - embedded bitmaps
	public static final int gaspTag = 0x67617370; // 'gasp' - hint/smooth sizes

	/* -- Other tags */
	public static final int ttcfTag = 0x74746366; // 'ttcf' - TTC file
	public static final int v1ttTag = 0x00010000; // 'v1tt' - Version 1 TT font
	public static final int trueTag = 0x74727565; // 'true' - Version 2 TT font
	public static final int ottoTag = 0x4f54544f; // 'otto' - OpenType font

	/* -- ID's used in the 'name' table */
	public static final int MS_PLATFORM_ID = 3;
	/* MS locale id for US English is the "default" */
	public static final short ENGLISH_LOCALE_ID = 0x0409; // 1033 decimal
	public static final int FAMILY_NAME_ID = 1;
	// public static final int STYLE_WEIGHT_ID = 2; // currently unused.
	public static final int FULL_NAME_ID = 4;
	public static final int POSTSCRIPT_NAME_ID = 6;

	private static final short US_LCID = 0x0409; // US English - default

	private static Map<String, Short> lcidMap;

	class DirectoryEntry {
		int tag;
		int offset;
		int length;
		int checkSum;
	}

	FileChannel channel = null;

	/* > 0 only if this font is a part of a collection */
	int fontIndex = 0;

	/* Number of fonts in this collection. ==1 if not a collection */
	public int directoryCount = 1;

	/* offset in file of table directory for this font */
	int directoryOffset; // 12 if its not a collection.

	/* number of table entries in the directory/offsets table */
	int numTables;

	/* The contents of the the directory/offsets table */
	DirectoryEntry[] tableDirectory;

	// protected byte []gposTable = null;
	// protected byte []gdefTable = null;
	// protected byte []gsubTable = null;
	// protected byte []mortTable = null;
	// protected boolean hintsTabledChecked = false;
	// protected boolean containsHintsTable = false;

	/*
	 * These are for faster access to the name of the font as typically exposed via
	 * API to applications.
	 */
	private Locale nameLocale;
	private String localeFamilyName;
	private String localeFullName;

	/**
	 * - does basic verification of the file - reads the header table for this font
	 * (within a collection) - reads the names (full, family). - determines the
	 * style of the font. - initializes the CMAP
	 * 
	 * @throws FontFormatException
	 *             - if the font can't be opened or fails verification, or there's
	 *             no usable cmap
	 */
	public TrueTypeFont(String platname)
			throws FontFormatException {
//		useJavaRasterizer = javaRasterizer;
		this.platName = platname;
		try {
			verify();
			int fIndex=0;
			init(fIndex);
			fIndex++;
			this.familyName=null;this.fullName=null;this.localeFamilyName=null;this.localeFullName=null;
			
			for(;fIndex<this.directoryCount;++fIndex) {
				init(fIndex);
			}
		} catch (Throwable t) {
			close();
			t.printStackTrace();
		}finally {
			close();
		}
	}


	private synchronized FileChannel open() throws FontFormatException {
		if (channel == null) {
			try {
				RandomAccessFile raf = new RandomAccessFile(platName, "r");
				channel = raf.getChannel();
				fileSize = (int) channel.size();
//				raf.close();
			} catch (NullPointerException e) {
				try {
					channel.close();
				} catch (IOException e1) {
				}
			} catch (Exception e) {
				
			}
		}
		return channel;
	}

	protected synchronized void close() {
		try {
			if (channel != null) {
				channel.close();
			}
		} catch (IOException e) {
		} finally {
			channel = null;
		}
	}

	private int readBlock(ByteBuffer buffer, int offset, int length) {
		int bread = 0;
		try {
			synchronized (this) {
				if (channel == null) {
					open();
				}
				if (offset + length > fileSize) {
					if (offset >= fileSize) {
						return -1;
					} else {
						length = fileSize - offset;
					}
				}
				buffer.clear();
				channel.position(offset);
				while (bread < length) {
					int cnt = channel.read(buffer);
					if (cnt == -1) {
						String msg = "Unexpected EOF " + this;
						int currSize = (int) channel.size();
						if (currSize != fileSize) {
							msg += " File size was " + fileSize + " and now is " + currSize;
						}
						// We could still flip() the buffer here because
						// it's possible that we did read some data in
						// an earlier loop, and we probably should
						// return that to the caller. Although if
						// the caller expected 8K of data and we return
						// only a few bytes then maybe it's better instead to
						// set bread = -1 to indicate failure.
						// The following is therefore using arbitrary values
						// but is meant to allow cases where enough
						// data was read to probably continue.
						if (bread > length / 2 || bread > 16384) {
							buffer.flip();
						} else {
							bread = -1;
						}
						throw new IOException(msg);
					}
					bread += cnt;
				}
				buffer.flip();
				if (bread > length) { // possible if buffer.size() > length
					bread = length;
				}
			}
		} catch (FontFormatException e) {
			bread = -1; // signal EOF
		} catch (ClosedChannelException e) {
			Thread.interrupted();
			close();
			return readBlock(buffer, offset, length);
		} catch (IOException e) {
			/*
			 * If we did not read any bytes at all and the exception is not a recoverable
			 * one (ie is not ClosedChannelException) then we should indicate that there is
			 * no point in re-trying. Other than an attempt to read past the end of the file
			 * it seems unlikely this would occur as problems opening the file are handled
			 * as a FontFormatException.
			 */
			if (bread == 0) {
				bread = -1; // signal EOF
			}
		}
		return bread;
	}

	private ByteBuffer readBlock(int offset, int length) {

		ByteBuffer buffer = ByteBuffer.allocate(length);
		try {
			synchronized (this) {
				if (channel == null) {
					open();
				}
				if (offset + length > fileSize) {
					if (offset > fileSize) {
						return null; // assert?
					} else {
						buffer = ByteBuffer.allocate(fileSize - offset);
					}
				}
				channel.position(offset);
				channel.read(buffer);
				buffer.flip();
			}
		} catch (FontFormatException e) {
			return null;
		} catch (ClosedChannelException e) {

			Thread.interrupted();
			close();
			readBlock(buffer, offset, length);
		} catch (IOException e) {
			return null;
		}
		return buffer;
	}

	private String convertIntToString(int i ) {
		ByteBuffer buffer=ByteBuffer.allocate(4);
		buffer.putInt(i);
		return new String(buffer.array());
	}
	private void verify() throws FontFormatException {
		open();
	}

	boolean writeOut = !false;
	/* sizes, in bytes, of TT/TTC header records */
	private static final int TTCHEADERSIZE = 12;
	private static final int DIRECTORYHEADERSIZE = 12;
	private static final int DIRECTORYENTRYSIZE = 16;

	protected void init(int fIndex) throws FontFormatException {
		int headerOffset = 0;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ByteBuffer buffer = readBlock(0, TTCHEADERSIZE);
		// bos.write(buffer.array(),0,TTCHEADERSIZE);
		try {
			switch (buffer.getInt()) {

			case ttcfTag:
				int ttcversionid = buffer.getInt(); // skip TTC version ID
				System.out.printf("TTC VersionID:%d\n", ttcversionid);
				directoryCount = buffer.getInt();
				if (fIndex >= directoryCount) {
					throw new FontFormatException("Bad collection index");
				}
				fontIndex = fIndex;
				buffer = readBlock(TTCHEADERSIZE + 4 * fIndex, 4);
				headerOffset = buffer.getInt();
//				System.out.printf("Headeroffset for idx[%d] is :%d\n", fIndex, headerOffset);
				break;

			case v1ttTag:
			case trueTag:
			case ottoTag:
				break;

			default:
				throw new FontFormatException("Unsupported sfnt " + this.platName);
			}

			/*
			 * Now have the offset of this TT font (possibly within a TTC) After the TT
			 * version/scaler type field, is the short representing the number of tables in
			 * the table directory. The table directory begins at 12 bytes after the header.
			 * Each table entry is 16 bytes long (4 32-bit ints)
			 */
			buffer = readBlock(headerOffset + 4, 2);

			numTables = buffer.getShort();
			System.out.printf("Num tables:%d\n", numTables);
			directoryOffset = headerOffset + DIRECTORYHEADERSIZE;
			buffer = ByteBuffer.allocate(TTCHEADERSIZE);
			buffer.putShort((short) 1);
			buffer.putShort((short) 0);
			buffer.putShort((short) numTables);
			buffer.putShort((short) 0x100);

			buffer.putShort((short) 4);
			buffer.putShort((short) 0x30);
			bos.write(buffer.array());
			ByteBuffer bbuffer = readBlock(directoryOffset, numTables * DIRECTORYENTRYSIZE);
			// bos.write(bbuffer.array(), 0, numTables*DIRECTORYENTRYSIZE);
			IntBuffer ibuffer = bbuffer.asIntBuffer();
			DirectoryEntry table;
			tableDirectory = new DirectoryEntry[numTables];
			for (int i = 0; i < numTables; i++) {
				tableDirectory[i] = table = new DirectoryEntry();
				table.tag = ibuffer.get();
				 System.out.println(convertIntToString(table.tag));
				table.checkSum = ibuffer.get();
				table.offset = ibuffer.get();
				table.length = ibuffer.get();
				if (table.offset + table.length > fileSize) {
					throw new FontFormatException("bad table, tag=" + table.tag);
				}

			}

			int startOffset = directoryOffset + numTables * DIRECTORYENTRYSIZE;
			ByteArrayOutputStream tmpBos = new ByteArrayOutputStream();
			initNames();
			ByteBuffer tmpBuffer = ByteBuffer.allocate(numTables * DIRECTORYENTRYSIZE);
			Map<Integer, ByteBuffer> data = new HashMap<Integer, ByteBuffer>();
			for (int i = 0; i < numTables; ++i) {
				DirectoryEntry tmpTable = tableDirectory[i];
				System.out.printf("Tag:[%08X] offset[%08X]  len[%d]\n", tmpTable.tag, tmpTable.offset, tmpTable.length);

				data.put(tmpTable.tag, getTableBuffer(tmpTable.tag));
				tmpBuffer.putInt(tmpTable.tag);
				tmpBuffer.putInt(tmpTable.checkSum);
				tmpBuffer.putInt(startOffset - headerOffset);
				tmpTable.offset = startOffset - headerOffset;
				startOffset += tmpTable.length;
				tmpBuffer.putInt(tmpTable.length);
			}
			DirectoryEntry[] sortedTable = new DirectoryEntry[numTables];
			System.arraycopy(tableDirectory, 0, sortedTable, 0, numTables);
			for (int i = 0; i < numTables - 1; i++) {
				for (int j = i + 1; j < numTables; ++j) {
					DirectoryEntry tmp = sortedTable[j];
					if (sortedTable[j].offset < sortedTable[i].offset) {
						sortedTable[j] = sortedTable[i];
						sortedTable[i] = tmp;
					} else {
						sortedTable[i] = sortedTable[i];
						sortedTable[j] = sortedTable[j];
					}
				}
			}


			for (int i = 0; i < numTables; ++i) {
				DirectoryEntry tmpTable = sortedTable[i];
				tmpBos.write(data.get(tmpTable.tag).array(), 0, tmpTable.length);
			}
			if (writeOut) {
				java.io.File out = new java.io.File(localeFullName + ".ttf");
				java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
				fos.write(bos.toByteArray());
				fos.write(tmpBuffer.array(), 0, numTables * DIRECTORYENTRYSIZE);
				fos.write(tmpBos.toByteArray());
				fos.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ByteBuffer getTableBuffer(int tag) {
		DirectoryEntry entry = null;

		for (int i = 0; i < numTables; i++) {
			if (tableDirectory[i].tag == tag) {
				entry = tableDirectory[i];
				break;
			}
		}
		if (entry == null || entry.length == 0 || entry.offset + entry.length > fileSize) {
			return null;
		}

		int bread = 0;
		ByteBuffer buffer = ByteBuffer.allocate(entry.length);
		synchronized (this) {
			try {
				if (channel == null) {
					open();
				}
				channel.position(entry.offset);
				bread = channel.read(buffer);
				buffer.flip();
			} catch (ClosedChannelException e) {
				/*
				 * NIO I/O is interruptible, recurse to retry operation. Clear interrupts before
				 * recursing in case NIO didn't.
				 */
				Thread.interrupted();
				close();
				return getTableBuffer(tag);
			} catch (IOException e) {
				return null;
			} catch (FontFormatException e) {
				return null;
			}

			if (bread < entry.length) {
				return null;
			} else {
				return buffer;
			}
		}
	}

	private int getTableSize(int tag) {
		for (int i = 0; i < numTables; i++) {
			if (tableDirectory[i].tag == tag) {
				return tableDirectory[i].length;
			}
		}
		return 0;
	}

	private String makeString(byte[] bytes, int len, short encoding) {

		/*
		 * Check for fonts using encodings 2->6 is just for some old DBCS fonts,
		 * apparently mostly on Solaris. Some of these fonts encode ascii names as
		 * double-byte characters. ie with a leading zero byte for what properly should
		 * be a single byte-char.
		 */
		if (encoding >= 2 && encoding <= 6) {
			byte[] oldbytes = bytes;
			int oldlen = len;
			bytes = new byte[oldlen];
			len = 0;
			for (int i = 0; i < oldlen; i++) {
				if (oldbytes[i] != 0) {
					bytes[len++] = oldbytes[i];
				}
			}
		}

		String charset;
		switch (encoding) {
		case 1:
			charset = "UTF-16";
			break; // most common case first.
		case 0:
			charset = "UTF-16";
			break; // symbol uses this
		case 2:
			charset = "SJIS";
			break;
		case 3:
			charset = "GBK";
			break;
		case 4:
			charset = "MS950";
			break;
		case 5:
			charset = "EUC_KR";
			break;
		case 6:
			charset = "Johab";
			break;
		default:
			charset = "UTF-16";
			break;
		}

		try {
			return new String(bytes, 0, len, charset);
		} catch (UnsupportedEncodingException e) {
			return null;
		} catch (Throwable t) {
			return null;
		}
	}

	private void initNames() {

		byte[] name = new byte[20480000];
		ByteBuffer buffer = getTableBuffer(nameTag);

		if (buffer != null) {
			ShortBuffer sbuffer = buffer.asShortBuffer();
			sbuffer.get(); // format - not needed.
			short numRecords = sbuffer.get();
			/*
			 * The name table uses unsigned shorts. Many of these are known small values
			 * that fit in a short. The values that are sizes or offsets into the table
			 * could be greater than 32767, so read and store those as ints
			 */
			int stringPtr = sbuffer.get() & 0xffff;

			nameLocale = Locale.CHINA;
			short nameLocaleID = getLCIDFromLocale(nameLocale);

			for (int i = 0; i < numRecords; i++) {
				short platformID = sbuffer.get();
				// if (platformID != MS_PLATFORM_ID) {
				// sbuffer.position(sbuffer.position()+5);
				// continue; // skip over this record.
				// }
				short encodingID = sbuffer.get();
				short langID = sbuffer.get();
				short nameID = sbuffer.get();
				int nameLen = ((int) sbuffer.get()) & 0xffff;
				int namePtr = (((int) sbuffer.get()) & 0xffff) + stringPtr;
				String tmpName = null;
				buffer.position(namePtr);
				buffer.get(name, 0, nameLen);
				tmpName = makeString(name, nameLen, encodingID);
				System.out.printf("PlatformId[%d]  encodingID[%d] langID[%d] nameID[%d]  value[%s]\n", platformID,
						encodingID, langID, nameID, tmpName);

				switch (nameID) {

				case FAMILY_NAME_ID:

					if (familyName == null || langID == ENGLISH_LOCALE_ID || langID == nameLocaleID) {
						buffer.position(namePtr);
						buffer.get(name, 0, nameLen);
						tmpName = makeString(name, nameLen, encodingID);

						if (familyName == null || langID == ENGLISH_LOCALE_ID) {
							familyName = tmpName;
						}
						if (langID == nameLocaleID) {
							localeFamilyName = tmpName;
						}
					}

					break;

				case FULL_NAME_ID:

					if (fullName == null || langID == ENGLISH_LOCALE_ID || langID == nameLocaleID) {
						buffer.position(namePtr);
						buffer.get(name, 0, nameLen);
						tmpName = makeString(name, nameLen, encodingID);

						if (fullName == null || langID == ENGLISH_LOCALE_ID) {
							fullName = tmpName;
						}
						if (langID == nameLocaleID) {
							localeFullName = tmpName;
						}
					}
					break;
				}
			}
			if (localeFamilyName == null) {
				localeFamilyName = familyName;
			}
			if (localeFullName == null) {
				localeFullName = fullName;
			}
		}
	}

	/*
	 * Return the requested name in the requested locale, for the MS platform ID. If
	 * the requested locale isn't found, return US English, if that isn't found,
	 * return null and let the caller figure out how to handle that.
	 */
	protected String lookupName(short findLocaleID, int findNameID) {
		String foundName = null;
		byte[] name = new byte[1024];

		ByteBuffer buffer = getTableBuffer(nameTag);
		if (buffer != null) {
			ShortBuffer sbuffer = buffer.asShortBuffer();
			sbuffer.get(); // format - not needed.
			short numRecords = sbuffer.get();

			/*
			 * The name table uses unsigned shorts. Many of these are known small values
			 * that fit in a short. The values that are sizes or offsets into the table
			 * could be greater than 32767, so read and store those as ints
			 */
			int stringPtr = ((int) sbuffer.get()) & 0xffff;

			for (int i = 0; i < numRecords; i++) {
				short platformID = sbuffer.get();
				if (platformID != MS_PLATFORM_ID) {
					sbuffer.position(sbuffer.position() + 5);
					continue; // skip over this record.
				}
				short encodingID = sbuffer.get();
				short langID = sbuffer.get();
				short nameID = sbuffer.get();
				int nameLen = ((int) sbuffer.get()) & 0xffff;
				int namePtr = (((int) sbuffer.get()) & 0xffff) + stringPtr;
				if (nameID == findNameID
						&& ((foundName == null && langID == ENGLISH_LOCALE_ID) || langID == findLocaleID)) {
					buffer.position(namePtr);
					buffer.get(name, 0, nameLen);
					foundName = makeString(name, nameLen, encodingID);
					if (langID == findLocaleID) {
						return foundName;
					}
				}
			}
		}
		return foundName;
	}

	// Return a Microsoft LCID from the given Locale.
	// Used when getting localized font data.

	private static void addLCIDMapEntry(Map<String, Short> map, String key, short value) {
		map.put(key, Short.valueOf(value));
	}

	private static synchronized void createLCIDMap() {
		if (lcidMap != null) {
			return;
		}

		Map<String, Short> map = new HashMap<String, Short>(200);

		// the following statements are derived from the langIDMap
		// in src/windows/native/java/lang/java_props_md.c using the following
		// awk script:
		// $1~/\/\*/ { next}
		// $3~/\?\?/ { next }
		// $3!~/_/ { next }
		// $1~/0x0409/ { next }
		// $1~/0x0c0a/ { next }
		// $1~/0x042c/ { next }
		// $1~/0x0443/ { next }
		// $1~/0x0812/ { next }
		// $1~/0x04/ { print " addLCIDMapEntry(map, " substr($3, 0, 3) "\", (short) "
		// substr($1, 0, 6) ");" ; next }
		// $3~/,/ { print " addLCIDMapEntry(map, " $3 " (short) " substr($1, 0, 6) ");"
		// ; next }
		// { print " addLCIDMapEntry(map, " $3 ", (short) " substr($1, 0, 6) ");" ; next
		// }
		// The lines of this script:
		// - eliminate comments
		// - eliminate questionable locales
		// - eliminate language-only locales
		// - eliminate the default LCID value
		// - eliminate a few other unneeded LCID values
		// - print language-only locale entries for x04* LCID values
		// (apparently Microsoft doesn't use language-only LCID values -
		// see http://www.microsoft.com/OpenType/otspec/name.htm
		// - print complete entries for all other LCID values
		// Run
		// awk -f awk-script langIDMap > statements
		addLCIDMapEntry(map, "ar", (short) 0x0401);
		addLCIDMapEntry(map, "bg", (short) 0x0402);
		addLCIDMapEntry(map, "ca", (short) 0x0403);
		addLCIDMapEntry(map, "zh", (short) 0x0404);
		addLCIDMapEntry(map, "cs", (short) 0x0405);
		addLCIDMapEntry(map, "da", (short) 0x0406);
		addLCIDMapEntry(map, "de", (short) 0x0407);
		addLCIDMapEntry(map, "el", (short) 0x0408);
		addLCIDMapEntry(map, "es", (short) 0x040a);
		addLCIDMapEntry(map, "fi", (short) 0x040b);
		addLCIDMapEntry(map, "fr", (short) 0x040c);
		addLCIDMapEntry(map, "iw", (short) 0x040d);
		addLCIDMapEntry(map, "hu", (short) 0x040e);
		addLCIDMapEntry(map, "is", (short) 0x040f);
		addLCIDMapEntry(map, "it", (short) 0x0410);
		addLCIDMapEntry(map, "ja", (short) 0x0411);
		addLCIDMapEntry(map, "ko", (short) 0x0412);
		addLCIDMapEntry(map, "nl", (short) 0x0413);
		addLCIDMapEntry(map, "no", (short) 0x0414);
		addLCIDMapEntry(map, "pl", (short) 0x0415);
		addLCIDMapEntry(map, "pt", (short) 0x0416);
		addLCIDMapEntry(map, "rm", (short) 0x0417);
		addLCIDMapEntry(map, "ro", (short) 0x0418);
		addLCIDMapEntry(map, "ru", (short) 0x0419);
		addLCIDMapEntry(map, "hr", (short) 0x041a);
		addLCIDMapEntry(map, "sk", (short) 0x041b);
		addLCIDMapEntry(map, "sq", (short) 0x041c);
		addLCIDMapEntry(map, "sv", (short) 0x041d);
		addLCIDMapEntry(map, "th", (short) 0x041e);
		addLCIDMapEntry(map, "tr", (short) 0x041f);
		addLCIDMapEntry(map, "ur", (short) 0x0420);
		addLCIDMapEntry(map, "in", (short) 0x0421);
		addLCIDMapEntry(map, "uk", (short) 0x0422);
		addLCIDMapEntry(map, "be", (short) 0x0423);
		addLCIDMapEntry(map, "sl", (short) 0x0424);
		addLCIDMapEntry(map, "et", (short) 0x0425);
		addLCIDMapEntry(map, "lv", (short) 0x0426);
		addLCIDMapEntry(map, "lt", (short) 0x0427);
		addLCIDMapEntry(map, "fa", (short) 0x0429);
		addLCIDMapEntry(map, "vi", (short) 0x042a);
		addLCIDMapEntry(map, "hy", (short) 0x042b);
		addLCIDMapEntry(map, "eu", (short) 0x042d);
		addLCIDMapEntry(map, "mk", (short) 0x042f);
		addLCIDMapEntry(map, "tn", (short) 0x0432);
		addLCIDMapEntry(map, "xh", (short) 0x0434);
		addLCIDMapEntry(map, "zu", (short) 0x0435);
		addLCIDMapEntry(map, "af", (short) 0x0436);
		addLCIDMapEntry(map, "ka", (short) 0x0437);
		addLCIDMapEntry(map, "fo", (short) 0x0438);
		addLCIDMapEntry(map, "hi", (short) 0x0439);
		addLCIDMapEntry(map, "mt", (short) 0x043a);
		addLCIDMapEntry(map, "se", (short) 0x043b);
		addLCIDMapEntry(map, "gd", (short) 0x043c);
		addLCIDMapEntry(map, "ms", (short) 0x043e);
		addLCIDMapEntry(map, "kk", (short) 0x043f);
		addLCIDMapEntry(map, "ky", (short) 0x0440);
		addLCIDMapEntry(map, "sw", (short) 0x0441);
		addLCIDMapEntry(map, "tt", (short) 0x0444);
		addLCIDMapEntry(map, "bn", (short) 0x0445);
		addLCIDMapEntry(map, "pa", (short) 0x0446);
		addLCIDMapEntry(map, "gu", (short) 0x0447);
		addLCIDMapEntry(map, "ta", (short) 0x0449);
		addLCIDMapEntry(map, "te", (short) 0x044a);
		addLCIDMapEntry(map, "kn", (short) 0x044b);
		addLCIDMapEntry(map, "ml", (short) 0x044c);
		addLCIDMapEntry(map, "mr", (short) 0x044e);
		addLCIDMapEntry(map, "sa", (short) 0x044f);
		addLCIDMapEntry(map, "mn", (short) 0x0450);
		addLCIDMapEntry(map, "cy", (short) 0x0452);
		addLCIDMapEntry(map, "gl", (short) 0x0456);
		addLCIDMapEntry(map, "dv", (short) 0x0465);
		addLCIDMapEntry(map, "qu", (short) 0x046b);
		addLCIDMapEntry(map, "mi", (short) 0x0481);
		addLCIDMapEntry(map, "ar_IQ", (short) 0x0801);
		addLCIDMapEntry(map, "zh_CN", (short) 0x0804);
		addLCIDMapEntry(map, "de_CH", (short) 0x0807);
		addLCIDMapEntry(map, "en_GB", (short) 0x0809);
		addLCIDMapEntry(map, "es_MX", (short) 0x080a);
		addLCIDMapEntry(map, "fr_BE", (short) 0x080c);
		addLCIDMapEntry(map, "it_CH", (short) 0x0810);
		addLCIDMapEntry(map, "nl_BE", (short) 0x0813);
		addLCIDMapEntry(map, "no_NO_NY", (short) 0x0814);
		addLCIDMapEntry(map, "pt_PT", (short) 0x0816);
		addLCIDMapEntry(map, "ro_MD", (short) 0x0818);
		addLCIDMapEntry(map, "ru_MD", (short) 0x0819);
		addLCIDMapEntry(map, "sr_CS", (short) 0x081a);
		addLCIDMapEntry(map, "sv_FI", (short) 0x081d);
		addLCIDMapEntry(map, "az_AZ", (short) 0x082c);
		addLCIDMapEntry(map, "se_SE", (short) 0x083b);
		addLCIDMapEntry(map, "ga_IE", (short) 0x083c);
		addLCIDMapEntry(map, "ms_BN", (short) 0x083e);
		addLCIDMapEntry(map, "uz_UZ", (short) 0x0843);
		addLCIDMapEntry(map, "qu_EC", (short) 0x086b);
		addLCIDMapEntry(map, "ar_EG", (short) 0x0c01);
		addLCIDMapEntry(map, "zh_HK", (short) 0x0c04);
		addLCIDMapEntry(map, "de_AT", (short) 0x0c07);
		addLCIDMapEntry(map, "en_AU", (short) 0x0c09);
		addLCIDMapEntry(map, "fr_CA", (short) 0x0c0c);
		addLCIDMapEntry(map, "sr_CS", (short) 0x0c1a);
		addLCIDMapEntry(map, "se_FI", (short) 0x0c3b);
		addLCIDMapEntry(map, "qu_PE", (short) 0x0c6b);
		addLCIDMapEntry(map, "ar_LY", (short) 0x1001);
		addLCIDMapEntry(map, "zh_SG", (short) 0x1004);
		addLCIDMapEntry(map, "de_LU", (short) 0x1007);
		addLCIDMapEntry(map, "en_CA", (short) 0x1009);
		addLCIDMapEntry(map, "es_GT", (short) 0x100a);
		addLCIDMapEntry(map, "fr_CH", (short) 0x100c);
		addLCIDMapEntry(map, "hr_BA", (short) 0x101a);
		addLCIDMapEntry(map, "ar_DZ", (short) 0x1401);
		addLCIDMapEntry(map, "zh_MO", (short) 0x1404);
		addLCIDMapEntry(map, "de_LI", (short) 0x1407);
		addLCIDMapEntry(map, "en_NZ", (short) 0x1409);
		addLCIDMapEntry(map, "es_CR", (short) 0x140a);
		addLCIDMapEntry(map, "fr_LU", (short) 0x140c);
		addLCIDMapEntry(map, "bs_BA", (short) 0x141a);
		addLCIDMapEntry(map, "ar_MA", (short) 0x1801);
		addLCIDMapEntry(map, "en_IE", (short) 0x1809);
		addLCIDMapEntry(map, "es_PA", (short) 0x180a);
		addLCIDMapEntry(map, "fr_MC", (short) 0x180c);
		addLCIDMapEntry(map, "sr_BA", (short) 0x181a);
		addLCIDMapEntry(map, "ar_TN", (short) 0x1c01);
		addLCIDMapEntry(map, "en_ZA", (short) 0x1c09);
		addLCIDMapEntry(map, "es_DO", (short) 0x1c0a);
		addLCIDMapEntry(map, "sr_BA", (short) 0x1c1a);
		addLCIDMapEntry(map, "ar_OM", (short) 0x2001);
		addLCIDMapEntry(map, "en_JM", (short) 0x2009);
		addLCIDMapEntry(map, "es_VE", (short) 0x200a);
		addLCIDMapEntry(map, "ar_YE", (short) 0x2401);
		addLCIDMapEntry(map, "es_CO", (short) 0x240a);
		addLCIDMapEntry(map, "ar_SY", (short) 0x2801);
		addLCIDMapEntry(map, "en_BZ", (short) 0x2809);
		addLCIDMapEntry(map, "es_PE", (short) 0x280a);
		addLCIDMapEntry(map, "ar_JO", (short) 0x2c01);
		addLCIDMapEntry(map, "en_TT", (short) 0x2c09);
		addLCIDMapEntry(map, "es_AR", (short) 0x2c0a);
		addLCIDMapEntry(map, "ar_LB", (short) 0x3001);
		addLCIDMapEntry(map, "en_ZW", (short) 0x3009);
		addLCIDMapEntry(map, "es_EC", (short) 0x300a);
		addLCIDMapEntry(map, "ar_KW", (short) 0x3401);
		addLCIDMapEntry(map, "en_PH", (short) 0x3409);
		addLCIDMapEntry(map, "es_CL", (short) 0x340a);
		addLCIDMapEntry(map, "ar_AE", (short) 0x3801);
		addLCIDMapEntry(map, "es_UY", (short) 0x380a);
		addLCIDMapEntry(map, "ar_BH", (short) 0x3c01);
		addLCIDMapEntry(map, "es_PY", (short) 0x3c0a);
		addLCIDMapEntry(map, "ar_QA", (short) 0x4001);
		addLCIDMapEntry(map, "es_BO", (short) 0x400a);
		addLCIDMapEntry(map, "es_SV", (short) 0x440a);
		addLCIDMapEntry(map, "es_HN", (short) 0x480a);
		addLCIDMapEntry(map, "es_NI", (short) 0x4c0a);
		addLCIDMapEntry(map, "es_PR", (short) 0x500a);

		lcidMap = map;
	}

	private static short getLCIDFromLocale(Locale locale) {
		// optimize for common case
		if (locale.equals(Locale.US)) {
			return US_LCID;
		}

		if (lcidMap == null) {
			createLCIDMap();
		}

		String key = locale.toString();
		while (!"".equals(key)) {
			Short lcidObject = (Short) lcidMap.get(key);
			if (lcidObject != null) {
				return lcidObject.shortValue();
			}
			int pos = key.lastIndexOf('_');
			if (pos < 1) {
				return US_LCID;
			}
			key = key.substring(0, pos);
		}

		return US_LCID;
	}

	public String getFamilyName(Locale locale) {
		if (locale == null) {
			return familyName;
		} else if (locale.equals(nameLocale) && localeFamilyName != null) {
			return localeFamilyName;
		} else {
			short localeID = getLCIDFromLocale(locale);
			String name = lookupName(localeID, FAMILY_NAME_ID);
			if (name == null) {
				return familyName;
			} else {
				return name;
			}
		}
	}

	/*
	 * This duplicates initNames() but that has to run fast as its used during
	 * typical start-up and the information here is likely never needed.
	 */
	private void initAllNames(int requestedID, HashSet names) {

		byte[] name = new byte[256];
		ByteBuffer buffer = getTableBuffer(nameTag);

		if (buffer != null) {
			ShortBuffer sbuffer = buffer.asShortBuffer();
			sbuffer.get(); // format - not needed.
			short numRecords = sbuffer.get();

			/*
			 * The name table uses unsigned shorts. Many of these are known small values
			 * that fit in a short. The values that are sizes or offsets into the table
			 * could be greater than 32767, so read and store those as ints
			 */
			int stringPtr = ((int) sbuffer.get()) & 0xffff;
			for (int i = 0; i < numRecords; i++) {
				short platformID = sbuffer.get();
				if (platformID != MS_PLATFORM_ID) {
					sbuffer.position(sbuffer.position() + 5);
					continue; // skip over this record.
				}
				short encodingID = sbuffer.get();
				sbuffer.get();
				short nameID = sbuffer.get();
				int nameLen = ((int) sbuffer.get()) & 0xffff;
				int namePtr = (((int) sbuffer.get()) & 0xffff) + stringPtr;

				if (nameID == requestedID) {
					buffer.position(namePtr);
					buffer.get(name, 0, nameLen);
					names.add(makeString(name, nameLen, encodingID));
				}
			}
		}
	}

	private char[] gaspTable;

	private char[] getGaspTable() {

		if (gaspTable != null) {
			return gaspTable;
		}

		ByteBuffer buffer = getTableBuffer(gaspTag);
		if (buffer == null) {
			return gaspTable = new char[0];
		}

		CharBuffer cbuffer = buffer.asCharBuffer();
		char format = cbuffer.get();
		/*
		 * format "1" has appeared for some Windows Vista fonts. Its presently
		 * undocumented but the existing values seem to be still valid so we can use it.
		 */
		if (format > 1) { // unrecognised format
			return gaspTable = new char[0];
		}

		char numRanges = cbuffer.get();
		if (4 + numRanges * 4 > getTableSize(gaspTag)) { // sanity check
			return gaspTable = new char[0];
		}
		gaspTable = new char[2 * numRanges];
		cbuffer.get(gaspTable);
		return gaspTable;
	}

	@Override
	public String toString() {
		return "** TrueType Font: Family=" + familyName + " Name=" + fullName + " style=" + style + " fileName="
				+ this.platName;
	}
}
