package utilities.database;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.util.PSQLException;
import utilities.fileio.FileOptions;
import utilities.fileio.JarUtility;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * Created by jarndt on 5/8/17.
 */
public class Database {
    public static void main(String[] args) throws SQLException, ClassNotFoundException, IOException {
        stopDatabase();
        DATABASE_TYPE = HSQL;
        Database d = Database.newDatabaseConnection();
//        d.executeQuery("insert into users(username,password,first_name,last_name)values('a','a','a','a');");
        d.executeQuery("select * from server")
                .forEach(System.out::println);

//        try {
//            d.stopThisDatabase();
//        }catch (Exception e){}
//        System.exit(0);
    }

    static {
        FileOptions.setLogger(FileOptions.DEFAULT_LOGGER_STRING);
    }

    private static Database databaseConnection;

    public static synchronized Database getExistingDatabaseConnection() throws SQLException, IOException, ClassNotFoundException {
        if(databaseConnection == null)
            databaseConnection = newDatabaseConnection();
        return databaseConnection;
    }

    public static Database newDatabaseConnection() throws SQLException, IOException, ClassNotFoundException {
        return new Database(DATABASE,USERNAME,PASSWORD);
    }


    private static final Logger LOGGER = LogManager.getLogger(Database.class.getName());
    public static final String DATABASE = "localhost:5432/ogame", USERNAME = "ogame_user", PASSWORD = "ogame";

    public static final String POSTGRES = "POSTGRES", HSQL = "HSQL";

    public static String DATABASE_TYPE = POSTGRES;


    public static boolean checkForPostgres(String server, String username, String password){ /*127.0.0.1:5432/ogame*/
        try {
//            DriverManager.getConnection()
            Class.forName("org.postgresql.Driver");
            Connection connection = DriverManager.getConnection(
                    "jdbc:postgresql://"+server, username, password);
            if (connection != null)
                return true;
        } catch (Exception e) { LOGGER.log(Level.DEBUG,"",e); /* */ }
        return false;
    }

    public static String SQL_SCRIPT_DIR = FileOptions.cleanFilePath(JarUtility.getResourceDir()+"/database_config/"),
                            DATABASE_DIR= FileOptions.cleanFilePath(JarUtility.getResourceDir() + "/databases/postgres/");
    public static void setDatabaseDir(String databaseDir){
        DATABASE_DIR = databaseDir;
    }
    private Connection connection;
    private String server,username,password;

