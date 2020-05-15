package ogame.pages;

import bot.Bot;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import utilities.webdriver.DriverController;

import java.util.concurrent.TimeUnit;

/**
 * Created by jarndt on 5/30/17.
 */
public class Shop implements OgamePage{
    public static final String SHOP = "Shop";

    @Override
    public String getPageName() {
        return SHOP;
    }
    @Override
    public String getXPathSelector() {
        return "//*[@id='menuTable']/li[12]/a/span";
    }

    @Override
    public String getCssSelector() {
        return "#menuTable > li:nth-child(12) > a";
    }
    @Override
    public String uniqueCssSelector() {
        return getCssSelector()+".selected";
    }

    @Override
    public boolean isPageLoaded(DriverController driverController) {
        return driverController.getDriver().findElements(By.cssSelector("#itemBox > div.aside > div:nth-child(2) > a")).size() > 0;
    }

    @Override
    public boolean waitForPageToLoad(DriverController driverController, TimeUnit timeUnit, long l) {
        return driverController.waitForElement(By.cssSelector("#itemBox > div.aside > div:nth-child(2) > a"),l,timeUnit);
    }

    @Override
    public void parsePage(Bot b, Document document) {
        //TODO
    }
}
