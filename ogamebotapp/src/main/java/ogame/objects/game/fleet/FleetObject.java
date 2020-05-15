package ogame.objects.game.fleet;

import bot.Bot;
import ogame.objects.game.Coordinates;
import ogame.objects.game.Resource;
import ogame.objects.game.Ship;
import ogame.pages.Fleet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utilities.data.HttpsClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by jarndt on 5/31/17.
 */
public class FleetObject {
    private HashMap<String, Integer> ships; //the ships being sent in this mission
    private Coordinates toCoordinates, fromCoordinates;
    private String mission, missionOwner, toUsername, fromUsername, toPlanetName, fromPlanetName;
    private Resource resourcesBeingCarried;
    private long dataArrivalTime, playerId, shipCount;
    private boolean isReturnFlight = false;
    private int speed = 10;

    LocalDateTime returnTime, sentTime, arrivalTime;

    public FleetObject() {
    }

    public FleetObject(Element tr){
        toCoordinates = new Coordinates(tr.select("td.destCoords > a").text().trim());
        fromCoordinates = new Coordinates(tr.select("td.coordsOrigin > a").text().trim());

        fromPlanetName = tr.select("td.originFleet").get(0).ownText().trim();

        String[] m = tr.select("td.missionFleet > img").attr("title").trim().split("\\|");
        mission = Mission.getMission(m[1]);
        missionOwner = Mission.getFleetType(m[0]);

        shipCount = Long.parseLong(tr.select("td.detailsFleet > span").text().trim());
        playerId = Long.parseLong(tr.select("a.sendMail").attr("data-playerid"));
        dataArrivalTime = Long.parseLong(tr.attr("data-arrival-time"));

        toUsername = tr.select("a.sendMail").attr("title").trim();
        toPlanetName = tr.select("td.destFleet").get(0).ownText();

        isReturnFlight = Boolean.parseBoolean(tr.attr("data-return-flight"));
        String move = isReturnFlight ? "icon_movement_reserve" : "icon_movement";
        parseFleet(Jsoup.parse(tr.select("td."+move+" > span").attr("title")));
    }

    private void parseFleet(Document title) {
        Elements tr = title.select("tr");
        boolean ships = false, resources = false;
        resourcesBeingCarried = new Resource();
        this.ships = new HashMap<>();
        for (Element e : tr){
            Elements th = e.select("th");
            if(th.size() != 0 && th.text().trim().contains("Ships:")) {
                ships = true;
                resources = false;
                continue;
            }if(th.size() != 0 && th.text().trim().contains("Shipment:")) {
                resources = true;
                ships = false;
                continue;
            }
            Elements tds = e.select("td");
            if(tds.size()!=2)
                continue;
            if(ships)
                this.ships.put(tds.get(0).text().trim().replace(":",""),Integer.parseInt(tds.get(1).text().trim()));
            if(resources){
                if(tds.get(0).text().trim().replace(":","").equals("Metal"))
                    resourcesBeingCarried.setMetal(Long.parseLong(tds.get(1).text().trim().replace(".","")));
                if(tds.get(0).text().trim().replace(":","").equals("Crystal"))
                    resourcesBeingCarried.setCrystal(Long.parseLong(tds.get(1).text().trim().replace(".","")));
                if(tds.get(0).text().trim().replace(":","").equals("Deuterium"))
                    resourcesBeingCarried.setDeuterium(Long.parseLong(tds.get(1).text().trim().replace(".","")));
            }
        }
    }

    public boolean isEnoughCargoCapacity() throws IOException {
        return getFuelCost() < getCargoCapacity();
    }

    public int getFuelCost() throws IOException {
        return getFuelCost(100);
    }public int getFuelCost(int speed) throws IOException {
        int distance = toCoordinates.getDistance(fromCoordinates);
        int fuel = 0;
        for(String s : getShips().keySet())
            fuel+=getShips().get(s)*(1+Math.round(distance*Ship.getShipByName(s).getFuel_consuption()/35000.*Math.pow(speed/100.+1,2)));
        return fuel;
    }

