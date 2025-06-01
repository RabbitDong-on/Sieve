package client;

import java.io.IOException;
import org.apache.hadoop.hbase.client.Result;

public class HBaseClientMain {
	static String stopNodeSH;
	static String startNodeSH;
	static String failSH;
	static boolean check = false;
	public static void main(String[] args) {
		if(args[2].equals("check")) {
			check = true;
		}
		startNodeSH = args[3];
		stopNodeSH = args[4];
		failSH = args[5];
		String testName = "NormalTest";
		try {
			HBaseClient.buildConnect(args[0], args[1]);
			
			//create table
			boolean createtable = TableSchema.createBigTable(4);//row start from 0, contains two column families
			System.out.println(testName+" created table "+TableSchema.myTable+" with 4 rows.");
			if(check) {
				if(!HBaseClient.tableExists(TableSchema.myTable)) {
					System.out.println(testName+" the talbe "+TableSchema.myTable+" does not exist after creating table");
				}
			}
			if(!createtable) {
				System.out.println(testName+" the table "+TableSchema.myTable+" was not created successfully, exit.");
				return;
			}
			
			//add a row
			boolean putrow400 = HBaseClient.putRow(TableSchema.myTable, "4", TableSchema.colunmFamilies[0], TableSchema.qualifiers[0][0], "new value");
			System.out.println(testName+" put 'new value' to row '4' at table "
			+TableSchema.myTable+" at colunm "+TableSchema.colunmFamilies[0]
					+", with qualifier "+TableSchema.qualifiers[0][0]);
			
			//add a column
			boolean addcol2 = HBaseClient.addColumnFamily(TableSchema.myTable, TableSchema.colunmFamilies[2]);
			System.out.println(testName+" add colunm "+TableSchema.colunmFamilies[2]
					+" to table "+TableSchema.myTable);
			
			//add a qualifier
			boolean putrow420_0 = HBaseClient.putRow(TableSchema.myTable, "4", TableSchema.colunmFamilies[2], TableSchema.qualifiers[2][0], "I like dogs");
			System.out.println(testName+" put 'I like dogs' to row '4' at table "
					+TableSchema.myTable+" at colunm "+TableSchema.colunmFamilies[2]
							+", with qualifier "+TableSchema.qualifiers[2][0]);
			
			//modify the cell
			boolean putrow420_1 = HBaseClient.putRow(TableSchema.myTable, "4", TableSchema.colunmFamilies[2], TableSchema.qualifiers[2][0], "I like cats");
			System.out.println(testName+" put 'I like cats' to row '4' at table "
					+TableSchema.myTable+" at colunm "+TableSchema.colunmFamilies[2]
							+", with qualifier "+TableSchema.qualifiers[2][0]);
			
			//delete a column
			boolean deletecol1 = HBaseClient.deleteColumnFamily(TableSchema.myTable, TableSchema.colunmFamilies[1]);
			System.out.println(testName+" delete colunm "+TableSchema.colunmFamilies[1]+" of table "+TableSchema.myTable);
			
			//delete a row
			boolean deleterow3 = HBaseClient.deleteRow(TableSchema.myTable, "3");
			System.out.println(testName+" delete row '3' in table "+TableSchema.myTable);
			
			//get a row
			Result rst = HBaseClient.getRow(TableSchema.myTable, Integer.toString(2));
			System.out.println(testName+" get row 2 from table "+TableSchema.myTable+": "+rst.toString());
			
			//delete table
			TableSchema.deleteBigTable();
			System.out.println(testName+" deleted table "+TableSchema.myTable);
			if(check) {
				if(HBaseClient.tableExists(TableSchema.myTable)) {
					System.out.println(testName+" the talbe "+TableSchema.myTable+" was not deleted!");
				}
			}
			System.out.println(testName+"exit successfully!");
		} finally {
	        if (HBaseClient.connection != null) {
	            try {
	            	HBaseClient.connection.close();
	            } catch (IOException e) {
	            	System.out.println(testName+"error occurs when closing connection "+e.getMessage());
	            }
	        }
	    }
	}
}
