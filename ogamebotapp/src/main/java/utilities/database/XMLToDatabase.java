package utilities.database;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import utilities.data.XMLAPIDownloader;
import utilities.fileio.FileOptions;
import utilities.fileio.JarUtility;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by jarndt on 5/8/17.
 */
public class XMLToDatabase {
    public static void main(String[] args) throws JDOMException, IOException, SQLException, ClassNotFoundException {
        parseAllFilesIntoDatabase();
        System.out.println("DONE");
    }

    static {
        FileOptions.setLogger(FileOptions.DEFAULT_LOGGER_STRING);
    }
    private static final Logger LOGGER = LogManager.getLogger(XMLToDatabase.class.getName());

    public static void parseAllFilesIntoDatabase(){
        FileOptions.runConcurrentProcess(
                FileOptions.getAllDirectories(XMLAPIDownloader.DOWNLOAD_DIR)
                        .stream().map(a->(Callable)()->{
                    try {
                        new XMLToDatabase(Database.DATABASE, Database.USERNAME, Database.PASSWORD, a.getName())
                                .parseAllFiles(Arrays.asList(
                                        FileOptions.getAllFilesContains(a.getAbsolutePath(), "server").get(0).getAbsolutePath(),
                                        FileOptions.getAllFilesContains(a.getAbsolutePath(), "alliances").get(0).getAbsolutePath(),
                                        FileOptions.getAllFilesContains(a.getAbsolutePath(), "players").get(0).getAbsolutePath(),
                                        FileOptions.getAllFilesContains(a.getAbsolutePath(), "planets").get(0).getAbsolutePath()
                                        ),
                                        Arrays.asList("server", "alliance", "player", "planet"))
                                .parseHighscore(a.getAbsolutePath());
                    }catch (Exception e){LOGGER.debug("",e);}
//                    System.out.println("Done writing to Database: "+a.getName());
                    return null;
                }).collect(Collectors.toList())
        );
    }

    String database, username, password, serverId, timestamp;
    List<String> files;
    SAXBuilder builder = new SAXBuilder();

    private List<String> ignoreList = Arrays.asList("deftotf","rapidfire");
    private HashMap<String,String> replaceMap = new HashMap<>();

    public XMLToDatabase(String database, String username, String password, String serverId) {
        this.database = database;
        this.username = username;
        this.password = password;
        this.serverId = serverId;
        init();
    }

    private void init(){
        replaceMap.put("number","server_id");
        replaceMap.put("alliance","alliance_id");
        replaceMap.put("player","player_id");
    }

    private String playerTimestamp, allianceTimestamp;


    private void parseHighscore(String absolutePath) throws IOException, JDOMException, SQLException, ClassNotFoundException {
        List<String> types = Arrays.asList(
                "total","economy","research","military","military_built","military_destroyed","military_total","honor"
        );
        List<String> ap = Arrays.asList("player_highscore","alliance_highscore");
        StringBuilder builder = new StringBuilder("");
        for (int j = 1; j <= 2; j++) {
            final int jj = j;
            List<File> highscore = FileOptions.getAllFilesContains(absolutePath, "highscore_" + j + "_");
//            FileOptions.runConcurrentProcess(IntStream.range(0, highscore.size()).boxed().map(i -> (Callable) () -> {
            for (int i = 0; i < highscore.size(); i++)
                builder.append(parseHighscoreXML(ap.get(jj-1),highscore.get(i), i+""));
//                return null;
//            }).collect(Collectors.toList()));
        }
        for(String s : builder.toString().split(";"))
            try {
                Database.getExistingDatabaseConnection().executeQuery(s);
            }catch (Exception e){/*DO NOTHING*/}
    }

    private String parseHighscoreXML(String tableName, File fileContents, String type) throws JDOMException, IOException {
        Document builder = new SAXBuilder().build(fileContents);
        String timestamp = builder.getRootElement().getAttributeValue("timestamp");
        String t = tableName.contains("player")?"player":"alliance";
        String tTimestamp = t.equals("player")?playerTimestamp:allianceTimestamp;
        String idR = "[ID]", positionR = "[POSITION]", scoreR = "[SCORE]", shipsR = "[SHIPS]";
        String insert = new StringBuilder(
                "insert into "+tableName+"(server_id,type,timestamp,"+t+"_t,"+t+"_id,position,score"+(t.equals("player") && type.equals("3")?",ships":"")+") " +
                        "values("+serverId+",'"+type+"',"+timestamp+","+tTimestamp+","+idR+","+positionR+","+scoreR+(t.equals("player") && type.equals("3")?","+shipsR:"")+") ON CONFLICT DO NOTHING;\n"
        ).toString();
        HashMap<String,String> replace = new HashMap<>();
        replace.put("id",idR);
        replace.put("position",positionR);
        replace.put("score",scoreR);
        replace.put("ships",shipsR);
        StringBuilder stringBuilder = new StringBuilder("");
        for(Element e : builder.getRootElement().getChildren()) {
            String v = insert;
            for (Attribute a : e.getAttributes())
                v = v.replace(replace.get(a.getName()),a.getValue());
            if(e.getAttribute("ships") == null)
                v = v.replace(replace.get("ships"),"0");
            stringBuilder.append(v);
        }

        return stringBuilder.toString();
    }


