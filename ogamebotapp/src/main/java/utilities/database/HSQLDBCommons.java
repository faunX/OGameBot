package utilities.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utilities.fileio.FileOptions;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Created by jarndt on 5/8/17.
 */
public class HSQLDBCommons {
    static {
        FileOptions.setLogger(FileOptions.DEFAULT_LOGGER_STRING);
    }

    private static final Logger LOGGER = LogManager.getLogger(HSQLDBCommons.class.getName());

    private static String defaultName = "ogame";
    private static String dbName = "ogame";
    private HSQLDB db = null;
    private HSQLDBCommons() throws IOException, SQLException {
        db = new HSQLDB(dbName);
        String file = FileOptions.readFileIntoString(Database.SQL_SCRIPT_DIR + "create_tables.sql")
                .replace("SERIAL","INTEGER GENERATED BY DEFAULT AS IDENTITY");
        try {
            db.executeQuery(file);
        }catch (Exception e){
            LOGGER.error("ERROR in HSQLDBCommons query:\n"+file,e);
        }
    }

    static HSQLDBCommons instance;

    public static HSQLDBCommons getInstance() throws IOException, SQLException {
        if(instance == null)
            instance = new HSQLDBCommons();
        return instance;
    }

    public static HSQLDB getDatabase() throws IOException, SQLException {return getInstance().db;}
    public static void setDbName(int dbNameNumber){
        dbName=defaultName+dbNameNumber;
    }
    public static void setDbName(String dbName){
        dbName=defaultName+"_"+dbName;
    }

    public static List<Map<String, Object>> executeQuery(String query) throws IOException, SQLException {
        return getInstance().db.executeQuery(query);
    }

}
