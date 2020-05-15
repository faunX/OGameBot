package ogame.objects.game.messages;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by jarndt on 6/14/17.
 */
public class CombatMessage {
    private long messageId, attackerGainsOrLosses, defenderGainsOrLosses, debrisFieldSize,
            actuallyRepaired, attackerHonorPointsGainOrLoss, defenderHonorPointsGainOrLoss,
            recyclerCount;
    private int lootPercent, moonChancePercent, attackerWeapons, attackerShields, attackerArmour, defenderWeapons, defenderShields, defenderArmour;
    private Resource loot, debrisField;
    private String attackerName, defenderName, api, tacticalRetreat, attackerPlanetName, defenderPlanetName, attackerStatus, defenderStatus;
    private Coordinates attackedPlanetCoordinates, attackerCoordinates, defenderCoordinates;
    private LocalDateTime messageDate;
    private HashMap<String,Integer> attackerShips = new HashMap<>(), attackerShipsLost = new HashMap<>(),
            defenderShipsDefence = new HashMap<>(), defenderShipsLost = new HashMap<>();

    public CombatMessage(MessageObject messageObject, String server, String cookies) throws IOException {
        messageId = messageObject.getMessageId();
        String coords = messageObject.getMessageTitle().replaceAll("Combat Report.*\\[","").replace("]","").trim();
        messageDate = messageObject.getMessageDate();
        attackedPlanetCoordinates = new Coordinates(coords);
        defenderCoordinates = attackedPlanetCoordinates;
        parseMessageContent(messageObject.getMessageContent());
        parseMoreDetails(
                Jsoup.parse(
                        new HttpsClient().getMoreDetails(
                                server,
                                21,
                                messageId,
                                cookies
                        )
                )
        );
    }

    public CombatMessage(Map<String, Object> a) {
        messageId = (int) a.get("message_id");
        messageDate = ((Timestamp)a.get("message_date")).toLocalDateTime();
        attackerGainsOrLosses = (int) a.get("attacker_gains");
        defenderGainsOrLosses = (int) a.get("defender_gains");
        debrisFieldSize = (int) a.get("debris_size");
        actuallyRepaired = (int) a.get("actually_repaired");
        attackerHonorPointsGainOrLoss = (int) a.get("attacker_honor");
        defenderHonorPointsGainOrLoss = (int) a.get("defender_honor");
        recyclerCount = (int) a.get("recycler_count");
        moonChancePercent = (int) a.get("moon_change_percent");
        attackerWeapons = (int) a.get("attacker_weapons");
        attackerShields = (int) a.get("attacker_shields");
        attackerArmour = (int) a.get("attacker_armour");
        defenderWeapons = (int) a.get("defender_weapons");
        defenderShields = (int) a.get("defender_shields");
        defenderArmour = (int) a.get("defender_armour");
        loot = new Resource();
        loot.setMetal((long)a.get("loot_metal"));
        loot.setCrystal((long)a.get("loot_crystal"));
        loot.setDeuterium((long)a.get("loot_deueterium"));
        debrisField = new Resource();
        debrisField.setMetal((long)a.get("debris_metal"));
        debrisField.setCrystal((long)a.get("debris_cyrstal"));
        attackerName = a.get("attacker_name").toString();
        defenderName = a.get("defender_name").toString();
        api = a.get("api").toString();
        attackerStatus = a.get("attacker_status").toString();
        defenderStatus = a.get("defender_status").toString();
        attackerCoordinates = new Coordinates(a.get("attacker_planet_coords").toString());
        defenderCoordinates = new Coordinates(a.get("defender_planet_coords").toString());
        attackerShips = new Gson().fromJson(a.get("json_attacker_ships").toString(),new TypeToken<HashMap<String,Integer>>(){}.getType());
        attackerShipsLost = new Gson().fromJson(a.get("json_attacker_ships_lost").toString(),new TypeToken<HashMap<String,Integer>>(){}.getType());
        defenderShipsDefence = new Gson().fromJson(a.get("json_defender_ships").toString(),new TypeToken<HashMap<String,Integer>>(){}.getType());
        defenderShipsLost = new Gson().fromJson(a.get("json_defender_ships_lost").toString(),new TypeToken<HashMap<String,Integer>>(){}.getType());
    }

