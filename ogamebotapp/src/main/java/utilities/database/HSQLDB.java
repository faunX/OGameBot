package utilities.database;

import org.hsqldb.DatabaseManager;
import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;
import utilities.fileio.FileOptions;
import utilities.fileio.JarUtility;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jarndt on 5/8/17.
 */
public class HSQLDB {
    public static void main(String[] args) throws IOException, SQLException {
        try {
            HSQLDBCommons.executeQuery("CREATE TABLE test (num INT IDENTITY, answer VARCHAR(250));");
            HSQLDBCommons.executeQuery("INSERT INTO test (answer) values ('this is a new answer');");
            List<Map<String, Object>> result = HSQLDBCommons.executeQuery("select * from test;");
            System.out.println(result);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            HSQLDBCommons.getDatabase().stopDBServer();
        }
//        HSQLDB hs = null;
//        try {
//            hs = new HSQLDB("tableMetaDataDB");
//            hs.executeQuery("CREATE TABLE test (num INT IDENTITY, answer VARCHAR(250));");
//            hs.executeQuery("INSERT INTO test (answer) values ('this is a new answer');");
//            List<Map<String, Object>> result = hs.executeQuery("select * from test;");
//            System.out.println(result);
//        } catch (IOException | SQLException e) {
////            e.printStackTrace();
//        }finally{
//            hs.stopDBServer();
//        }
    }

    public String dbName;
    final String dbLocation = FileOptions.cleanFilePath(JarUtility.getResourceDir()+"/databases/HSQLDB/");//FileOptions.cleanFilePath(System.getProperty("user.dir")+"/HSQL/");
    public HSQLDB(String dbName) throws IOException{
        Logger.getLogger("hsqldb.db").setLevel(Level.OFF);
        System.setProperty("hsqldb.reconfig_logging", "false");
        this.dbName = dbName;
        startDBServer(dbName);
    }
    Server sonicServer;
    Connection dbConn = null;

    public void startDBServer(String dbName) {
        if(sonicServer != null){
            stopDBServer();
        }
        HsqlProperties props = new HsqlProperties();
        props.setProperty("server.database.0", "file:" + dbLocation + dbName+";");
        props.setProperty("server.dbname.0", "xdb");
        props.setProperty("shutdown","true");
        props.setProperty("hsqldb.reconfig_logging", "false");
        sonicServer = new org.hsqldb.Server();
        try {
            sonicServer.setProperties(props);
        } catch (Exception e) {
            return;
        }
        sonicServer.start();
    }

    public void stopDBServer() {
        DatabaseManager.closeDatabases(0);
        sonicServer.shutdown();
    }

    public Connection getDBConn() {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            dbConn = DriverManager.getConnection(
                    "jdbc:hsqldb:hsql://localhost/ogame", "ogame_user", "ogame");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dbConn;
    }

    public List<Map<String,Object>> executeQuery(String query) throws SQLException{
        if(sonicServer == null)
            startDBServer(query);

        dbConn = getDBConn();
        Statement stmt = dbConn.createStatement();
        ResultSet rs = null;
        try{
            rs = stmt.executeQuery(query);
        }catch(SQLException sql){
            if(sql.getMessage().contains("Table already exists")){
                return null;
            }else if(sql.getMessage().contains("Unexpected token: POSITION in statement")){
                rs = stmt.executeQuery(query.toUpperCase().replace("POSITION", "\"POSITION\""));
            }else{
                throw sql;
            }
        }
        List<Map<String,Object>> results = new ArrayList<Map<String, Object>>();
        while(rs.next()){
            Map<String, Object> subMap = new HashMap<String, Object>();
            ResultSetMetaData rsmd = rs.getMetaData();
            for(int i = 1; i<=rsmd.getColumnCount(); i++){
                subMap.put(rsmd.getColumnLabel(i).toLowerCase(), rs.getObject(i));
            }
            results.add(subMap);
        }
        return results;
    }
}
