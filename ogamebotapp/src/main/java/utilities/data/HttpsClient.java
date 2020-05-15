package utilities.data;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.openqa.selenium.Cookie;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Created by jarndt on 6/6/17.
 */
public class HttpsClient {
    /*
    settings
    https://s135-en.ogame.gameforge.com/game/index.php?page=preferences&mode=save&selectedTab=0&token=a779327bba9096708786685e0d47ffd1&db_character=&db_character_password=&db_password=&newpass1=&newpass2=&db_email=&db_email_confirm=&db_email_password=&spio_anz=2&activateAutofocus=on&eventsShow=1&settings_sort=0&settings_order=0&showDetailOverlay=on&animatedSliders=on&animatedOverview=on&msgResultsPerPage=10&auctioneerNotifications=on&showActivityMinutes=1


    POST
    Host: s135-en.ogame.gameforge.com
    Connection: keep-alive
    Content-Length: 387
    Pragma: no-cache
    Cache-Control: no-cache
    Origin: https://s135-en.ogame.gameforge.com
    Upgrade-Insecure-Requests: 1
    User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36
    Content-Type: application/x-www-form-urlencoded
    Referer: https://s135-en.ogame.gameforge.com/game/index.php?page=preferences
    Accept-Encoding: gzip, deflate, br
    Accept-Language: en-US,en;q=0.8
    Cookie: tabBoxFleets=%7B%2215456842%22%3A%5B1%2C%221496876662%22%5D%7D; maximizeId=null; _ga=GA1.2.11480322.1494359309; __auc=27d3b96315c55f5e10b8293d19e; PHPSESSID=4236e65a3ce6e4f961a1d8ce0f1b7f2a9b98a5d9; login_116678=U_en135%3Aunsupo%3A980b0959875bb3ecbaedab53202437f9; pc_idt=ADYNWsRfbrvbYQM7vcKOLxDWRoRCO1nrKrjEMGtKk1pzcEzMOJkkws4AYKzWKPS7y9LIBp-vHiq3wB4Udujmwu0TVAPCJzxi-wP1tBPSlZ2C1KSxhcEZi2B8uVRh9nUagmSUlswn_VJNSEaMXXi-Qx0q3mTnHMUAtD-uMg; prsess_116678=3290155968aab9a2ceac3a86d11430e9

    mode:save
    selectedTab:0
    token:a779327bba9096708786685e0d47ffd1
    db_character:
    db_character_password:
    db_password:
    newpass1:
    newpass2:
    db_email:
    db_email_confirm:
    db_email_password:
    spio_anz:2
    activateAutofocus:on
    eventsShow:1
    settings_sort:0
    settings_order:0
    showDetailOverlay:on
    animatedSliders:on
    animatedOverview:on
    msgResultsPerPage:10
    auctioneerNotifications:on
    showActivityMinutes:1
     */

    /*
    Galaxy info
    https://s135-en.ogame.gameforge.com/game/index.php?page=galaxyContent&ajax=1

    POST

    PARAMETERS

    galaxy:8
    system:308

    HEADERS

    Host: s135-en.ogame.gameforge.com
    Connection: keep-alive
    Content-Length: 19
    Pragma: no-cache
    Cache-Control: no-cache
    Origin: https://s135-en.ogame.gameforge.com
    User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36
    Content-Type: application/x-www-form-urlencoded; charset=UTF-8
    X-Requested-With: XMLHttpRequest
    Referer: https://s135-en.ogame.gameforge.com/game/index.php?page=galaxy
    Accept-Encoding: gzip, deflate, br
    Accept-Language: en-US,en;q=0.8
    Cookie: tabBoxFleets=%7B%2215456842%22%3A%5B1%2C%221496876662%22%5D%7D; maximizeId=null; _ga=GA1.2.11480322.1494359309; __auc=27d3b96315c55f5e10b8293d19e; pc_idt=ADYNWsRfbrvbYQM7vcKOLxDWRoRCO1nrKrjEMGtKk1pzcEzMOJkkws4AYKzWKPS7y9LIBp-vHiq3wB4Udujmwu0TVAPCJzxi-wP1tBPSlZ2C1KSxhcEZi2B8uVRh9nUagmSUlswn_VJNSEaMXXi-Qx0q3mTnHMUAtD-uMg; PHPSESSID=222ec1907d80a9629f8d0ae0171251a3a57fdc52; login_116678=U_en135%3Aunsupo%3A980b0959875bb3ecbaedab53202437f9; prsess_116678=c1d95af921beabecfe2253bf56a66fe7
     */

