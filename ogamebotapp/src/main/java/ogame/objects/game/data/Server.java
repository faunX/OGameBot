package ogame.objects.game.data;

import utilities.database.Database;
import utilities.fileio.FileOptions;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Created by jarndt on 5/26/17.
 */
public class Server {
    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
        System.out.println(new Server(Server.IZAR));
    }

    private static HashMap<String, Server> serverHashMap = new HashMap<>();

    public static Server getServer(String serverName) throws SQLException, IOException, ClassNotFoundException {
        if(serverHashMap.containsKey(serverName))
            return serverHashMap.get(serverName);
        serverHashMap.put(serverName,new Server(serverName));
        return getServer(serverName);
    }

    public static final String
            JAPETUS = "Japetus",
            VEGA = "Vega",
            LIBRA = "Libra",
            QUANTUM = "Quantum",
            IZAR = "Izar",
            HYPERION = "Hyperion",
            GANIMED = "Ganimed",
            SPICA = "Spica",
            TAURUS = "Taurus",
            POLARIS = "Polaris",
            ERIDANUS = "Eridanus",
            RHEA = "Rhea",
            FIDIS = "Fidis",
            WASAT = "Wasat",
            QUAOAR = "Quaoar",
            NUSAKAN = "Nusakan",
            BETELGEUSE = "Betelgeuse",
            RIGEL = "Rigel",
            DEIMOS = "Deimos",
            OBERON = "Oberon",
            UNITY = "Unity",
            CYGNUS = "Cygnus",
            KALLISTO = "Kallisto",
            PASIPHAE = "Pasiphae";

    int serverID, speed, speedFleet, galaxies, systems,
        newbieProtectionLimit, newbieProtectionHigh, bonusFields, wfMinimumRessLost,
            wfMinimumLossPercentage, wfBasicPercentageRepairable;
    long topScore, latestTimestamp;
    double debrisFactor, debrisFactorDef, repairFactor, globalDueteriumSaveFactor;
    String serverName, language, timezone, timezoneOffset, domain, version;

    boolean isDonuteGalaxy, isDonutSystem, isWFEnabled, isACSActive, isRapidFireActive, isDefenseToDebrisField;

    private HashMap<String, PlayerData> players = new HashMap<>();

    public Server(int serverID) throws SQLException, IOException, ClassNotFoundException {
        parse("select * from server where server_id = '"+serverID+"' order by timestamp desc limit 1;");
    }public Server(String serverName) throws SQLException, IOException, ClassNotFoundException {
        parse("select * from server where server_name = '"+serverName+"' order by timestamp desc limit 1;");
    }

    private void parse(String query) throws SQLException, IOException, ClassNotFoundException {
        List<Map<String, Object>> results = Database.newDatabaseConnection().executeQuery(query);
        if(results != null && results.size() != 0 && results.get(0) != null && results.get(0).size() != 0){
            Map<String, Object> v = results.get(0);
            serverID = (int) v.get("server_id");
            serverName = v.get("server_name").toString();
            language = v.get("language").toString();
            timezone = v.get("timezone").toString();
            timezoneOffset = v.get("timezone_offset").toString();
            domain = v.get("domain").toString();
            version = v.get("version").toString();
            speed = (int)v.get("speed");
            speedFleet = (int)v.get("speed_fleet");
            galaxies = (int)v.get("galaxies");
            systems = (int)v.get("systems");
            isACSActive = (int)v.get("acs") == 1 ? true: false;
            isRapidFireActive = (int)v.get("rapidfire") == 1 ? true: false;
            isDefenseToDebrisField = (int)v.get("deftotf") == 1 ? true: false;
            debrisFactor = ((BigDecimal)v.get("debris_factor")).doubleValue();
            debrisFactorDef = ((BigDecimal)v.get("debris_factor_def")).doubleValue();
            repairFactor = ((BigDecimal)v.get("repair_factor")).doubleValue();
            newbieProtectionLimit = (int)v.get("newbie_protection_limit");
            newbieProtectionHigh = (int)v.get("newbie_protection_high");
            topScore = (long)v.get("top_score");
            bonusFields = (int)v.get("bonus_fields");
            isDonuteGalaxy = (int)v.get("donut_galaxy") == 1 ? true: false;
            isDonutSystem = (int)v.get("donut_system") == 1 ? true: false;
            isWFEnabled = (int)v.get("wf_enabled") == 1 ? true: false;
            wfMinimumRessLost = (int)v.get("wf_minimum_ress_lost");
            wfMinimumLossPercentage = (int)v.get("wf_minimum_loss_percentage");
            wfBasicPercentageRepairable = (int)v.get("wf_basic_percentage_repairable");
            globalDueteriumSaveFactor = ((BigDecimal)v.get("global_deuterium_save_factor")).doubleValue();;//(int)v.get("global_deuterium_save_factor");
            latestTimestamp = (long)v.get("timestamp");
        }else
            throw new IOException("Invalid value given in constructor: "+query);
    }

    public PlayerData getPlayer(String player) throws SQLException, IOException, ClassNotFoundException {
        if(players.containsKey(player))
            return players.get(player);
        players.put(player,new PlayerData(player, this));
        return getPlayer(player);
    }

    public List<PlayerData> getAllPlayers() throws SQLException, IOException, ClassNotFoundException {
        List<Map<String, Object>> results = Database.getExistingDatabaseConnection().executeQuery("select distinct name from player where server_id = " + getServerID());
        if (results != null && results.size() != 0 && results.get(0) != null && results.get(0).size() != 0)
            FileOptions.runConcurrentProcess(
                    results.stream().map(a -> (Callable) () -> getPlayer(a.get("name").toString())).collect(Collectors.toList())
            );
        return new ArrayList<>(players.values());
    }


    public ZoneId getZoneId() {
        return ZoneId.of(getTimezone());
    }

    public double getRepairFactor() {
        return repairFactor;
    }

    public void setRepairFactor(double repairFactor) {
        this.repairFactor = repairFactor;
    }

    public int getServerID() {
        return serverID;
    }

    public void setServerID(int serverID) {
        this.serverID = serverID;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getSpeedFleet() {
        return speedFleet;
    }

    public void setSpeedFleet(int speedFleet) {
        this.speedFleet = speedFleet;
    }

    public int getGalaxies() {
        return galaxies;
    }

    public void setGalaxies(int galaxies) {
        this.galaxies = galaxies;
    }

    public int getSystems() {
        return systems;
    }

    public void setSystems(int systems) {
        this.systems = systems;
    }

    public boolean isACSActive() {
        return isACSActive;
    }

    public void setACSActive(boolean ACSActive) {
        isACSActive = ACSActive;
    }

    public boolean isRapidFireActive() {
        return isRapidFireActive;
    }

    public void setRapidFireActive(boolean rapidFireActive) {
        isRapidFireActive = rapidFireActive;
    }

    public boolean isDefenseToDebrisField() {
        return isDefenseToDebrisField;
    }

    public void setDefenseToDebrisField(boolean defenseToDebrisField) {
        isDefenseToDebrisField = defenseToDebrisField;
    }

    public int getNewbieProtectionLimit() {
        return newbieProtectionLimit;
    }

    public void setNewbieProtectionLimit(int newbieProtectionLimit) {
        this.newbieProtectionLimit = newbieProtectionLimit;
    }

    public int getNewbieProtectionHigh() {
        return newbieProtectionHigh;
    }

    public void setNewbieProtectionHigh(int newbieProtectionHigh) {
        this.newbieProtectionHigh = newbieProtectionHigh;
    }

    public int getBonusFields() {
        return bonusFields;
    }

    public void setBonusFields(int bonusFields) {
        this.bonusFields = bonusFields;
    }

    public int getWfMinimumRessLost() {
        return wfMinimumRessLost;
    }

    public void setWfMinimumRessLost(int wfMinimumRessLost) {
        this.wfMinimumRessLost = wfMinimumRessLost;
    }

    public int getWfMinimumLossPercentage() {
        return wfMinimumLossPercentage;
    }

    public void setWfMinimumLossPercentage(int wfMinimumLossPercentage) {
        this.wfMinimumLossPercentage = wfMinimumLossPercentage;
    }

    public int getWfBasicPercentageRepairable() {
        return wfBasicPercentageRepairable;
    }

    public void setWfBasicPercentageRepairable(int wfBasicPercentageRepairable) {
        this.wfBasicPercentageRepairable = wfBasicPercentageRepairable;
    }

    public double getGlobalDueteriumSaveFactor() {
        return globalDueteriumSaveFactor;
    }

    public void setGlobalDueteriumSaveFactor(double globalDueteriumSaveFactor) {
        this.globalDueteriumSaveFactor = globalDueteriumSaveFactor;
    }

    public long getTopScore() {
        return topScore;
    }

    public void setTopScore(long topScore) {
        this.topScore = topScore;
    }

    public long getLatestTimestamp() {
        return latestTimestamp;
    }

    public void setLatestTimestamp(long latestTimestamp) {
        this.latestTimestamp = latestTimestamp;
    }

    public double getDebrisFactor() {
        return debrisFactor;
    }

    public void setDebrisFactor(double debrisFactor) {
        this.debrisFactor = debrisFactor;
    }

    public double getDebrisFactorDef() {
        return debrisFactorDef;
    }

    public void setDebrisFactorDef(double debrisFactorDef) {
        this.debrisFactorDef = debrisFactorDef;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getTimezoneOffset() {
        return timezoneOffset;
    }

    public void setTimezoneOffset(String timezoneOffset) {
        this.timezoneOffset = timezoneOffset;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isDonuteGalaxy() {
        return isDonuteGalaxy;
    }

    public void setDonuteGalaxy(boolean donuteGalaxy) {
        isDonuteGalaxy = donuteGalaxy;
    }

    public boolean isDonutSystem() {
        return isDonutSystem;
    }

    public void setDonutSystem(boolean donutSystem) {
        isDonutSystem = donutSystem;
    }

    public boolean isWFEnabled() {
        return isWFEnabled;
    }

    public void setWFEnabled(boolean WFEnabled) {
        isWFEnabled = WFEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Server server = (Server) o;

        if (serverID != server.serverID) return false;
        if (speed != server.speed) return false;
        if (speedFleet != server.speedFleet) return false;
        if (galaxies != server.galaxies) return false;
        if (systems != server.systems) return false;
        if (newbieProtectionLimit != server.newbieProtectionLimit) return false;
        if (newbieProtectionHigh != server.newbieProtectionHigh) return false;
        if (bonusFields != server.bonusFields) return false;
        if (wfMinimumRessLost != server.wfMinimumRessLost) return false;
        if (wfMinimumLossPercentage != server.wfMinimumLossPercentage) return false;
        if (wfBasicPercentageRepairable != server.wfBasicPercentageRepairable) return false;
        if (topScore != server.topScore) return false;
        if (latestTimestamp != server.latestTimestamp) return false;
        if (Double.compare(server.debrisFactor, debrisFactor) != 0) return false;
        if (Double.compare(server.debrisFactorDef, debrisFactorDef) != 0) return false;
        if (Double.compare(server.repairFactor, repairFactor) != 0) return false;
        if (Double.compare(server.globalDueteriumSaveFactor, globalDueteriumSaveFactor) != 0) return false;
        if (isDonuteGalaxy != server.isDonuteGalaxy) return false;
        if (isDonutSystem != server.isDonutSystem) return false;
        if (isWFEnabled != server.isWFEnabled) return false;
        if (isACSActive != server.isACSActive) return false;
        if (isRapidFireActive != server.isRapidFireActive) return false;
        if (isDefenseToDebrisField != server.isDefenseToDebrisField) return false;
        if (serverName != null ? !serverName.equals(server.serverName) : server.serverName != null) return false;
        if (language != null ? !language.equals(server.language) : server.language != null) return false;
        if (timezone != null ? !timezone.equals(server.timezone) : server.timezone != null) return false;
        if (timezoneOffset != null ? !timezoneOffset.equals(server.timezoneOffset) : server.timezoneOffset != null)
            return false;
        if (domain != null ? !domain.equals(server.domain) : server.domain != null) return false;
        return version != null ? version.equals(server.version) : server.version == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = serverID;
        result = 31 * result + speed;
        result = 31 * result + speedFleet;
        result = 31 * result + galaxies;
        result = 31 * result + systems;
        result = 31 * result + newbieProtectionLimit;
        result = 31 * result + newbieProtectionHigh;
        result = 31 * result + bonusFields;
        result = 31 * result + wfMinimumRessLost;
        result = 31 * result + wfMinimumLossPercentage;
        result = 31 * result + wfBasicPercentageRepairable;
        result = 31 * result + (int) (topScore ^ (topScore >>> 32));
        result = 31 * result + (int) (latestTimestamp ^ (latestTimestamp >>> 32));
        temp = Double.doubleToLongBits(debrisFactor);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(debrisFactorDef);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(repairFactor);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(globalDueteriumSaveFactor);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (serverName != null ? serverName.hashCode() : 0);
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + (timezone != null ? timezone.hashCode() : 0);
        result = 31 * result + (timezoneOffset != null ? timezoneOffset.hashCode() : 0);
        result = 31 * result + (domain != null ? domain.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (isDonuteGalaxy ? 1 : 0);
        result = 31 * result + (isDonutSystem ? 1 : 0);
        result = 31 * result + (isWFEnabled ? 1 : 0);
        result = 31 * result + (isACSActive ? 1 : 0);
        result = 31 * result + (isRapidFireActive ? 1 : 0);
        result = 31 * result + (isDefenseToDebrisField ? 1 : 0);
        return result;
    }


    @Override
    public String toString() {
        return "Server{" +
                "serverID=" + serverID +
                ", speed=" + speed +
                ", speedFleet=" + speedFleet +
                ", galaxies=" + galaxies +
                ", systems=" + systems +
                ", newbieProtectionLimit=" + newbieProtectionLimit +
                ", newbieProtectionHigh=" + newbieProtectionHigh +
                ", bonusFields=" + bonusFields +
                ", wfMinimumRessLost=" + wfMinimumRessLost +
                ", wfMinimumLossPercentage=" + wfMinimumLossPercentage +
                ", wfBasicPercentageRepairable=" + wfBasicPercentageRepairable +
                ", topScore=" + topScore +
                ", latestTimestamp=" + latestTimestamp +
                ", debrisFactor=" + debrisFactor +
                ", debrisFactorDef=" + debrisFactorDef +
                ", repairFactor=" + repairFactor +
                ", globalDueteriumSaveFactor=" + globalDueteriumSaveFactor +
                ", serverName='" + serverName + '\'' +
                ", language='" + language + '\'' +
                ", timezone='" + timezone + '\'' +
                ", timezoneOffset='" + timezoneOffset + '\'' +
                ", domain='" + domain + '\'' +
                ", version='" + version + '\'' +
                ", isDonuteGalaxy=" + isDonuteGalaxy +
                ", isDonutSystem=" + isDonutSystem +
                ", isWFEnabled=" + isWFEnabled +
                ", isACSActive=" + isACSActive +
                ", isRapidFireActive=" + isRapidFireActive +
                ", isDefenseToDebrisField=" + isDefenseToDebrisField +
                '}';
    }
}