    public int getCargoCapacity() throws IOException {
        int cargoCapacity = 0;
        for(String s : getShips().keySet())
            cargoCapacity+=getShips().get(s)*Ship.getShipByName(s).getCargo_capacity();
        return cargoCapacity;
    }

    public FleetObject addShip(String smallCargo, int smallCargoCount) {
        getShips().put(smallCargo, smallCargoCount);
        return this;
    }

    public HashMap<String, Integer> getShips() {
        if(ships == null)
            ships = new HashMap<>();
        return ships;
    }

    public FleetObject setShips(HashMap<String, Integer> ships) {
        this.ships = ships;
        return this;
    }

    public Coordinates getToCoordinates() {
        return toCoordinates;
    }

    public FleetObject setToCoordinates(Coordinates toCoordinates) {
        this.toCoordinates = toCoordinates;
        return this;
    }

    public Coordinates getFromCoordinates() {
        return fromCoordinates;
    }

    public FleetObject setFromCoordinates(Coordinates fromCoordinates) {
        this.fromCoordinates = fromCoordinates;
        return this;
    }

    public String getMission() {
        return mission;
    }

    public FleetObject setMission(String mission) {
        this.mission = mission;
        return this;
    }

    public Resource getResourcesBeingCarried() {
        return resourcesBeingCarried;
    }

    public void setResourcesBeingCarried(Resource resourcesBeingCarried) {
        this.resourcesBeingCarried = resourcesBeingCarried;
    }

    public LocalDateTime getReturnTime() {
        return returnTime;
    }

    public void setReturnTime(LocalDateTime returnTime) {
        this.returnTime = returnTime;
    }

    public LocalDateTime getSentTime() {
        return sentTime;
    }

