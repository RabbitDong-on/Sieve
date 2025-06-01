package client;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.RegionInfo;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.SnapshotDescription;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.util.Bytes;

public class HBaseClient {

    public static Connection connection;

    // 172.30.0.2: 
    public static void buildConnect(String ip, String port) {
    	connection = getConnection(ip, port);
    	
    }

    public static Connection getConnection(String ip, String port) {
    	int count = 0;
    	while(true) {
    		try {
                Configuration configuration = getConfiguration(ip, port);
                HBaseAdmin.available(configuration);
                return ConnectionFactory.createConnection(configuration);
            } catch (Exception e) {
                System.err.println(e.getMessage()+e.getStackTrace());
                if(count >= 5 ) {
                	System.err.println("Exit after trying 5 times ...");
                	throw new RuntimeException(e);
                }
                try {
					Thread.currentThread().sleep(500);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
                count++;
            }
    	}
    }

    private static Configuration getConfiguration(String ip, String port) {
        Configuration config = HBaseConfiguration.create();
		config.set("hbase.zookeeper.property.clientPort", port);
//		config.set("hbase.zookeeper.quorum", "C1hb-zk");
		config.set("hbase.zookeeper.quorum", ip);
		config.set("hbase.client.sync.wait.timeout.msec", "300000");
		config.set("hbase.client.retries.number", "100");
		config.set("hbase.client.pause", "3000");
		config.set("hbase.rpc.timeout", "300000");
		config.set("zookeeper.recovery.retry", "100");
		config.set("zookeeper.recovery.retry.intervalmill", "1000");
//		config.set("ipc.socket.timeout", "60000");
//		config.set("hbase.zookeeper.quorum", "localhost");
		return config;
    }

    public static boolean tableExists(String tableName) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
                HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
                return admin.tableExists(TableName.valueOf(tableName));
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
    	return false;
    }

