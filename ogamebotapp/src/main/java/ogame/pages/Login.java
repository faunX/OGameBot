package ogame.pages;

import bot.Bot;
import com.google.gson.Gson;
import ogame.objects.User;
import ogame.objects.game.data.Server;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import utilities.database.Database;
import utilities.email.OneEmail;
import utilities.webdriver.DriverController;
import utilities.webdriver.DriverControllerBuilder;
import utilities.webdriver.JavaScriptFunctions;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by jarndt on 5/13/17.
 */
public class Login implements OgamePage{
    public static void main(String[] args) throws Exception {
//        System.out.println(new URI("https://en.ogame.gameforge.com/").getHost());

        System.out.println(new Gson().toJson(new Login(new User("bc3ew9p4yh9qdv8wvj1h",Server.QUANTUM))));

    }

    public static final String HOMEPAGE = "Homepage";
    public static final String OGAME_HOMEPAGE = "https://en.ogame.gameforge.com/";
    User user;
    boolean isLoggedIn = false;
    Server server;

    private transient DriverController driverController;

    public Login(){}

    public Login(User user) throws SQLException, IOException, ClassNotFoundException {
        this.user = user;
        init();
    }
    public Login(User user, DriverController controller) throws SQLException, IOException, ClassNotFoundException {
        this.user = user;
        this.driverController = controller;
        init();
    }

    private void init() throws SQLException, IOException, ClassNotFoundException {
        this.server = new Server(user.getUniverse());
    }

    public void setDriverController(DriverController controller){
        this.driverController = controller;
    }

    public DriverController getDriverController(){
        if(driverController == null)
            driverController = new DriverControllerBuilder().build();
        return driverController;
    }

    public boolean login() throws SQLException, IOException, ClassNotFoundException, URISyntaxException {
        //first check if already logged in based on current page
        if(getDriverController().elementExists(By.xpath("//*[@id='loginBtn']")))
            isLoggedIn = false;
        //check database for user, if the user doesn't exist, or if the user isn't created then register the user
        if(!(user != null && user.isCreated())) {
            boolean registered = register();
            if(registered) {
                isLoggedIn = true;
                user.setCreated(true);
            }
        }

        //after registration then play the game.
        if(!getDriverController().elementExists(By.xpath("//*[@id='playerName']")))
            isLoggedIn = false;

        //then log in if not at correct page.
        if(!isLoggedIn)
            _login();

        //verify the account if not verified
        if(!user.isVerified())
            verifyAccount();

        return isLoggedIn;
    }

    private void _login() throws URISyntaxException, SQLException, IOException, ClassNotFoundException {
        String url = getDriverController().getDriver().getCurrentUrl();
        if(!url.contains("en.ogame.gameforge.com"))
            try{            getDriverController().getDriver().navigate().to(OGAME_HOMEPAGE); }
            catch (Exception e){/*DO NOTHING*/}

        Elements ad = Jsoup.parse(getDriverController().getDriver().getPageSource()).select("div.openX_int_closeButton");
        if(ad != null && ad.size() > 0)
            getDriverController().executeJavaScript(ad.get(0).select("a").get(0).attr("onclick"));

        //if login button is minimized click on it.
        if(Jsoup.parse(getDriverController().getDriver().getPageSource()).select("#loginBtn").text().equals("Login"))
            try {
                getDriverController().clickWait(By.xpath("//*[@id='loginBtn']"), 1L, TimeUnit.MINUTES);
            }catch (Exception e){/*DO NOTHING*/}

        getDriverController().waitForElement(By.xpath("//*[@id='loginSubmit']"),1L,TimeUnit.MINUTES);
        JavaScriptFunctions.fillFormByXpath(getDriverController(), "//*[@id='usernameLogin']", user.getUsername());
        JavaScriptFunctions.fillFormByXpath(getDriverController(), "//*[@id='passwordLogin']", user.getPassword());
        JavaScriptFunctions.fillFormByXpath(getDriverController(), "//*[@id='serverLogin']", server.getDomain());

        try {
            getDriverController().clickWait(By.xpath("//*[@id='loginSubmit']"), 1L, TimeUnit.MINUTES);
        }catch (Exception e){/*DO NOTHING*/}
        isLoggedIn = true;
        driverController.waitForElement(By.xpath("//*[@id='playerName']"),1L, TimeUnit.MINUTES);
    }

