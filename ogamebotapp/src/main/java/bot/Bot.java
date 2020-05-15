package bot;

import bot.attacking.AttackManager;
import bot.attacking.Target;
import bot.settings.SettingsManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ogame.objects.User;
import ogame.objects.game.*;
import ogame.objects.game.data.Server;
import ogame.objects.game.fleet.FleetInfo;
import ogame.objects.game.fleet.FleetObject;
import ogame.objects.game.fleet.Mission;
import ogame.objects.game.messages.MessageObject;
import ogame.objects.game.planet.Planet;
import ogame.pages.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.TimeoutException;
import utilities.database.Database;
import utilities.fileio.FileOptions;
import utilities.webdriver.DriverController;
import utilities.webdriver.DriverControllerBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created by jarndt on 5/10/17.
 */
public class Bot {
    public static void main(String[] args) throws Exception {
//        new Bot(new Login(User.newRandomUser(Server.QUANTUM))).startBot();
//        new Bot(new Login(new User("bc3ew9p4yh9qdv8wvj1h", "ib5f982wc4oedy2q1xfn",Server.QUANTUM))).startBot();
        //password: ib5f982wc4oedy2q1xfn
        Bot b = new Bot(new Login(new User("bc3ew9p4yh9qdv8wvj1h", "ib5f982wc4oedy2q1xfn", Server.PASIPHAE)));
        System.out.println(b.getNextBuildTask());
        System.out.println(new Gson().toJson(b));
    }

    private transient DriverController driverController;
    private transient Login login;
    private HashMap<String,Planet> planets = new HashMap<>(); //cords->Planet
    private HashMap<String,Integer> research = new HashMap<>();
    private HashMap<String,Buildable> researchBuildable = new HashMap<>();
    private String currentPage;
    private Coordinates currentPlanetCoordinates;
    private transient PageController pageController;
    private AttackManager attackManager;
    private FleetInfo fleetInfo = new FleetInfo();
    private Set<MessageObject> messages = new HashSet<>();

    private long darkMatter = 0;
    private int unreadMessages = 0;

    private transient Database d;

    private int id, ogameUserId, webdriverId, rank, totalRanks, honorPoints, points;
    private String name;
    private boolean isActive, isBeingAttacked;
    private String startDate;
    private BuildTask currentResearchBeingBuilt;

    private transient ExecutorService buildTaskService;

    private List<BuildTask> buildTasks;

    private String botDataDirectory;

    public Database getDatabase() throws SQLException, IOException, ClassNotFoundException {
        if(d == null)
            d = Database.newDatabaseConnection();
        return d;
    }

    public Bot(DriverController driverController, Login login) throws SQLException, IOException, ClassNotFoundException {
        this.driverController = driverController;
        this.login = login;
        init();
    }

    public Bot(Login login) throws SQLException, IOException, ClassNotFoundException {
        /*This will use a provided email or ogame user to create or log into an account*/
        this.login = login;
        driverController =
                new DriverControllerBuilder()
                        .setName(login.getUser().getUniverse()+"_"+login.getUser().getUsername())
                        .setStartImageThread(true)
                        .setImageThreadValue(1)
                        .setImageThreadTimeUnit(TimeUnit.SECONDS)
                        .build();
        driverController.getDriver().manage().timeouts().pageLoadTimeout(10,TimeUnit.SECONDS);
        init();
    }

    public Bot(){
        /*This will use an unused email to create a new account*/
    }

    private void init() throws SQLException, IOException, ClassNotFoundException {
        pageController = new PageController(this);
        research = new HashMap<>();
        Buildable.getResearch().forEach(a->research.put(a.getName(),0));

        botDataDirectory = driverController.getImageOutputDirectory()+"../data";
        new File(botDataDirectory).mkdirs();

        this.login.setDriverController(driverController);

        FileOptions.runConcurrentProcessNonBlocking((Callable)()->{
            List<Map<String, Object>> v = getDatabase().executeQuery("select * from bot where ogame_user_id = " + login.getUser().getID() + " " +
                    "and webdriver_id = " + driverController.getId());

            if(v != null && v.size() > 0 && v.get(0) != null && v.get(0).size() > 0){
                Map<String, Object> bot = v.get(0);
                id = Integer.parseInt(bot.get("id").toString());
                ogameUserId = Integer.parseInt(bot.get("ogame_user_id").toString());
                webdriverId = Integer.parseInt(bot.get("webdriver_id").toString());
                name = bot.get("name").toString();
                isActive = bot.get("active").toString().equals("A") ? true : false;
                startDate = bot.get("start_date").toString();
            }else{
                getDatabase().executeQuery(
                        "insert into bot(ogame_user_id,webdriver_id,name,active)" +
                        "   values("+login.getUser().getID()+","+driverController.getId()+",'"+driverController.getDriverName()+"','A');");

                v = getDatabase().executeQuery("select * from bot where ogame_user_id = " + login.getUser().getID() + " " +
                        "and webdriver_id = " + driverController.getId());

                Map<String, Object> bot = v.get(0);
                id = Integer.parseInt(bot.get("id").toString());
                ogameUserId = Integer.parseInt(bot.get("ogame_user_id").toString());
                webdriverId = Integer.parseInt(bot.get("webdriver_id").toString());
                name = bot.get("name").toString();
                isActive = bot.get("active").toString().equals("A") ? true : false;
                startDate = bot.get("start_date").toString();
            }
            return null;
        });

        try {
            driverController.getDriver().navigate().to(Login.OGAME_HOMEPAGE);
        }catch (org.openqa.selenium.TimeoutException e){/*DO NOTHING*/}

        loadBot();
    }

    private void loadBot() throws IOException, SQLException, ClassNotFoundException {
        List<File> botData = FileOptions.findFile(botDataDirectory, getName() + "_data.json");
        Bot bot = null;
        if(botData != null && botData.size() > 0){
            bot = new Gson().fromJson(FileOptions.readFileIntoString(botData.get(0).getAbsolutePath()),
                    new TypeToken<Bot>() {}.getType());
        }else {
            List<Map<String, Object>> v = getDatabase().executeQuery("select * from json_data where bot_id = " + getId());
            if(v != null && v.size() > 0 && v.get(0) != null && v.get(0).size() > 0)
                bot = new Gson().fromJson(v.get(0).get("json_data").toString(),
                        new TypeToken<Bot>() {}.getType());
        }
        if(bot == null)
            return;

        this.planets = bot.planets;
        this.research = bot.research;
        this.currentPage = bot.currentPage;
        this.currentPlanetCoordinates = bot.currentPlanetCoordinates;
        fleetInfo = bot.fleetInfo;
        darkMatter = bot.darkMatter;
        unreadMessages = bot.unreadMessages;
        id = bot.id;
        ogameUserId = bot.ogameUserId;
        webdriverId = bot.webdriverId;
        name = bot.name;
        isActive = bot.isActive;
        startDate = bot.startDate;
    }