    public Database(String server, String username, String password) throws SQLException, ClassNotFoundException, IOException {
        this.server = server; this.username = username; this.password = password;
        getConnection();
        String file = null;
        try {
            executeQuery(file = FileOptions.readFileIntoString(SQL_SCRIPT_DIR + "create_tables.sql"));
        }catch (Exception e){
            if(!e.getMessage().equals("type not found or user lacks privilege: SERIAL"))
                LOGGER.error(file,e);
        }
    }
    int attempt = 0;
    private Connection getConnection() throws SQLException {
        if(connection == null) {
            if(DATABASE_TYPE.equals(POSTGRES)) {
                try {
                    Class.forName("org.postgresql.Driver");
                    try {
                        connection = DriverManager.getConnection(
                                "jdbc:postgresql://" + server, username, password);
                        connection.setAutoCommit(true);
                    } catch (PSQLException e) {
                        if (e.getMessage().contains("Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections."))
                            startDatabase();
                        if (e.getMessage().equals("FATAL: database \"ogame\" does not exist"))
                            init();

                    }
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
            }
            if(DATABASE_TYPE.equals(HSQL) || attempt++ > 5){
                LOGGER.error("Attempted to connect and start postgres 5 times, temporarily going to use hsql now....  " +
                                "\n\tFix Postgres.  All future database transactions will be to a HSQL database." +
                            "\nNext database connection will attempt to connect to postgres again.  On next successful" +
                            "\npostgres connection will dump HSQL data to postgres.");
                try {
                    newHSQLConnection();
                    DATABASE_TYPE = HSQL;
                } catch (IOException e) {
                    LOGGER.log(Level.DEBUG,"ERROR Starting HSQL database",e);
                }
                return null;
            }
            return getConnection();
        }
        return connection;
    }

    String databaseType = POSTGRES;
    private void newHSQLConnection() throws IOException, SQLException {
        connection = HSQLDBCommons.getDatabase().getDBConn();
        databaseType = HSQL;
    }

    private void init() throws ClassNotFoundException, SQLException, IOException {
        Class.forName("org.postgresql.Driver");
        Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/ogame", "postgres", "postgres");
        try {
            conn.prepareCall("CREATE USER OGAME_USER WITH PASSWORD 'ogame';")
                    .executeUpdate();
        }catch (PSQLException e){
            if(!e.getMessage().equals("ERROR: role \"ogame_user\" already exists"))
                throw e;
        }
        conn.prepareCall("CREATE DATABASE ogame OWNER = OGAME_USER; ")
            .executeUpdate();
    }

    public List<Map<String,Object>> executeQuery(String query) throws SQLException {
        Statement stmt = getConnection().createStatement();
        if(databaseType.equals(HSQL)){
            query.replaceAll("SERIAL","IDENTITY");
        }
        ResultSet rs = null;
        try{
            rs = stmt.executeQuery(query);
        }catch(SQLException sql){
            if(sql.getMessage().contains("Table already exists")
                    || sql.getMessage().contains("No results were returned by the query")){
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

    public static void startDatabase() throws IOException {
        String databaseDir = FileOptions.cleanFilePath(JarUtility.getResourceDir() + "/databases/");
        File postgresDir = new File(DATABASE_DIR);
        if(!(postgresDir.exists() && postgresDir.isDirectory())) {
            String zipFile = "postgres.zip";
            if(!new File(databaseDir+zipFile).exists())
                throw new IOException("No postgres.zip file found or no postgres directory at: "+postgresDir);
            String windowsCommand = "powershell.exe -nologo -noprofile -command \"& { Add-Type -A 'System.IO.Compression.FileSystem'; [IO.Compression.ZipFile]::ExtractToDirectory('"+zipFile+"', '.'); }\"";
            String macCommand = "unzip "+zipFile;
            String command = macCommand;
            if(FileOptions.OS.substring(0,3).equals(JarUtility.WINDOWS))
                command = windowsCommand;
            ExecutorService service = FileOptions.runSystemProcess(command, databaseDir);
        }
        if(!checkForPostgres(DATABASE,USERNAME,PASSWORD)) {
            String dataDir = "../postgres";
            String binDir = FileOptions.cleanFilePath(databaseDir + "/postgres/bin/"),
                    process = "pg_ctl";
            if(FileOptions.OS.substring(0,3).equals(JarUtility.LINUX)) {
                binDir = FileOptions.cleanFilePath(databaseDir + "/postgres/linux_bin/");
                process = binDir+"pg_ctl";
            }if(FileOptions.OS.substring(0,3).equals(JarUtility.WINDOWS)) {
                binDir = FileOptions.cleanFilePath(DATABASE_DIR+"/App/PgSQL/bin/");
                dataDir = "../../../Data/data";
                process = "pg_ctl.exe";
            }
            try {
                FileOptions.runSystemProcess(process + " -D "+dataDir+" -l server.log start",
                        binDir);
            }catch (IOException io){
                LOGGER.debug("Database connection failed",io);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void stopThisDatabase() throws IOException, SQLException {
        stopDatabase();
        HSQLDBCommons.getDatabase().stopDBServer();
    }

    public static void stopDatabase() throws IOException {
        String databaseDir = FileOptions.cleanFilePath(JarUtility.getResourceDir() + "/databases/");
        String dataDir = "../postgres";
        String binDir = FileOptions.cleanFilePath(databaseDir + "/postgres/bin/"),
                process = "pg_ctl";
        if(FileOptions.OS.substring(0,3).equals(JarUtility.LINUX)) {
            binDir = FileOptions.cleanFilePath(databaseDir + "/postgres/linux_bin/");
            process = binDir+"pg_ctl";
        }if(FileOptions.OS.substring(0,3).equals(JarUtility.WINDOWS)) {
            binDir = FileOptions.cleanFilePath(DATABASE_DIR+"/App/PgSQL/bin/");
            dataDir = "../../../Data/data";
            process = "pg_ctl.exe";
        }
        FileOptions.runSystemProcess(process + " -D "+dataDir+" -l server.log stop",
                binDir);
    }
}
