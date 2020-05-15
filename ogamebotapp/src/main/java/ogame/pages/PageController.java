package ogame.pages;

import bot.Bot;
import com.google.gson.Gson;
import ogame.objects.game.BuildTask;
import ogame.objects.game.Buildable;
import ogame.objects.game.Coordinates;
import ogame.objects.game.Resource;
import ogame.objects.game.fleet.FleetObject;
import ogame.objects.game.planet.Planet;
import ogame.objects.game.planet.PlanetBuilder;
import ogame.objects.game.planet.PlanetProperties;
import ogame.objects.game.planet.ResourceObject;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import utilities.fileio.FileOptions;
import utilities.webdriver.DriverController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by jarndt on 5/30/17.
 */
public class PageController {
    Bot b;
    HashMap<String,OgamePage> ogamePages = new HashMap<>();

    public PageController(Bot b){this.b = b; init();}

    private void init(){
        ogamePages.put(Login.HOMEPAGE.toLowerCase(),new Login());
        ogamePages.put(Messages.MESSAGES.toLowerCase(),new Messages());
        ogamePages.put(Overview.OVERVIEW.toLowerCase(),new Overview());
        ogamePages.put(Resources.RESOURCES.toLowerCase(),new Resources());
        ogamePages.put(Facilities.FACILITIES.toLowerCase(),new Facilities());
        ogamePages.put(Merchant.MERCHANT.toLowerCase(),new Merchant());
        ogamePages.put(Research.RESEARCH.toLowerCase(),new Research());
        ogamePages.put(Shipyard.SHIPYARD.toLowerCase(),new Shipyard());
        ogamePages.put(Defense.DEFENSE.toLowerCase(),new Defense());
        ogamePages.put(Fleet.FLEET.toLowerCase(),new Fleet());
        ogamePages.put(Galaxy.GALAXY.toLowerCase(),new Galaxy());
        ogamePages.put(Alliance.ALLIANCE.toLowerCase(),new Alliance());
        ogamePages.put(RecruitOfficers.RECRUIT_OFFICERS.toLowerCase(),new RecruitOfficers());
        ogamePages.put(Shipyard.SHIPYARD.toLowerCase(),new Shipyard());
    }

    public boolean goToPage(String pageName) throws Exception {
        //TODO utilize tooltip on each planet (right side) to navigate to pages this allows direct navigation to different planets
        OgamePage page = ogamePages.get(pageName.toLowerCase());
        b.setCurrentPage(pageName);
        if(getCurrentPage().equals(Login.HOMEPAGE.toLowerCase()))
            throw new Exception("Got Logged Out");
        if(getCurrentPage().equals(pageName))
            b.getDriverController().getDriver().navigate().refresh();
        else
            b.getDriverController().clickWait(By.xpath(page.getXPathSelector()),1L,TimeUnit.MINUTES);
        boolean r = page.waitForPageToLoad(b.getDriverController(),TimeUnit.MINUTES,1L);
        parsePage(page);
        return r;
    }
    public void parsePage(String pageName) throws IOException {
        parsePage(ogamePages.get(pageName.toLowerCase()));
    }
    public void parsePage(OgamePage page) throws IOException {
        b.getDriverController().waitForElement(By.xpath("//*[@id='metal_box']/div"),1L, TimeUnit.MINUTES);
        Document d = Jsoup.parse(b.getDriverController().getDriver().getPageSource());
        FileOptions.runConcurrentProcess((Callable)()->{
            parseAllPageContent(d);

            page.parsePage(b,d);

            FileOptions.runConcurrentProcessNonBlocking((Callable)()->{writeBotDataToFile(); return null;});

            return null;
        });
    }