    public static boolean createTable(String tableName, List<String> columnFamilies) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
                HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
                if (admin.tableExists(TableName.valueOf(tableName))) {
                    return false;
                }
                TableDescriptorBuilder tableDescriptor = TableDescriptorBuilder.newBuilder(TableName.valueOf(tableName));
//                columnFamilies.forEach(columnFamily -> {
//                    ColumnFamilyDescriptorBuilder cfDescriptorBuilder = ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily));
//                    cfDescriptorBuilder.setMaxVersions(1);
//                    ColumnFamilyDescriptor familyDescriptor = cfDescriptorBuilder.build();
//                    tableDescriptor.setColumnFamily(familyDescriptor);
//                });
                for(String columnFamily:columnFamilies) {
                	ColumnFamilyDescriptorBuilder cfDescriptorBuilder = ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily));
                    cfDescriptorBuilder.setMaxVersions(3);
                    ColumnFamilyDescriptor familyDescriptor = cfDescriptorBuilder.build();
                    tableDescriptor.setColumnFamily(familyDescriptor);
                }
                admin.createTable(tableDescriptor.build());
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }

    public static boolean addColumnFamily(String tableName, String columnFamily) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
        		ColumnFamilyDescriptorBuilder cfDescriptorBuilder = ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily));
        		cfDescriptorBuilder.setMaxVersions(3);
        		ColumnFamilyDescriptor familyDescriptor = cfDescriptorBuilder.build();
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
        		admin.addColumnFamily(TableName.valueOf(tableName), familyDescriptor);
                return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }
    
    public static boolean addColumnFamilyAsync(String tableName, String columnFamily) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
        		ColumnFamilyDescriptorBuilder cfDescriptorBuilder = ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily));
        		cfDescriptorBuilder.setMaxVersions(3);
        		ColumnFamilyDescriptor familyDescriptor = cfDescriptorBuilder.build();
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
        		admin.addColumnFamilyAsync(TableName.valueOf(tableName), familyDescriptor);
                return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
    	return false;
    }
    
    public static boolean assignARegion(RegionInfo region) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
        		admin.assign(region.getRegionName());
        		return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }
    
    public static boolean balance(boolean force) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
        		return admin.balance(force);
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
    	return false;
    }
    
    //Create a new table by cloning the snapshot content.
    public static boolean cloneSnapshot(String snapshotName, String tableName) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
        		admin.cloneSnapshot(snapshotName, TableName.valueOf(tableName));
        		return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }
    
    public static boolean cloneSnapshotAsync(String snapshotName, String tableName) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
        		admin.cloneSnapshotAsync(snapshotName, TableName.valueOf(tableName));
        		return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }
    
    //Create a new table by cloning the existent table schema.
    public static boolean cloneTableSchema(String tableName, String newTableName, boolean preserveSplits) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
        		admin.cloneTableSchema(TableName.valueOf(tableName), TableName.valueOf(newTableName), preserveSplits);
        		return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }
    
    //Mark region server(s) as decommissioned to prevent additional regions from getting assigned to them.
    public static boolean decommissionRegionServers(List<ServerName> servers, boolean offload) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
        		admin.decommissionRegionServers(servers, offload);
        		return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }
    
    public static boolean deleteColumnFamily(String tableName, String columnFamily) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
        		admin.deleteColumnFamily(TableName.valueOf(tableName), Bytes.toBytes(columnFamily));
        		return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }
    
    public static boolean deleteColumnFamilyAsync(String tableName, String columnFamily) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
        		admin.deleteColumnFamilyAsync(TableName.valueOf(tableName), Bytes.toBytes(columnFamily));
        		return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }
    
    public static boolean disableTable(String tableName, boolean async) {
    	int attempt = 0;
    	while(attempt<2) {
        	try {
        		attempt++;
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
        		if(async) {
        			admin.disableTableAsync(TableName.valueOf(tableName));
        		} else {
        			admin.disableTable(TableName.valueOf(tableName));
        		}
        		return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }
    
    public static boolean enableTable(String tableName, boolean async) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
        		if(async) {
        			admin.enableTableAsync(TableName.valueOf(tableName));
        		} else {
        			admin.enableTable(TableName.valueOf(tableName));
        		}
        		return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }


    public static boolean deleteTable(String tableName) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
                HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
                admin.deleteTable(TableName.valueOf(tableName));
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }
    
    public static List<TableDescriptor> listTable() {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
                HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
                List<TableDescriptor> tables = admin.listTableDescriptors();
                return tables;
            } catch (Exception e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return null;
    }

    public static boolean putRow(String tableName, String rowKey, String columnFamilyName, String qualifier,
                                 String value) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
                Table table = connection.getTable(TableName.valueOf(tableName));
                Put put = new Put(Bytes.toBytes(rowKey));
                put.addColumn(Bytes.toBytes(columnFamilyName), Bytes.toBytes(qualifier), Bytes.toBytes(value));
                table.put(put);
                table.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }


    public static boolean putRow(String tableName, String rowKey, String columnFamilyName, List<Pair<String, String>> pairList) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
                Table table = connection.getTable(TableName.valueOf(tableName));
                Put put = new Put(Bytes.toBytes(rowKey));
                pairList.forEach(pair -> put.addColumn(Bytes.toBytes(columnFamilyName), Bytes.toBytes(pair.getKey()), Bytes.toBytes(pair.getValue())));
                table.put(put);
                table.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }


    public static Result getRow(String tableName, String rowKey) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
                Table table = connection.getTable(TableName.valueOf(tableName));
                Get get = new Get(Bytes.toBytes(rowKey));
                get = get.readAllVersions();
                return table.get(get);
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return null;
    }


    public static String getCell(String tableName, String rowKey, String columnFamily, String qualifier) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
                Table table = connection.getTable(TableName.valueOf(tableName));
                Get get = new Get(Bytes.toBytes(rowKey));
                if (!get.isCheckExistenceOnly()) {
                    get.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifier));
                    Result result = table.get(get);
                    byte[] resultValue = result.getValue(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifier));
                    return Bytes.toString(resultValue);
                } else {
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return null;
    }


    public static ResultScanner getScanner(String tableName) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
                Table table = connection.getTable(TableName.valueOf(tableName));
                Scan scan = new Scan();
                return table.getScanner(scan);
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return null;
    }

    public static Table getTable(String tableName) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
                Table table = connection.getTable(TableName.valueOf(tableName));
                return table;
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return null;
    }


    public static ResultScanner getScanner(String tableName, FilterList filterList) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
                Table table = connection.getTable(TableName.valueOf(tableName));
                Scan scan = new Scan();
                scan.setFilter(filterList);
                return table.getScanner(scan);
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return null;
    }


    public static ResultScanner getScanner(String tableName, String startRowKey, String endRowKey,
                                           FilterList filterList) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
                Table table = connection.getTable(TableName.valueOf(tableName));
                Scan scan = new Scan();
                scan.withStartRow(Bytes.toBytes(startRowKey));
                scan.withStopRow(Bytes.toBytes(endRowKey));
                scan.setFilter(filterList);
                return table.getScanner(scan);
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return null;
    }

    public static boolean deleteRow(String tableName, String rowKey) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
                Table table = connection.getTable(TableName.valueOf(tableName));
                Delete delete = new Delete(Bytes.toBytes(rowKey));
                table.delete(delete);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }

    public static List<RegionInfo> getRegionsOnARS(ServerName serverName) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
            	HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            	return admin.getRegions(serverName);
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return null;
    }

    public static List<RegionInfo> getRegionsOfATable(String tname) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
            	HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            	TableName tableName = TableName.valueOf(tname);
            	return admin.getRegions(tableName);
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return null;
    }
    
    public static Collection<ServerName> getRegionServers(boolean excludeDecommissionedRS) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
            	HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            	return admin.getRegionServers(excludeDecommissionedRS);
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return null;
    }
    
    public static boolean splitEachRegionForTable(String tname) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
            	HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            	TableName tableName = TableName.valueOf(tname);
            	admin.split(tableName);
            	return true;
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }
    
    public static boolean splitRegionByPoint(String tname, byte[] splitPoint) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
            	HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            	TableName tableName = TableName.valueOf(tname);
            	admin.split(tableName, splitPoint);
            	return true;
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }
    
    public static boolean stopMaster() {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
            	HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            	admin.stopMaster();
            	return true;
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }
    
    public static ServerName getMaster() {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
            	HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            	return admin.getMaster();
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return null;
    }
    
    public static boolean stopRegionServer(String hostnamePort) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
            	HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            	admin.stopRegionServer(hostnamePort);
            	return true;
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }

    public static boolean truncateTable(String tname, boolean preserveSplits) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            	TableName tableName = TableName.valueOf(tname);
