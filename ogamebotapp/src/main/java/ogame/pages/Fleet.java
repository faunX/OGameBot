package ogame.pages;

import bot.Bot;
import bot.settings.SettingsManager;
import ogame.objects.game.BuildTask;
import ogame.objects.game.Buildable;
import ogame.objects.game.Ship;
import ogame.objects.game.planet.Planet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import utilities.webdriver.DriverController;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.*;

import static ogame.objects.game.Buildable.getResearch;

/**
 * Created by jarndt on 5/30/17.
 */
public class Fleet implements OgamePage{
    public static final String FLEET = "FleetInfo";

    @Override
    public String getPageName() {
        return FLEET;
    }
    @Override
    public String getXPathSelector() {
        return "//*[@id='menuTable']/li[8]/a/span";
    }

    @Override
    public String getCssSelector() {
        return "#menuTable > li:nth-child(8) > a";
    }

    @Override
    public String uniqueCssSelector() {
        return getCssSelector()+".selected";
    }

    @Override
    public boolean isPageLoaded(DriverController driverController) {
        return driverController.getDriver().findElements(By.cssSelector("#button203")).size() > 0 ||
                driverController.getDriver().findElements(By.cssSelector("#warning > p > span")).size() > 0;
    }

    @Override
    public boolean waitForPageToLoad(DriverController driverController, TimeUnit timeUnit, long l) {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        boolean b = true;
        try {
            exec.submit(new Callable<Boolean>(){
                @Override public Boolean call() throws Exception {
                    return _waitForText(driverController);
                }
            }).get(l, timeUnit);
            exec.shutdown();
            exec.awaitTermination(l, timeUnit);
        } catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
            b = false;
        }finally{
            exec.shutdownNow();
        }
        return b;
    }private boolean _waitForText(DriverController driverController){
        while(!isPageLoaded(driverController))
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) { /* DO NOTHING */ }
        return true;
    }

    @Override
    public void parsePage(Bot b, Document document) {
        //TODO expedition, dueterium cost
        Elements v = document.select("div.buildingimg");
        Planet p = b.getCurrentPlanet();
        for(Element e : v) {
            try {
                String name = e.select("a > span > span > span").text().trim();
                if(name.isEmpty())
                    name = e.parent().attr("title").replaceAll("[0-9\\(\\)]","").trim();
                Buildable bb = Buildable.getBuildableByName(name);
                Integer level = Integer.parseInt(e.select("span.level").get(0).ownText().trim());
                bb.setCurrentLevel(level);
                bb.setRef(e.select("a").attr("ref").trim());
                p.addBuildable(bb);
            }catch (IndexOutOfBoundsException ioobe){/*DO NOTHING, building is currently being built*/}
        }

        String[] fleets = document.select("#slots > div:nth-child(1) > span").text().replaceAll("[A-Za-z: ]","").trim().split("/");
        int fleetSlots = Integer.parseInt(fleets[1]), used = Integer.parseInt(fleets[0]);
        b.getFleetInfo().setFleetsTotal(fleetSlots);
        b.getFleetInfo().setFleetsUsed(used);

        try {
            if(b.getCurrentPlanet().getSetting(SettingsManager.AUTO_BUILD_LARGE_CARGOS,b.getOgameUserId()).equalsIgnoreCase("true")){
                //if the database has the setting true for large cargos
                double lc = Math.ceil(b.getCurrentPlanet().getResources().getTotal() / 25000);
                int cargos = b.getCurrentPlanet().getShips().get(Ship.LARGE_CARGO);
                if(lc > cargos)
                    b.buildRequest(new BuildTask().setBuildable(
                            Buildable
                                    .getBuildableByName(Ship.LARGE_CARGO))
                            .setCountOrLevel((int) (lc-cargos))
                            .setBuildPriority(b.getCurrentPlanet().getQueueManager(b.getOgameUserId(),b.getResearch()).getMaxPriority())
                    );
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