    private void parseMoreDetails(Document moreDetails) {
        tacticalRetreat = moreDetails.select("p.detail_txt > span").get(0).text().trim()+","+
                            moreDetails.select("div.detailReport > p > span").get(1).text();

        Elements debris = moreDetails.select("div.resourcedisplay");
        //TODO this was 8000
        recyclerCount = Long.parseLong(debris.select("div > span").get(3).text().replaceAll("[A-Za-z >=&;]",""));
        debrisField = new Resource();
        for(Element e : debris.get(1).select("li"))
            if(e.select("div.metal").size() > 0)
                debrisField.setMetal(Long.parseLong(e.select("span.res_value").text().replace(".","")));
            else if(e.select("div.crystal").size() > 0)
                debrisField.setMetal(Long.parseLong(e.select("span.res_value").text().replace(".","")));
        Elements honor = moreDetails.select("div.fightdetails > span");
        String s = honor.get(0).text().trim().replaceAll("[A-Za-z:+\\. ]","");
        attackerHonorPointsGainOrLoss = Long.parseLong(s.equals("-")?"0":s);
        s = honor.get(1).text().trim().replaceAll("[A-Za-z:+\\. ]","");
        defenderHonorPointsGainOrLoss = Long.parseLong(s.equals("-")?"0":s);

        Elements attacker = moreDetails.select("div.combat_participant.attacker");
        attackerStatus = attacker.attr("class").replace("combat_participant","").replace("attacker","").trim();
        String attackNamePlanet = attacker.select("div.common_info > span").get(0).text();
        attackerPlanetName = attackNamePlanet.replaceAll(".*from ","").replaceAll("[0-9]:[0-9]{3}:[0-9]+.*","").trim();
        attackerCoordinates = new Coordinates(attackNamePlanet.replaceAll(".*"+attackerPlanetName,"").trim());

        for(Element e : attacker.select("ul.common_info > li"))
            if(e.text().contains("Weapons")) {
                String v = e.text().replace("Weapons: ", "");
                attackerWeapons = Integer.parseInt(v.equals("-")?"0":v);
            }else if(e.text().contains("Shields")) {
                String v = e.text().replace("Shields: ", "");
                attackerShields = Integer.parseInt(v.equals("-")?"0":v);
            }else if(e.text().contains("Armour")) {
                String v = e.text().replace("Armour: ", "");
                attackerArmour = Integer.parseInt(v.equals("-")?"0":v);
            }

        for(Element e : attacker.select("div.buildingimg")){
            Elements span = e.select("span");
            //TODO this didn't list the small cargos lost
            attackerShips.put(span.get(0).text().trim(),Integer.parseInt(span.get(1).text().replace(".","").trim()));
            attackerShipsLost.put(span.get(0).text().trim(),Integer.parseInt(span.get(2).text().trim().equals("-")?"0":span.get(2).text().replace(".","").trim()));
        }

        Elements defender = moreDetails.select("div.combat_participant.defender");
        defenderStatus = defender.attr("class").replace("combat_participant","").replace("defender","").trim();
        String defenderNamePlanet = defender.select("div.common_info > span").get(0).text();
        defenderPlanetName = defenderNamePlanet.replaceAll(".*from ","").replaceAll("[0-9]:[0-9]{3}:[0-9]+.*","").trim();
        defenderCoordinates = new Coordinates(defenderNamePlanet.replaceAll(".*"+defenderPlanetName,"").trim());

        for(Element e : defender.select("ul.common_info > li"))
            if(e.text().contains("Weapons")) {
                String v = e.text().replace("Weapons: ", "");
                defenderWeapons = Integer.parseInt(v.equals("-")?"0":v);
            }else if(e.text().contains("Shields")) {
                String v = e.text().replace("Shields: ", "");
                defenderShields = Integer.parseInt(v.equals("-")?"0":v);
            }else if(e.text().contains("Armour")) {
                String v = e.text().replace("Armour: ", "");
                defenderArmour = Integer.parseInt(v.equals("-")?"0":v);
            }

        for(Element e : defender.select("div.buildingimg")){
            Elements span = e.select("span");
            //TODO this didn't list defender ships/defense
            defenderShipsDefence.put(span.get(0).text().trim(),Integer.parseInt(span.get(1).text().trim()));
            defenderShipsLost.put(span.get(0).text().trim(),Integer.parseInt(span.get(2).text().trim().equals("-")?"0":span.get(2).text().trim()));
        }

    }

