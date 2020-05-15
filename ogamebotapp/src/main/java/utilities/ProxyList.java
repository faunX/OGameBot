package utilities;

import org.openqa.selenium.By;
import utilities.fileio.FileOptions;
import utilities.fileio.JarUtility;
import utilities.webdriver.DriverController;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Created by jarndt on 5/4/17.
 */
public class ProxyList {
    public static void main(String[] args) throws IOException, InterruptedException {
        parallelTestIPS();
    }


    public final static String PROXY_LIST_DIR = FileOptions.cleanFilePath(JarUtility.getResourceDir()+"proxylist/");
    public static void parallelTestIPS() throws IOException, InterruptedException {
        Set<String> ips = new HashSet<>(), removeIps = new HashSet<>();

        String path = PROXY_LIST_DIR;
        FileOptions.getAllFilesEndsWith(path,".txt").parallelStream()
                .forEach(a -> {
                    try {
                        List<String> files = FileOptions.readFileIntoListString(FileOptions.cleanFilePath(a.getAbsolutePath()))
                                .parallelStream().map(b->b.trim())
                                .filter(b->b.contains(":") && b.split("\\.").length == 4)
                                .collect(Collectors.toList());
                        if(!a.getAbsolutePath().toLowerCase().contains("failed"))
                            ips.addAll(files);
                        else
                            removeIps.addAll(files);
                    } catch (IOException e) { /*DO NOTHING*/ }
                });

        FileOptions.getAllFilesEndsWith(PROXY_LIST_DIR, ".zip").stream().forEach(a -> {
            try {
                HashMap<String, List<String>> zip = FileOptions.getZipFileContents(a.getAbsolutePath());
                zip.keySet().stream().filter(b->b.toLowerCase().contains("failed"))
                        .forEach(b->zip.get(b).stream()
                                .filter(c->c.contains(":") && c.split("\\.").length == 4)
                                .forEach(c->removeIps.add(c.trim())));
                zip.keySet().stream().filter(b->!b.toLowerCase().contains("failed"))
                        .forEach(b->zip.get(b).stream()
                                .filter(c->c.contains(":") && c.split("\\.").length == 4)
                                .forEach(c->ips.add(c.trim())));
            } catch (IOException e) {
//                e.printStackTrace();
            }
        });

        ips.removeAll(removeIps);
        String nanoTime = System.nanoTime()+"";
        String workingPath = FileOptions.cleanFilePath(path+"/test/working"+nanoTime),
                failedPath = FileOptions.cleanFilePath(path+"/test/failed"+nanoTime);
        new File(workingPath).mkdirs();
        new File(failedPath).mkdirs();

        Set<String> failedIPs = new HashSet<>();
        HashMap<String,String> workingIPs = new HashMap<>();
        List<Callable> callables = ips.stream().map(a -> (Callable) () -> {
            DriverController driverController = new DriverController();
            try {
                driverController.setProxy(a);
                driverController.setDriverType(DriverController.PHANTOMJS);
                utilities.Timer t = new utilities.Timer().start();
                driverController.getDriver().navigate().to("http://www.whatsmyip.org/");
                String ip = driverController.getDriver().findElements(By.id("ip")).get(0).getText();
                t.stop();
                System.out.println(a+"\tIP: "+ip+"\tTook: "+t.getTime());
                workingIPs.put(a,t.getNanoTime()+","+ip);
                FileOptions.writeToFileOverWrite(FileOptions.cleanFilePath(workingPath+"/"+ UUID.randomUUID().toString()+".txt"),a);
            }catch (Exception e){
                System.out.println("IP FAILED: "+a);
                failedIPs.add(a);
                try {
                    FileOptions.writeToFileOverWrite(FileOptions.cleanFilePath(failedPath+"/"+UUID.randomUUID().toString()+".txt"),a);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            driverController.quit();
            return null;
        }).collect(Collectors.toList());
        ExecutorService service = FileOptions.runConcurrentProcess(callables);

        while (!service.isTerminated() && !service.isShutdown())
            Thread.sleep(1000);

        Set<String> working = new HashSet<>();
        working.addAll(workingIPs.keySet());
        working.addAll(FileOptions.readFileIntoListString(PROXY_LIST_DIR+"working.txt").stream().map(a->a.trim()).collect(Collectors.toList()));

        failedIPs.addAll(FileOptions.readFileIntoListString(PROXY_LIST_DIR+"failed.txt").stream().map(a->a.trim()).collect(Collectors.toList()));

        FileOptions.writeToFileOverWrite(PROXY_LIST_DIR+"working.txt",working.stream().reduce("",(u, s) -> u+"\n"+s));
        FileOptions.writeToFileOverWrite(PROXY_LIST_DIR+"failed.txt",failedIPs.stream().reduce("",(u, s) -> u+"\n"+s));

        FileOptions.deleteDirectory(workingPath);
        FileOptions.deleteDirectory(failedPath);
    }

}
