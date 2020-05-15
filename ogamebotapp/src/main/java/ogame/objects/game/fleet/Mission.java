package ogame.objects.game.fleet;

import bot.Bot;
import ogame.objects.game.Coordinates;
import ogame.objects.game.Ship;
import ogame.pages.Fleet;
import ogame.pages.Overview;
import ogame.pages.Research;
import org.jsoup.Jsoup;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import utilities.webdriver.DriverController;
import utilities.webdriver.JavaScriptFunctions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by jarndt on 5/31/17.
 */
public class Mission {
    public static final String
            ATTACKING       = "Attacking",
            DEPLOYMENT      = "Deplyoment",
            ESPIONAGE       = "Espionage",
            TRANSPORT       = "Transport",
            COLONIZATION    = "Colonization",
            HARVESTING      = "Harvesting",
            DESTROY         = "Destroy",
            EXPEDITION      = "Expedition",

    OWN_FLEET       = "Own Fleet",
            FOREIGN_FLEET   = "Foreign Fleet";

    private HashMap<String,String> missionSelector;

    private int espionageMission = 6,
            missileAttack = 10,
            expedition = 15,
            colonization = 7,
            recycleDebrisField = 8,
            transport = 3,
            deployment = 4,
            acsDefend = 5,
            attack = 1,
            acsAttack = 2,
            moonDestruction = 9;

    public Mission(){
        init();
    }
    private void init(){
        missionSelector = new HashMap<>();
        missionSelector.put(TRANSPORT,"#missionButton"+transport);
        missionSelector.put(ATTACKING,"#missionButton"+attack);
        missionSelector.put(ESPIONAGE,"#missionButton"+espionageMission);
    }

    private int type = 1;
    //attack https://s135-en.ogame.gameforge.com/game/index.php?page=fleet1&amp;galaxy=8&amp;system=307&amp;position=11&amp;type=1&amp;mission=1

