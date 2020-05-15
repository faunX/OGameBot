package ogame.objects.game.messages;

import bot.Bot;
import ogame.objects.game.Buildable;
import ogame.objects.game.Coordinates;
import ogame.objects.game.Resource;
import ogame.objects.game.Ship;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utilities.data.HttpsClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static ogame.objects.game.messages.MessageObject.FORMATTER;

/**
 * Created by jarndt on 6/8/17.
 */
public class EspionageMessage {
    private long messageId, loot;
    private int counterEspionagePercent, smallCargosNeeded, largeCargosNeeded, lootPercent, maxInfo = 0;
    private LocalDateTime messageDate;
    private String planetName, playerName, status, activity, api;
    private Coordinates coordinates;
    private boolean isHonorable;
    private Resource resources;
    private HashMap<String,Integer> levels = new HashMap<>(); //ships, defence, building, research
    private HashMap<String,Integer> activeRepair = new HashMap<>();

    public EspionageMessage(){}

    public EspionageMessage(MessageObject messageObject, String domain, String cookies) throws IOException {
        messageId = messageObject.getMessageId();
        String[] planetAndCoords = messageObject.getMessageTitle().replace("Espionage report from ","").split(" \\[");
        planetName = planetAndCoords[0];
        messageDate = messageObject.getMessageDate();
        coordinates = new Coordinates(planetAndCoords[1]);
        parseMessageContent(messageObject.getMessageContent());
        parseMoreDetails(
                Jsoup.parse(
                        new HttpsClient().getMoreDetails(
                                domain,
                                20,
                                messageId,
                                cookies
                        )
                )
        );
    }

    private void parseMoreDetails(Document moreDetails) throws IOException {
        resources.setEnergy(Long.parseLong(moreDetails.select("li.resource_list_el").get(3).attr("title").replace(".","")));
        List<Element> results = moreDetails.select("ul").subList(2, 6);
        for (int i = 0; i < results.size(); i++) {
            if(results.get(i).select("li").hasClass("detail_list_fail"))
                continue;
            if(results.get(i).text().isEmpty()) {
                Element type = results.get(i);
                String typeName = type.attr("data-type");
                if("ships".equals(typeName))
                    Buildable.getShipyard().forEach(a->levels.put(a.getName(),0));
                else if("defense".equals(typeName))
                    Buildable.getDefense().forEach(a->levels.put(a.getName(),0));
                else if("buildings".equals(typeName)) {
                    Buildable.getResources().forEach(a -> levels.put(a.getName(), 0));
                    Buildable.getFacilities().forEach(a -> levels.put(a.getName(), 0));
                }else if("research".equals(typeName))
                    Buildable.getResearch().forEach(a->levels.put(a.getName(),0));
                continue;
            }
            for(Element e : results.get(i).select("li.detail_list_el"))
                levels.put(e.select("span.detail_list_txt").text().trim(),Integer.parseInt(e.select("span").get(1).text().trim().replace(".","")));

            maxInfo = i+1;
        }
    }

    private void parseMessageContent(Elements messageContent) {
        parseTitleInfo(Jsoup.parse(messageContent.select("div:nth-child(4) > span.ctn.ctn4.fright.tooltipRight.tooltipClose").attr("title")));
        Elements lootAndCounterE = messageContent.select("div:nth-child(5) > span");
        counterEspionagePercent = Integer.parseInt(lootAndCounterE.get(1).text().replaceAll("[A-Za-z-:% ]",""));
        lootPercent = Integer.parseInt(lootAndCounterE.get(0).text().replaceAll("[A-Za-z-:% ]",""));
        Elements honorAndPlayer = messageContent.select("div:nth-child(3) > span");
        playerName = honorAndPlayer.get(1).ownText().trim();
        isHonorable = honorAndPlayer.get(1).hasClass("status_abbr_honorableTarget");
        status = honorAndPlayer.get(2).className();
        activity = messageContent.select("div:nth-child(3) > span.ctn.ctn4.fright").text();
        Elements ressies = messageContent.select("div:nth-child(4) > span:nth-child(1) > span");
        resources = new Resource();
        for(Element e : ressies)
            if(e.text().contains("Metal: "))
                resources.setMetal(Long.parseLong(e.text().replace("Mn","000").replaceAll("[A-Za-z:\\. ]","")));
            else if(e.text().contains("Crystal: "))
                resources.setCrystal(Long.parseLong(e.text().replace("Mn","000").replaceAll("[A-Za-z:\\. ]","")));
            else if(e.text().contains("Deuterium: "))
                resources.setDeuterium(Long.parseLong(e.text().replace("Mn","000").replaceAll("[A-Za-z:\\. ]","")));
    }

    private void parseTitleInfo(Document title) {
        long loot = Long.parseLong(title.select("body").get(0).ownText().replace("Loot: ","").replace(".","").replace("Mn","000"));
        for(Element e : title.select("a")) {
            String[] split = e.text().split(": ");
            if (split[0].equals("S.Cargo"))
                smallCargosNeeded = Integer.parseInt(split[1]);
            else if(split[0].equals("L.Cargo"))
                largeCargosNeeded = Integer.parseInt(split[1]);
        }
    }

