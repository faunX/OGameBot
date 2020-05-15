package ogame.pages;

import bot.Bot;
import ogame.objects.game.BuildTask;
import ogame.objects.game.Buildable;
import ogame.objects.game.planet.Planet;
import ogame.objects.game.planet.PlanetProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import utilities.Timer;
import utilities.webdriver.DriverController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by jarndt on 5/30/17.
 */
public class Overview implements OgamePage {
    public static final String OVERVIEW = "Overview";

    @Override
    public String getPageName() {
        return OVERVIEW;
    }

    @Override
    public String getXPathSelector() {
        return "//*[@id='menuTable']/li[1]/a/span";
    }

    @Override
    public String getCssSelector() {
        return "#menuTable > li:nth-child(1) > a";
    }

    @Override
    public String uniqueCssSelector() {
        return getCssSelector()+".selected";
    }

    @Override
    public boolean isPageLoaded(DriverController driverController) {
        String v = Jsoup.parse(driverController.getDriver().getPageSource()).select("#honorContentField").text().trim();
        try{
            Integer.parseInt(v);
            return true;
        }catch (Exception e){
            return false;
        }
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
    public void parsePage(Bot b, Document d) {
        List<String> v = Arrays.asList(
                "#overviewBottom > div:nth-child(1) > div.content > table",
                "#overviewBottom > div:nth-child(2) > div.content > table",
                "#overviewBottom > div:nth-child(3) > div.content > table"
        );
        Planet p = b.getCurrentPlanet();
        p.setCurrentShipyardBeingBuild(null);
        for (int i = 0; i < v.size(); i++) {
            Elements activeConstruction = d.select(v.get(i));
            boolean works = !(activeConstruction.select("tr").size() == 1 &&
                    (activeConstruction.select("a").attr("title").toLowerCase().contains("There is no".toLowerCase()) ||
                            activeConstruction.select("a").attr("title").toLowerCase().contains("There are no".toLowerCase())
                    ));
            if (works) {
                List<Element> ac = activeConstruction;
                if(i != 2)
                    ac = Arrays.asList(activeConstruction.get(0));
                else {
                    ac = new ArrayList<>(Arrays.asList(activeConstruction.get(0)));
                    if(activeConstruction.size()>1)
                        ac.addAll(activeConstruction.get(1).select("td"));
                }
                for(Element e : ac) {
                    BuildTask buildTask = new BuildTask();
                    String name = e.select("tr > th").text().trim();
                    if(name.isEmpty())
                        name = e.select("img").attr("alt");
                    buildTask.setBuildable(Buildable.getBuildableByName(name));

                    int level = 0;
                    long time = 0;
                    if (i != 2) {
                        level = Integer.parseInt(activeConstruction.select("span.level").text().replace("Level ", "").trim());
                        time = Bot.parseTime(activeConstruction.select("td.desc.timer > span").text().trim());
                    } else {
                        String s = e.select("div.shipSumCount").text().trim();
                        if(!s.isEmpty())
                            level = Integer.parseInt(s);
                        else
                            level = Integer.parseInt(e.select("a").text().trim());
                        time = Bot.parseTime(activeConstruction.select("span.shipAllCountdown").text().trim());
                    }

                    buildTask.setCountOrLevel(level);

                    buildTask.setCompleteTime(LocalDateTime.now().plusSeconds(time));

                    if (i == 1) {
                        b.setCurrentResearchBeingBuilt(buildTask);
                        b.getResearch().put(name,level);
                    } else if (i == 0) {
                        p.setCurrentBuildingBeingBuild(buildTask);
                        p.addBuildable(buildTask.getBuildable());
                    } else if (i == 2) {
                        p.getCurrentShipyardBeingBuild().add(buildTask);
                        p.addBuildable(buildTask.getBuildable());
                    }
                }
            }else{
                if(i == 0)
                    p.setCurrentBuildingBeingBuild(null);
                else if(i == 1)
                    b.setCurrentResearchBeingBuilt(null);
                else if(i == 2)
                    p.setCurrentShipyardBeingBuild(null);
            }
        }
        try {
            String rank = d.select("#scoreContentField > a").text().trim();
            int points = Integer.parseInt(rank.replaceAll(" \\(.*\\)", ""));
            String[] split = rank.replaceAll(".*\\(Place ", "")
                    .replace(".", "")
                    .replace(")", "")
                    .split(" of ");
            int ownRank = Integer.parseInt(split[0].trim()),
                    totalRanks = Integer.parseInt(split[1]);
            b.setRank(ownRank);
            b.setTotalRanks(totalRanks);
            b.setPoints(points);
        }catch (Exception e){/*DO NOTHING*/}
        try {
            int honorPoints = Integer.parseInt(d.select("#honorContentField").text().trim());
            b.setHonorPoints(honorPoints);
        }catch (Exception e){/*DO NOTHING*/}
    }
}
