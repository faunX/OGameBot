package ogame.pages;

import bot.Bot;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import utilities.webdriver.DriverController;

import java.util.concurrent.*;

/**
 * Created by jarndt on 5/30/17.
 */
public class Galaxy implements OgamePage{
    public static final String GALAXY = "Galaxy";

    @Override
    public String getPageName() {
        return GALAXY;
    }
    @Override
    public String getXPathSelector() {
        return "//*[@id='menuTable']/li[9]/a/span";
    }

    @Override
    public String getCssSelector() {
        return "#menuTable > li:nth-child(9) > a";
    }

    @Override
    public String uniqueCssSelector() {
        return getCssSelector()+".selected";
    }

    @Override
    public boolean isPageLoaded(DriverController driverController) {
        String style = Jsoup.parse(driverController.getDriver().getPageSource()).select("#galaxyLoading").attr("style");
        return style.contains("display") && style.contains("none");
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
        //TODO
    }
}
