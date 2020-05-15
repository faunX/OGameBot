package ogame.pages;

import bot.Bot;
import ogame.objects.game.Buildable;
import ogame.objects.game.Resource;
import ogame.objects.game.planet.Planet;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import utilities.webdriver.DriverController;

import java.util.concurrent.TimeUnit;

/**
 * Created by jarndt on 5/8/17.
 */
public class Facilities implements OgamePage{
    public static final String ID = "ref";

    public static final String FACILITIES = "Facilities";

    public static final String
            ROBOTICS_FACTORY    = "Robotics Factory",
            RESEARCH_LAB        = "Research Lab",
            SHIPYARD            = "Shipyard",
            ALLIANCE_DEPOT      = "Alliance Depot",
            MISSILE_SILO        = "Missile Silo",
            NANITE_FACTORY      = "Nanite Factory",
            TERRAFORMER         = "Terraformer",
            SPACE_DOCK          = "Space Dock",
            LUNAR_BASE          = "Lunar Base",
            SENSOR_PHALANX      = "Sensor Phalanx",
            JUMP_GATE           = "Jump Gate";

    public static final String[] names = {ROBOTICS_FACTORY, RESEARCH_LAB, SHIPYARD, ALLIANCE_DEPOT, MISSILE_SILO, NANITE_FACTORY,
            TERRAFORMER,SPACE_DOCK, LUNAR_BASE, SENSOR_PHALANX, JUMP_GATE};

//    public static final Resource
//            ROBOTICS_FACTORY_COST   = new Resource(400,120,200),
//            RESEARCH_LAB_COST       = new Resource(200,400,200),
//            SHIPYARD_COST           = new Resource(400,200,100),
//            ALLIANCE_DEPOT_COST     = new Resource(200,400,200),
//            MISSILE_SILO_COST       = new Resource(20000,40000,0),
//            NANITE_FACTORY_COST     = new Resource(1000000,500000,100000),
//            TERRAFORMER_COST        = new Resource(0,50000,100000,1000),
//            SPACE_DOCK_COST         = new Resource(200,0,50,50),
//            LUNAR_BASE_COST         = new Resource(20000,40000,20000),
//            SENSOR_PHALANX_COST     = new Resource(20000,40000,20000),
//            JUMP_GATE_COST          = new Resource(2000000,4000000,2000000);


    @Override
    public String getPageName() {
        return FACILITIES;
    }

    @Override
    public String getXPathSelector() {
        return "//*[@id='menuTable']/li[3]/a/span";
    }

    @Override
    public String getCssSelector() {
        return "#menuTable > li:nth-child(3) > a";
    }

    @Override
    public String uniqueCssSelector() {
        return getCssSelector()+".selected";
    }

    @Override
    public boolean isPageLoaded(DriverController driverController) {
        return driverController.getDriver().findElements(By.cssSelector("#details44")).size() > 0;
    }

    @Override
    public boolean waitForPageToLoad(DriverController driverController, TimeUnit timeUnit, long l) {
        return driverController.waitForElement(By.cssSelector("#details44"),l,timeUnit);
    }


    @Override
    public void parsePage(Bot b, Document document) {
        PageController.parseGenericBuildings(document,b);
    }
}