    private void parseAllPageContent(Document d) throws IOException {
        //TODO test fleet movement
        FileOptions.runConcurrentProcessNonBlocking((Callable)()->{
            b.getFleetInfo().setFleets(new HashSet<>(FleetObject.getFleetObjects(b)));
            return null;
        });

        if(!d.select("#attack_alert").hasClass("noAttack"))
            b.setBeingAttacked(true);
        else
            b.setBeingAttacked(false);

        for(Element planet : d.select("#planetList > div")){
            Coordinates coords = new Coordinates(planet.select("span.planet-koords").text())
                    .setUniverse(b.getLogin().getUser().getUniverse());
            String planetName = planet.select("span.planet-name").text().trim();
            if(!b.getPlanets().containsKey(coords.getStringValue()))
                b.getPlanets().put(coords.getStringValue(),
                        new PlanetBuilder()
                                .setId(planet.id())
                                .setClassName(planet.className())
                                .setPlanetImageURL(planet.select("img").attr("src"))
                                .setLink(planet.select("a").attr("href"))
                                .setCoordinates(coords)
                                .setPlanetName(planetName)
                                .setPlanetSize(parsePlanetProperties(planet,coords))
                                .build()
                );
            else{
                Planet p = b.getPlanets().get(coords.getStringValue());
                p.setId(planet.id());
                p.setId(planet.className());
                p.setPlanetImageURL(planet.select("img").attr("src"));
                p.setCoordinates(coords);
                p.setPlanetName(planetName);
                p.setPlanetSize(parsePlanetProperties(planet,coords));
            }
        }

        if(b.getPlanets().size() == 1)
            b.setCurrentPlanetCoordinates(new Coordinates(new ArrayList<>(b.getPlanets().keySet()).get(0),b.getLogin().getUser().getUniverse()));
        else {
            System.out.println("Not implemented yet");
            System.exit(-10);
        }
        Planet currentPlanet = b.getCurrentPlanet();

        //parse resources
        List<Long> values = new ArrayList<>();
        String[] storage = new String[4];
        int i = 0;
        for(String v : Arrays.asList("metal","crystal","deuterium","energy","darkmatter")) {
            Elements vv = d.select("#resources_" + v);
            values.add(Long.parseLong(vv.text().trim().replace(".", "")));
            if(i<4)
                storage[i] = vv.attr("class").trim();

            i++;
        }
        currentPlanet.setMetalStorageString(storage[0]);
        currentPlanet.setCrystalStorageString(storage[1]);
        currentPlanet.setDueteriumStorageString(storage[2]);
        currentPlanet.setEnergyStorageString(storage[3]);

        currentPlanet.setResources(new Resource(values.get(0),values.get(1),values.get(2),values.get(3)));

        resourcesFromToolTip(b,d);

        b.setDarkMatter(values.get(4));

        //parse message count
        Elements v = d.select("#message-wrapper > a.comm_menu.messages.tooltip.js_hideTipOnMobile > span");
        int message = 0;
        if(v!=null && v.text()!=null && !v.text().isEmpty())
            try{ message = Integer.parseInt(v.text().trim());}
            catch (Exception e){ /*DO NOTHING*/}
        b.setUnreadMessages(message);

        //parse current page name
        b.setCurrentPage(d.select("#menuTable > li > a.selected").select("span").text().trim());

        try{
            boolean validated = d.select("#advice-bar > a").attr("title").trim().isEmpty();
            b.getLogin().getUser().setVerified(validated);
        }catch (Exception e){/*DO NOTHING*/}
        try {
            String rank = d.select("#bar > ul > li:nth-child(2)").get(0).ownText().trim().replaceAll("[\\(|\\)]", "");
            b.setRank(Integer.parseInt(rank));
        }catch (Exception e){/*DO NOTHING*/}
    }

    private PlanetProperties parsePlanetProperties(Element planet, Coordinates planetName) {
        List<String> v = Arrays.asList(planet.select("a").attr("title")
                .replaceAll("<a.*", "")
                .split("<\\/?br?\\/?>")
        ).stream().filter(a -> a != null && !a.isEmpty()).collect(Collectors.toList());

        PlanetProperties planetProperties = new PlanetProperties();
        boolean parse = false;
        int j = 0;
        for (int i = 0; i < v.size(); i++) {
            if(v.get(i).contains(planetName.getStringValue())) {
                parse = true;
                continue;
            }if(!parse) continue;
            if(j>1) return planetProperties;
            if(j == 0){
                String[] split = v.get(i).split("km \\(");
                planetProperties.setSize(Integer.parseInt(split[0].replace(".","")));
                String[] sizeSplit = split[1].replace(")","").split("/");
                planetProperties.setTotalFields(Integer.parseInt(sizeSplit[1]));
                planetProperties.setUsedFields(Integer.parseInt(sizeSplit[0]));
            }else if(j == 1){
                String[] split = v.get(i).replace("°C","").split(" to ");
                planetProperties.setMaxTemp(Integer.parseInt(split[1]));
                planetProperties.setMinTemp(Integer.parseInt(split[0]));
            }
            j++;
        }

        return planetProperties;
    }