    //TODO send fleet
    /*
    Event List
    https://s135-en.ogame.gameforge.com/game/index.php?page=eventList&ajax=1

    GET

    Accept-Encoding:gzip, deflate, sdch, br
    Accept-Language:en-US,en;q=0.8
    Cache-Control:no-cache
    Connection:keep-alive
    Cookie:tabBoxFleets=%7B%2215456842%22%3A%5B1%2C%221496876662%22%5D%7D; maximizeId=null; _ga=GA1.2.11480322.1494359309; __auc=27d3b96315c55f5e10b8293d19e; pc_idt=ADYNWsRfbrvbYQM7vcKOLxDWRoRCO1nrKrjEMGtKk1pzcEzMOJkkws4AYKzWKPS7y9LIBp-vHiq3wB4Udujmwu0TVAPCJzxi-wP1tBPSlZ2C1KSxhcEZi2B8uVRh9nUagmSUlswn_VJNSEaMXXi-Qx0q3mTnHMUAtD-uMg; PHPSESSID=fec97b4739d75dbc0a88e98811acb1e738bd7892; login_116678=U_en135%3Aunsupo%3A980b0959875bb3ecbaedab53202437f9; prsess_116678=fb594d0edacdecc8145599bf4cc7de70
    Host:s135-en.ogame.gameforge.com
    Pragma:no-cache
    Referer:https://s135-en.ogame.gameforge.com/game/index.php?page=overview
    User-Agent:Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36
    X-Requested-With:XMLHttpRequest
     */

