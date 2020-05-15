package runner;

import bot.Bot;
import ogame.objects.Email;
import ogame.objects.User;
import ogame.objects.game.data.Server;
import ogame.pages.Login;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;
import utilities.data.XMLAPIDownloader;
import utilities.database.Database;
import utilities.database.JsonPlanetData;
import utilities.email.OneEmail;
import utilities.fileio.FileOptions;
import utilities.fileio.JarUtility;
import utilities.password.PasswordEncryptDecrypt;

import java.io.*;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.quartz.CronScheduleBuilder.cronSchedule;

/**
 * Created by jarndt on 5/2/17.
 */
public class Runner {
    public static void main(String[] args) throws Exception {
        run(args);
    }
    static {
        FileOptions.setLogger(FileOptions.DEFAULT_LOGGER_STRING);
    }
    private static final Logger LOGGER = LogManager.getLogger(Runner.class.getName());

    /**
     * Start the Ogame bot with this method and arguments.
     *
     * @param args
     * @throws IOException
     * @throws URISyntaxException
     * @throws GeneralSecurityException
     */
    public static void run(String[] args) throws Exception {
        parseCommandLineArgs(args);
        extractFiles();
        createAndStartDatabase();
        startQuarzJobs();

        startBots();
    }

    private static void startBots() throws Exception {
//        new Bot(new Login(User.newRandomUser(Server.QUANTUM))).startBot();
        new Bot(
                new Login(
                        new User(new Email("bc3ew9p4yh9qdv8wvj1h@michaelgutin.one"),
                                "bc3ew9p4yh9qdv8wvj1h","ib5f982wc4oedy2q1xfn", Server.QUANTUM)
                )
        ).startBot();
    }

    private static void createAndStartDatabase() throws IOException {
//        if(FileOptions.OS.substring(0,3).equals(JarUtility.LINUX))
//            JarUtility.exportJarDirectory("linux_bin");
        String f = null;
        if(FileOptions.OS.substring(0,3).equals(JarUtility.WINDOWS)) {
//            f = JarUtility.exportJarDirectory("postgres_windows");
            Database.setDatabaseDir(FileOptions.cleanFilePath(JarUtility.getResourceDir() + "/databases/postgres_windows/"));
        }
//        }else
        f = JarUtility.exportJarDirectory("postgres");
        if(f!=null) {
            String postgresDir = Database.DATABASE_DIR;
            FileOptions.runConcurrentProcess(
                    FileOptions.readFileIntoListString(postgresDir + "permissions.txt")
                            .stream().map(a -> (Callable) () -> {
                        String[] file = a.split(",");
                        file[0] = FileOptions.cleanFilePath(postgresDir + file[0].substring(1));
                        if (file[3].equals("d"))
                            new File(file[0]).mkdirs();
                        else
                            new File(file[0]).createNewFile();
                        LOGGER.log(Level.INFO,"Working on: "+file[0]+" chmod "+file[2]);
                        return FileOptions.setPermissionUnix(Integer.parseInt(file[2]), file[0]);
                    }).collect(Collectors.toList())
                    , 20);
        }
        Database.startDatabase();
    }

    public static void startQuarzJobs() throws SchedulerException {
        XMLAPIDownloader.startDownloadXMLThreads();
//        OneEmail.startOneEmailCreateThread();
        JsonPlanetData.startJsonToDatabaseThread();
    }

    public static List<String> extractFiles() throws IOException, URISyntaxException {
        return JarUtility.extractFiles(Arrays.asList(
            "email_list.txt", "failed.txt", "working.txt",
            "ogame_ship_info.csv", "research_info.csv",
            "facilities_info.csv", "building_info.csv", "shipyard_info.csv",
            "defense_info.csv", "mapper.csv", "create_tables.sql","postgres_commands.sql"
        ));
    }

    private static void parseCommandLineArgs(String[] args) throws IOException, GeneralSecurityException, SQLException, ClassNotFoundException {
        if(args!=null && args.length != 0){
            List<String> ecryptSwitches = new ArrayList<>(Arrays.asList("-encrypt","-e"));
            List<String> defaultDriver = new ArrayList<>(Arrays.asList("-defaultDriver","-dd"));
            List<String> exportPath = new ArrayList<>(Arrays.asList("-exportPath","-ep"));
            List<String> webDriverPath = new ArrayList<>(Arrays.asList("-webDriverPath","-wdp"));
            List<String> runQuery = new ArrayList<>(Arrays.asList("-runQuery","-q"));
            for(int i = 0; i<args.length; i++){
                if(ecryptSwitches.contains(args[i])){
                    try{
                        PasswordEncryptDecrypt.encryptReader();
                    }catch (IndexOutOfBoundsException e){
                        System.err.println("No value to encrypt");
                    }
                    System.exit(0);
                }
                try {
                    if(defaultDriver.contains(args[i]))
                        JarUtility.setDefaultDrivers(Arrays.asList(args[i + 1].split(",")));
                    if(exportPath.contains(args[i]))
                        JarUtility.setExportPath(args[i + 1]);
                    if(webDriverPath.contains(args[i]))
                        JarUtility.setWebDriverPath(args[i + 1]);
                    if(runQuery.contains(args[i])) {
                        new Database(Database.DATABASE, Database.USERNAME, Database.PASSWORD)
                                .executeQuery(args[i + 1]);
                        System.exit(0);
                    }
                }catch(IndexOutOfBoundsException e){
                    System.err.println("No given switch option for: "+args[i]);
                }
            }
        }
    }
}