    public boolean sendFleet(FleetObject fleetObject, Bot b) throws Exception {
        b.getPageController().goToPage(Fleet.FLEET);
        b.getPageController().parsePage(Fleet.FLEET);
        if(b.getFleetInfo().getFleetsRemaining() == 0) {
            System.out.println("No fleet slots");
            return false;
        }
        if(fleetObject.getMission().equals(Mission.COLONIZATION) && b.canGetAnotherPlanet(fleetObject.getToCoordinates())){
            System.out.println("Astrophysics too low to colonize");
            return false;
        }
        if(fleetObject.getMission().equals(Mission.ESPIONAGE) &&
                fleetObject.getShips().containsKey(Ship.ESPIONAGE_PROBE) &&
                fleetObject.getShips().get(Ship.ESPIONAGE_PROBE) < 1){
            System.out.println("No probes in mission, can't go on an espionage mission");
            return false;
        }if(fleetObject.getMission().equals(Mission.ESPIONAGE) &&
                fleetObject.getShips().containsKey(Ship.ESPIONAGE_PROBE) &&
                fleetObject.getShips().get(Ship.ESPIONAGE_PROBE) == 1) {
            sendProbe(b.getDriverController(), fleetObject.getToCoordinates());
            return false;
        }

        String shipIdR = "[SHIP_ID]", shipCountR = "[SHIP_COUNT]";
        String jsSelectShip = "toggleMaxShips(\"#shipsChosen\", "+shipIdR+","+shipCountR+"); ",
                jsSubmit    = "checkShips(\"shipsChosen\"); trySubmit();";

        StringBuilder page1 = new StringBuilder("");
        for(Map.Entry<String, Integer> s : fleetObject.getShips().entrySet())
            page1.append(
                    jsSelectShip
                            .replace(shipIdR,Ship.getShipByName(s.getKey()).getShipID().replace("am",""))
                            .replace(shipCountR,s.getValue()+"")
            );
        page1.append(jsSubmit);

        DriverController d = b.getDriverController();
        d.executeJavaScript(page1.toString());
        Thread.sleep(500);
        if(!d.waitForElement(By.xpath("//*[@id='mission']"),1L, TimeUnit.MINUTES)) {
            //next page didn't load, try again
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            b.getPageController().goToPage(Overview.OVERVIEW);
            sendFleet(fleetObject, b);
        }

        //PAGE 2
        try {
            //TODO org.openqa.selenium.WebDriverException: {"errorMessage":"undefined is not an object (evaluating '$x(\"//*[@id='galaxy']\")[0].value=\"8\"')","request":{"headers":{"Accept-Encoding":"gzip,deflate","Connection":"Keep-Alive","Content-Length":"377","Content-Type":"application/json; charset=utf-8","Host":"localhost:7743","User-Agent":"Apache-HttpClient/4.5.2 (Java/1.8.0_121)"},"httpVersion":"1.1","method":"POST","post":"{\"script\":\"var $x = function(xpathToExecute){\\n  var result = [];\\n  var nodesSnapshot = document.evaluate(xpathToExecute, document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null );\\n  for ( var i=0 ; i < nodesSnapshot.snapshotLength; i++ ){\\n    result.push( nodesSnapshot.snapshotItem(i) );\\n  }\\n  return result;\\n};$x(\\\"//*[@id='galaxy']\\\")[0].value=\\\"8\\\";\",\"args\":[]}","url":"/execute","urlParsed":{"anchor":"","query":"","file":"execute","directory":"/","path":"/execute","relative":"/execute","port":"","host":"","password":"","user":"","userInfo":"","authority":"","protocol":"","source":"/execute","queryKey":{},"chunks":["execute"]},"urlOriginal":"/session/c7f2b470-627f-11e7-b075-4532bba2b786/execute"}}
            Thread.sleep(500);
            Coordinates c = fleetObject.getToCoordinates();
            try {
                JavaScriptFunctions.fillFormByXpath(d, "//*[@id='galaxy']", c.getGalaxy() + "");
                JavaScriptFunctions.fillFormByXpath(d, "//*[@id='system']", c.getSystem() + "");
                JavaScriptFunctions.fillFormByXpath(d, "//*[@id='position']", c.getPlanet() + "");
            }catch (WebDriverException e){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                JavaScriptFunctions.fillFormByXpath(d, "//*[@id='galaxy']", c.getGalaxy() + "");
                JavaScriptFunctions.fillFormByXpath(d, "//*[@id='system']", c.getSystem() + "");
                JavaScriptFunctions.fillFormByXpath(d, "//*[@id='position']", c.getPlanet() + "");
            }
            //TODO Can't find variable: currentPage
            d.executeJavaScript("updateVariables();");

            //TODO fleet speed needed for fleet saves
            //TODO can't find variable trySubmit
            try{ d.executeJavaScript("trySubmit();");}catch (WebDriverException e){
                try {
                    Thread.sleep(1000);
                    d.executeJavaScript("trySubmit();");
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }

            if(!d.waitForElement(By.xpath("//*[@id='fleetStatusBar']"),1L, TimeUnit.MINUTES)){
                //next page didn't load, try again
                System.out.println("Something went wrong with coordinates: "+c);
                return false;
            }
            //PAGE 3
            int count = 0;
            do{
                String js = getJSForMissionType(fleetObject.getMission(), b);
                //TODO can't find variable: serverTime i suspect it's too fast and potential sleeps are needed
                try {
                    d.executeJavaScript(js);
                    Thread.sleep(1000);
                } catch (WebDriverException e) {
                    try {
                        Thread.sleep(1000);
                        d.executeJavaScript(js);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
                if(count++ > 10)
                    throw new Exception("Stuck on Fleet page 3");
            }while(Jsoup.parse(b.getDriverController().getDriver().getPageSource()).select("#missionName").text()
                    .equalsIgnoreCase("Nothing has been selected"));//fleetObject.getMission()
        }catch (WebDriverException e){
            b.getPageController().goToPage(Overview.OVERVIEW);
            throw e;
        }
        Thread.sleep(500);
        //TODO load resources
        d.executeJavaScript("trySubmit();");
        return true;
    }

    private String getJSForMissionType(String mission, Bot b) {
        return Jsoup.parse(b.getDriverController().getDriver().getPageSource()).select(missionSelector.get(mission)).attr("onclick");
    }


    private String missionR = "[MISSION]", galaxyR = "[GALAXY]", systemR = "[SYSTEM]", planetR = "[PLANET]", typeR = "[TYPE]", countR = "[COUNT]";
    private String sendShips = "" +
            "sendShipsWithPopup(\n" +
            "                "+missionR+",\n" + //mission
            "                "+galaxyR+",\n" + //galaxy
            "                "+systemR+",\n" + //system
            "                "+planetR+",\n" + //planet
            "                "+typeR+",\n" + //type
            "                "+countR+" \n" +//count
            "        ); return";


    public void sendProbe(DriverController controller, Coordinates coordinates) {
        sendProbe(controller,coordinates,1);
    }public void sendProbe(DriverController controller, Coordinates coordinates, int count){
        controller.executeJavaScript(
                sendShips.replace(missionR,espionageMission+"")
                        .replace(galaxyR,coordinates.getGalaxy()+"")
                        .replace(systemR,coordinates.getSystem()+"")
                        .replace(planetR,coordinates.getPlanet()+"")
                        .replace(typeR,type+"")
                        .replace(countR,count+"")
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////// STATIC STUFF ///////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String getMission(String s) {
        if(s.equals("Attack") || s.equals("Attack (R)"))
            return ATTACKING;
        if(s.contains("Espionage"))
            return ESPIONAGE;

        return null;
    }
    public static String getFleetType(String s) {
        if(s.equals("Own fleet"))
            return OWN_FLEET;

        return null;
    }
}