//            	boolean disabled = admin.isTableDisabled(tableName);
//            	System.out.println("Is table disabled:"+tname+", "+disabled);
//
//        		boolean disabled = admin.isTableDisabled(tableName);
//            	System.out.println("Is table disabled before truncate:"+tableName+", "+disabled);
            	admin.truncateTable(tableName, preserveSplits);
//            	disabled = admin.isTableDisabled(tableName);
//            	System.out.println("Disabled table after truncate the table:"+tname+", "+disabled);
            	return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
    	return false;
    }
    
    public static boolean unassign(RegionInfo regionName) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            	admin.unassign(regionName.getRegionName());
            	return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
        	}
    	}
    	return false;
    }
    
    public static List<SnapshotDescription> listSnapshots() {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            	return admin.listSnapshots();
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
    	return null;
    }
    
    //Move the region encodedRegionName to a random server.
    public static boolean move(RegionInfo encodedRegionName) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            	admin.move(encodedRegionName.getEncodedNameAsBytes());
            	return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
    	return false;
    }
    
    public static boolean move(RegionInfo encodedRegionName, ServerName destServerName) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
    			attempt++;
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            	admin.move(encodedRegionName.getEncodedNameAsBytes(),destServerName);
            	return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
    	return false;
    }
   
    //Offline specified region from master's in-memory state.
    public static boolean 	offline(RegionInfo regionName) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            	admin.offline(regionName.getRegionName());
            	return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
    	return false;
    }
    
    public static boolean 	recommissionRegionServer(ServerName server, List<byte[]> encodedRegionNames) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
        		HBaseAdmin admin = (HBaseAdmin) connection.getAdmin();
            	admin.recommissionRegionServer(server, encodedRegionNames);
            	return true;
        	} catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
    	return false;
    }

    public static boolean deleteColumn(String tableName, String rowKey, String familyName,
                                          String qualifier) {
    	int attempt = 0;
    	while(attempt<2) {
    		try {
                Table table = connection.getTable(TableName.valueOf(tableName));
                Delete delete = new Delete(Bytes.toBytes(rowKey));
                delete.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(qualifier));
                table.delete(delete);
                table.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                if(attempt<2) {
                	System.out.println("Try another time:"+attempt);
                }
            }
    	}
        return false;
    }

}