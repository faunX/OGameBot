package application;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import utilities.password.PasswordEncryptDecrypt;
import utilities.database.Database;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Created by jarndt on 4/13/17.
 */
@RestController
public class ApplicationController {
//    public static void main(String[] args) {
//        DriverController driverController = new DriverController();
//        driverController.setDriverName(DriverController.PHANTOMJS);
//        driverController.getDriver().navigate().to("http://google.com");
//        byte[] bytesArr = ((TakesScreenshot) driverController.getDriver()).getScreenshotAs(OutputType.BYTES);
//        System.out.println();
//    }

//    DriverController driverController = new DriverController();
//    List<String> webpages = Arrays.asList(
//            "http://google.com",
//            "http://stackoverflow.com",
//            "https://www.w3schools.com/angular/ng_ng-src.asp"
//    );
//    public ApplicationController(){
//        driverController.setDriverName(DriverController.PHANTOMJS);
//        new Thread(()->{
//            int i = 0;
//            Thread t;
//            while (true) {
//                driverController.getDriver().navigate().to(webpages.get((i++) % webpages.size()));
////                t = new Thread(()->{
////                    driverController.getDriver().navigate().to(webpages.get((i++) % webpages.size()));
////                }).start();
//                try {
//                    Thread.sleep(5000);
////                    t.interrupt();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }

    @RequestMapping("/greeting")
    public String greeting(@RequestParam(value="name",required = false, defaultValue = "World") String name){
        return "greeting";
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String register(@RequestHeader(value = "username") String username,
                           @RequestHeader(value = "password") String password,
                           @RequestHeader(value = "firstName") String firstName,
                           @RequestHeader(value = "lastName") String lastName){
        try {
            Database d = new Database(Database.DATABASE, Database.USERNAME, Database.PASSWORD);
            List<Map<String, Object>> results = d
                    .executeQuery("select * from USERS where username = '" + username +"';");
            if(results.size() != 0)
                return "Username Exists";
            else
                d.executeQuery("insert into USERS(username,password,first_name,last_name) " +
                        "values('"+username+"','"+PasswordEncryptDecrypt.encrypt(password)+"','"+firstName+"','"+lastName+"');"
                );
            return "User created";
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String login(@RequestHeader(value = "username") String username,
                        @RequestHeader(value = "password") String password) throws Exception {
        String token = null;
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            // authenticate against IDM
            try {
                token = validateUser(username,password);
            } catch (SQLException | IOException | ClassNotFoundException | GeneralSecurityException e) {
                e.printStackTrace();
//                return e.getMessages();
            }
            if (token != null)
                return token;
        }
        throw new Exception("Invalid Username and Password");
    }

    private boolean validateToken(String token) throws SQLException, IOException, ClassNotFoundException {
        List<Map<String, Object>> results = new Database(Database.DATABASE, Database.USERNAME, Database.PASSWORD)
                .executeQuery("select * from TOKENS where token = '"+token+"';");
        if(results.size() != 1) return false;
        Object timestamp = results.get(0).get("TIMESTAMP".toLowerCase()),
                expireTimestamp = results.get(0).get("EXPIRE_TIMESTAMP".toLowerCase());
        Timestamp t= null, et = null;
        if(timestamp instanceof Timestamp)
            t = (Timestamp) timestamp;
        if(expireTimestamp instanceof Timestamp)
            et = (Timestamp) expireTimestamp;
        if(t == null || et == null) return false;
        return et.after(t);
    }

    private String validateUser(String username, String password) throws SQLException, IOException, ClassNotFoundException, GeneralSecurityException {
        Database d = new Database(Database.DATABASE, Database.USERNAME, Database.PASSWORD);
        List<Map<String, Object>> results = d
                .executeQuery("select * from USERS where username = '" + username + "' and password = '"+ PasswordEncryptDecrypt.encrypt(password)+"'");
        String result = null;
        if(results.size() == 1 && results.get(0).get("ACTIVE".toLowerCase()).equals("A")) {
            List<Map<String, Object>> v = d.executeQuery("select * from TOKENS where users_id = '" + results.get(0).get("id")+"'");
            result = UUID.randomUUID().toString();
            if(v.size() == 0)
                d.executeQuery("insert into TOKENS(USERS_ID,TOKEN) values('" + results.get(0).get("ID".toLowerCase())+"','"+result+"');");
            else
                d.executeQuery("update tokens set TOKEN = '"+result+"' where users_id = '"+results.get(0).get("id")+"'");
        }
        return result;
    }


//    @ResponseBody
//    @RequestMapping(value = "/getImage", method = RequestMethod.GET)//, produces = MediaType.IMAGE_PNG_VALUE)
//    public String getScreenShot(/*@RequestParam(value = "driverName") String driverName*/) throws IOException {
//        byte[] bytesArr = ((TakesScreenshot) driverController.getDriver()).getScreenshotAs(OutputType.BYTES);
//        String b64 = Base64.encodeBase64String(bytesArr);
//
////        URL url = new URL(driverController.getDriver().getCurrentUrl());
////        String baseUrl = url.getProtocol() + "://" + url.getHost();
////        String s = driverController.getDriver().getPageSource().replaceAll("src=\"\\/","src=\""+baseUrl+"/");
//        return b64;
//    }
}
