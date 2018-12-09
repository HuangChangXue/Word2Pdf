package org.hcx.tools.poi.tool;

public class PoiUtil {

	private static String getBinaryString(int a) {
		StringBuilder sb = new StringBuilder();
		do {
			sb.append((a & 8) == 8 ? 1 : 0);
			a = a << 1;
		} while (sb.length() < 4);
		return sb.toString();
	}

	public static boolean isRowMergeStart(int x, int y, int[][] layout) {

		int value = layout[y][x];
		return (value & 12) == 12;

	}

	public static boolean isColMergeStart(int x, int y, int[][] layout) {

		int value = layout[y][x];
		return (value & 3) == 3;

	}

	public static boolean isMergeStart(int x, int y, int[][] layout) {
		int value = layout[y][x];
		return (value & 12) == 12 || (value & 3) == 3;
	}

	public static boolean isMerge(int x, int y, int[][] layout) {
		return layout[y][x] != 0;
	}

	public static boolean isMergeHEnd(int x, int y, int[][] layout) {
		boolean end = false;
		if (layout != null && layout[0] != null) {
			if (x >= layout[0].length - 1) {
				end = true;
			} else {
				end = (layout[y][x] & 12) == 4 && (layout[y][x + 1] & 12) != 4;
			}
		}
		return end;
	}

	public static boolean isMergVEnd(int x, int y, int[][] layout) {
		boolean end = false;
		if (layout != null) {
			if (y >= layout.length - 1) {
				end = true;
			} else {
				end = (layout[y][x] & 3) == 1 && (layout[y + 1][x] & 3) != 1;
			}
		}
		return end;
	}

	public static boolean isMergeEnd(int x, int y, int[][] layout) {
		boolean ret = false;
		int value = layout[y][x];
		switch (value) {
			case 1:
				ret = isMergVEnd(x, y, layout);
				break;
			case 4:
				ret = isMergeHEnd(x, y, layout);
				break;
			case 5:
				ret = isMergVEnd(x, y, layout) && isMergeHEnd(x, y, layout);
				break;

			default:

		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	public static int[][] createTableLayout(org.dom4j.Element table) {
		StringBuilder rowws=new StringBuilder();
		try {
			int rownum = Integer.valueOf(table.attributeValue("rows"));
			int[][] tableRec = new int[rownum][];
			int[][] colWidth = new int[rownum][];
			org.dom4j.Element[][] cells = new org.dom4j.Element[rownum][];
			java.util.List<Integer> ends = new java.util.ArrayList<Integer>();
			int maxCols = 0;
			int[] sepWidth = null;
			java.util.List<org.dom4j.Element> rows = table.elements("row");
			for (int i = 0; i < rownum; ++i) {
				org.dom4j.Element row = rows.get(i);
				int colnum = Integer.valueOf(row.attributeValue("cols"));
				rowws.append(row.attributeValue("height")).append(" ");
				java.util.List<org.dom4j.Element> cols = row.elements("col");
				int[] rowRec = new int[colnum];
				int[] width = new int[colnum];
				cells[i] = new org.dom4j.Element[colnum];
				int end = 0;
				for (int j = 0; j < colnum; ++j) {
					org.dom4j.Element col = cols.get(j);
					cells[i][j] = col;
					java.util.List<org.dom4j.Element> subtables = col.elements("table");
					if (subtables != null) {
						for (org.dom4j.Element t : subtables) {
							createTableLayout(t);
						}
					}

					int w = Integer.valueOf(col.attributeValue("width"));
					width[j] = w;
					end += w;
					if (!ends.contains(end)) {
						ends.add(end);
					}
					if (Boolean.valueOf(col.attributeValue("verticalMerged"))) {
						rowRec[j] = 1;// 列合并单元格
						if (Boolean.valueOf(col.attributeValue("firstVerticalMerged"))) {
							rowRec[j] = rowRec[j] | 2;// 列合并单元格第一格
						}
					}
				}
				tableRec[i] = rowRec;
				colWidth[i] = width;
			}
			Integer[] edges = ends.toArray(new Integer[] {});
			sepWidth = new int[edges.length];
			maxCols = edges.length;
			for (int i = 0; i < edges.length - 1; ++i) {
				for (int j = i + 1; j < edges.length; ++j) {
					if (edges[i] > edges[j]) {
						int tm = edges[i];
						edges[i] = edges[j];
						edges[j] = tm;
					}
				}
			}
			for (int i = 0; i < sepWidth.length; ++i) {

				sepWidth[i] = i == 0 ? edges[i] : edges[i] - edges[i - 1];
			}

			for (int i = 0; i < rownum; ++i) {
				int[] rowRec = tableRec[i];
				if (rowRec.length > maxCols) {
					continue;
				} else {
					int[] newRowRec = new int[maxCols];
					tableRec[i] = newRowRec;
					int[] crowW = colWidth[i];
					org.dom4j.Element[] currowCell = cells[i];
					cells[i] = new org.dom4j.Element[maxCols];
					int newIdx = 0;
					for (int j = 0; j < rowRec.length; ++j) {
						if (sepWidth[newIdx] == crowW[j]) {
							newRowRec[newIdx] = rowRec[j];
							cells[i][newIdx] = currowCell[j];
							newIdx++;
						} else {
							int targetW = crowW[j];
							int curw = 0;
							StringBuilder sb = new StringBuilder();
							do {
								newRowRec[newIdx] = rowRec[j] | 4;// 行合并单元格
								cells[i][newIdx] = currowCell[j];
								sb.append(newIdx).append(" ");
								if (curw == 0) {
									newRowRec[newIdx] = newRowRec[newIdx] | 8;// 行合并单元格的第一格
								}
								curw += sepWidth[newIdx];
								newIdx++;
							} while (curw < targetW);
							cells[i][j].addAttribute("colcross", sb.toString().trim());
						}
					}
				}
			}

			for (int i = 0; i < rownum; ++i) {
				int[] rowRec = tableRec[i];
				for (int j = 0; j < rowRec.length; ++j) {
					
					System.out.print(String.format("%8s", getBinaryString(rowRec[j])));
				}
				System.out.println();
			}
			for ( int i =0;i<maxCols;++i) {
				for( int j =0;j<rownum;++j) {
					if(isColMergeStart(i, j, tableRec)) {
						int startidx=j;
						StringBuilder sb=new StringBuilder();
						do {
							sb.append(j).append(" ");
							startidx=j;
							if(isMergVEnd(i, j, tableRec)) {
								break;
							}
							j++;
						}while(j<rownum);
						cells[startidx][i].addAttribute("rowcross", sb.toString().trim());
					}
				}
			}
			
			
			System.out.println();
			System.out.println();

			char[][] marged = new char[rownum][];
			for (int i = 0; i < rownum; ++i) {

				marged[i] = new char[maxCols];
				for (int j = 0; j < maxCols; j++) {
					if (isMerge(j, i, tableRec)) {
						marged[i][j] = '\u25a1';//
					} else {
						marged[i][j] = '\u25a0';
					}

				}

			}

			for (int i = 0; i < rownum; ++i) {
				int[] rowRec = tableRec[i];
				for (int j = 0; j < rowRec.length; ++j) {
					System.out.print(marged[i][j] + " ");
				}
				System.out.println();
			}
			StringBuilder sb=new StringBuilder();
			for(int i =0;i<sepWidth.length;++i) {
					sb.append(sepWidth[i]).append(" ");
			}
			table.addAttribute("colws", sb.toString().trim());
			table.addAttribute("rowws", rowws.toString().trim());
			return tableRec;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
