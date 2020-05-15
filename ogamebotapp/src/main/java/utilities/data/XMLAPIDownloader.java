package utilities.data;

/**
 * Created by jarndt on 5/8/17.
 */
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import utilities.database.Database;
import utilities.database.HSQLDBCommons;
import utilities.database.XMLToDatabase;
import utilities.fileio.FileOptions;
import utilities.fileio.JarUtility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Created by jarndt on 9/27/16.
 */
public class XMLAPIDownloader {
    public static void main(String[] args) throws IOException, SQLException, SchedulerException {
        System.out.println("Downloading");
        XMLAPIDownloader.downloadAllXML();
        XMLToDatabase.parseAllFilesIntoDatabase();
        System.out.println("Done Downloading");

//        startDownloadXMLThreads();
    }
    static {
        FileOptions.setLogger(FileOptions.DEFAULT_LOGGER_STRING);
    }
    private static final Logger LOGGER = LogManager.getLogger(XMLAPIDownloader.class.getName());



    private static Scheduler scheduler;
    private static Scheduler getScheduler() throws SchedulerException {
        if(scheduler == null){
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
        }
        return scheduler;
    }
    public static void scheduleJob(String cronSchedule) throws SchedulerException {
        if(getScheduler().getJobKeys(GroupMatcher.jobGroupEquals("group1")).stream().filter(a->a.getName().equals("dummyJobName")).collect(Collectors.toList()).size() != 0)
            return;
        JobDetail job = JobBuilder.newJob(DownloadXMLFileJob.class)
                .withIdentity("dummyJobName", "group1").build();

        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity("dummyTriggerName", "group1")
                .withSchedule(
                        CronScheduleBuilder.cronSchedule(cronSchedule))
                .build();

        //schedule it
        getScheduler().scheduleJob(job, trigger);
    }
    public static class DownloadXMLFileJob implements Job{
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            System.out.println("Downloading");
            XMLAPIDownloader.downloadAllXML();
            XMLToDatabase.parseAllFilesIntoDatabase();
            System.out.println("Done Downloading");
        }
    }

    public static void startDownloadXMLThreads() throws SchedulerException {
//        scheduleJob("30 2 * * * ?"); //run download job at 2:30 am every day
        scheduleJob("0 0 0/2 * * ?"); //run download job every 2 hours
    }




    public static void downloadAllXML(){
        FileOptions.runConcurrentProcess(Arrays.asList(universeNumbers).stream().map(a->(Callable)()->{
            downloadAllFiles(a+"");
            return null;
        }).collect(Collectors.toList()));
    }

    final static public String regReplace = "{UNI}", numOneReplace = "{NUM1}", numTwoReplace = "{NUM2}";
    public static final String
            UNIVERSES               = "universes",
            LOCALIZATION            = "localization",
            SERVER_DATA_FILE        = "server",
            //The homeplanet is the one with the lowest planet id
            UNIVERSE_FILE           = "planets",
            PLAYERS_FILE            = "players",
            ALLIANCES_FILE          = "alliances",
            HIGHSCORE_PLAYER_FILE   = "highscore";
//            HIGHSCORE_ALLIANCE_FILE = "highscore_alliance";

    final static public String
            UNIVERSES_DATA       = "https://"+regReplace+"/api/universes.xml",
            LOCALIZATION_DATA    = "https://"+regReplace+"/api/localization.xml",
            SERVER_DATA          = "https://"+regReplace+"/api/serverData.xml",
            UNIVERSE             = "https://"+regReplace+"/api/universe.xml",
            PLAYERS              = "https://"+regReplace+"/api/players.xml",
            ALLIANCES            = "https://"+regReplace+"/api/alliances.xml",
            HIGHSCORE_PLAYER     = "https://"+regReplace+"/api/highscore.xml?category="+numOneReplace+"&type="+numTwoReplace;