    private void parseMessageContent(Elements messageContent) {
        Elements leftSide = messageContent.select("div.combatLeftSide > span"),
                    rightSide = messageContent.select("div.combatRightSide > span");
        Elements attacker = leftSide, defender = rightSide;
        String nameLosses = leftSide.get(0).text();
        if(nameLosses.contains("Defender")) {
            attacker = rightSide;
            defender = leftSide;
            nameLosses = rightSide.get(0).text();
        }
        String[] v = nameLosses.replace("Attacker:", "").replaceAll("[\\(\\)]", "").split(": ");
        attackerName = v[0].trim();
        attackerGainsOrLosses = Integer.parseInt(v[1].replace(".","").trim());
        loot = new Resource();
        for(String s : attacker.get(1).attr("title").split("<br/>"))
            if(s.contains("Metal"))
                loot.setMetal(Long.parseLong(s.replaceAll("[A-Za-z:\\. ]","")));
            else if(s.contains("Crystal"))
                loot.setCrystal(Long.parseLong(s.replaceAll("[A-Za-z:\\. ]","")));
            else if(s.contains("Deuterium"))
                loot.setDeuterium(Long.parseLong(s.replaceAll("[A-Za-z:\\. ]","")));
        lootPercent = Integer.parseInt(attacker.get(1).text().replaceAll(".*Loot: ","").replaceAll("[A-Za-z:% ]",""));
        debrisFieldSize = Long.parseLong(attacker.get(2).text().replaceAll("[A-Za-z:\\.\\(\\) ]",""));

        String[] defString = defender.get(0).text().replace("Defender:", "").replaceAll("[\\(\\)]", "").split(": ");
        defenderName = defString[0].trim();
        defenderGainsOrLosses = Long.parseLong(defString[1].replace(".",""));

        actuallyRepaired = Long.parseLong(defender.get(1).text().replaceAll("[A-Za-z:\\. ]",""));
        if(defender.size() > 2)
            moonChancePercent = Integer.parseInt(defender.get(2).text().replaceAll("[A-Za-z:%\\. ]",""));

        api = messageContent.select("div.msg_actions.clearfix > div > span").attr("title")
                .replaceAll(".*value='","").replaceAll("' .*","");
    }