    //javascript: reloadPage() //relead fleet movement
    /*
    recall fleet
    https://s135-en.ogame.gameforge.com/game/index.php?page=movement&return=15456842
     */
    /*
    HOST: https://s135-en.ogame.gameforge.com/game/index.php?page=fleet2&galaxy=8&system=307&position=8&type=1&mission=0&speed=10&am204=&am210=1
    ending query string parameters (1 probe):
    galaxy:8
    system:307
    position:8
    type:1       //1 is planet,3 is moon, i assume debris field is 2
    mission:0    //1 is attack
    speed:10     //1 through 10 int
    am204: //light fighter count
    am210:1 //probe count

    METHOD: post

    HEADERS
    Host: s135-en.ogame.gameforge.com
    Connection: keep-alive
    Content-Length: 71
    Pragma: no-cache
    Cache-Control: no-cache
    Origin: https://s135-en.ogame.gameforge.com
    Upgrade-Insecure-Requests: 1
    User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36
    Content-Type: application/x-www-form-urlencoded
    Referer: https://s135-en.ogame.gameforge.com/game/index.php?page=fleet1
    Accept-Encoding: gzip, deflate, br
    Accept-Language: en-US,en;q=0.8
    Cookie: maximizeId=null; _ga=GA1.2.11480322.1494359309; __auc=27d3b96315c55f5e10b8293d19e; pc_idt=ADYNWsRfbrvbYQM7vcKOLxDWRoRCO1nrKrjEMGtKk1pzcEzMOJkkws4AYKzWKPS7y9LIBp-vHiq3wB4Udujmwu0TVAPCJzxi-wP1tBPSlZ2C1KSxhcEZi2B8uVRh9nUagmSUlswn_VJNSEaMXXi-Qx0q3mTnHMUAtD-uMg; PHPSESSID=fec97b4739d75dbc0a88e98811acb1e738bd7892; login_116678=U_en135%3Aunsupo%3A980b0959875bb3ecbaedab53202437f9; prsess_116678=506510c03a518e7d1c04c52220bf798d

    FLEET PAGE 3
    https://s135-en.ogame.gameforge.com/game/index.php?page=fleet3&type=1&mission=0&union=0&am204=8&am210=1&galaxy=8&system=307&position=11&acsValues=-&speed=10

    post

    parameters parsed
    type:1
    mission:0
    union:0
    am204:8
    am210:1
    galaxy:8
    system:307
    position:11
    acsValues:-
    speed:10

    HEADERS:
    Accept:text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,
    Content-Length:93
    Content-Type:application/x-www-form-urlencoded
    Cookie:maximizeId=null; _ga=GA1.2.11480322.1494359309; __auc=27d3b96315c55f5e10b8293d19e; pc_idt=ADYNWsRfbrvbYQM7vcKOLxDWRoRCO1nrKrjEMGtKk1pzcEzMOJkkws4AYKzWKPS7y9LIBp-vHiq3wB4Udujmwu0TVAPCJzxi-wP1tBPSlZ2C1KSxhcEZi2B8uVRh9nUagmSUlswn_VJNSEaMXXi-Qx0q3mTnHMUAtD-uMg; PHPSESSID=fec97b4739d75dbc0a88e98811acb1e738bd7892; login_116678=U_en135%3Aunsupo%3A980b0959875bb3ecbaedab53202437f9; prsess_116678=21783503075b9f47b3bcb6016c9d624f
    Host:s135-en.ogame.gameforge.com
    Origin:https://s135-en.ogame.gameforge.com
    Pragma:no-cache
    Referer:https://s135-en.ogame.gameforge.com/game/index.php?page=fleet2
    Upgrade-Insecure-Requests:1
    User-Agent:Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36

    MOVEMENT
    https://s135-en.ogame.gameforge.com/game/index.php?page=movement&holdingtime=1&token=f0a8c3a8e28b385cc27d691ed088723c&galaxy=8&system=307&position=11&type=1&mission=1&union2=0&holdingOrExpTime=0&speed=10&acsValues=-&prioMetal=1&prioCrystal=2&prioDeuterium=3&am204=8&am210=1&metal=0&crystal=0&deuterium=0

    POST

    PARSED:
    holdingtime:1
    token:f0a8c3a8e28b385cc27d691ed088723c
    galaxy:8
    system:307
    position:11
    type:1
    mission:1
    union2:0
    holdingOrExpTime:0
    speed:10
    acsValues:-
    prioMetal:1
    prioCrystal:2
    prioDeuterium:3
    am204:8
    am210:1
    metal:0
    crystal:0
    deuterium:0

    Host: s135-en.ogame.gameforge.com
    Content-Length: 238
    Pragma: no-cache
    Origin: https://s135-en.ogame.gameforge.com
    Upgrade-Insecure-Requests: 1
    User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36
    Content-Type: application/x-www-form-urlencoded
    Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*
    Referer: https://s135-en.ogame.gameforge.com/game/index.php?page=fleet3
    Cookie: maximizeId=null; _ga=GA1.2.11480322.1494359309; __auc=27d3b96315c55f5e10b8293d19e; pc_idt=ADYNWsRfbrvbYQM7vcKOLxDWRoRCO1nrKrjEMGtKk1pzcEzMOJkkws4AYKzWKPS7y9LIBp-vHiq3wB4Udujmwu0TVAPCJzxi-wP1tBPSlZ2C1KSxhcEZi2B8uVRh9nUagmSUlswn_VJNSEaMXXi-Qx0q3mTnHMUAtD-uMg; PHPSESSID=fec97b4739d75dbc0a88e98811acb1e738bd7892; login_116678=U_en135%3Aunsupo%3A980b0959875bb3ecbaedab53202437f9; prsess_116678=21783503075b9f47b3bcb6016c9d624f
    */


    public static final String GET = "GET", POST = "POST";

    public HttpsClient(){}

    private String host, requestMethod = GET, cookies;
    private List<NameValuePair> requestProperties = new ArrayList<>();
    private HttpURLConnection connection;


    private void setGenericRequestProperties(){
        connection.setRequestProperty("Pragma","no-cache");
        connection.setRequestProperty("Accept-Encoding","gzip, deflate, br");
        connection.setRequestProperty("Cache-Control","no-cache");
        connection.setRequestProperty("Accept-Language","en-US,en;q=0.8");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
        if(cookies != null && !cookies.isEmpty())
            connection.setRequestProperty("Cookie",cookies);

    }private void setGetReqeustProperties(){

    }private void setPostReqeustProperties(){

    }

