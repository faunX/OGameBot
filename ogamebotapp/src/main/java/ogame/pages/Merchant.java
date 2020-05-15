package ogame.pages;

import bot.Bot;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import utilities.webdriver.DriverController;

import java.util.concurrent.TimeUnit;

/**
 * Created by jarndt on 5/30/17.
 */
public class Merchant implements OgamePage{
    public static final String MERCHANT = "Merchant";

    @Override
    public String getPageName() {
        return MERCHANT;
    }

    @Override
    public String getXPathSelector() {
        return "//*[@id='menuTable']/li[4]/a/span";
    }

    @Override
    public String getCssSelector() {
        return "#menuTable > li:nth-child(4) > a";
    }

    @Override
    public String uniqueCssSelector() {
        return getCssSelector()+".selected";
    }

    @Override
    public boolean isPageLoaded(DriverController driverController) {
        return driverController.getDriver().findElements(By.cssSelector("#js_traderImportExport")).size() > 0;
    }

    @Override
    public boolean waitForPageToLoad(DriverController driverController, TimeUnit timeUnit, long l) {
        return driverController.waitForElement(By.cssSelector("#js_traderImportExport"),l,timeUnit);
    }

    @Override
    public void parsePage(Bot b, Document document) {
        //NOTHING TO PARSE
    }
}