    public boolean hasDefense() throws IOException {
        Set<Buildable> builds = Buildable.getDefense();
        builds.addAll(Buildable.getShipyard());
        builds.removeIf(a->a.getName().equals(Ship.SOLAR_SATELLITE));
        for(Buildable b : builds)
            if(defenderShipsDefence.containsKey(b.getName()) && defenderShipsDefence.get(b.getName()) > 0)
                return true;
        return false;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public long getAttackerGainsOrLosses() {
        return attackerGainsOrLosses;
    }

    public void setAttackerGainsOrLosses(long attackerGainsOrLosses) {
        this.attackerGainsOrLosses = attackerGainsOrLosses;
    }

    public long getDefenderGainsOrLosses() {
        return defenderGainsOrLosses;
    }

    public void setDefenderGainsOrLosses(long defenderGainsOrLosses) {
        this.defenderGainsOrLosses = defenderGainsOrLosses;
    }

    public long getDebrisFieldSize() {
        return debrisFieldSize;
    }

    public void setDebrisFieldSize(long debrisFieldSize) {
        this.debrisFieldSize = debrisFieldSize;
    }

    public long getActuallyRepaired() {
        return actuallyRepaired;
    }

    public void setActuallyRepaired(long actuallyRepaired) {
        this.actuallyRepaired = actuallyRepaired;
    }

    public long getAttackerHonorPointsGainOrLoss() {
        return attackerHonorPointsGainOrLoss;
    }

    public void setAttackerHonorPointsGainOrLoss(long attackerHonorPointsGainOrLoss) {
        this.attackerHonorPointsGainOrLoss = attackerHonorPointsGainOrLoss;
    }

    public long getDefenderHonorPointsGainOrLoss() {
        return defenderHonorPointsGainOrLoss;
    }

    public void setDefenderHonorPointsGainOrLoss(long defenderHonorPointsGainOrLoss) {
        this.defenderHonorPointsGainOrLoss = defenderHonorPointsGainOrLoss;
    }

    public int getLootPercent() {
        return lootPercent;
    }

    public void setLootPercent(int lootPercent) {
        this.lootPercent = lootPercent;
    }

    public int getMoonChancePercent() {
        return moonChancePercent;
    }

    public void setMoonChancePercent(int moonChancePercent) {
        this.moonChancePercent = moonChancePercent;
    }

    public int getAttackerWeapons() {
        return attackerWeapons;
    }

    public void setAttackerWeapons(int attackerWeapons) {
        this.attackerWeapons = attackerWeapons;
    }

    public int getAttackerShields() {
        return attackerShields;
    }

    public void setAttackerShields(int attackerShields) {
        this.attackerShields = attackerShields;
    }

    public int getAttackerArmour() {
        return attackerArmour;
    }

    public void setAttackerArmour(int attackerArmour) {
        this.attackerArmour = attackerArmour;
    }

    public int getDefenderWeapons() {
        return defenderWeapons;
    }

    public void setDefenderWeapons(int defenderWeapons) {
        this.defenderWeapons = defenderWeapons;
    }

    public int getDefenderShields() {
        return defenderShields;
    }

    public void setDefenderShields(int defenderShields) {
        this.defenderShields = defenderShields;
    }

    public int getDefenderArmour() {
        return defenderArmour;
    }

    public void setDefenderArmour(int defenderArmour) {
        this.defenderArmour = defenderArmour;
    }

    public Resource getLoot() {
        return loot;
    }

    public void setLoot(Resource loot) {
        this.loot = loot;
    }

    public Resource getDebrisField() {
        return debrisField;
    }

    public void setDebrisField(Resource debrisField) {
        this.debrisField = debrisField;
    }

    public String getAttackerName() {
        return attackerName;
    }

    public void setAttackerName(String attackerName) {
        this.attackerName = attackerName;
    }

    public String getDefenderName() {
        return defenderName;
    }

    public void setDefenderName(String defenderName) {
        this.defenderName = defenderName;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getTacticalRetreat() {
        return tacticalRetreat;
    }

    public void setTacticalRetreat(String tacticalRetreat) {
        this.tacticalRetreat = tacticalRetreat;
    }

    public String getAttackerPlanetName() {
        return attackerPlanetName;
    }

    public void setAttackerPlanetName(String attackerPlanetName) {
        this.attackerPlanetName = attackerPlanetName;
    }

    public String getDefenderPlanetName() {
        return defenderPlanetName;
    }

    public void setDefenderPlanetName(String defenderPlanetName) {
        this.defenderPlanetName = defenderPlanetName;
    }

    public Coordinates getAttackedPlanetCoordinates() {
        return attackedPlanetCoordinates;
    }

    public void setAttackedPlanetCoordinates(Coordinates attackedPlanetCoordinates) {
        this.attackedPlanetCoordinates = attackedPlanetCoordinates;
    }

    public Coordinates getAttackerCoordinates() {
        return attackerCoordinates;
    }

    public void setAttackerCoordinates(Coordinates attackerCoordinates) {
        this.attackerCoordinates = attackerCoordinates;
    }

    public Coordinates getDefenderCoordinates() {
        return defenderCoordinates;
    }

    public void setDefenderCoordinates(Coordinates defenderCoordinates) {
        this.defenderCoordinates = defenderCoordinates;
    }

    public LocalDateTime getMessageDate() {
        return messageDate;
    }

    public void setMessageDate(LocalDateTime messageDate) {
        this.messageDate = messageDate;
    }

    public HashMap<String, Integer> getAttackerShips() {
        return attackerShips;
    }

    public void setAttackerShips(HashMap<String, Integer> attackerShips) {
        this.attackerShips = attackerShips;
    }

    public HashMap<String, Integer> getDefenderShipsDefence() {
        return defenderShipsDefence;
    }

    public void setDefenderShipsDefence(HashMap<String, Integer> defenderShipsDefence) {
        this.defenderShipsDefence = defenderShipsDefence;
    }

    public String getAttackerStatus() {
        return attackerStatus;
    }

    public void setAttackerStatus(String attackerStatus) {
        this.attackerStatus = attackerStatus;
    }

    public String getDefenderStatus() {
        return defenderStatus;
    }

    public void setDefenderStatus(String defenderStatus) {
        this.defenderStatus = defenderStatus;
    }

    public long getRecyclerCount() {
        return recyclerCount;
    }

    public void setRecyclerCount(long recyclerCount) {
        this.recyclerCount = recyclerCount;
    }

    public HashMap<String, Integer> getAttackerShipsLost() {
        return attackerShipsLost;
    }

    public void setAttackerShipsLost(HashMap<String, Integer> attackerShipsLost) {
        this.attackerShipsLost = attackerShipsLost;
    }

    public HashMap<String, Integer> getDefenderShipsLost() {
        return defenderShipsLost;
    }

    public void setDefenderShipsLost(HashMap<String, Integer> defenderShipsLost) {
        this.defenderShipsLost = defenderShipsLost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CombatMessage that = (CombatMessage) o;

        if (messageId != that.messageId) return false;
        if (attackerGainsOrLosses != that.attackerGainsOrLosses) return false;
        if (defenderGainsOrLosses != that.defenderGainsOrLosses) return false;
        if (debrisFieldSize != that.debrisFieldSize) return false;
        if (actuallyRepaired != that.actuallyRepaired) return false;
        if (attackerHonorPointsGainOrLoss != that.attackerHonorPointsGainOrLoss) return false;
        if (defenderHonorPointsGainOrLoss != that.defenderHonorPointsGainOrLoss) return false;
        if (recyclerCount != that.recyclerCount) return false;
        if (lootPercent != that.lootPercent) return false;
        if (moonChancePercent != that.moonChancePercent) return false;
        if (attackerWeapons != that.attackerWeapons) return false;
        if (attackerShields != that.attackerShields) return false;
        if (attackerArmour != that.attackerArmour) return false;
        if (defenderWeapons != that.defenderWeapons) return false;
        if (defenderShields != that.defenderShields) return false;
        if (defenderArmour != that.defenderArmour) return false;
        if (loot != null ? !loot.equals(that.loot) : that.loot != null) return false;
        if (debrisField != null ? !debrisField.equals(that.debrisField) : that.debrisField != null) return false;
        if (attackerName != null ? !attackerName.equals(that.attackerName) : that.attackerName != null) return false;
        if (defenderName != null ? !defenderName.equals(that.defenderName) : that.defenderName != null) return false;
        if (api != null ? !api.equals(that.api) : that.api != null) return false;
        if (tacticalRetreat != null ? !tacticalRetreat.equals(that.tacticalRetreat) : that.tacticalRetreat != null)
            return false;
        if (attackerPlanetName != null ? !attackerPlanetName.equals(that.attackerPlanetName) : that.attackerPlanetName != null)
            return false;
        if (defenderPlanetName != null ? !defenderPlanetName.equals(that.defenderPlanetName) : that.defenderPlanetName != null)
            return false;
        if (attackerStatus != null ? !attackerStatus.equals(that.attackerStatus) : that.attackerStatus != null)
            return false;
        if (defenderStatus != null ? !defenderStatus.equals(that.defenderStatus) : that.defenderStatus != null)
            return false;
        if (attackedPlanetCoordinates != null ? !attackedPlanetCoordinates.equals(that.attackedPlanetCoordinates) : that.attackedPlanetCoordinates != null)
            return false;
        if (attackerCoordinates != null ? !attackerCoordinates.equals(that.attackerCoordinates) : that.attackerCoordinates != null)
            return false;
        if (defenderCoordinates != null ? !defenderCoordinates.equals(that.defenderCoordinates) : that.defenderCoordinates != null)
            return false;
        if (messageDate != null ? !messageDate.equals(that.messageDate) : that.messageDate != null) return false;
        if (attackerShips != null ? !attackerShips.equals(that.attackerShips) : that.attackerShips != null)
            return false;
        if (attackerShipsLost != null ? !attackerShipsLost.equals(that.attackerShipsLost) : that.attackerShipsLost != null)
            return false;
        if (defenderShipsDefence != null ? !defenderShipsDefence.equals(that.defenderShipsDefence) : that.defenderShipsDefence != null)
            return false;
        return defenderShipsLost != null ? defenderShipsLost.equals(that.defenderShipsLost) : that.defenderShipsLost == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (messageId ^ (messageId >>> 32));
        result = 31 * result + (int) (attackerGainsOrLosses ^ (attackerGainsOrLosses >>> 32));
        result = 31 * result + (int) (defenderGainsOrLosses ^ (defenderGainsOrLosses >>> 32));
        result = 31 * result + (int) (debrisFieldSize ^ (debrisFieldSize >>> 32));
        result = 31 * result + (int) (actuallyRepaired ^ (actuallyRepaired >>> 32));
        result = 31 * result + (int) (attackerHonorPointsGainOrLoss ^ (attackerHonorPointsGainOrLoss >>> 32));
        result = 31 * result + (int) (defenderHonorPointsGainOrLoss ^ (defenderHonorPointsGainOrLoss >>> 32));
        result = 31 * result + (int) (recyclerCount ^ (recyclerCount >>> 32));
        result = 31 * result + lootPercent;
        result = 31 * result + moonChancePercent;
        result = 31 * result + attackerWeapons;
        result = 31 * result + attackerShields;
        result = 31 * result + attackerArmour;
        result = 31 * result + defenderWeapons;
        result = 31 * result + defenderShields;
        result = 31 * result + defenderArmour;
        result = 31 * result + (loot != null ? loot.hashCode() : 0);
        result = 31 * result + (debrisField != null ? debrisField.hashCode() : 0);
        result = 31 * result + (attackerName != null ? attackerName.hashCode() : 0);
        result = 31 * result + (defenderName != null ? defenderName.hashCode() : 0);
        result = 31 * result + (api != null ? api.hashCode() : 0);
        result = 31 * result + (tacticalRetreat != null ? tacticalRetreat.hashCode() : 0);
        result = 31 * result + (attackerPlanetName != null ? attackerPlanetName.hashCode() : 0);
        result = 31 * result + (defenderPlanetName != null ? defenderPlanetName.hashCode() : 0);
        result = 31 * result + (attackerStatus != null ? attackerStatus.hashCode() : 0);
        result = 31 * result + (defenderStatus != null ? defenderStatus.hashCode() : 0);
        result = 31 * result + (attackedPlanetCoordinates != null ? attackedPlanetCoordinates.hashCode() : 0);
        result = 31 * result + (attackerCoordinates != null ? attackerCoordinates.hashCode() : 0);
        result = 31 * result + (defenderCoordinates != null ? defenderCoordinates.hashCode() : 0);
        result = 31 * result + (messageDate != null ? messageDate.hashCode() : 0);
        result = 31 * result + (attackerShips != null ? attackerShips.hashCode() : 0);
        result = 31 * result + (attackerShipsLost != null ? attackerShipsLost.hashCode() : 0);
        result = 31 * result + (defenderShipsDefence != null ? defenderShipsDefence.hashCode() : 0);
        result = 31 * result + (defenderShipsLost != null ? defenderShipsLost.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CombatMessage{" +
                "messageId=" + messageId +
                ", attackerGainsOrLosses=" + attackerGainsOrLosses +
                ", defenderGainsOrLosses=" + defenderGainsOrLosses +
                ", debrisFieldSize=" + debrisFieldSize +
                ", actuallyRepaired=" + actuallyRepaired +
                ", attackerHonorPointsGainOrLoss=" + attackerHonorPointsGainOrLoss +
                ", defenderHonorPointsGainOrLoss=" + defenderHonorPointsGainOrLoss +
                ", recyclerCount=" + recyclerCount +
                ", lootPercent=" + lootPercent +
                ", moonChancePercent=" + moonChancePercent +
                ", attackerWeapons=" + attackerWeapons +
                ", attackerShields=" + attackerShields +
                ", attackerArmour=" + attackerArmour +
                ", defenderWeapons=" + defenderWeapons +
                ", defenderShields=" + defenderShields +
                ", defenderArmour=" + defenderArmour +
                ", loot=" + loot +
                ", debrisField=" + debrisField +
                ", attackerName='" + attackerName + '\'' +
                ", defenderName='" + defenderName + '\'' +
                ", api='" + api + '\'' +
                ", tacticalRetreat='" + tacticalRetreat + '\'' +
                ", attackerPlanetName='" + attackerPlanetName + '\'' +
                ", defenderPlanetName='" + defenderPlanetName + '\'' +
                ", attackerStatus='" + attackerStatus + '\'' +
                ", defenderStatus='" + defenderStatus + '\'' +
                ", attackedPlanetCoordinates=" + attackedPlanetCoordinates +
                ", attackerCoordinates=" + attackerCoordinates +
                ", defenderCoordinates=" + defenderCoordinates +
                ", messageDate=" + messageDate +
                ", attackerShips=" + attackerShips +
                ", attackerShipsLost=" + attackerShipsLost +
                ", defenderShipsDefence=" + defenderShipsDefence +
                ", defenderShipsLost=" + defenderShipsLost +
                '}';
    }
}