    private void resourcesFromToolTip(Bot b, Document d) {
        List<Element> v = d.getElementsByTag("script").stream()
                .filter(a -> !a.hasAttr("src") && a.toString().contains("resourceTooltip"))
                .collect(Collectors.toList());
        List<String> functions = new ArrayList<>(Arrays.asList(v.get(0).toString().split("function")))
                .stream().filter(a->a.contains("resourceTooltip")).collect(Collectors.toList());

        List<String> resources = new ArrayList<>(Arrays.asList(functions.get(0).split("[{|}]")))
                .stream().filter(a -> a.contains("actualFormat")).collect(Collectors.toList());

        JSONObject jo = new JSONObject(functions.get(0).replaceAll(".*\\(","").replace(");}",""));

        for(String s : jo.keySet()){
            if("honorScore".equals(s)) {
                b.setHonorPoints(jo.getInt(s));
                continue;
            }
            ResourceObject ro = new ResourceObject(s,jo.getJSONObject(s));
            if(ResourceObject.DARK_MATTER.equals(s))
                b.setDarkMatter(ro.getActual());
            b.getCurrentPlanet().getResourceObjects().add(ro);
        }
    }

    private void writeBotDataToFile() throws IOException {
        FileOptions.writeToFileOverWrite(
                b.getBotDataDirectory()+"/"+b.getName()+"_data.json",
                new Gson().toJson(b)
        );
    }

    public OgamePage getPage(String pageName) {
        return ogamePages.get(pageName.toLowerCase());
    }

    public void goToPageOnPlanet(Coordinates planetCoordinates, String page) throws Exception {
        //TODO test navigating between planets
        b.getDriverController().getDriver().navigate().to(b.getPlanets().get(planetCoordinates.getStringValue()).getLinkToPage(page.toLowerCase()));
        goToPage(page);
    }

