package utilities.fileio;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utilities.database.Database;
import utilities.webdriver.DriverController;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by jarndt on 5/2/17.
 */
public class JarUtility {
    static {
        FileOptions.setLogger(FileOptions.DEFAULT_LOGGER_STRING);
    }

    private static final Logger LOGGER = LogManager.getLogger(JarUtility.class.getName());


    public static final String  WINDOWS = "win", LINUX = "lin", MAC = "mac",
                                CHROME = DriverController.CHROME, GECKO = DriverController.FIREFOX, FIREFOX = GECKO, PHANTOMJS = DriverController.PHANTOMJS;
    public static final String s = FileOptions.SEPERATOR;

    private String  resourceDir     = FileOptions.cleanFilePath(System.getProperty("user.dir")+"/src/main/resources/"),
                    webDriverPath   = resourceDir+"web_drivers",
                    exportPath      = System.getProperty("user.dir")+s+"jarResources";

    Class className = JarUtility.class;
    String classNameString = "JarUtility";


    private HashMap<String,HashMap<String,String>> drivers = new HashMap<>();
    private List<String> defaultDrivers = new ArrayList<>(Arrays.asList(PHANTOMJS));
    private List<String> extractedFiles = new ArrayList<>();

    private void init(){
        List<String> os = Arrays.asList(WINDOWS,LINUX,MAC);
        List<String> driverName = Arrays.asList(CHROME,GECKO,PHANTOMJS);
        for(String s : os) {
            HashMap<String, String> map = new HashMap<>();
            String name = s.substring(0,3);
            for(String driver : driverName)
                map.put(driver,name+"_"+driver+"driver"+(s.equals(WINDOWS)?".exe":""));
            drivers.put(name, map);
        }

    }
    private static JarUtility instance;
    private JarUtility(){init();}
    public static JarUtility getInstance(){
        if(instance == null)
            instance = new JarUtility();
        return instance;
    }

    public static String getResourceDir(){
        return getInstance().resourceDir;
    }
    public static void setResourceDir(String resourceDir){
        getInstance().resourceDir = resourceDir;
    }

    public static void setClassAndClassString(Class className, String classNameString){
        getInstance().className = className;
        getInstance().classNameString = classNameString;
    }

    public static String getWebDriverPath(){
        return getInstance().webDriverPath;
    }
    public static void setWebDriverPath(String webDriverPath){
        getInstance().webDriverPath = webDriverPath;
    }

    public static List<String> getDefaultDrivers() {
        return getInstance().defaultDrivers;
    }

    public static void setDefaultDrivers(List<String> defaultDrivers) {
        getInstance().defaultDrivers = defaultDrivers;
    }

    public static HashMap<String, HashMap<String, String>> getDrivers() {
        return getInstance().drivers;
    }

    public static String getExportPath() {
        return getInstance().exportPath;
    }

    public static void setExportPath(String exportPath) {
        getInstance().exportPath = exportPath;
    }
    public static List<String> extractFiles() throws IOException, URISyntaxException {
        return extractFiles(new ArrayList<>(), null);
    }
    public static List<String> extractFiles(List<String> resourceFiles) throws IOException, URISyntaxException {
        return extractFiles(new ArrayList<>(), resourceFiles);
    }
    public static List<String> getExtractedFiles(){
        return getInstance().extractedFiles;
    }

