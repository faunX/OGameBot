package ogame.pages;

import bot.Bot;
import ogame.objects.game.Buildable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import utilities.webdriver.DriverController;

import java.util.concurrent.TimeUnit;

/**
 * Created by jarndt on 5/8/17.
 */
public class Research implements OgamePage{
    public static final String RESEARCH = "Research";

    public static final String ID = "id";

    public static final String
            ENERGY              = "Energy Technology",
            LASER               = "Laser Technology",
            ION                 = "Ion Technology",
            HYPERSPACE_TECH     = "Hyperspace Technology",
            PLASMA              = "Plasma Technology",
            COMBUSTION          = "Combustion Drive",
            IMPULSE             = "Impulse Drive",
            HYPERSPACE_DRIVE    = "Hyperspace Drive",
            ESPIONAGE           = "Espionage Technology",
            COMPUTER            = "Computer Technology",
            ASTROPHYSICS        = "Astrophysics",
            INTERGALACTIC       = "Intergalactic Research Network",
            GRAVITON            = "Graviton Technology",
            WEAPONS             = "Weapons Technology",
            SHIELDING           = "Shielding Technology",
            ARMOUR              = "Armour Technology";


    public static final String[] names = {
            ENERGY, LASER, ION, HYPERSPACE_TECH, PLASMA, COMBUSTION, IMPULSE,
            HYPERSPACE_DRIVE, ESPIONAGE, COMPUTER, ASTROPHYSICS, INTERGALACTIC,
            GRAVITON, WEAPONS, SHIELDING, ARMOUR
    };

    @Override
    public String getPageName() {
        return RESEARCH;
    }
    @Override
    public String getXPathSelector() {
        return "//*[@id='menuTable']/li[5]/a/span";
    }

    @Override
    public String getCssSelector() {
        return "#menuTable > li:nth-child(5) > a";
    }

    @Override
    public String uniqueCssSelector() {
        return getCssSelector()+".selected";
    }

    @Override
    public boolean isPageLoaded(DriverController driverController) {
        return driverController.getDriver().findElements(By.cssSelector("#details122")).size() > 0;
    }

    @Override
    public boolean waitForPageToLoad(DriverController driverController, TimeUnit timeUnit, long l) {
        return driverController.waitForElement(By.cssSelector("#details122"),l,timeUnit);
    }

    @Override
    public void parsePage(Bot b, Document document) {
        PageController.parseGenericBuildings(document,b);

        //TODO Currently researching research
    }

}