    public void setSentTime(LocalDateTime sentTime) {
        this.sentTime = sentTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getMissionOwner() {
        return missionOwner;
    }

    public void setMissionOwner(String missionOwner) {
        this.missionOwner = missionOwner;
    }

    public String getToUsername() {
        return toUsername;
    }

    public void setToUsername(String toUsername) {
        this.toUsername = toUsername;
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }

    public String getToPlanetName() {
        return toPlanetName;
    }

    public void setToPlanetName(String toPlanetName) {
        this.toPlanetName = toPlanetName;
    }

    public String getFromPlanetName() {
        return fromPlanetName;
    }

    public void setFromPlanetName(String fromPlanetName) {
        this.fromPlanetName = fromPlanetName;
    }

    public long getDataArrivalTime() {
        return dataArrivalTime;
    }

    public void setDataArrivalTime(long dataArrivalTime) {
        this.dataArrivalTime = dataArrivalTime;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public long getShipCount() {
        return shipCount;
    }

    public void setShipCount(long shipCount) {
        this.shipCount = shipCount;
    }

    public boolean isReturnFlight() {
        return isReturnFlight;
    }

    public void setReturnFlight(boolean returnFlight) {
        isReturnFlight = returnFlight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FleetObject that = (FleetObject) o;

        if (dataArrivalTime != that.dataArrivalTime) return false;
        if (playerId != that.playerId) return false;
        if (shipCount != that.shipCount) return false;
        if (isReturnFlight != that.isReturnFlight) return false;
        if (ships != null ? !ships.equals(that.ships) : that.ships != null) return false;
        if (toCoordinates != null ? !toCoordinates.equals(that.toCoordinates) : that.toCoordinates != null)
            return false;
        if (fromCoordinates != null ? !fromCoordinates.equals(that.fromCoordinates) : that.fromCoordinates != null)
            return false;
        if (mission != null ? !mission.equals(that.mission) : that.mission != null) return false;
        if (missionOwner != null ? !missionOwner.equals(that.missionOwner) : that.missionOwner != null) return false;
        if (toUsername != null ? !toUsername.equals(that.toUsername) : that.toUsername != null) return false;
        if (fromUsername != null ? !fromUsername.equals(that.fromUsername) : that.fromUsername != null) return false;
        if (toPlanetName != null ? !toPlanetName.equals(that.toPlanetName) : that.toPlanetName != null) return false;
        if (fromPlanetName != null ? !fromPlanetName.equals(that.fromPlanetName) : that.fromPlanetName != null)
            return false;
        if (resourcesBeingCarried != null ? !resourcesBeingCarried.equals(that.resourcesBeingCarried) : that.resourcesBeingCarried != null)
            return false;
        if (returnTime != null ? !returnTime.equals(that.returnTime) : that.returnTime != null) return false;
        if (sentTime != null ? !sentTime.equals(that.sentTime) : that.sentTime != null) return false;
        return arrivalTime != null ? arrivalTime.equals(that.arrivalTime) : that.arrivalTime == null;
    }

    @Override
    public int hashCode() {
        int result = ships != null ? ships.hashCode() : 0;
        result = 31 * result + (toCoordinates != null ? toCoordinates.hashCode() : 0);
        result = 31 * result + (fromCoordinates != null ? fromCoordinates.hashCode() : 0);
        result = 31 * result + (mission != null ? mission.hashCode() : 0);
        result = 31 * result + (missionOwner != null ? missionOwner.hashCode() : 0);
        result = 31 * result + (toUsername != null ? toUsername.hashCode() : 0);
        result = 31 * result + (fromUsername != null ? fromUsername.hashCode() : 0);
        result = 31 * result + (toPlanetName != null ? toPlanetName.hashCode() : 0);
        result = 31 * result + (fromPlanetName != null ? fromPlanetName.hashCode() : 0);
        result = 31 * result + (resourcesBeingCarried != null ? resourcesBeingCarried.hashCode() : 0);
        result = 31 * result + (int) (dataArrivalTime ^ (dataArrivalTime >>> 32));
        result = 31 * result + (int) (playerId ^ (playerId >>> 32));
        result = 31 * result + (int) (shipCount ^ (shipCount >>> 32));
        result = 31 * result + (isReturnFlight ? 1 : 0);
        result = 31 * result + (returnTime != null ? returnTime.hashCode() : 0);
        result = 31 * result + (sentTime != null ? sentTime.hashCode() : 0);
        result = 31 * result + (arrivalTime != null ? arrivalTime.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FleetObject{" +
                "ships=" + ships +
                ", toCoordinates=" + toCoordinates +
                ", fromCoordinates=" + fromCoordinates +
                ", mission='" + mission + '\'' +
                ", missionOwner='" + missionOwner + '\'' +
                ", toUsername='" + toUsername + '\'' +
                ", fromUsername='" + fromUsername + '\'' +
                ", toPlanetName='" + toPlanetName + '\'' +
                ", fromPlanetName='" + fromPlanetName + '\'' +
                ", resourcesBeingCarried=" + resourcesBeingCarried +
                ", dataArrivalTime=" + dataArrivalTime +
                ", playerId=" + playerId +
                ", shipCount=" + shipCount +
                ", isReturnFlight=" + isReturnFlight +
                ", returnTime=" + returnTime +
                ", sentTime=" + sentTime +
                ", arrivalTime=" + arrivalTime +
                '}';
    }




    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////// STATIC METHODS ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static List<FleetObject> getFleetObjects(Bot b) throws IOException {
        return getFleetObjects(Jsoup.parse(new HttpsClient().getEventInfo(b.getServerDomain(),b.getCookies())));
    }
    public static List<FleetObject> getFleetObjects(Document d){
        List<FleetObject> fleetObjects = new ArrayList<>();
        d.select("tr.eventFleet").forEach(a->fleetObjects.add(new FleetObject(a)));
        return fleetObjects;
    }
}
