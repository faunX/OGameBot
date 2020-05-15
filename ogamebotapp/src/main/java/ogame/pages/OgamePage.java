package ogame.pages;

import bot.Bot;
import org.jsoup.nodes.Document;
import utilities.webdriver.DriverController;

import java.util.concurrent.TimeUnit;

/**
 * Created by jarndt on 5/30/17.
 */
public interface OgamePage {
    public String getPageName();
    public String getXPathSelector();
    public String getCssSelector();
    public String uniqueCssSelector();
    public boolean isPageLoaded(DriverController driverController);
    public boolean waitForPageToLoad(DriverController driverController,TimeUnit timeUnit, long l);
    public void parsePage(Bot b, Document document);
}