    public static void parseGenericBuildings(Document document, Bot b, boolean...pass){
        Elements v = document.select("div.buildingimg");
        Planet p = b.getCurrentPlanet();
        for(Element e : v) {
            try {
                String name = e.select("a > span > span > span").text().trim();
                if(name.isEmpty())
                    name = e.parent().attr("title").replaceAll("[0-9\\(\\)]","").trim();
                Buildable bb = Buildable.getBuildableByName(name);
                try {
                    Integer level = Integer.parseInt(e.select("span.level").get(0).ownText().trim());
                    bb.setCurrentLevel(level);
                    bb.setCssSelector(e.cssSelector());
                    bb.setQuickBuildLink(e.select("a").attr("onclick").trim());
                }catch (Exception ee){
                    bb.setQuickBuildLink(null);
                }
                bb.setRef(e.select("a").attr("ref").trim());
                if(b.getCurrentPage().equalsIgnoreCase(Research.RESEARCH))
                    b.addResearch(bb);
                else
                    p.addBuildable(bb);
            }catch (IndexOutOfBoundsException ioobe){/*DO NOTHING, building is currently being built*/}
        }
        if(pass != null && pass.length > 0 && pass[0])
            return;

        Elements activeConstruction = document.select("#inhalt").select("div.content-box-s > div.content > table");
        boolean works = activeConstruction.select("tr").size() == 5;
        if(works) {
            BuildTask buildTask = new BuildTask();
            String name = activeConstruction.select("tr > th").text().trim();
            Buildable bb = p.getBuildable(name);
            if(bb == null)
                bb = Buildable.getBuildableByName(name).setCurrentLevel(b.getResearch().get(name));
            buildTask.setBuildable(bb);
            int level = 0;
            try {
                level = Integer.parseInt(activeConstruction.select("span.level").text().replace("Level ", "").trim());
            }catch (Exception e){
                level = Integer.parseInt(activeConstruction.select("div.shipSumCount").text().trim());
            }
            buildTask.setCountOrLevel(level);
            long time;
            try {
                time = Bot.parseTime(activeConstruction.select("td.desc.timer > span").text().trim());
            }catch (NumberFormatException nfe){
                return;
            }
            if(time == 0){
                try {
                    time = Bot.parseTime(activeConstruction.select("span.shipAllCountdown").text().trim());
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            buildTask.setCompleteTime(LocalDateTime.now().plusSeconds(time));

            if(activeConstruction.select("#researchCountdown").size() != 0) {
                b.setCurrentResearchBeingBuilt(buildTask);
                b.getResearch().put(name,level);
            }else {
                p.setCurrentBuildingBeingBuild(buildTask);
                p.addBuildable(buildTask.getBuildable());
            }
        }else{
            String headerText = document.select("#header_text").text();
            if(headerText.startsWith(Research.RESEARCH))
                b.setCurrentResearchBeingBuilt(null);
            else if(headerText.startsWith(Facilities.FACILITIES) || headerText.startsWith(Resources.RESOURCES))
                p.setCurrentBuildingBeingBuild(null);
            else if(headerText.startsWith(Shipyard.SHIPYARD) || headerText.startsWith(Defense.DEFENSE))
                p.setCurrentShipyardBeingBuild(new HashSet<>());
        }
    }

    public static void parseShips(Bot b, Document document) {
        List<Element> elements = new ArrayList<>();
        elements.addAll(document.select("table.construction.active"));
        elements.addAll(document.select("ul.item > li.tooltip"));

        Planet p = b.getCurrentPlanet();
        p.setCurrentShipyardBeingBuild(null);
        for(Element e : elements){
            BuildTask buildTask = new BuildTask();
            String name = "";
            int count = 0;
            long time = 0;
            if(e.tagName().equals("li")){
                String[] dd = e.attr("title").split("<br>");
                name = dd[0].replaceAll("[0-9]+","").trim();
                count = Integer.parseInt(dd[0].replaceAll("[A-Za-z ]+",""));
                time = Bot.parseTime(dd[1].replace("Building duration ",""));
            }else {
                name = e.select("tr > th").text();
                count = Integer.parseInt(e.select("div.shipSumCount").text().trim());
                time = Bot.parseTime(e.select("span.shipAllCountdown").text());
            }
            Buildable bb = Buildable.getBuildableByName(name);
            bb.setCurrentLevel(count);
            buildTask.setBuildable(bb);
            buildTask.setCountOrLevel(count);
            buildTask.setCompleteTime(LocalDateTime.now().plusSeconds(time));

            p.getCurrentShipyardBeingBuild().add(buildTask);
        }
    }

    public String getCurrentPage() {
        final Document source = Jsoup.parse(b.getDriverController().getDriver().getPageSource());
        List<Callable> callables = ogamePages.values().stream().map(a -> (Callable) () -> {
            if (source.select(a.uniqueCssSelector()).size() > 0)
                return a.getPageName();
            return null;
        }).collect(Collectors.toList());

        FileOptions.runConcurrentProcess(callables);
        List<String> values = callables.stream().map(a -> {
            try {
                return a.call().toString();
            } catch (Exception e) { /*DO NOTHING*/ }
            return null;
        }).filter(a -> a != null).collect(Collectors.toList());

//        for(OgamePage p : ogamePages.values())
//            if(b.getDriverController().getDriver().findElements(By.cssSelector(p.uniqueCssSelector())).size() > 0)
//                return p.getPageName();

        return values.size() == 1 ? values.get(0): Overview.OVERVIEW;
    }


//    private PageController(DriverController driverController){
//        this.driverController = driverController;
//        init();
//    }
//    private static PageController instance;
//    private DriverController driverController;
//    private static PageController getInstance(DriverController driverController){
//        if(instance == null)
//            instance = new PageController(driverController);
//        return instance;
//    }
//
//    public static PageController getStaticPageController(DriverController driverController){
//        return getInstance(driverController);
//    }
}
