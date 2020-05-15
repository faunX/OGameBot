package ogame.pages;

import bot.Bot;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import utilities.webdriver.DriverController;

import java.util.concurrent.TimeUnit;

/**
 * Created by jarndt on 5/30/17.
 */
public class RecruitOfficers implements OgamePage{
    public static final String RECRUIT_OFFICERS = "Recruit Officers";

    @Override
    public String getPageName() {
        return RECRUIT_OFFICERS;
    }
    @Override
    public String getXPathSelector() {
        return "//*[@id='menuTable']/li[11]/a/span";
    }

    @Override
    public String getCssSelector() {
        return "#menuTable > li:nth-child(11) > a";
    }
    @Override
    public String uniqueCssSelector() {
        return getCssSelector()+".selected";
    }

    @Override
    public boolean isPageLoaded(DriverController driverController) {
        return driverController.getDriver().findElements(By.cssSelector("#button5")).size() > 0;
    }

    @Override
    public boolean waitForPageToLoad(DriverController driverController, TimeUnit timeUnit, long l) {
        return driverController.waitForElement(By.cssSelector("#button5"),l,timeUnit);
    }

    @Override
    public void parsePage(Bot b, Document document) {
        //TODO
    }
}