    public boolean hasDefense() throws IOException {
        Set<Buildable> builds = Buildable.getDefense();
        builds.addAll(Buildable.getShipyard());
        builds.removeIf(a->a.getName().equals(Ship.SOLAR_SATELLITE));
        for(Buildable b : builds)
            if(levels.containsKey(b.getName()) && levels.get(b.getName()) > 0)
                return true;
        return false;
    }

    public int getMaxInfo() {
        return maxInfo;
    }

    public void setMaxInfo(int maxInfo) {
        this.maxInfo = maxInfo;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public int getCounterEspionagePercent() {
        return counterEspionagePercent;
    }

    public void setCounterEspionagePercent(int counterEspionagePercent) {
        this.counterEspionagePercent = counterEspionagePercent;
    }

    public LocalDateTime getMessageDate() {
        return messageDate;
    }

    public void setMessageDate(LocalDateTime messageDate) {
        this.messageDate = messageDate;
    }

    public String getPlanetName() {
        return planetName;
    }

    public void setPlanetName(String planetName) {
        this.planetName = planetName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public boolean isHonorable() {
        return isHonorable;
    }

    public void setHonorable(boolean honorable) {
        isHonorable = honorable;
    }

    public Resource getResources() {
        return resources;
    }

    public void setResources(Resource resources) {
        this.resources = resources;
    }

    public HashMap<String, Integer> getLevels() {
        return levels;
    }

    public void setLevels(HashMap<String, Integer> levels) {
        this.levels = levels;
    }

    public HashMap<String, Integer> getActiveRepair() {
        return activeRepair;
    }

    public void setActiveRepair(HashMap<String, Integer> activeRepair) {
        this.activeRepair = activeRepair;
    }

    public long getLoot() {
        return loot;
    }

    public void setLoot(long loot) {
        this.loot = loot;
    }

    public int getSmallCargosNeeded() {
        return smallCargosNeeded;
    }

    public void setSmallCargosNeeded(int smallCargosNeeded) {
        this.smallCargosNeeded = smallCargosNeeded;
    }

    public int getLargeCargosNeeded() {
        return largeCargosNeeded;
    }

    public void setLargeCargosNeeded(int largeCargosNeeded) {
        this.largeCargosNeeded = largeCargosNeeded;
    }

    public int getLootPercent() {
        return lootPercent;
    }

    public void setLootPercent(int lootPercent) {
        this.lootPercent = lootPercent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EspionageMessage that = (EspionageMessage) o;

        if (messageId != that.messageId) return false;
        if (loot != that.loot) return false;
        if (counterEspionagePercent != that.counterEspionagePercent) return false;
        if (smallCargosNeeded != that.smallCargosNeeded) return false;
        if (largeCargosNeeded != that.largeCargosNeeded) return false;
        if (lootPercent != that.lootPercent) return false;
        if (isHonorable != that.isHonorable) return false;
        if (messageDate != null ? !messageDate.equals(that.messageDate) : that.messageDate != null) return false;
        if (planetName != null ? !planetName.equals(that.planetName) : that.planetName != null) return false;
        if (playerName != null ? !playerName.equals(that.playerName) : that.playerName != null) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (activity != null ? !activity.equals(that.activity) : that.activity != null) return false;
        if (api != null ? !api.equals(that.api) : that.api != null) return false;
        if (coordinates != null ? !coordinates.equals(that.coordinates) : that.coordinates != null) return false;
        if (resources != null ? !resources.equals(that.resources) : that.resources != null) return false;
        if (levels != null ? !levels.equals(that.levels) : that.levels != null) return false;
        return activeRepair != null ? activeRepair.equals(that.activeRepair) : that.activeRepair == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (messageId ^ (messageId >>> 32));
        result = 31 * result + (int) (loot ^ (loot >>> 32));
        result = 31 * result + counterEspionagePercent;
        result = 31 * result + smallCargosNeeded;
        result = 31 * result + largeCargosNeeded;
        result = 31 * result + lootPercent;
        result = 31 * result + (messageDate != null ? messageDate.hashCode() : 0);
        result = 31 * result + (planetName != null ? planetName.hashCode() : 0);
        result = 31 * result + (playerName != null ? playerName.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (activity != null ? activity.hashCode() : 0);
        result = 31 * result + (api != null ? api.hashCode() : 0);
        result = 31 * result + (coordinates != null ? coordinates.hashCode() : 0);
        result = 31 * result + (isHonorable ? 1 : 0);
        result = 31 * result + (resources != null ? resources.hashCode() : 0);
        result = 31 * result + (levels != null ? levels.hashCode() : 0);
        result = 31 * result + (activeRepair != null ? activeRepair.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EspionageMessage{" +
                "messageId=" + messageId +
                ", loot=" + loot +
                ", counterEspionagePercent=" + counterEspionagePercent +
                ", smallCargosNeeded=" + smallCargosNeeded +
                ", largeCargosNeeded=" + largeCargosNeeded +
                ", lootPercent=" + lootPercent +
                ", messageDate=" + messageDate +
                ", planetName='" + planetName + '\'' +
                ", playerName='" + playerName + '\'' +
                ", status='" + status + '\'' +
                ", activity='" + activity + '\'' +
                ", api='" + api + '\'' +
                ", coordinates=" + coordinates +
                ", isHonorable=" + isHonorable +
                ", resources=" + resources +
                ", levels=" + levels +
                ", activeRepair=" + activeRepair +
                '}';
    }
}
