package ogame.pages;

import bot.Bot;
import ogame.objects.game.Buildable;
import ogame.objects.game.planet.Planet;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import utilities.webdriver.DriverController;

import java.util.concurrent.TimeUnit;

/**
 * Created by jarndt on 5/30/17.
 */
public class Defense implements OgamePage{
    public static final String DEFENSE = "Defense";

    @Override
    public String getPageName() {
        return DEFENSE;
    }
    @Override
    public String getXPathSelector() {
        return "//*[@id='menuTable']/li[7]/a/span";
    }

    @Override
    public String getCssSelector() {
        return "#menuTable > li:nth-child(7) > a";
    }
    @Override
    public String uniqueCssSelector() {
        return getCssSelector()+".selected";
    }

    @Override
    public boolean isPageLoaded(DriverController driverController) {
        return driverController.getDriver().findElements(By.cssSelector("#details405")).size()>0;
    }

    @Override
    public boolean waitForPageToLoad(DriverController driverController, TimeUnit timeUnit, long l) {
        return driverController.waitForElement(By.cssSelector("#details405"),l,timeUnit);
    }

    @Override
    public void parsePage(Bot b, Document document) {
        PageController.parseGenericBuildings(document,b,true);

        PageController.parseShips(b,document);
    }
}