//            HIGHSCORE_ALLIANCE   = "https://"+regReplace+"/api/highscore.xml?category=2&type=1";


    final static public String
            PLAYERS_UPDATE_INTERVAL     = 1+"DAY",
            UNIVERSE_UPDATE_INTERVAL    = 1+"WEEK",
            HIGHSCORE_UPDATE_INTERVAL   = 1+"HOUR",
            SERVER_UPDATE_INTERVAL      = 1+"DAY",
            PLAYER_UPDATE_INTERVAL      = 1+"WEEK";

    final public static HashMap<String,String> FILE_DATA = new HashMap<>();
    public static String DOWNLOAD_DIR = FileOptions.cleanFilePath(JarUtility.getResourceDir() + "/ogame/xml_downloads/");

    static{
        FILE_DATA.put(SERVER_DATA_FILE,SERVER_DATA);
        FILE_DATA.put(UNIVERSE_FILE,UNIVERSE);
        FILE_DATA.put(PLAYERS_FILE,PLAYERS);
        FILE_DATA.put(ALLIANCES_FILE,ALLIANCES);
        FILE_DATA.put(LOCALIZATION,LOCALIZATION_DATA);
        FILE_DATA.put(UNIVERSES,UNIVERSES_DATA);
//        FILE_DATA.put(HIGHSCORE_PLAYER_FILE,HIGHSCORE_PLAYER);
//        FILE_DATA.put(HIGHSCORE_ALLIANCE_FILE,HIGHSCORE_ALLIANCE);
    }


    public static boolean readLastUpdate(String dir, String file){
        new File(dir).mkdirs();
        try {
            String lastUpdate = FileOptions.readFileIntoString(dir+file).trim();
            LocalDateTime dateTime = LocalDateTime.from(FileOptions.FORMAT.parse(lastUpdate));

            if(dateTime.plusDays(1).isAfter(LocalDateTime.now()))
                return true;

        } catch (IOException e) {        }
        return false;
    }

    public static void downloadAllFiles(final String universeNumber){
        final String dir = DOWNLOAD_DIR +universeNumber;
        new File(dir).mkdirs();

        List<String> highscore = new ArrayList<>();
        for(int i = 1; i<=2; i++)
            for(int j = 0; j<=7; j++)
                FILE_DATA.put(HIGHSCORE_PLAYER_FILE+"_"+i+"_"+j,
                        HIGHSCORE_PLAYER.replace(numOneReplace,i+"").replace(numTwoReplace,j+""));

        FileOptions.runConcurrentProcess(FILE_DATA.entrySet().stream().map(a->(Callable)()->{
            String uni = "s"+universeNumber+"-en.ogame.gameforge.com";
            try {
                URL website = new URL(a.getValue().replace(regReplace, uni));
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                FileOutputStream fos = new FileOutputStream(dir+"/"+a.getKey()+"_"+universeNumber+".xml");
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (IOException e) {
                LOGGER.debug("Download Failed for: "+uni,e);
            }
//            System.out.println("Downloaded: "+uni);
            return null;
        }).collect(Collectors.toList()));

//        System.out.println("Done Downloading universe: "+universeNumber);


        writeDateToFile(dir,universeNumber);
    }

    private static void writeDateToFile(String dir, String universeNumber) {
        try {
            FileOptions.writeToFileOverWrite(dir+"/last_update_"+universeNumber,LocalDateTime.now().format(FileOptions.FORMAT));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void parseAllFiles() {
        Arrays.asList(universeNumbers).stream().forEach(a->{
            final String dir = DOWNLOAD_DIR +a;
            if(!readLastUpdate(dir,"/last_update_"+a)) {
                downloadAllFiles(a + "");
                parseFileIntoDatabase(a + "");
            }
            writeDateToFile(dir, a+"");
        });
    }public static void parseFiles(int universeNumber) {
        if(!Arrays.asList(universeNumber).contains(universeNumber))
            throw new IllegalArgumentException(universeNumber+" is not a valid universeNumber");

        final String dir = DOWNLOAD_DIR +universeNumber;
        new File(dir).mkdirs();
        if(!readLastUpdate(dir,"/last_update_"+universeNumber)) {
            downloadAllFiles(universeNumber + "");
            parseFileIntoDatabase(universeNumber + "");
        }

        writeDateToFile(dir, universeNumber+"");
    }
    public static Integer[] universeNumbers = {
            1,
            117, //QUANTUM
            118,
            120,
            122,
            123,
            128,//BETELGEUSE
            129,//CYGNUS
            130,//DEIMOS
            131,//ERIDANUS
            132,//FIDIS
            133,//GANIMED
            134,
            135,
            136,
            137,
            138,
            139,
            140,
            141,
            142,
            143,
            144,
            145
    };
    //    private static List<String> unique = Arrays.asList(UNIVERSE,); //for debugging
    public static void parseFileIntoDatabase(final String universeNumber) {
        FileOptions.runConcurrentProcess(FILE_DATA.entrySet().stream().map(a->(Callable)()->{
            try {
                new XMLFileParser(a,universeNumber).parse();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList()));

    }

    private static class XMLFileParser{
        Element xmlFile;
        String type, universeNumber, name;

        public XMLFileParser(Map.Entry<String, String> type,String universeNumber){
            SAXBuilder builder = new SAXBuilder();
            try {
                xmlFile = builder.build(DOWNLOAD_DIR +type.getKey()+"_"+universeNumber+".xml").getRootElement();
                this.type = type.getValue();
                this.universeNumber = universeNumber;
                this.name = type.getKey();
            } catch (JDOMException | IOException e) {
                e.printStackTrace();
            }
        }

        public void parse() throws IOException, SQLException {
            switch (type){
                case SERVER_DATA:
                    parseUniverseFiles(universeNumber); break;
                case HIGHSCORE_PLAYER:
//                case HIGHSCORE_ALLIANCE:
                case PLAYERS:
                case UNIVERSE:
                case ALLIANCES:
                    parseAttributeFiles(universeNumber); break;
            }
        }

        private void parseAttributeFiles(String universeNumber) throws IOException, SQLException {
            String regReplaceNames = "{NAMES}", regReplaceValues = "{VALUES}";

            xmlFile.getChildren().parallelStream().forEach( e-> {
                String query = "INSERT INTO "+name+"(" + regReplaceNames + ") VALUES (" + regReplaceValues + ");";

                String names = "", values = "", updateQuery;
                updateQuery = "UPDATE "+name+" SET ";
                String extra = "", id = null;
                if(e.getChildren().size() != 0)
                    for(Element el : e.getChildren())
                        for(Attribute a : el.getAttributes()) {
                            names += "," + el.getName()+"_"+a.getName();
                            values += "," + "'" + a.getValue() + "'";

                            extra += ", "+el.getName()+"_"+a.getName()+" = '"+a.getValue()+"'";
                        }

                for(Attribute a : e.getAttributes()) {
                    names += "," + a.getName();
                    values += "," + "'" + a.getValue() + "'";

                    extra += ", "+a.getName()+" = '"+a.getValue()+"'";
                    if(a.getName().equals("id"))
                        id =a.getValue();
                }
                names+=",universe_id";
                values+=","+universeNumber;

                names = names.substring(1);
                values = values.substring(1);
                query = query.replace(regReplaceNames, names).replace(regReplaceValues, values);
                try {
                    HSQLDBCommons.executeQuery(query);
                } catch (Exception ex) {
                    if (ex.getMessage().contains("unique constraint")) {
                        query=updateQuery;
                        extra = extra.substring(1);
                        query+=extra+" where id = "+id+" and universe_id = "+universeNumber;
                        try {
                            HSQLDBCommons.executeQuery(query);
                        }catch (Exception exc){
                            exc.printStackTrace();
                        }
                    }else{
                        System.err.println("FAILED QUERY: "+query);
                        ex.printStackTrace();
                    }
                }
            });
            System.out.println("DONE PARSING: "+name+" universe: "+universeNumber);
        }

        private void parseUniverseFiles(String universeNumber) throws IOException, SQLException {
            String regReplaceNames = "{NAMES}", regReplaceValues = "{VALUES}";
            String query = "INSERT INTO "+name+"(" + regReplaceNames + ") VALUES (" + regReplaceValues + ");";

            String names = "", values = "", domain = "", extra = "";
            for (Element e : xmlFile.getChildren()) {
                names += "," + e.getName();
                values += "," +"'"+ e.getText()+"'";
                extra += ", "+e.getName()+" = '"+e.getText()+"'";

                if(e.getName().equals("domain"))
                    domain= e.getValue();
            }
            names = names.substring(1);
            values = values.substring(1);
            query = query.replace(regReplaceNames, names).replace(regReplaceValues, values);
            try {
                HSQLDBCommons.executeQuery(query);
            } catch (Exception e) {
                if (e.getMessage().contains("unique constraint")) {
                    query = "UPDATE "+name+" SET ";
                    extra = extra.substring(1);
                    query+=extra+" where domain = '"+domain+"'";
                    HSQLDBCommons.executeQuery(query);
                } else{
                    System.err.println("FAILED QUERY: "+query);
                    throw e;
                }
            }
            System.out.println("DONE PARSING: "+name+" universe: "+universeNumber);
        }
    }
}