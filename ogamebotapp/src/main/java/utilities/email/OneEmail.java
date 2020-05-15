package utilities.email;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ogame.objects.Email;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import utilities.password.Password;
import utilities.password.PasswordEncryptDecrypt;
import utilities.database.Database;
import utilities.fileio.FileOptions;
import utilities.fileio.JarUtility;
import utilities.webdriver.DriverController;
import utilities.webdriver.DriverControllerBuilder;
import utilities.webdriver.JavaScriptFunctions;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by jarndt on 5/5/17.
 */
public class OneEmail {
    public static void main(String[] args) throws Exception {
        parallelCreateAccounts(1);//30
        cleanUpEmailDirectory();
        parseEmailResults();
    }
    static {
        FileOptions.setLogger(FileOptions.DEFAULT_LOGGER_STRING);
    }
    private static final Logger LOGGER = LogManager.getLogger(OneEmail.class.getName());
    private static Scheduler scheduler;
    private static Scheduler getScheduler() throws SchedulerException {
        if(scheduler == null){
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
        }
        return scheduler;
    }
    public static void scheduleJob(String cronSchedule) throws SchedulerException {
//        getScheduler().getCurrentlyExecutingJobs().
        String jobName = "oneEmailCreation";
        if(getScheduler().getJobKeys(GroupMatcher.jobGroupEquals("group2")).stream().filter(a->a.getName().equals(jobName)).collect(Collectors.toList()).size() != 0)
            return;
        JobDetail job = JobBuilder.newJob(OneEmail.CreateEmailJob.class)
                .withIdentity(jobName, "group2").build();

        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity(jobName+"Trigger", "group2")
                .withSchedule(
                        CronScheduleBuilder.cronSchedule(cronSchedule))
                .build();

        //schedule it
        getScheduler().scheduleJob(job, trigger);
    }
    public static class CreateEmailJob implements Job{
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            System.out.println("Creating Emails");
            parallelCreateAccounts(20);
            try {
                cleanUpEmailDirectory();
                parseEmailResults();
            } catch (Exception e){ /*Do nothing, if it fails it fails*/}
            System.out.println("Done Creating Emails");
        }
    }

    public static void startOneEmailCreateThread() throws SchedulerException {
//        scheduleJob("30 2 * * * ?"); //run download job at 2:30 am every day
        scheduleJob("0 0 0/1 * * ?"); //run download job every 2 hours
//        scheduleJob("0 0/10 * * * ?"); //run download job every 2 hours
    }
    public static class VerifyEmailJob implements Job{
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            System.out.println("Verifying Emails");
            try {
                parallelVerifyAccounts(20);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Done Verifying Emails");
        }
    }

    public static void scheduleVerifyJob(String cronSchedule) throws SchedulerException {
//        getScheduler().getCurrentlyExecutingJobs().
        String jobName = "oneEmailVerify";
        if(getScheduler().getJobKeys(GroupMatcher.jobGroupEquals("group2")).stream().filter(a->a.getName().equals(jobName)).collect(Collectors.toList()).size() != 0)
            return;
        JobDetail job = JobBuilder.newJob(OneEmail.VerifyEmailJob.class)
                .withIdentity(jobName, "group2").build();

        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity(jobName+"Trigger", "group2")
                .withSchedule(
                        CronScheduleBuilder.cronSchedule(cronSchedule))
                .build();

        //schedule it
        getScheduler().scheduleJob(job, trigger);
    }
    public static void startOneEmailVerifyThread() throws SchedulerException {
//        scheduleJob("30 2 * * * ?"); //run download job at 2:30 am every day
        scheduleVerifyJob("0/10 * * * * ?"); //run download job every 2 hours
//        scheduleJob("0 0/10 * * * ?"); //run download job every 2 hours
    }



    private static void parseEmailResults() throws IOException, SQLException, ClassNotFoundException {
        Gson gson = new Gson();
        final List<HashMap<String,String>> emails = new ArrayList<>();
        FileOptions.getAllFilesEndsWith(EMAILS_JSON_DIR, ".json")
                .stream().map(a -> {
                    try {
                        return FileOptions.readFileIntoString(a.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).filter(a -> a != null)
        .forEach(a->emails.addAll(gson.fromJson(a,new TypeToken<List<HashMap<String,String>>>(){}.getType())));
        Set<HashMap<String,String>> set = new HashSet<>();
        set.addAll(emails);
        ArrayList<HashMap<String, String>> emailAddress = new ArrayList<>();
        emailAddress.addAll(set);

        List<String> contentList = emailAddress.stream().filter(a -> !a.get("Disk usage").contains("?")).map(a -> a.get("Address")).collect(Collectors.toList());
        String content = contentList.stream().reduce("", (a, b) -> a + "\n" + b);
        FileOptions.writeToFileOverWrite(EMAILS_DIR+"/working_email_list.txt",content);
        FileOptions.deleteDirectory(EMAILS_JSON_DIR);
        toDatabase(contentList);
    }

    public static void toDatabase(List<String> emailList) throws SQLException, IOException, ClassNotFoundException {
        Database d = new Database(Database.DATABASE,Database.USERNAME,Database.PASSWORD);
        for(String s : emailList)
            try{
                d.executeQuery("INSERT INTO EMAIL(EMAIL,PASSWORD) VALUES('"+s+"','"+
                                    PasswordEncryptDecrypt.encrypt(s.replace("@michaelgutin.one",""))+"');");
            } catch (Exception e){/*DO NOTHING*/}
    }

    public static final String EMAILS_DIR =
            FileOptions.cleanFilePath(JarUtility.getResourceDir()+"/emails/");

    public static final String EMAILS_JSON_DIR =
            FileOptions.cleanFilePath(EMAILS_DIR+"/JSON/");

    public static final int EMAIL_LENGTH = 20;


    public static void cleanUpEmailDirectory() throws IOException {
        final String path = FileOptions.cleanFilePath(EMAILS_DIR+"working/");
        Set<String> emails = new HashSet<>();
        emails.addAll(FileOptions.getAllFilesEndsWith(path,".txt").stream().map(a -> {
            try {
                return FileOptions.readFileIntoString(a.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }).filter(a->a!=null).collect(Collectors.toList()));

        String emailFile = "email_list.txt";
        FileOptions.writeToFileOverWrite(EMAILS_DIR+emailFile,emails.stream().reduce("",(a,b)->a+"\n"+b));

        FileOptions.deleteDirectory(path);
    }

    public static void parallelCreateAccounts(int numAccounts){
        final String path = FileOptions.cleanFilePath(EMAILS_DIR+"working/");
        new File(path).mkdirs();
        new File(EMAILS_JSON_DIR).mkdirs();
        List<Callable> names = IntStream.range(0, numAccounts).boxed().map(i -> (Callable) () -> {
            try {
                String name = Password.randomString(EMAIL_LENGTH);
                new OneEmail().createNewOneEmail(name, name);
                FileOptions.writeToFileOverWrite(path + UUID.randomUUID().toString() + ".txt", name+"@michaelgutin.one");
                return name;
            }catch (Exception e){
//                e.printStackTrace();
                LOGGER.debug("",e);
                return null;
            }
        }).collect(Collectors.toList());
        FileOptions.runConcurrentProcess(names);
    }public static void parallelVerifyAccounts(int numAccounts) throws SQLException, IOException, ClassNotFoundException {
        final Database d = Database.newDatabaseConnection();
        List<Map<String, Object>> notVerified = d.executeQuery("select e.* from ogame_user o, email e where o.email_id = e.id and o.verified = 'N';");
        FileOptions.runConcurrentProcess(notVerified.stream().map(a->(Callable)()->{
            new OneEmail().verifyOneEmailAccount(a.get("email").toString(),PasswordEncryptDecrypt.decrypt(a.get("password").toString()));
            return null;
        }).collect(Collectors.toList()));
    }

    private DriverController driverController;
    public OneEmail(){}
    public OneEmail(DriverController driverController){
        this.driverController = driverController;
    }

    private DriverController getDriverController(){
        if(driverController == null)
            driverController = new DriverControllerBuilder()
//                    .setStartImageThread(true).setImageThreadValue(1)
                    .build();
        return driverController;
    }

    public void verifyOneEmailAccount(String username, String password) throws Exception {
        DriverController driverController = getDriverController();
        driverController.getDriver().navigate().to(
                new EmailUtility(username,password)
                .getOgameVerifyLink()
        );
//        driverController.setDriverName(username+"_verify");
//
//        try{
//            driverController.getDriver().navigate().to("https://login.one.com/mail");
//
//            String emailXpath = "//*[@id='asyncWebmailLogin']/input[1]";
//            driverController.waitForElement(By.xpath(emailXpath),1L,TimeUnit.MINUTES);
//            JavaScriptFunctions.fillFormByXpath(driverController,emailXpath,username);
//            JavaScriptFunctions.fillFormByXpath(driverController,"//*[@id='asyncWebmailLogin']/input[2]",password);
//
//            driverController.clickWait(By.xpath("//*[@id='asyncWebmailLogin']/div[3]/button"),1L,TimeUnit.MINUTES);
//
//            FileOptions.runConcurrentProcess((Callable)()-> {
//                List<WebElement> elements = driverController.getDriver().findElements(By.xpath("//text()[contains(.,'here')]/parent::*"));
//                while(elements.size()==0)
//                    elements = driverController.getDriver().findElements(By.xpath("//text()[contains(.,'here')]/parent::*"));
//
//                for (WebElement element : elements) {
//                    try {
//                        element.click();
//                    } catch (Exception e) {
//
//                    }
//                }
//                return null;
//            });
//
//            String modelDialogXpath = "/html/body/div[2]/sequence-dialog[1]/modal-dialog/modal-overlay";
//            driverController.waitForElement(By.xpath(modelDialogXpath),1L,TimeUnit.MINUTES);
//            driverController.executeJavaScript(JavaScriptFunctions.XPATH_FUNCTION+
//                    "\n$x(\""+modelDialogXpath+"\")[0].style.visibility = 'hidden';");
//
//            driverController.clickWait(By.xpath("//text()[contains(., \\\"Welcome to OGame.org\\\")]/parent::*"),1L,TimeUnit.MINUTES);
//            driverController.clickWait(By.xpath("/html/body/div[2]/div[3]/div[2]/div[2]/div/div/div/div[2]/div[6]/div[2]/div/div/div/table/tbody/tr/td/table/tbody/tr[2]/td/table/tbody/tr[2]/td/table/tbody/tr/td/table/tbody/tr/td/table/tbody/tr/td/table/tbody/tr/td/div/table/tbody/tr/td/a/font/b"),1L,TimeUnit.MINUTES);
//        }catch (Exception e){
//            String dirverName = driverController.getDriverName();
//            driverController.quit();
//            throw new Exception(dirverName+": "+e.getMessages()+"\n"+Arrays.asList(e.getStackTrace()));
//        }
//        driverController.quit();
    }

    private static List<String> characters = Password.getCharacters();

    String oneUserName = "Wz5qIq65vmbVV6x+8bX0zN+p50n7NvwK", onePassword = "CgviEZKrU1NjECiLH412+g==";

    public void createNewOneEmail(String username, String password) throws Exception {
        final String    oneUser = PasswordEncryptDecrypt.decrypt(oneUserName),
                        onePass = PasswordEncryptDecrypt.decrypt(onePassword);
        DriverController driverController = getDriverController();
        driverController.setDriverName(username+"_create");
        try {
            driverController.setDriverType(DriverController.PHANTOMJS);
            driverController.getDriver().navigate().to("https://login.one.com/");

            driverController.waitForElement(By.xpath("/html/body/div[3]/div[2]/div[2]/div[1]/div/form/div[3]/button"), 1l, TimeUnit.MINUTES);
            JavaScriptFunctions.fillFormByXpath(driverController, "/html/body/div[3]/div[2]/div[2]/div[1]/div/form/input[2]", oneUser);
            JavaScriptFunctions.fillFormByXpath(driverController, "/html/body/div[3]/div[2]/div[2]/div[1]/div/form/input[5]", onePass);

            driverController.clickWait(By.xpath("/html/body/div[3]/div[2]/div[2]/div[1]/div/form/div[3]/button"),1L,TimeUnit.MINUTES);

            driverController.clickWait(By.xpath("//*[@id=\"frontpageMailLink\"]"), 1l, TimeUnit.MINUTES);
            String emailPage = "//*[@id=\"content\"]/div[2]/fieldset[1]/a";
            driverController.clickWait(By.xpath(emailPage), 1l, TimeUnit.MINUTES);

            driverController.waitForElement(By.xpath("//*[@id='name']"), 1l, TimeUnit.MINUTES);

            JavaScriptFunctions.fillFormByXpath(driverController, "//*[@id='name']", username, 1);
            JavaScriptFunctions.fillFormByXpath(driverController, "//*[@id='mailPassword1']", password);
            JavaScriptFunctions.fillFormByXpath(driverController, "//*[@id='mailPassword2']", password);

            driverController.getDriver().findElements(By.xpath("//*[@id=\"mailAccountForm\"]/div[2]/input[2]")).get(0).click();

            driverController.waitForElement(By.xpath(emailPage), 1l, TimeUnit.MINUTES);

            String f = new Gson().toJson(parseResults(driverController.getDriver().getPageSource()));
            FileOptions.writeToFileOverWrite(EMAILS_JSON_DIR+driverController.getDriverName()+".json",f);
        }catch (Exception e){
            String dirverName = driverController.getDriverName();
            driverController.quit();
            throw new Exception(dirverName+": "+e.getMessage()+"\n"+Arrays.asList(e.getStackTrace()));
        }
        driverController.quit();
    }

    private List<HashMap<String, String>> parseResults(String pageSource){
        Document d = Jsoup.parse(pageSource);
        List<HashMap<String,String>> emails = new ArrayList<>();
        Elements tbody = d.select("tbody").get(0).select("tr");
        for(Element tr : tbody) {
            HashMap<String,String> email = new HashMap<>();
            for(Element td : tr.select("td"))
                email.put(td.select("label").get(0).text(),td.select("span").get(0).text());
            emails.add(email);
        }
        return emails;
    }
}