    public static List<String> extractFiles(List<String> files, List<String>...resourceFiles) throws URISyntaxException, IOException {
        List<String> resourcesFileList = new ArrayList<>();
        if(resourceFiles != null && resourceFiles.length != 0)
            resourcesFileList.addAll(resourceFiles[0]);
        if(getInstance().className.getResource(getInstance().classNameString +".class").toString().startsWith("jar:")){
            System.out.println("Extracting jar resources, this is only done once");
            //YOU ARE IN A JAR FILE
            String path = getExportPath();
            new File(path).mkdir();
            List<String> findFiles = new ArrayList<>();
            findFiles.addAll(resourcesFileList);
            String jarFileLocation = FileOptions.getAllFilesEndsWith(System.getProperty("user.dir"),".jar")
                    .stream().filter(a->a.getName().contains("ogame")).collect(Collectors.toList()).get(0).getAbsolutePath();
            try {
                jarFileLocation = new File(getInstance().className.getProtectionDomain()
                        .getCodeSource().getLocation().toURI().getPath()).getAbsolutePath();
            }catch (Exception e){}

            List<String> fil = new ArrayList<>();
            for(String v : findFiles) {
                List<File> f = FileOptions.getAllFilesWithName(path, v);
                if(f != null && f.size()!=0) {
                    fil.add(f.get(0).getAbsolutePath());
                    continue;
                }if (!new File(path + "/" + v).exists())
                    try {
                        fil.add(exportJarResources(path, jarFileLocation, v).getAbsolutePath());
                    } catch (NullPointerException e) {}
                else
                    fil.add(path + "/" + v);
            }
            files.addAll(fil);

            //extract all webdrivers for your perticular os
            String os = FileOptions.OS;
            String nPath = path+s+"web_drivers";
            if(new File(getWebDriverPath()).exists())
                nPath = getWebDriverPath();
            setWebDriverPath(nPath);

            if(!new File(nPath).exists())
                new File(nPath).mkdir();
            Map<String,String> filesDirs = new HashMap<>();
            for(String driver : getDefaultDrivers())
                filesDirs.put(driver,getDrivers().get(os.substring(0,3)).get(driver));

            final String p = path;
            for(String dir : filesDirs.keySet()){
                if(!new File(p).exists())
                    new File(p).mkdirs();
                if(new File(nPath+s+dir+s+filesDirs.get(dir)).exists())
                    continue;
                List<File> f = FileOptions.getAllFilesWithName(path, filesDirs.get(dir));
                if(f != null && f.size()!=0) {
                    files.add(f.get(0).getAbsolutePath());
                    List<String> v = Arrays.asList(f.get(0).getAbsolutePath().split("\\\\"));
                    String WDP = "";
                    for(String vv : v)
                        if (vv.equals("web_drivers")) {
                            WDP += FileOptions.SEPERATOR+vv;
                            break;
                        }else WDP += FileOptions.SEPERATOR+vv;
                    setWebDriverPath(WDP);
                    continue;
                }

                File s = exportJarResources(p,jarFileLocation,filesDirs.get(dir));//.getAbsolutePath();
                s.setExecutable(true);
                files.add(s.getAbsolutePath());
                List<String> v = Arrays.asList(s.getAbsolutePath().split(
                        FileOptions.OS.substring(0,3).equals(WINDOWS) ?
                        "\\\\" :
                        FileOptions.SEPERATOR
                ));
                String WDP = "";
                for(String vv : v)
                    if (vv.equals("web_drivers")) {
                        WDP += FileOptions.SEPERATOR+vv;
                        break;
                    }else WDP += FileOptions.SEPERATOR+vv;
                setWebDriverPath(WDP);
                if(s != null)
                    System.out.println("Extracted: "+s.getAbsolutePath());
            }
            File f = new File(FileOptions.cleanFilePath(getExportPath()+"/"));
            f.mkdirs();
            setResourceDir(f.getAbsolutePath());
            files.forEach(a -> {
                try {
                    FileOptions.setPermissionUnix(777, a);
                } catch (IOException e) {
                    LOGGER.error(a,e);
                }
            });
        }else{
            //YOU ARE RUNNING FROM AN IDE
            List<String> v = resourcesFileList;
            for(String s : v)
                try {
                    files.add(FileOptions.findFile(FileOptions.DEFAULT_DIR, s)
                            .get(0).getAbsolutePath());
                }catch (IndexOutOfBoundsException e){
                    LOGGER.error("Can't find file: "+s+" on path: "+FileOptions.DEFAULT_DIR);
                }
        }
        getExtractedFiles().addAll(files);
        return files;
    }
    public static File exportJarResources(String destDir, String jarFileLocation, String fileToExtract) throws IOException {
        if(fileToExtract == null)
            return null;
//		if(new File(destDir+s+fileToExtract).exists())
//			return null;
        java.util.jar.JarFile jarFile = new java.util.jar.JarFile(new java.io.File(jarFileLocation)); //jar file path(here sqljdbc4.jar)
        java.util.Enumeration<java.util.jar.JarEntry> enu= jarFile.entries();
        File fl = null;
        while(enu.hasMoreElements()){
            java.util.jar.JarEntry je = enu.nextElement();

            if(!je.getName().contains(fileToExtract))
                continue;
//				System.out.println(je.getName());

            fl = new java.io.File(destDir, je.getName());
            if(!fl.exists()){
                fl.getParentFile().mkdirs();
                fl = new java.io.File(destDir, je.getName());
            }
            if(je.isDirectory())
                continue;

            InputStream is = jarFile.getInputStream(je);
            OutputStream out = FileUtils.openOutputStream(fl);
            IOUtils.copy(is, out);
            is.close();
            out.close();
        }
        return fl;
    }
    public static String exportJarDirectory(String directory) throws IOException {
        if(getInstance().className.getResource(getInstance().classNameString +".class").toString().startsWith("jar:")) {
            System.out.println("Extracting jar resources, this is only done once");
            //YOU ARE IN A JAR FILE
            String path = getExportPath();
            new File(path).mkdir();
            String jarFileLocation = FileOptions.getAllFilesEndsWith(System.getProperty("user.dir"), ".jar")
                    .stream().filter(a -> a.getName().contains("ogame")).collect(Collectors.toList()).get(0).getAbsolutePath();
            try {
                jarFileLocation = new File(getInstance().className.getProtectionDomain()
                        .getCodeSource().getLocation().toURI().getPath()).getAbsolutePath();
            } catch (Exception e) {
            }

            List<File> f = FileOptions.getAllFilesWithName(path, directory);
            if (f != null && f.size() != 0)
                return path;
            if (!new File(path + "/" + directory).exists())
                try {
                    return exportJarDirectoryResource(path, jarFileLocation, directory);
                } catch (NullPointerException e) {
                }
        }
        return null;
    }