    public String execute() throws IOException {
        connection = (HttpsURLConnection) (new URL(host).openConnection());
        setGenericRequestProperties();
        if(host == null)
            throw new IllegalArgumentException("Host must not be null");
        if(requestMethod.equals(POST))
            setPostReqeustProperties();
        else
            setGetReqeustProperties();
        requestProperties.forEach(a->connection.setRequestProperty(a.getName(),a.getValue()));

        connection.connect();

        return readConnection();
    }

    public String getRequest() throws IOException {
        this.requestMethod = GET;
        return execute();
    }
    public String postRequest() throws IOException {
        this.requestMethod = POST;
        return execute();
    }
    public String deleteMessage(String server, String messageId, String cookieString) throws IOException, URISyntaxException {
        return deleteMessage(server,messageId,"1",cookieString);
    }
    public String deleteMessage(String server, String messageId, String pageNumber, String cookieString) throws IOException, URISyntaxException {
        this.host = "https://"+server+"/game/index.php?page=messages&messageId="+messageId+"&action=103&ajax="+pageNumber;
        return deleteMessage(host,cookieString);
    }
    public String getMessages(String server, int tabId, String cookieString) throws IOException{
        return getMessages(server,tabId,1,cookieString);
    }
    public String getMessages(String server, int tabId, int pageNumber, String cookieString) throws IOException {
        this.host = "https://"+server+"/game/index.php?page=messages&tab="+tabId+"&ajax=1";
        if(pageNumber!=1) {
            this.host = "https://" + server + "/game/index.php?page=messages&tab=" + tabId + "&pagination=" + pageNumber + "&ajax=1";
            setRequestProperties(
                    "Host:"+server+"\n" +
                    "Origin:https://"+server+"\n" +
                    "Pragma:no-cache\n" +
                    "Referer:https://"+server+"/game/index.php?page=messages\n" +
                    "X-Requested-With:XMLHttpRequest\n" +
                    "Accept:text/html, */*; q=0.01\n" +
                    "Content-Length:52\n" +
                    "Connection:keep-alive\n" +
                    "Content-Type:application/x-www-form-urlencoded; charset=UTF-8"
            );
        }
        return getMessages(host,cookieString);
    }
    public String getEventInfo(String server, String cookies) throws IOException {
        return this.setRequestMethod(HttpsClient.GET)
                .setHost("https://"+server+"/game/index.php?page=eventList&ajax=1")
                .setRequestProperties(
                        "Connection:keep-alive" +
                        "Host:"+server+"\n" +
                        "Pragma:no-cache\n" +
                        "Referer:https://"+server+"/game/index.php?page=overview\n" +
                        "User-Agent:Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36\n" +
                        "X-Requested-With:XMLHttpRequest"
                )
                .setCookies(cookies)
                .getRequest();
    }

    public String getMoreDetails(String server, int tabId, long messageId, String cookies) throws IOException {
        this.host = "https://"+server+"/game/index.php?page=messages&messageId="+messageId+"&tabid="+tabId+"&ajax=1";
        this.cookies = cookies;
        this.setRequestProperties(
                "Accept:*/*\n" +
                "Connection:keep-alive\n" +
                "Host:"+server+"\n" +
                "Referer:https://"+server+"/game/index.php?page=messages\n" +
                "X-Requested-With:XMLHttpRequest"
        );
        return getRequest();
    }

    public String renamePlanet(String server, String newName, String cookies) throws IOException {
        if(newName.length() < 2)
            throw  new IllegalArgumentException("Name must be 2 characters long or longer");
        return this.setRequestMethod(POST)
                .setHost("https://"+server+"/game/index.php?page=planetRename&newPlanetName="+newName)
                .setRequestProperties(
                        "Host: "+server+"\n" +
                                "Content-Length: 16\n" +
                                "Origin: https://"+server+"\n" +
                                "Content-Type: application/x-www-form-urlencoded; charset=UTF-8\n" +
                                "Accept: */*\n" +
                                "X-Requested-With: XMLHttpRequest\n" +
                                "Referer: https://"+server+"/game/index.php?page=overview\n" +
                                "Cookie: "+cookies
                ).getRequest();
    }

//    public
/*
Send fleet
https://s135-en.ogame.gameforge.com/game/index.php?page=minifleet&ajax=1&
 */