    public boolean register() throws SQLException, IOException, ClassNotFoundException {
        List<Map<String, Object>> v = Database.getExistingDatabaseConnection().executeQuery(
                "select * from player where server_id = " + server.getServerID() + " and name = '" + user.getUsername() + "';"
        );
        if(v != null && v.size() > 0 && v.get(0) != null && v.get(0).size() > 0)
            return true;

        getDriverController().getDriver().navigate().to(OGAME_HOMEPAGE);
        String usernameXPath = "//*[@id='username']";
        getDriverController().waitForElement(new By.ByXPath(usernameXPath),1L, TimeUnit.MINUTES);
        JavaScriptFunctions.fillFormByXpath(getDriverController(), usernameXPath, user.getUsername());
        JavaScriptFunctions.fillFormByXpath(getDriverController(), "//*[@id='password']", user.getPassword());
        JavaScriptFunctions.fillFormByXpath(getDriverController(), "//*[@id='email']", user.getEmail().getEmailAddress());

        String serverSelectScript = Jsoup.parse(getDriverController().getDriver().getPageSource()).select("#server")
                .get(0).select("div").stream().filter(a->a.select("span").text().equals(user.getUniverse()))
                .collect(Collectors.toList()).get(0).attr("onclick");
        getDriverController().executeJavaScript(serverSelectScript);

        getDriverController().clickWait(By.xpath("//*[@id='regSubmit']"),1L,TimeUnit.MINUTES);
        boolean alreadyExists = getDriverController().waitForElement(By.xpath("//*[@id='subscribeForm']/div[1]/div/div/div[1]/div"),1L,TimeUnit.MINUTES);
        if(alreadyExists)
            return true;

        boolean b = getDriverController().waitForElement(By.xpath("//*[@id='menuTable']/li[1]/a"), 1L, TimeUnit.MINUTES);

        if(!verifyAccount())
            return false;

        return b;
    }

    private boolean verifyAccount() throws SQLException, IOException, ClassNotFoundException {
        if(getDriverController().waitForElement(By.xpath("//*[@id='playerName']"),2L,TimeUnit.MINUTES))
            if(getDriverController().getDriver().findElements(By.cssSelector("#advice-bar > a")).size() == 0) {
                getUser().setVerified(true);
                return true;
            }

        try {
            new OneEmail(driverController).verifyOneEmailAccount(user.getEmail().getEmailAddress(),user.getEmail().getPassword());
            user.setVerified(true);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Login login = (Login) o;

        if (isLoggedIn != login.isLoggedIn) return false;
        if (user != null ? !user.equals(login.user) : login.user != null) return false;
        if (server != null ? !server.equals(login.server) : login.server != null) return false;
        return driverController != null ? driverController.equals(login.driverController) : login.driverController == null;
    }

    @Override
    public int hashCode() {
        int result = user != null ? user.hashCode() : 0;
        result = 31 * result + (isLoggedIn ? 1 : 0);
        result = 31 * result + (server != null ? server.hashCode() : 0);
        result = 31 * result + (driverController != null ? driverController.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Login{" +
                "user=" + user +
                ", isLoggedIn=" + isLoggedIn +
                ", server=" + server +
                '}';
    }

    @Override
    public String getPageName() {
        return HOMEPAGE;
    }

    @Override
    public String getXPathSelector() {
        return "//*[@id=\"regSubmit\"]";
    }

    @Override
    public String getCssSelector() {
        return "#regSubmit";
    }

    @Override
    public String uniqueCssSelector() {
        return "#regSubmit";
    }

    @Override
    public boolean isPageLoaded(DriverController driverController) {
        return driverController.getDriver().findElements(By.cssSelector("#loginBtn")).size() > 0;
    }

    @Override
    public boolean waitForPageToLoad(DriverController driverController, TimeUnit timeUnit, long l) {
        return driverController.waitForElement(By.cssSelector("#loginBtn"),l,timeUnit);
    }

    @Override
    public void parsePage(Bot b, Document document) {

    }
}