    public static String exportJarDirectoryResource(String destDir, String jarFileLocation, String directoryToExtract) throws IOException {
        if(directoryToExtract == null)
            return null;
//		if(new File(destDir+s+fileToExtract).exists())
//			return null;
        java.util.jar.JarFile jarFile = new java.util.jar.JarFile(new java.io.File(jarFileLocation)); //jar file path(here sqljdbc4.jar)
        java.util.Enumeration<java.util.jar.JarEntry> enu= jarFile.entries();
//        List<Callable> v = new ArrayList<>();
        File found = null;
//        JarFile jarFile = new JarFile(jarFileLocation);//;((JarURLConnection)new URL(jarFileLocation).openConnection()).getJarFile();
        while(enu.hasMoreElements()){
            File fl = null;
            java.util.jar.JarEntry je = enu.nextElement();
            fl = new java.io.File(destDir, je.getName());
            if(fl.getName().equals(directoryToExtract))
                found = fl;

            if(!Arrays.asList(fl.getAbsolutePath().split(FileOptions.OS.substring(0,3).equals(WINDOWS) ?
                    "\\\\" :
                    FileOptions.SEPERATOR)).contains(directoryToExtract))
                continue;

            if(!fl.exists()){
                fl.getParentFile().mkdirs();
                fl = new java.io.File(destDir, je.getName());
            }
            if(je.isDirectory())
                continue;

            InputStream is = jarFile.getInputStream(je);
            OutputStream out = FileUtils.openOutputStream(fl);
            IOUtils.copy(is, out);
            is.close();
            out.close();

            LOGGER.debug("Done with: "+fl.getAbsolutePath());
        }
//        FileOptions.runConcurrentProcess(v);
        return found.getAbsolutePath();
    }
    /**
     * This method will copy resources from the jar file of the current thread and extract it to the destination folder.
     *
     * @param jarConnection
     * @param destDir
     * @throws IOException
     */
    public void copyJarResourceToFolder(JarURLConnection jarConnection, File destDir) {

        try {
            JarFile jarFile = jarConnection.getJarFile();

            /**
             * Iterate all entries in the jar file.
             */
            for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {

                JarEntry jarEntry = e.nextElement();
                String jarEntryName = jarEntry.getName();
                String jarConnectionEntryName = jarConnection.getEntryName();

                /**
                 * Extract files only if they match the path.
                 */
                if (jarEntryName.startsWith(jarConnectionEntryName)) {

                    String filename = jarEntryName.startsWith(jarConnectionEntryName) ? jarEntryName.substring(jarConnectionEntryName.length()) : jarEntryName;
                    File currentFile = new File(destDir, filename);

                    if (jarEntry.isDirectory()) {
                        currentFile.mkdirs();
                    } else {
                        InputStream is = jarFile.getInputStream(jarEntry);
                        OutputStream out = FileUtils.openOutputStream(currentFile);
                        IOUtils.copy(is, out);
                        is.close();
                        out.close();
                    }
                }
            }
        } catch (IOException e) {
            // TODO add logger
            e.printStackTrace();
        }

    }
}