    public String getMessages(String host, String cookieString) throws IOException {
        this.host = host;
        this.cookies = cookieString;
        this.requestProperties.addAll(Arrays.asList(
                new BasicNameValuePair("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"),
                new BasicNameValuePair("Upgrade-Insecure-Requests", "1")
        ));

        return execute();
    }
    public String markMessagesAsRead(String domain, String newMessages, String cookies) throws IOException{
        return markMessagesAsRead(domain,newMessages,1,cookies);
    }
    public String markMessagesAsRead(String domain, String newMessages, int pageNumber, String cookies) throws IOException {
        String formData = "player=116678&action=111&newMessageIds=["+newMessages+"]&ajax="+pageNumber;
        return this.setRequestMethod(HttpsClient.GET)
                .setHost("https://s135-en.ogame.gameforge.com/game/index.php?page=ajaxMessageCount&"+formData)
                .setRequestProperties("Host: s135-en.ogame.gameforge.com\n" +
                        "Content-Length: 60\n" +
                        "Origin: https://"+domain+"\n" +
                        "Content-Type: application/x-www-form-urlencoded; charset=UTF-8\n" +
                        "Accept: */*\n" +
                        "X-Requested-With: XMLHttpRequest\n" +
                        "Referer: https://"+domain+"/game/index.php?page=messages\n" +
                        "Cookie: "+cookies)
                .getRequest();
    }


    public String deleteMessage(String host, String cookieString) throws IOException, URISyntaxException {
        this.host = host;
        String domain = new URI(host).getHost();
        this.setRequestMethod(POST);
        this.cookies = cookieString;
        setRequestProperties(
                "Host: "+domain+"\n" +
                "Content-Length: 36\n" +
                "Origin: https://s135-en.ogame.gameforge.com\n" +
                "Content-Type: application/x-www-form-urlencoded; charset=UTF-8\n" +
                "Accept: application/json, text/javascript, */*; q=0.01\n" +
                "X-Requested-With: XMLHttpRequest\n" +
                "Referer: https://"+domain+"/game/index.php?page=messages"
        );

        return execute();
    }

    public String readConnection() throws IOException {
        BufferedReader br;
        if (200 <= connection.getResponseCode() && connection.getResponseCode() <= 299) {
            if ("gzip".equals(connection.getContentEncoding()))
                br = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream())));
            else
                br = new BufferedReader(new InputStreamReader((connection.getInputStream())));
        } else {
            if ("gzip".equals(connection.getContentEncoding()))
                br = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getErrorStream())));
            else
                br = new BufferedReader(new InputStreamReader((connection.getErrorStream())));
        }
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        connection.disconnect();
        return sb.toString();
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public HttpsClient setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
        return this;
    }

    public String getHost() {
        return host;
    }

    public HttpsClient setHost(String host) {
        this.host = host;
        return this;
    }

    public String getCookies() {
        return cookies;
    }

    public HttpsClient setCookies(String cookies) {
        this.cookies = cookies;
        return this;
    }

    public HttpsClient setCookies(Set<Cookie> cookies) {
        StringBuilder builder = new StringBuilder("");
        for(Cookie c : cookies)
            builder.append(c.getName() + "=" + c.getValue() + "; ");
        this.cookies = builder.toString();
        return this;
    }

    public List<NameValuePair> getRequestProperties() {
        return requestProperties;
    }

    public HttpsClient setRequestProperties(List<NameValuePair> requestProperties) {
        this.requestProperties = requestProperties;
        return this;
    }

    public HttpsClient setRequestProperties(String headers){
        this.requestProperties.addAll(Arrays.asList(headers.split("\n")).stream().map(a->{
            String[] split = a.split(":");
            return new BasicNameValuePair(split[0],split[1]);
        }).collect(Collectors.toList()));
        return this;
    }
//    public HttpsClient addRequestProperties(String headers){
//
//    }
}
