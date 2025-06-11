package client;

import java.util.ArrayList;
import java.util.List;

public class TableSchema {
	public static String myTable = "FAVMyInfo";
	public static String[] colunmFamilies = 
			new String[]{"Personal", "Office", "Interests"};
	public static String[][] qualifiers = {
			{"Name", "Gender", "Phone"}, 
			{"Position", "Phone", "Address"}, 
			{"Food", "Sports", "Movies"}};
	public static boolean createBigTable(int maxRow) {
		List<String> columnFamilies = new ArrayList<>();
		String tName = myTable;
		columnFamilies.add(colunmFamilies[0]);
		columnFamilies.add(colunmFamilies[1]);
		boolean rst = HBaseClient.createTable(tName, columnFamilies);
		
		if(!rst) {
			return false;
		}
		
		for(int row = 0; row <maxRow; row ++) {
			for(int co =0; co<2; co++) {
				for(int qualifier = 0; qualifier < 2; qualifier++) {
					rst = HBaseClient.putRow(tName, 
							Integer.toString(row), 
							colunmFamilies[co], 
							qualifiers[co][qualifier], 
							colunmFamilies[co]+"-"+qualifiers[co][qualifier]+row);
					if(!rst) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public static boolean deleteBigTable() {
		return HBaseClient.deleteTable(myTable);
	}
}