    public XMLToDatabase parseAllFiles(List<String> files, List<String> tableNames) throws JDOMException, IOException, SQLException, ClassNotFoundException {
        if(files.size() != tableNames.size())
            throw new IllegalArgumentException("files list and table names list must be same size, files:"+files.size()+", tableNames: "+tableNames.size());
        String result = null;
        for (int i = 0; i < files.size(); i++) {
            replaceMap.put("id", tableNames.get(i)+"_id");
            if (files.get(i).contains("server")) {
                replaceMap.put("name","server_name");
                result = insertResult(parseFileServer(files.get(i)), tableNames.get(i));
                replaceMap.remove("name");
            }else
                result = insertResultList(parseFile(files.get(i)), tableNames.get(i));
            try {
                new Database(database, username, password).executeQuery(result);
            }catch (Exception e){
                if(!e.getMessage().contains("duplicate key value violates unique constraint")
                        && !e.getMessage().contains("\"planet\" violates foreign key constraint \"planet_player_id_fkey\"")) {
                    System.out.println(files.get(i)+"\n"+result);
                    throw e;
                }
            }
        }
        return this;
    }
    private List<String> parseFileServer(String file) throws JDOMException, IOException{
        String regex = "[A-Z]";
        Pattern p = Pattern.compile(regex);

        SAXBuilder builder = new SAXBuilder();
        Element v = builder.build(file).getRootElement();
        timestamp = v.getAttributeValue("timestamp");
        List<String> vvv = v.getChildren().stream().map(a -> {
            String s = a.getName(), vv = a.getText();
            if (replaceMap.containsKey(s))
                s = replaceMap.get(s);
            if (ignoreList.contains(s.toLowerCase()))
                return s + splitString + vv;
            return s.replaceAll("([A-Z])", "_$1").toLowerCase() + splitString + "'" + vv + "'";
        }).collect(Collectors.toList());
        vvv.add("timestamp"+splitString+"'"+timestamp+"'");
        return vvv;
    }
    private static String splitString = "SPLIT_BY_ME";
    private List<List<String>> parseFile(String file) throws JDOMException, IOException{
        String regex = "[A-Z]";
        Pattern p = Pattern.compile(regex);
        SAXBuilder builder = new SAXBuilder();
        Element v = builder.build(file).getRootElement();
        timestamp = v.getAttributeValue("timestamp");
        if(file.contains("players"))
            playerTimestamp = timestamp;
        else if(file.contains("alliances"))
            allianceTimestamp = timestamp;
        return v.getChildren().stream().map(a->{
            List<String> m = a.getAttributes().stream().map(b -> attributeMapper(b)).collect(Collectors.toList());
            String id = replaceMap.get("id");
            if(a.getChild("moon")!=null) {
                replaceMap.put("id","moon_id");
                replaceMap.put("name","moon_name");
                replaceMap.put("size","moon_size");
                m.addAll(a.getChild("moon").getAttributes().stream().map(b -> attributeMapper(b)).collect(Collectors.toList()));
            }
            replaceMap.remove("size");
            replaceMap.remove("name");
            replaceMap.put("id",id);
            m.add("timestamp"+splitString+"'"+timestamp+"'");
            return m;
        }).collect(Collectors.toList());
    }
    private String attributeMapper(Attribute b){
        String s = b.getName(), vv = b.getValue();
        if (replaceMap.containsKey(s))
            s = replaceMap.get(s);
        if (ignoreList.contains(s.toLowerCase()))
            return s + splitString + vv;
        return s.replaceAll("([A-Z])", "_$1").toLowerCase() + splitString + "'" + vv + "'";
    }

    private String insertResult(List<String> queryList, String tableName) throws SQLException, IOException, ClassNotFoundException {
        StringBuilder query = new StringBuilder("INSERT INTO "+tableName.toUpperCase()+"("),
                res = new StringBuilder(")\n\tVALUES(");
        for(String q : queryList){
            String[] vv = q.split(splitString);
            query.append(vv[0]+",");
            res.append(vv[1]+",");
        }
        query = new StringBuilder(query.substring(0,query.length()-1));
        res = new StringBuilder(res.substring(0,res.length()-1)).append(") ON CONFLICT DO NOTHING;");
        return query.append(res).toString();
    }

    private String insertResultList(List<List<String>> queryListList, String tableName) throws SQLException, IOException, ClassNotFoundException {
        StringBuilder result = new StringBuilder("");
        for(List<String> queryList : queryListList) {
            queryList.add("server_id"+splitString+"'"+serverId+"'");
            result.append(insertResult(queryList, tableName)).append("\n");
        }
        return result.toString();
    }
}