    private transient boolean isGotInitialState = false;
    public void startBot() throws InterruptedException, ClassNotFoundException, SQLException, URISyntaxException, IOException {
        String ogamePage = Overview.OVERVIEW;
        while (true){ //this is the main loop.  This will happen on every page change
            try {
                //try to log in, if not logged in then sleep for a second and then try to log in again.
                if (!login.login()) {
                    Thread.sleep(1000);
                    continue;
                }
                waitForCallable((Callable)()-> {
                    try {
                        if (!isBeingAttacked() && !isGotInitialState)
                            getInitialState();

                        setBuildTaskService(FileOptions.runConcurrentProcessNonBlocking((Callable) () -> {
                            pageController.parsePage(pageController.getCurrentPage());
                            setBuildTasks(new ArrayList<>());
                            setBuildTasks(getNextBuildTask());
                            return null;
                        }));

                        performNextAction();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    return null;
                },10L, TimeUnit.MINUTES);
                if (getPageController().getCurrentPage().equalsIgnoreCase(Login.HOMEPAGE))
                    throw new Exception("You've been logged out");

            }catch (Exception | Error e){
                /*Garbage collect and keep going*/
                System.gc();
                e.printStackTrace();
            }
        }
    }


    public void getInitialState() {
        System.out.println("Getting inital state");
        List<String> pages = Arrays.asList(Resources.RESOURCES, Facilities.FACILITIES, Research.RESEARCH, Shipyard.SHIPYARD, Defense.DEFENSE, Fleet.FLEET);
        Collections.sort(pages);
        pages.forEach(a -> {
            try {
                getPageController().goToPage(a);
            } catch (Exception e) {
                if(e instanceof TimeoutException)
                    System.err.println("Failed to wait for page to load in 1 minute: "+a);
                else
                    e.printStackTrace();
            }
        });

        isGotInitialState = true;
    }

    private void performNextAction() throws Exception {
        //Check if being attacked perform attack action
        if(isBeingAttacked()) {
            System.out.println("Being attacked, performing attacked action");
            preformBeingAttackedAction();
            return;
        }

        //if the executor service is done and there are build task to do then do them
        //in other words build the next building that you can
        if(getBuildTasks() != null && getBuildTasks().size() > 0) {
            System.out.println("Performing build action");
            try {
                performNextBuildTask();
                return;
            }catch (IndexOutOfBoundsException e){
                e.printStackTrace();
            }
        }

        if(performIdleTask())
            return;

        //Literally nothing to do, just go to the overview page.
        getPageController().goToPage(Overview.OVERVIEW);
        System.out.println("Nothing to do, waiting");
        long max = 120000, min = 30000;
        final long sleepTime = ThreadLocalRandom.current().nextLong(min,max);//wait between 30 and 60 seconds before updating page.  This helps from ogame logging you out
        waitForCallable((Callable)()-> {
            Thread.sleep(sleepTime);
            return null;
        },10L, TimeUnit.MINUTES);
        if (getPageController().getCurrentPage().equalsIgnoreCase(Login.HOMEPAGE))
            throw new Exception("You've been logged out");
        getPageController().goToPage(Overview.OVERVIEW);
    }
    private transient Random r = new Random();

    public void recallFleet() throws Exception {
        //TODO set a specific fleet to recall.  I believe this will recall the first fleet in the list.
        getPageController().goToPage(Fleet.FLEET);
        getDriverController().clickWait(By.cssSelector("#movements > a"),1L, TimeUnit.MINUTES);
        getDriverController().clickWait(By.cssSelector("a.icon_link.tooltipHTML"),1L,TimeUnit.MINUTES);
    }

    private boolean waitForElement(By by, long time, TimeUnit timeUnit) throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        boolean b = true;
        try {
            b = exec.submit(new Callable<Boolean>(){
                @Override public Boolean call() throws Exception {
                    while(true) {
                        if (getPageController().getCurrentPage().equalsIgnoreCase(Login.HOMEPAGE))
                            throw new Exception("You've been logged out, logging back in");
                        if(getDriverController().getDriver().findElements(by).size() > 0)
                            break;
                        Thread.sleep(500);
                    }
                    return true;
                }
            }).get(time, timeUnit);
            exec.shutdown();
            exec.awaitTermination(time, timeUnit);
        } catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
            b = false;
        }finally{
            exec.shutdownNow();
        }
        return b;
    }

    private boolean performIdleTask() throws Exception {
        //TODO
        //Unread messages
        if(getUnreadMessages() != 0){
//            ((Messages)getPageController().getPage(Messages.MESSAGES)).parseAllMessages(this);
            getPageController().goToPage(Messages.MESSAGES);
            return true;
        }
        //TODO PUT BREAKPOINT HERE
        List<Planet> merchantItems = getPlanets().values().stream().filter(a -> a.canGetMerchantItem()).collect(Collectors.toList());
        if(merchantItems.size() != 0){
            //TODO
            List<Planet> currentPlanet = merchantItems.stream().filter(a -> a.getCoordinates().equals(getCurrentPlanetCoordinates())).collect(Collectors.toList());
            if(currentPlanet.size() == 1){
                //you are on the current planet
                //get the merchant item
                //TODO
                if(getPageController().getCurrentPage().toLowerCase().equals(Merchant.MERCHANT.toLowerCase())){
                    //You're on the merchant page
                    getDriverController().getJavaScriptExecutor().executeScript("arguments[0].click();",
                            getDriverController().getDriver().findElement(By.cssSelector("#js_traderImportExport")));
                    //buy the merchant item
                    String selector = "#div_traderImportExport > div.content > div.left_box > div.left_content > div.price.js_import_price";

                    if(!waitForElement(By.cssSelector(selector),1L, TimeUnit.MINUTES))
                        throw new Exception("You've been logged out, logging back in");

                    Document doc = Jsoup.parse(getDriverController().getDriver().getPageSource());
                    long price = Long.parseLong(
                            doc.select(selector).text().trim().replace(".","")
                    );
                    getCurrentPlanet().setMerchantItemCost(price);

                    if(getCurrentPlanet().canGetMerchantItem()) {
                        getDriverController().getJavaScriptExecutor().executeScript("arguments[0].click();",
                                getDriverController().getDriver().findElement(By.cssSelector("#div_traderImportExport > div.content > div.right_box > div.right_content > div.payment > div > table > tbody > tr:nth-child(1) > td:nth-child(5) > a")));

                        getDriverController().getJavaScriptExecutor().executeScript("arguments[0].click();",
                                getDriverController().getDriver().findElement(By.cssSelector("#div_traderImportExport > div.content > div.right_box > div.right_content > div.payment > a")));

                        getCurrentPlanet().setMerchantItemCost(-1);
                    }

                    return true;
                }
                //you're not on the merchant page, go to the merchant page
                //this will go to the merchant page then cycle back through the other more important events all over again.
                getPageController().goToPage(Merchant.MERCHANT);

                return true;
            }

            //go to next planet in merchantItems list that can get a merchant item and
            pageController.goToPageOnPlanet(merchantItems.get(0).getCoordinates(),Merchant.MERCHANT);
            return true;
        }

        //Send fleet out if you only have 1 fleet slot or you have more than 1 free fleet slot
        getPageController().goToPage(Fleet.FLEET);
        if((getFleetInfo().getFleetsTotal() == 1 && getFleetInfo().getFleetsUsed() != 1 ) || getFleetInfo().getFleetsRemaining() - 1 > 0){
            getPageController().parsePage(Fleet.FLEET);
            if(isCurrentPlanetHasNoShips()) {
                System.out.println("No ships to send fleets");
                if(getCurrentPlanet().getSetting(SettingsManager.AUTO_BUILD_ESPIONAGE_PROBES,getOgameUserId()).equals("true"))
                    buildRequest(new BuildTask().setBuildable(
                            Buildable
                                    .getBuildableByName(Ship.ESPIONAGE_PROBE))
                            .setCountOrLevel(1)
                            .setBuildPriority(getCurrentPlanet().getQueueManager(getOgameUserId(),getResearch()).getMaxPriority())
                    );
                if(getCurrentPlanet().getSetting(SettingsManager.AUTO_BUILD_SMALL_CARGOS,getOgameUserId()).equals("true"))
                    buildRequest(new BuildTask().setBuildable(
                            Buildable
                                    .getBuildableByName(Ship.SMALL_CARGO))
                            .setCountOrLevel(1)
                            .setBuildPriority(getCurrentPlanet().getQueueManager(getOgameUserId(),getResearch()).getMaxPriority())
                    );
                return false;
            }
            if(getCurrentPlanet().getShips().get(Ship.SMALL_CARGO) > 0 && getAttackManager().getSafeAttackTargets().size() > 0) {
                quickAttack(getAttackManager().getSafeAttackTargets().get(0));
                return true;
            }
            if(getCurrentPlanet().getShips().get(Ship.SMALL_CARGO) == 0)
                if(getCurrentPlanet().getSetting(SettingsManager.AUTO_BUILD_SMALL_CARGOS,getOgameUserId()).equals("true"))
                    buildRequest(new BuildTask().setBuildable(
                            Buildable
                                    .getBuildableByName(Ship.SMALL_CARGO))
                            .setCountOrLevel(10)
                            .setBuildPriority(getCurrentPlanet().getQueueManager(getOgameUserId(),getResearch()).getMaxPriority())
                    );

            if(getCurrentPlanet().getBuildable(Ship.ESPIONAGE_PROBE).getCurrentLevel()>0){
                //TODO fix issue with fuel.  Can't send if not enough storage capacity
                List<Target> espionageTargets = getAttackManager().getEspionageTargets();
                HashMap<String,Integer> ships = new HashMap<>();
                ships.put(Ship.ESPIONAGE_PROBE,1);
                FleetObject fleet = new FleetObject()
                        .setFromCoordinates(getCurrentPlanetCoordinates())
                        .setShips(ships);
                espionageTargets = espionageTargets.stream().filter(a -> {
                    try {
                        return fleet.setToCoordinates(a.getCoordinates()).isEnoughCargoCapacity();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                }).collect(Collectors.toList());
                //TODO trying to probe an uninhabited planet, how to know that or find out after a probed attempt.
                if(espionageTargets.size() > 0)
                    sendProbe(espionageTargets);
                return true;
            }
            quickAttack(getAttackManager().getBlindAttackTargets().get(0));
            return true;

            //TODO
            //probably shouldn't loop over each planet?????
            //loop over each planet one by one and try to send out fleets.
            //first see if you can send a probe or check database info etc
            //check if this bot should send fleet out
        }

        return false;
    }
    public boolean waitForCallable(Callable c, long l, TimeUnit timeUnit){
        ExecutorService exec = Executors.newSingleThreadExecutor();
        boolean b = true;
        try {
            exec.submit(c);//.get(l, timeUnit);
            exec.shutdown();
//            exec.awaitTermination(l, timeUnit); //blocking call
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit((Callable)()-> {
                while (!exec.awaitTermination(1, TimeUnit.SECONDS))
                    if (getPageController().getCurrentPage().equalsIgnoreCase(Login.HOMEPAGE)) {
                        exec.shutdownNow();
                        return false;
                    }
                if (!exec.awaitTermination(1, TimeUnit.SECONDS))
                    exec.shutdownNow();
                return true;
            });
            executorService.shutdown();
            executorService.awaitTermination(l,timeUnit);
        } catch (InterruptedException e) {
            b = false;
        }finally{
            exec.shutdownNow();
        }
        return b;
    }
    public boolean waitForCallable(Callable c) throws Exception {
        return waitForCallable(c,5l,TimeUnit.MINUTES);
    }

    private void sendProbe(List<Target> espionageTargets) throws Exception {
        //TODO send probes out
        //TODO won't wait for probes to come back before sending more probes
        //TODO tries to send probes when no probes are left
        int espionageProbesCount = getCurrentPlanet().getBuildable(Ship.ESPIONAGE_PROBE).getCurrentLevel();
        int espionageProbesNeeded = espionageTargets.get(0).getEspionageProbesNeeded(getResearch());
        if(espionageProbesCount < espionageProbesNeeded)
            buildRequest(new BuildTask().setBuildable(
                    Buildable
                            .getBuildableByName(Ship.ESPIONAGE_PROBE))
                    .setCountOrLevel((espionageProbesNeeded-espionageProbesCount))
                    .setBuildPriority(getCurrentPlanet().getQueueManager(getOgameUserId(),getResearch()).getMaxPriority())
            );
        if(espionageProbesCount == 0)
            return;
        if(!new Mission().sendFleet(
                new FleetObject()
                        .setMission(Mission.ESPIONAGE)
                        .setToCoordinates(espionageTargets.get(0).getCoordinates())
                        .addShip(Ship.ESPIONAGE_PROBE, espionageProbesNeeded)
                , this
        ))
            //TODO somehow mark this as a target where soemthing went wrong;
            espionageTargets.get(0).somethingWentWrong();
        if (getPageController().getCurrentPage().equalsIgnoreCase(Login.HOMEPAGE))
            throw new Exception("You've been logged out");

        espionageTargets.get(0).setLastEspionage(LocalDateTime.now());
        espionageTargets.get(0).setLastProbeSentCount(espionageProbesNeeded);
        System.out.println("Finished probing: "+espionageTargets.get(0));
    }

    private void quickAttack(Target attackTargets) throws Exception {
        //TODO send fleet out
        int smallCargoCount = getCurrentPlanet().getBuildable(Ship.SMALL_CARGO).getCurrentLevel();
        int smallCargoNeeded = attackTargets.getSmallCargosNeeded();
        if(smallCargoCount < smallCargoNeeded)
            buildRequest(new BuildTask().setBuildable(
                    Buildable
                        .getBuildableByName(Ship.SMALL_CARGO))
                        .setCountOrLevel((smallCargoNeeded-smallCargoCount))
                        .setBuildPriority(getCurrentPlanet().getQueueManager(getOgameUserId(),getResearch()).getMaxPriority())
            );
        if(!new Mission().sendFleet(
                new FleetObject()
                    .setMission(Mission.ATTACKING)
                    .setToCoordinates(attackTargets.getCoordinates())
                    .addShip(Ship.SMALL_CARGO,smallCargoNeeded)
                ,this
        ))
            //TODO Somehow mark this as a something went wrong target
            attackTargets.somethingWentWrong();
        if (getPageController().getCurrentPage().equalsIgnoreCase(Login.HOMEPAGE))
            throw new Exception("You've been logged out");
        attackTargets.setLastAttack(LocalDateTime.now());
        System.out.println("Finished attacking: "+attackTargets);
    }
    public void buildRequest(BuildTask buildTask) throws SQLException, IOException, ClassNotFoundException {
        buildRequest(getCurrentPlanet().getBotPlanetID(),buildTask);
    }
    public void buildRequest(String botPlanetId, BuildTask buildTask) throws SQLException, IOException, ClassNotFoundException {
        //TODO insert into planet_queue;
        if(Arrays.asList(Shipyard.SHIPYARD.toLowerCase(),Defense.DEFENSE.toLowerCase()).contains(buildTask.getBuildable().getType().toLowerCase())) {
            List<Map<String, Object>> v = getDatabase().executeQuery("select sum(build_level) from planet_queue " +
                    "where bot_planets_id = " + botPlanetId +
                    " and buildable_id = " + buildTask.getBuildable().getId()+
                    " and done = 'N'"
            );
            if (v != null && v.size() > 0 && v.get(0) != null && v.get(0).size() > 0) {
                if(v.get(0).get("sum") != null) {
                    long sum = (long) v.get(0).get("sum");
                    if (buildTask.getCountOrLevel() <= sum)
                        return;
                    buildTask.setCountOrLevel((int)(buildTask.getCountOrLevel()-sum));
                }
            }
        }else{
            List<Map<String, Object>> v = getDatabase().executeQuery("select count(*) from planet_queue " +
                    "where bot_planets_id = " + botPlanetId +
                    " and buildable_id = " + buildTask.getBuildable().getId()+
                    " and build_level >= "+buildTask.getCountOrLevel()+
                    " and done = 'N'"
            );
            if (v != null && v.size() > 0 && v.get(0) != null && v.get(0).size() > 0) {
                long sum = (long) v.get(0).get("count");
                if(sum > 0)
                    return;
            }
        }

        getDatabase().executeQuery(
                "insert into planet_queue(bot_planets_id,buildable_id,build_level,BUILD_PRIORITY) " +
                "   values("+botPlanetId+","+buildTask.getBuildable().getId()+","+buildTask.getCountOrLevel()+","+buildTask.getBuildPriority()+") " +
                        "ON CONFLICT DO NOTHING; "
        );
    }

    private void performNextBuildTask() throws Exception {
        //TODO
        List<BuildTask> tasks = getBuildTasks();
        if(tasks != null && tasks.size() > 0){
            List<BuildTask> currentPlanetBuild = tasks.stream().filter(a -> a.getCoordinates().equals(currentPlanetCoordinates)).collect(Collectors.toList());
            //no currentPlanetBuild, but there are BuildTasks then the build task isn't on this planet
            if(currentPlanetBuild.size() == 0) {
                pageController.goToPageOnPlanet(tasks.get(0).getCoordinates(),tasks.get(0).getBuildable().getType());
                return;
            }

            BuildTask build = tasks.get(0);
            HashMap<String, Integer> requirements = Buildable.getBuildableRequirements(build.getBuildable().getName());
            if(!(requirements.size() == 1 && requirements.containsValue(build.getBuildable().getName())))
                for(Map.Entry<String, Integer> e : requirements.entrySet()) {
                    Buildable b = Buildable.getBuildableByName(e.getKey());
                    b.setCurrentLevel(e.getValue());
                    if (b.getType().toLowerCase().equals(Research.RESEARCH.toLowerCase())) {
                        if (getResearch().get(e.getKey()) < e.getValue()) {
                            System.out.println("Don't have prerequisites, can't build yet");
                            getNextBuildTask().add(0,new BuildTask(b,LocalDateTime.now()));
                            return;
                        }else continue;
                    }if(getCurrentPlanet().getAllBuildables().containsKey(e.getKey()))
                        if(getCurrentPlanet().getBuildable(e.getKey()).getCurrentLevel() < e.getValue()) {
                            System.out.println("Don't have prerequisites, can't build yet");
                            getNextBuildTask().add(0,new BuildTask(b,LocalDateTime.now()));
                            return;
                        }
                }
            //TODO check level with build level use case, it built a level 5 when it wanted to build a level 4 because the data wasn't updated
            if(getPageController().getCurrentPage().toLowerCase().equals(build.getBuildable().getType().toLowerCase())) {
                getPageController().parsePage(getPageController().getCurrentPage());
                //YOU are on the correct page build the item in question
                //if you can can only afford with dark matter
                Buildable b = build.getBuildable();
                boolean dm = false;
                Resource cost = b.getLevelCost(b.getCurrentLevel());
                Buildable currentPlanetBuildable = null;
                if(build.getBuildable().getType().toLowerCase().equalsIgnoreCase(Research.RESEARCH))
                    currentPlanetBuildable = researchBuildable.get(build.getBuildable().getName());
                else
                    currentPlanetBuildable = getCurrentPlanet().getBuildable(build.getBuildable().getName());
                System.out.println("Trying to build: "+b.getName()+", level: "+b.getCurrentLevel()+", cost: "+cost+", dm: "+cost.subtract(getCurrentPlanet().getResources()).getDarkMatterCost());
                if(currentPlanetBuildable.getCurrentLevel() >= b.getCurrentLevel()){
                    System.out.println("Current level is: "+currentPlanetBuildable.getCurrentLevel()+", already completed this build request");
                    markAsDone(build);
                    return;
                }

                if(getCurrentPlanet().getResources().lessThan(cost))
                    if(cost.subtract(getCurrentPlanet().getResources()).getDarkMatterCost() <= getDarkMatter())
                        dm = true;

                List<String> useSmallButton = Arrays.asList(Resources.RESOURCES.toLowerCase(), Facilities.FACILITIES.toLowerCase()),
                            shipyard        = Arrays.asList(Shipyard.SHIPYARD.toLowerCase(),Defense.DEFENSE.toLowerCase());

                BuildTask buildingBeingBuilt = getCurrentPlanet().getCurrentBuildingBeingBuild();
                Set<BuildTask> shipyardBeingBuilt = getCurrentPlanet().getCurrentShipyardBeingBuild();
                boolean isBuilding = useSmallButton.contains(build.getBuildable().getType().toLowerCase()),
                        isShipyard = shipyard.contains(build.getBuildable().getType().toLowerCase()),
                        isResearch = build.getBuildable().getType().equalsIgnoreCase(Research.RESEARCH);
                if(isShipyard && shipyardBeingBuilt!= null && shipyardBeingBuilt.size() > 0) {
                    System.out.println("Won't build yet, shipyard has queue: "+shipyardBeingBuilt);
                    setBuildTasks(new ArrayList<>());
                    return;
                }if(isBuilding && buildingBeingBuilt!= null && !(buildingBeingBuilt.isComplete() && buildingBeingBuilt.isDone())) {
                    System.out.println("Can't build yet, building currently being built: "+
                            buildingBeingBuilt.getBuildable().getName()+", level: "+buildingBeingBuilt.getBuildable().getCurrentLevel()+
                            ", completeTime: "+buildingBeingBuilt.getCompleteTime());
                    setBuildTasks(new ArrayList<>());
                    return;
                }if(isResearch && currentResearchBeingBuilt != null && !(currentResearchBeingBuilt.isDone() && currentResearchBeingBuilt.isComplete())){
                    System.out.println("Can't research yet, research currently in progress: "+
                            currentResearchBeingBuilt.getBuildable().getName()+", level: "+currentResearchBeingBuilt.getBuildable().getCurrentLevel()+
                            ", completeTime: "+currentResearchBeingBuilt.getCompleteTime());
                    setBuildTasks(new ArrayList<>());
                    return;
                }if(!dm && getCurrentPlanet().getResources().lessThan(cost)) {
                    System.out.println("Can't build yet, can't afford.  Current resources: "+getCurrentPlanet().getResources()+", dm: "+getDarkMatter());
                    setBuildTasks(new ArrayList<>());
                    return;
                }if(!canBuild(build)){
                    System.out.println("can't build yet");
                    setBuildTasks(new ArrayList<>());
                    return;
                }if(!dm &&
                        Jsoup.parse(getDriverController().getDriver().getPageSource())
                                .select("li.off").select("span.textlabel").stream()
                                .map(a->a.text().trim()).collect(Collectors.toList())
                                .contains(b.getName())
                        ){
                    System.out.println("Can't build yet");
                    return;
                }
                if(isResearch)
                    isBuilding = true;
                if(!dm && isBuilding) {
                    //use the quick build link
                    //TODO fix, sometimes it builds it even though its already building it
                    PageController.parseGenericBuildings(Jsoup.parse(getDriverController().getDriver().getPageSource()), this);
                    if(currentPlanetBuildable != null && currentPlanetBuildable.getQuickBuildLink() != null && !currentPlanetBuildable.getQuickBuildLink().isEmpty()) {
                        getDriverController().executeJavaScript(currentPlanetBuildable.getQuickBuildLink());
                        Thread.sleep(500);
                    }else
                        System.out.println("Can't build, something went wrong");
                    currentPlanetBuildable.setQuickBuildLink("");
                    getPageController().goToPage(Overview.OVERVIEW);
                    tasks.remove(0);
                    System.out.println("Build complete");
                    return;
                }
                //TODO test shipyard and defense with dark matter
                //TODO fix, ended up building several more probes than what was in build list
                driverController.clickWait(By.cssSelector(currentPlanetBuildable.getCssSelector()+" > a"),1L,TimeUnit.MINUTES);
                if(!waitForElement(By.cssSelector("#content"),1L, TimeUnit.MINUTES))
                    throw new Exception("You've been logged out, logging back in");
                if(parseOpenedPanel(getCurrentPlanet(),driverController.getDriver().getPageSource())) {
                    //SHIPS
                    if(!useSmallButton.contains(build.getBuildable().getType().toLowerCase())) {
                        String xpath = "//*[@id='number']";
                        if(!waitForElement(By.xpath(xpath),1L, TimeUnit.MINUTES))
                            throw new Exception("You've been logged out, logging back in");
                        WebElement e = getDriverController().getDriver().findElement(By.xpath(xpath));
                        e.clear();
                        e.sendKeys(b.getCurrentLevel()+"",Keys.ENTER);
                    }else {
                        String xpath = "//*[@id='content']/div[2]/a";
                        if(!waitForElement(By.xpath(xpath),1L, TimeUnit.MINUTES))
                            throw new Exception("You've been logged out, logging back in");
                        getDriverController().getJavaScriptExecutor().executeScript("arguments[0].click();",
                                getDriverController().getDriver().findElement(By.xpath(xpath)));
                    }
                    if(dm)
                        getDriverController().clickWait(By.xpath("//*[@id='premiumConfirmButton']"),1L,TimeUnit.MINUTES);
                }
                tasks.remove(0);
                System.out.println("Build complete");
                return;
            }
            //you are not on the correct page, go to the correct page
            pageController.goToPage(build.getBuildable().getType());
            return;
        }
    }

    private boolean parseOpenedPanel(Planet currentPlanet, String pageSource) {
        Elements v = Jsoup.parse(pageSource).select("#content");
        String currentLevel = v.select("span.level").text().trim();

        String name = v.select("h2").text().trim();

        String time = v.select("#buildDuration").get(0).ownText().trim();

        try {
            String energyNeeded = v.select("span.time").get(1).text().trim();
        }catch (Exception e){/*DO NOTHING*/}

        Elements costs = v.select("#costs");
        List<String> resources = Arrays.asList("metal","crystal","deuterium");
        for(String r : resources){
            Elements res = v.select("li." + r + " > div.cost");
            String cost = res.text().trim();
            boolean hasEnough = !res.hasAttr("overmark");
        }

        Elements build = v.select("a.build-it");
        boolean canBuild = v.select("a.build-it_disabled").size() != 0 ? false : true;
        Buildable b = currentPlanet.getBuildable(name);
        try{b.setCurrentLevel(Integer.parseInt(currentLevel.replace("Level ","").trim()));}
        catch (NumberFormatException nfe){/*DO NOTHING*/}
        BuildTask bt = new BuildTask(b, LocalDateTime.now().plusSeconds(parseTime(time)));
        if(canBuild)
            currentPlanet.setCurrentBuildingBeingBuild(bt);

        return canBuild;
    }

    public static long parseTime(String time) {
        long t = 0;
        String[] split = time.split(" ");
        for(String s : split){
            if(s.contains("s"))
                t+=Integer.parseInt(s.replace("s","").trim());
            if(s.contains("m"))
                t+=Integer.parseInt(s.replace("m","").trim())*60;
            if(s.contains("h"))
                t+=Integer.parseInt(s.replace("h","").trim())*60*60;
            if(s.contains("d"))
                t+=Integer.parseInt(s.replace("d","").trim())*60*60*24;
            //TODO time larger than days
        }
        return t;
    }

    private void preformBeingAttackedAction() throws Exception {
        //TODO
//        i'd imagine you'd first try to send everything away, then spend remaining resources
////        check how long until the attacking fleet will land on your planet.
////        if attacking fleet is less than say... 10 minutes away then
// //       go to Fleet Page
//        send all ships and resources at 10% speed to:
//              if you have a moon, send it to your moon, if you have another colony send it to your colony,
//              otherwise get an inactive planet and send it to that planet
//        save that fleet to be recalled later.
//        if remaining resources greater than a certain amount say... 10k metal then spend it all
//         until fleet arrives.  Metal on rocket launchers, crystal on espionage probes, just spend it all
        //then after attacking fleet lands or the attacking fleet is recalled, then recall your fleet.

        //prevent this algorithm from running if only probe tries to attack.
        //  If your espionage is high enough to see attacking fleet then check that,
        //  otherwise ignore if less than a minute is left before attacking fleet arrives.

        HashSet<FleetObject> fleets = new HashSet<>(FleetObject.getFleetObjects(this));
        getFleetInfo().setFleets(fleets);
        //get enemy fleets
        List<FleetObject> enemyFleets = fleets.stream().filter(
                a -> (a.getMissionOwner() == null || !a.getMissionOwner().equalsIgnoreCase(Mission.OWN_FLEET))
                    && a.getMission().equalsIgnoreCase(Mission.ATTACKING)
                    && isMoreThanJustProbes(a)
        ).collect(Collectors.toList());
        if(enemyFleets.isEmpty() && !isBeingAttacked())
            return;
        //TODO switch to the planet that is being attacked.
        //get the first enemy fleet to land on your planet.
        Collections.sort(enemyFleets,(a,b)->new Long(a.getDataArrivalTime()).compareTo(b.getDataArrivalTime()));
        LocalDateTime enemyFleetLandsDate =
                Instant.ofEpochMilli(enemyFleets.get(0).getDataArrivalTime()*1000).atZone(ZoneId.systemDefault()).toLocalDateTime();
        if(enemyFleetLandsDate.isBefore(LocalDateTime.now().plusMinutes(10)) && enemyFleetLandsDate.isAfter(LocalDateTime.now())) { //you are 10 minutes away from being attacked
            //TODO perfect world you'd run sim and check losses
            getPageController().goToPage(Fleet.FLEET);
            if(!isCurrentPlanetHasNoShips()){//if the planet has ships. Then send them away with the the resources you can.
                //find where you're going to fleet save. //for simplicities sake, just send it to

            }
            System.out.println();
        }
    }

    private boolean isMoreThanJustProbes(FleetObject fleet) {
        List<String> excludeShips = Arrays.asList(Ship.ESPIONAGE_PROBE,Ship.COLONY_SHIP);
        for(String s : fleet.getShips().keySet())
            if(!excludeShips.contains(s) && fleet.getShips().get(s) != 0)
                return false;
        return true;
    }

    public List<BuildTask> getNextBuildTask() throws SQLException, IOException, ClassNotFoundException {
        //TODO buildable requirements
        //TODO maintain add a column to database and check for it here
        //TODO if maintain then make sure that defense/ships count is >= level else add to build queue the difference
        List<BuildTask> buildTasks = new ArrayList<>();
        HashMap<String,Planet> planetIDMap = new HashMap<>();
        for(Map.Entry<String, Planet> planet : getPlanets().entrySet()) {
            List<BuildTask> queue = planet.getValue().getQueueManager(getOgameUserId(),getResearch()).setResearch(getResearch()).getQueue(id)
                    .stream().filter(a->!isDone(a,planet.getValue())).collect(Collectors.toList());

            planetIDMap.put(planet.getValue().getBotPlanetID(),planet.getValue());
            ArrayList<BuildTask> removeList = new ArrayList<>();

            for(BuildTask b : queue){
                if(b.isDone())
                    removeList.add(b);
                Buildable buildable;
                if(getResearch().containsKey(b.getBuildable().getName()))
                    buildable = Buildable.getBuildableByName(b.getBuildable().getName());
                else
                    buildable = planet.getValue().getBuildable(b.getBuildable().getName());
                b.setCoordinates(new Coordinates(planet.getKey(),getLogin().getUser().getUniverse()));
                if(b.getCountOrLevel() < 0)
                    buildable.setLevelNeeded(buildable.getCurrentLevel()+1);
                else
                    buildable.setLevelNeeded(b.getCountOrLevel());

                buildTasks.add(b);
            }
            //TODO if cost has energy then put solar sates to get that cost if you don't have it
            HashMap<String,BuildTask> totalRequirements = new HashMap<>();
            for(BuildTask b : buildTasks) {
                HashMap<String, Integer> requirements = getBotRemainingRequirements(b.getBuildable(), planet);
                for (BuildTask buildTask : queue) {
                    Buildable bb = buildTask.getBuildable();
                    if (requirements.containsKey(bb.getName()) && requirements.get(bb.getName()) <= bb.getCurrentLevel())
                        requirements.remove(bb.getName());
                }
                for(String name : requirements.keySet())
                    if(totalRequirements.containsKey(name)) {
                        if (totalRequirements.get(name).getBuildable().getCurrentLevel() < requirements.get(name))
                            totalRequirements.put(name,
                                    totalRequirements.get(name).setBuildable(
                                            totalRequirements.get(name).getBuildable().setCurrentLevel(requirements.get(name))
                                    )
                            );
                    }else
                        totalRequirements.put(name,
                                new BuildTask().setBuildable(
                                        Buildable.getBuildableByName(name).setCurrentLevel(requirements.get(name))
                                ).setBuildPriority(b.getBuildPriority())
                        );
            }
            if(totalRequirements.size()!=0) {
                addTotalRequirements(totalRequirements, planet.getValue());
                return getNextBuildTask();
            }
        }
        if(buildTasks.size() <= 0)
            return new ArrayList<>();
        Collections.shuffle(buildTasks);
        Collections.sort(buildTasks);

        //hopefully by this point, buildablePerPlanet has one buildTask per planet of the highest priority
        List<BuildTask> canBuild = new ArrayList<>();
        for(BuildTask buildTask : buildTasks)
            if(canBuild(buildTask))
                canBuild.add(buildTask);

        return canBuild;
    }

    private boolean canBuild(BuildTask buildTask){
        Buildable b = buildTask.getBuildable();
        Planet p = getPlanets().get(buildTask.getCoordinates().getStringValue());
        //if you don't have the prerequisites, you can't build it
        if(!p.hasPrerequisites(b.getName(),getResearch()))
            return false;

        //it is a research or the research lab
        if(b.getType().toLowerCase().equals(Research.RESEARCH.toLowerCase()) || b.getName().equalsIgnoreCase(Facilities.RESEARCH_LAB))
            if(getCurrentResearchBeingBuilt() != null) {
                if (getCurrentResearchBeingBuilt().isDone() && getCurrentResearchBeingBuilt().isComplete()) {
                    return Buildable.getBuildableByName(b.getName()).getNextLevelCost().canAfford(getDarkMatter(), p.getResources());
                } else return false;
            }else if(b.getType().toLowerCase().equals(Research.RESEARCH.toLowerCase()))
                return researchBuildable.get(b.getName()).getNextLevelCost().canAfford(getDarkMatter(), p.getResources());

        HashMap<String, Integer> rr = getResearch();
        if(getCurrentResearchBeingBuilt() != null && !(getCurrentResearchBeingBuilt().isDone() && getCurrentResearchBeingBuilt().isComplete()))
            rr.put(getCurrentResearchBeingBuilt().getBuildable().getName(),getCurrentResearchBeingBuilt().getCountOrLevel()-1);

        return p.canBuild(b.getName(),getResearch());
    }

    private void addTotalRequirements(HashMap<String, BuildTask> totalRequirements, Planet value) throws SQLException, IOException, ClassNotFoundException {
        //TODO add to database the prerequisites  needs priority
        //TODO check if queue already has it
        for(String s : totalRequirements.keySet())
            buildRequest(value.getBotPlanetID(),totalRequirements.get(s));
//            getDatabase().executeQuery(
//                    "insert into planet_queue(bot_planets_id,buildable_id,build_level,build_priority)" +
//                    "   values("+value.getBotPlanetID()+","+totalRequirements.get(s).getBuildable().getId()+","+totalRequirements.get(s).getBuildable().getCurrentLevel()+","+totalRequirements.get(s).getBuildPriority()+");"
//            );
    }

    private HashMap<String, Integer> getBotRemainingRequirements(Buildable buildable, Map.Entry<String, Planet> planet) {
        HashMap<String, Integer> requirements = Buildable.getBuildableRequirements(buildable.getName());
        List<String> removeMe = new ArrayList<>();
        for(Map.Entry<String, Integer> s : requirements.entrySet()){
            Buildable bb = Buildable.getBuildableByName(s.getKey());
            if(bb.getType().toLowerCase().equals(Research.RESEARCH.toLowerCase())){
                if(getResearch().get(s.getKey()) >= s.getValue())
                    removeMe.add(s.getKey());
                continue;
            }else if(planet.getValue().getAllBuildables().get(s.getKey()).getCurrentLevel() >= s.getValue())
                removeMe.add(s.getKey());
        }
        for(String s : removeMe)
            requirements.remove(s);
        return requirements;
    }

    private boolean isDone(BuildTask task, Planet value) {
        Buildable buildable = task.getBuildable();
        boolean b = true;
        int level = buildable.getLevelNeeded();
        level = level == 0 ? buildable.getCurrentLevel() : level;
        if(level < 0)
            b = false;
        else if(buildable.getType().toLowerCase().equals(Research.RESEARCH.toLowerCase()))
            b = research.get(buildable.getName()) >= level;
        else if(Arrays.asList(Defense.DEFENSE.toLowerCase(),Shipyard.SHIPYARD.toLowerCase()).contains(buildable.getType().toLowerCase())) {
            int currentLevel = value.getAllBuildables().get(buildable.getName()).getCurrentLevel();
            if(value.getCurrentShipyardBeingBuild() != null && value.getCurrentShipyardBeingBuild().size() != 0)
                for (BuildTask bb : value.getCurrentShipyardBeingBuild())
                    if (bb.getBuildable().getName().equals(task.getBuildable().getName()))
                        currentLevel+=bb.getCountOrLevel();
            b = currentLevel >= level;
            task.setCountOrLevel(level - currentLevel);
        }else {
            int currentLevel = value.getAllBuildables().get(buildable.getName()).getCurrentLevel();
            b = currentLevel >= level;
        }if(b) {
            final int buildLevel = level;
            markAsDone(task);
        }
        return b;
    }

    private void markAsDone(BuildTask task){
        FileOptions.runConcurrentProcessNonBlocking((Callable) () -> {
            try {
                getDatabase().executeQuery(
                        "update planet_queue set done = 'Y' " +
                                "where bot_planets_id = " + task.getBotPlanetID() + " and " +
                                "buildable_id = " + task.getBuildable().getId() + " and " +
                                "build_level = " + task.getBuildable().getCurrentLevel()
                );
            }catch (Exception e){
                if(e.getMessage().contains("ERROR: duplicate key value violates unique constraint"))
                    getDatabase().executeQuery(
                            "update planet_queue set " +
                                    "build_level = build_level + " + task.getBuildable().getCurrentLevel()+" "+
                                        "where bot_planets_id = " + task.getBotPlanetID() + " and " +
                                        "buildable_id = " + task.getBuildable().getId() + " and " +
                                        "build_level = " + task.getBuildable().getCurrentLevel() + " and " +
                                        "done = 'Y'; " +
                            "delete from planet_queue where bot_planets_id = " + task.getBotPlanetID() + " and " +
                                "buildable_id = " + task.getBuildable().getId() + " and " +
                                "build_level = " + task.getBuildable().getCurrentLevel() + " and " +
                                "done = 'N';"
                    );
            }
            return null;
        });
    }

    public BuildTask getCurrentResearchBeingBuilt() {
        return currentResearchBeingBuilt;
    }

    public void setCurrentResearchBeingBuilt(BuildTask currentResearchBeingBuilt) {
        this.currentResearchBeingBuilt = currentResearchBeingBuilt;
    }

    public String getServerDomain(){
        return getLogin().getServer().getDomain();
    }
    public String getCookies(){
        Set<Cookie> cookies = getDriverController().getDriver().manage().getCookies();
        StringBuilder builder = new StringBuilder("");
        for(Cookie c : cookies)
            builder.append(c.getName() + "=" + c.getValue() + "; ");
        return builder.toString();
    }

    public boolean canGetAnotherPlanet(Coordinates toCoordinates) {
        return canGetAnotherPlanet(getPlanets().size(),getResearch().get(Research.ASTROPHYSICS),toCoordinates);
    }
    public boolean canGetAnotherPlanet(Integer numPlanets,Integer astroLevel, Coordinates toCoordinates){
        if(astroLevel < 1)
            return false;
        int planets = numPlanets, allowedPlanets = 1+(astroLevel+1)/2;
        if(planets >= allowedPlanets)
            return false;
        if(astroLevel < 4 && (toCoordinates.getPlanet() > 12 || toCoordinates.getPlanet() < 4))
            return false;
        if(astroLevel < 6 && (toCoordinates.getPlanet() > 13 || toCoordinates.getPlanet() < 3))
            return false;
        if(astroLevel < 8 && (toCoordinates.getPlanet() > 14 || toCoordinates.getPlanet() < 2))
            return false;
        return true;
    }

    public AttackManager getAttackManager() {
        if(attackManager == null)
            attackManager = new AttackManager(login.getUser().getUsername(),login.getServer(), getOgameUserId(), getPoints(), getRank(),getCurrentPlanetCoordinates());
        return attackManager.setRankPoints(getRank(),getPoints());
    }


    public boolean isCurrentPlanetHasNoShips() {
        return getCurrentPlanet().getShips().values().stream().filter(a->a != 0).collect(Collectors.toList()).size() == 0;
    }

    public void setAttackManager(AttackManager attackManager) {
        this.attackManager = attackManager;
    }

    public Set<MessageObject> getMessages() {
        return messages;
    }

    public void setMessages(Set<MessageObject> messages) {
        this.messages = messages;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getTotalRanks() {
        return totalRanks;
    }

    public void setTotalRanks(int totalRanks) {
        this.totalRanks = totalRanks;
    }

    public int getHonorPoints() {
        return honorPoints;
    }

    public void setHonorPoints(int honorPoints) {
        this.honorPoints = honorPoints;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public ExecutorService getBuildTaskService() {
        return buildTaskService;
    }

    public void setBuildTaskService(ExecutorService buildTaskService) {
        this.buildTaskService = buildTaskService;
    }

    public List<BuildTask> getBuildTasks() {
        return buildTasks;
    }

    public void setBuildTasks(List<BuildTask> buildTasks) {
        this.buildTasks = buildTasks;
    }

    public boolean isBeingAttacked() {
        return !Jsoup.parse(getDriverController().getDriver().getPageSource()).select("#attack_alert").hasClass("noAttack");
    }

    public void setBeingAttacked(boolean beingAttacked) {
        isBeingAttacked = beingAttacked;
    }

    public void addResearch(Buildable bb) {
        researchBuildable.put(bb.getName(),bb);
        research.put(bb.getName(),bb.getCurrentLevel());
    }

    public Planet getCurrentPlanet(){
        return planets.get(currentPlanetCoordinates.getStringValue());
    }

    public DriverController getDriverController() {
        return driverController;
    }

    public void setDriverController(DriverController driverController) {
        this.driverController = driverController;
    }

    public Login getLogin() {
        return login;
    }

    public void setLogin(Login login) {
        this.login = login;
    }

    public HashMap<String, Planet> getPlanets() {
        return planets;
    }

    public void setPlanets(HashMap<String, Planet> planets) {
        this.planets = planets;
    }

    public HashMap<String, Integer> getResearch() {
        return research;
    }

    public void setResearch(HashMap<String, Integer> research) {
        this.research = research;
    }

    public long getDarkMatter() {
        return darkMatter;
    }

    public void setDarkMatter(long darkMatter) {
        this.darkMatter = darkMatter;
    }

    public int getUnreadMessages() {
        return unreadMessages;
    }

    public void setUnreadMessages(int unreadMessages) {
        this.unreadMessages = unreadMessages;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOgameUserId() {
        return ogameUserId;
    }

    public void setOgameUserId(int ogameUserId) {
        this.ogameUserId = ogameUserId;
    }

    public int getWebdriverId() {
        return webdriverId;
    }

    public void setWebdriverId(int webdriverId) {
        this.webdriverId = webdriverId;
    }

    public String getName() {
        if(name == null)
            name = login.getUser().getUniverse()+"_"+login.getUser().getUsername();
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getBotDataDirectory() {
        return botDataDirectory;
    }

    public void setBotDataDirectory(String botDataDirectory) {
        this.botDataDirectory = botDataDirectory;
    }

    public String getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(String currentPage) {
        this.currentPage = currentPage;
    }

    public Coordinates getCurrentPlanetCoordinates() {
        return currentPlanetCoordinates;
    }

    public void setCurrentPlanetCoordinates(Coordinates currentPlanetCoordinates) {
        this.currentPlanetCoordinates = currentPlanetCoordinates;
    }

    public FleetInfo getFleetInfo() {
        return fleetInfo;
    }

    public void setFleetInfo(FleetInfo fleetInfo) {
        this.fleetInfo = fleetInfo;
    }

    public PageController getPageController() {
        return pageController;
    }

    public void setPageController(PageController pageController) {
        this.pageController = pageController;
    }

    @Override
    public String toString() {
        return "Bot{" +
                "driverController=" + driverController +
                ", login=" + login +
                ", planets=" + planets +
                ", research=" + research +
                ", darkMatter=" + darkMatter +
                ", unreadMessages=" + unreadMessages +
                ", id=" + id +
                ", ogameUserId=" + ogameUserId +
                ", webdriverId=" + webdriverId +
                ", name='" + name + '\'' +
                ", isActive=" + isActive +
                ", startDate='" + startDate + '\'' +
                ", botDataDirectory='" + botDataDirectory + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bot bot = (Bot) o;

        if (darkMatter != bot.darkMatter) return false;
        if (unreadMessages != bot.unreadMessages) return false;
        if (id != bot.id) return false;
        if (ogameUserId != bot.ogameUserId) return false;
        if (webdriverId != bot.webdriverId) return false;
        if (isActive != bot.isActive) return false;
        if (driverController != null ? !driverController.equals(bot.driverController) : bot.driverController != null)
            return false;
        if (login != null ? !login.equals(bot.login) : bot.login != null) return false;
        if (planets != null ? !planets.equals(bot.planets) : bot.planets != null) return false;
        if (research != null ? !research.equals(bot.research) : bot.research != null) return false;
        if (d != null ? !d.equals(bot.d) : bot.d != null) return false;
        if (name != null ? !name.equals(bot.name) : bot.name != null) return false;
        if (startDate != null ? !startDate.equals(bot.startDate) : bot.startDate != null) return false;
        return botDataDirectory != null ? botDataDirectory.equals(bot.botDataDirectory) : bot.botDataDirectory == null;
    }

    @Override
    public int hashCode() {
        int result = driverController != null ? driverController.hashCode() : 0;
        result = 31 * result + (login != null ? login.hashCode() : 0);
        result = 31 * result + (planets != null ? planets.hashCode() : 0);
        result = 31 * result + (research != null ? research.hashCode() : 0);
        result = 31 * result + (int) (darkMatter ^ (darkMatter >>> 32));
        result = 31 * result + unreadMessages;
        result = 31 * result + (d != null ? d.hashCode() : 0);
        result = 31 * result + id;
        result = 31 * result + ogameUserId;
        result = 31 * result + webdriverId;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (isActive ? 1 : 0);
        result = 31 * result + (startDate != null ? startDate.hashCode() : 0);
        result = 31 * result + (botDataDirectory != null ? botDataDirectory.hashCode() : 0);
        return result;
    }
}
