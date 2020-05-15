package ogame.objects.game.planet;

import bot.queue.QueueManager;
import bot.settings.SettingsManager;
import ogame.objects.game.*;
import ogame.pages.*;
import utilities.database.Database;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jarndt on 5/10/17.
 */
public class Planet {
    public static final String OVERMARK = "overmark", UNDERMARK = "undermark";

    private PlanetProperties planetSize;
    private String planetName, webElement, attribute, value, id, className, link, tacticalRetreat, botPlanetID;
    private Planet moon;
    private Resource resources;
    private String planetImageURL;
    private QueueManager queueManager;
    private SettingsManager settingsManager;

    private HashMap<String, Buildable> allBuildables = new HashMap<>();

    private HashMap<String, Integer>
            buildings   = new HashMap<>(), //building name, level, facilities are included
            facilities  = new HashMap<>(),
            defense     = new HashMap<>(), //defense name, count
            ships       = new HashMap<>(); //ship name, count

    private double metalProduction, crystalProduction, dueteriumProduction;
    private long metalStorage, crystalStorage, dueteriumStorage;

    private String metalStorageString, crystalStorageString, dueteriumStorageString, energyStorageString;

    private long energyAvailable, energyProduction, energyConsuption, merchantItemCost = -1;

    private Set<ResourceObject> resourceObjects = new HashSet<>();

    private Coordinates coordinates;

    private BuildTask currentFacilityBeingBuild, currentBuildingBeingBuild;
    private Set<BuildTask> currentShipyardBeingBuild = new HashSet<>();

    private LocalDateTime lastUpdate, lastMerchantItem;

    public Planet() throws IOException {
        init();
    }

    public Planet(String link, PlanetProperties planetSize, String planetName, String webElement, String attribute, String value, String id, String className, Planet moon, Resource resources, String planetImageURL, HashMap<String, Integer> buildings, HashMap<String, Integer> facilities, HashMap<String, Integer> defense, HashMap<String, Integer> ships, long metalProduction, long crystalProduction, long dueteriumProduction, Coordinates coordinates, BuildTask currentFacilityBeingBuild, BuildTask currentBuildingBeingBuild, Set<BuildTask> currentShipyardBeingBuild, String tacticalRetreat, String botPlanetID) throws IOException {
        this.link = link;
        this.planetSize = planetSize;
        this.planetName = planetName;
        this.webElement = webElement;
        this.attribute = attribute;
        this.value = value;
        this.id = id;
        this.className = className;
        this.moon = moon;
        this.resources = resources;
        this.planetImageURL = planetImageURL;
        this.buildings = buildings;
        this.facilities = facilities;
        this.defense = defense;
        this.ships = ships;
        this.metalProduction = metalProduction;
        this.crystalProduction = crystalProduction;
        this.dueteriumProduction = dueteriumProduction;
        this.coordinates = coordinates;
        this.currentFacilityBeingBuild = currentFacilityBeingBuild;
        this.currentBuildingBeingBuild = currentBuildingBeingBuild;
        this.currentShipyardBeingBuild = currentShipyardBeingBuild;
        this.tacticalRetreat = tacticalRetreat;
        this.botPlanetID = botPlanetID;

        init();
    }

    private void init() throws IOException {
        initBuildables(buildings);
        initBuildables(facilities);
        initBuildables(ships);
        initBuildables(defense);
    }

    public void update(){
        LocalDateTime now = LocalDateTime.now();
        if(lastUpdate!= null) {
            Resource r = getResources(false);
            long seconds = ChronoUnit.SECONDS.between(lastUpdate, now);
            double metal = getMetalProduction()*seconds+r.getMetal(),
                crystal = getCrystalProduction()*seconds+r.getCrystal(),
                deueterium = getDueteriumProduction()*seconds+r.getDeuterium();
            r.setMetal(Math.round(metal));
            r.setCrystal(Math.round(crystal));
            r.setDeuterium(Math.round(deueterium));

            if(getCurrentBuildingBeingBuild() != null && getCurrentBuildingBeingBuild().isComplete())
                currentBuildingBeingBuild = null;
            List<BuildTask> remove = new ArrayList<>();
            getCurrentShipyardBeingBuild().forEach(a->{
                if(a.isComplete())
                    remove.add(a);
            });
            getCurrentShipyardBeingBuild().removeAll(remove);
        }
        lastUpdate = now;
    }

    public String getLinkToPage(String pageName){
        return getLink().replaceAll("=.*&","="+pageName+"&");
    }

    private void initBuildables(HashMap<String,Integer> map){
        map.forEach((a,b)->addBuildableNoOverride(Buildable.getBuildableByName(a).setCurrentLevel(b)));
    }
    private void initBuildables(List<Buildable> buildables){
        buildables.forEach(a->addBuildableNoOverride(a.setCurrentLevel(0)));
    }

    public boolean isMetalOutOfStorage(){
        if(getMetal() >= metalStorage - 100)
            return true;
        return false;
    }
    public boolean isCrystalOutOfStorage(){
        if(getCrystal() >= crystalStorage - 100)
            return true;
        return false;
    }
    public boolean isDeueteriumOutOfStorage(){
        if(getDueterium() >= dueteriumStorage - 100)
            return true;
        return false;
    }
    public void addBuildableNoOverride(Buildable b){
        if(!allBuildables.containsKey(b.getName()))
            addBuildable(b);
    }
    public void addBuildable(Buildable b){
        getAllBuildables().put(b.getName(),b);
        HashMap<String,Integer> map = null;
        if(b.getType().equals(Resources.RESOURCES.toLowerCase()))
            map = buildings;
        else if(b.getType().toLowerCase().equals(Facilities.FACILITIES.toLowerCase()))
            map = facilities;
        else if(b.getType().toLowerCase().equals(Defense.DEFENSE.toLowerCase()))
            map = defense;
        else if(b.getType().toLowerCase().equals(Shipyard.SHIPYARD.toLowerCase()))
            map = ships;

        map.put(b.getName(),b.getCurrentLevel());
    }

    public Buildable getBuildable(String name){
        return allBuildables.get(name);
    }

    public boolean canBuild(String name){
        return canBuild(name,new HashMap<>());
    }
    public boolean canBuild(String name, HashMap<String,Integer> research){
        update();
        if(!hasPrerequisites(name,research))
            return false;
        Buildable b = getAllBuildables().get(name);
        if(b.getType().toLowerCase().equals(Resources.RESOURCES.toLowerCase()) || b.getType().toLowerCase().equals(Facilities.FACILITIES.toLowerCase())) {
            if (currentBuildingBeingBuild != null) {
                if (currentBuildingBeingBuild.isDone() && currentBuildingBeingBuild.isComplete())
                    return canAfford(name);
                else
                    return false;
            }
        }
        //if it is a defense or shipyard and the shipyard or nanite factory is build built, then you can't build it.
        //or if a defense or shipyard is being built, you can't built the nanite factory or shipyard.
        if(Arrays.asList(Defense.DEFENSE.toLowerCase(),Shipyard.SHIPYARD.toLowerCase()).contains(b.getType().toLowerCase())
                || Arrays.asList(Facilities.SHIPYARD.toLowerCase(),Facilities.NANITE_FACTORY.toLowerCase()).contains(name.toLowerCase()))
            if (currentBuildingBeingBuild != null &&
                    Arrays.asList(Facilities.SHIPYARD.toLowerCase(),Facilities.NANITE_FACTORY.toLowerCase())
                            .contains(currentBuildingBeingBuild.getBuildable().getName().toLowerCase())) {
                if (currentBuildingBeingBuild.isDone() && currentBuildingBeingBuild.isComplete()) {
                    //don't build another ship/defense if there is something in the queue
                    if (getCurrentShipyardBeingBuild().size() > 0)
                        return false;
                    else
                        return canAfford(name);
                }else
                    return false;
            }else {
                if (getCurrentShipyardBeingBuild().size() > 0)
                    return false;
                else
                    return canAfford(name);
            }

        return canAfford(name);
    }

    public boolean hasPrerequisites(String name) {
        HashMap<String, Integer> requirements = Buildable.getBuildableRequirements(name);
        if(requirements.size() == 0) return true;
        for(String m : requirements.keySet())
            if(getAllBuildables().get(m).getCurrentLevel() < requirements.get(m))
                return false;
        return true;
    }public boolean hasPrerequisites(String name, HashMap<String,Integer> research) {
        HashMap<String, Integer> allBs = new HashMap<>();
        getAllBuildables().forEach((a,b)->allBs.put(a,b.getCurrentLevel()));
        allBs.putAll(research);
        if(getCurrentBuildingBeingBuild() != null && !(getCurrentBuildingBeingBuild().isComplete() && getCurrentBuildingBeingBuild().isDone()))
            allBs.put(getCurrentBuildingBeingBuild().getBuildable().getName(),getCurrentBuildingBeingBuild().getCountOrLevel()-1);

        HashMap<String, Integer> requirements = Buildable.getBuildableRequirements(name);
        if(requirements.size() == 0) return true;
        for(String m : requirements.keySet())
            if(allBs.get(m) < requirements.get(m))
                return false;
        return true;
    }

    public boolean canAfford(String name){
        Resource r = getAllBuildables().get(name).getNextLevelCost();
        return r.lessThan(getResources());
    }public boolean canAfford(Buildable buildable){
        Resource r = buildable.getNextLevelCost();
        return r.lessThan(getResources());
    }

    public String getSetting(String setting, int ogameUserId) throws SQLException, IOException, ClassNotFoundException {
        if(getSettingsManager(ogameUserId).getSettings().containsKey(setting))
            return getSettingsManager(ogameUserId).getSettings().get(setting);
        return "";
    }public String getSetting(String setting, String botPlanetID) throws SQLException, IOException, ClassNotFoundException {
        if(getSettingsManager(botPlanetID).getSettings().containsKey(setting))
            return getSettingsManager(botPlanetID).getSettings().get(setting);
        return "";
    }
    public SettingsManager getSettingsManager(int ogameUserId) throws SQLException, IOException, ClassNotFoundException {
        if(settingsManager == null) {
            if(botPlanetID == null) {
                List<Map<String, Object>> v = Database.getExistingDatabaseConnection().executeQuery(
                        "select * from bot_planets where ogame_user_id = " + ogameUserId + " and coords = '" + coordinates.getStringValue() + "';"
                );
                if (v != null && v.size() > 0 && v.get(0) != null && v.get(0).size() > 0) {
                    botPlanetID = v.get(0).get("id").toString();
                    settingsManager = new SettingsManager(this);
                }
            }else
                settingsManager = new SettingsManager(this);
        }
        return settingsManager.setPlanet(this);
    }public SettingsManager getSettingsManager(String botPlanetID) throws SQLException, IOException, ClassNotFoundException {
        if(settingsManager == null)
            settingsManager = new SettingsManager(this);
        return settingsManager.setPlanet(this);
    }



    public void setEnergyStorageString(String energyStorageString) {
        this.energyStorageString = energyStorageString;
    }

    public String getEnergyStorageString(){
        return energyStorageString;
    }

    public Set<ResourceObject> getResourceObjects() {
        return resourceObjects;
    }

    public void setResourceObjects(Set<ResourceObject> resourceObjects) {
        this.resourceObjects = resourceObjects;
    }

    public void setSettingsManager(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public HashMap<String, Buildable> getAllBuildables() {
        return allBuildables;
    }

    public String getTacticalRetreat() {
        return tacticalRetreat;
    }

    public String getMetalStorageString() {
        return metalStorageString;
    }

    public void setMetalStorageString(String metalStorageString) {
        this.metalStorageString = metalStorageString;
    }

    public String getCrystalStorageString() {
        return crystalStorageString;
    }

    public void setCrystalStorageString(String crystalStorageString) {
        this.crystalStorageString = crystalStorageString;
    }

    public String getDueteriumStorageString() {
        return dueteriumStorageString;
    }

    public void setDueteriumStorageString(String dueteriumStorageString) {
        this.dueteriumStorageString = dueteriumStorageString;
    }

    public long getMerchantItemCost() {
        return merchantItemCost;
    }

    public void setMerchantItemCost(long merchantItemCost) {
        this.merchantItemCost = merchantItemCost;
    }

    public void setTacticalRetreat(String tacticalRetreat) {
        this.tacticalRetreat = tacticalRetreat;
    }

    public void setAllBuildables(HashMap<String, Buildable> allBuildables) {
        this.allBuildables = allBuildables;
    }

    public long getMetalStorage() {
        return metalStorage;
    }

    public void setMetalStorage(long metalStorage) {
        this.metalStorage = metalStorage;
    }

    public long getCrystalStorage() {
        return crystalStorage;
    }

    public void setCrystalStorage(long crystalStorage) {
        this.crystalStorage = crystalStorage;
    }

    public long getDueteriumStorage() {
        return dueteriumStorage;
    }

    public void setDueteriumStorage(long dueteriumStorage) {
        this.dueteriumStorage = dueteriumStorage;
    }

    public long getEnergyAvailable() {
        return getResourceObject(ResourceObject.ENERGY).getActual();
    }

    public void setEnergyAvailable(long energyAvailable) {
        this.energyAvailable = energyAvailable;
    }

    public double getEnergyProduction() {
        return getResourceObject(ResourceObject.ENERGY).getProduction();
    }

    public void setEnergyProduction(long energyProduction) {
        this.energyProduction = energyProduction;
    }

    public long getEnergyConsuption() {
        return getResourceObject(ResourceObject.ENERGY).getConsumption();
    }

    public ResourceObject getResourceObject(String resourceName){
        return getResourceObjects().stream().filter(a -> a.getResourceName().equals(resourceName)).collect(Collectors.toList()).get(0);
    }

    public void setEnergyConsuption(long energyConsuption) {
        this.energyConsuption = energyConsuption;
    }

    public Resource getResources() {
        update();
        return resources;
    }public Resource getResources(boolean b) {
        return resources;
    }

    public long getMetal(){
        return getResources().getMetal();
    }public long getCrystal(){
        return getResources().getCrystal();
    }public long getDueterium(){
        return getResources().getDeuterium();
    }

    public boolean canGetMerchantItem(){
        boolean canGet = true;
        if(merchantItemCost >= 0) {
            Resource r = getResources();
//            long total = (long) (r.metal+r.crystal*1.5+r.deuterium*3);
            long total = r.metal;
            canGet = total >= merchantItemCost;
        }
        return canGet && getLastMerchantItem().plusDays(1).isAfter(LocalDateTime.now());
    }

    public LocalDateTime getLastMerchantItem() {
        if(lastMerchantItem == null)
            lastMerchantItem = LocalDateTime.now().minusDays(2);
        return lastMerchantItem;
    }

    public void setLastMerchantItem(LocalDateTime lastMerchantItem) {
        this.lastMerchantItem = lastMerchantItem;
    }

    public long getEnergy(){
        return getResources().getEnergy();
    }

    public void setResources(Resource resources) {
        this.resources = resources;
    }

    public PlanetProperties getPlanetSize() {
        return planetSize;
    }

    public void setPlanetSize(PlanetProperties planetSize) {
        this.planetSize = planetSize;
    }

    public String getPlanetName() {
        return planetName;
    }

    public void setPlanetName(String planetName) {
        this.planetName = planetName;
    }

    public String getWebElement() {
        return webElement;
    }

    public void setWebElement(String webElement) {
        this.webElement = webElement;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Planet getMoon() {
        return moon;
    }

    public void setMoon(Planet moon) {
        this.moon = moon;
    }

    public HashMap<String, Integer> getBuildings() {
        return buildings;
    }

    public void setBuildings(HashMap<String, Integer> buildings) {
        this.buildings = buildings;
    }

    public HashMap<String, Integer> getFacilities() {
        return facilities;
    }

    public void setFacilities(HashMap<String, Integer> facilities) {
        this.facilities = facilities;
    }

    public HashMap<String, Integer> getDefense() {
        return defense;
    }

    public void setDefense(HashMap<String, Integer> defense) {
        this.defense = defense;
    }

    public HashMap<String, Integer> getShips() {
        return ships;
    }

    public void setShips(HashMap<String, Integer> ships) {
        this.ships = ships;
    }

    public double getMetalProduction() {
        return getResourceObject(ResourceObject.METAL).getCurrentProduction();
    }

    public void setMetalProduction(double metalProduction) {
        this.metalProduction = metalProduction;
    }

    public double getCrystalProduction() {
        return getResourceObject(ResourceObject.CRYSTAL).getCurrentProduction();
    }

    public void setCrystalProduction(double crystalProduction) {
        this.crystalProduction = crystalProduction;
    }

    public double getDueteriumProduction() { //per second
        return getResourceObject(ResourceObject.DEUETERIUM).getCurrentProduction();
    }

    public void setDueteriumProduction(double dueteriumProduction) {
        this.dueteriumProduction = dueteriumProduction;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public BuildTask getCurrentFacilityBeingBuild() {
        return currentFacilityBeingBuild;
    }

    public void setCurrentFacilityBeingBuild(BuildTask currentFacilityBeingBuild) {
        this.currentFacilityBeingBuild = currentFacilityBeingBuild;
    }

    public BuildTask getCurrentBuildingBeingBuild() {
        return currentBuildingBeingBuild;
    }

    public void setCurrentBuildingBeingBuild(BuildTask currentBuildingBeingBuild) {
        this.currentBuildingBeingBuild = currentBuildingBeingBuild;
    }

    public Set<BuildTask> getCurrentShipyardBeingBuild() {
        if(currentShipyardBeingBuild == null)
            currentShipyardBeingBuild = new HashSet<>();
        Set<BuildTask> remove = new HashSet<>();
        for(BuildTask b : currentShipyardBeingBuild)
            if(b.isComplete() && b.isDone())
                remove.add(b);
        currentShipyardBeingBuild.removeAll(remove);
        return currentShipyardBeingBuild;
    }

    public void setCurrentShipyardBeingBuild(Set<BuildTask> currentShipyardBeingBuild) {
        this.currentShipyardBeingBuild = currentShipyardBeingBuild;
    }

    public String getId() {
        return id;
    }

    public String getBotPlanetID() {
        return botPlanetID;
    }

    public void setBotPlanetID(String botPlanetID) {
        this.botPlanetID = botPlanetID;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getPlanetImageURL() {
        return planetImageURL;
    }


    public double getEnergyPercent() {
        double d = Math.abs(getEnergyConsuption()/(getEnergyProduction() == 0?1:(double)getEnergyProduction())),
                productionFactor = d > 1 ? 1 : d;

        return productionFactor;
    }

    public void setPlanetImageURL(String planetImageURL) {
        this.planetImageURL = planetImageURL;
    }

    public QueueManager getQueueManager(int ogameUserId, HashMap<String,Integer> research) throws SQLException, IOException, ClassNotFoundException {
        if(queueManager == null) {
            if(botPlanetID == null) {
                List<Map<String, Object>> v = Database.getExistingDatabaseConnection().executeQuery(
                        "select * from bot_planets where ogame_user_id = " + ogameUserId + " and coords = '" + coordinates.getStringValue() + "';"
                );
                if (v != null && v.size() > 0 && v.get(0) != null && v.get(0).size() > 0) {
                    botPlanetID = v.get(0).get("id").toString();
                    queueManager = new QueueManager(this, research);
                }
            }else
                queueManager = new QueueManager(this, research);
        }
        return queueManager.setPlanet(this);
    }

    public void setQueueManager(QueueManager queueManager) {
        this.queueManager = queueManager;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Planet planet = (Planet) o;

        if (Double.compare(planet.metalProduction, metalProduction) != 0) return false;
        if (Double.compare(planet.crystalProduction, crystalProduction) != 0) return false;
        if (Double.compare(planet.dueteriumProduction, dueteriumProduction) != 0) return false;
        if (metalStorage != planet.metalStorage) return false;
        if (crystalStorage != planet.crystalStorage) return false;
        if (dueteriumStorage != planet.dueteriumStorage) return false;
        if (energyAvailable != planet.energyAvailable) return false;
        if (energyProduction != planet.energyProduction) return false;
        if (energyConsuption != planet.energyConsuption) return false;
        if (planetSize != null ? !planetSize.equals(planet.planetSize) : planet.planetSize != null) return false;
        if (planetName != null ? !planetName.equals(planet.planetName) : planet.planetName != null) return false;
        if (webElement != null ? !webElement.equals(planet.webElement) : planet.webElement != null) return false;
        if (attribute != null ? !attribute.equals(planet.attribute) : planet.attribute != null) return false;
        if (value != null ? !value.equals(planet.value) : planet.value != null) return false;
        if (id != null ? !id.equals(planet.id) : planet.id != null) return false;
        if (className != null ? !className.equals(planet.className) : planet.className != null) return false;
        if (link != null ? !link.equals(planet.link) : planet.link != null) return false;
        if (tacticalRetreat != null ? !tacticalRetreat.equals(planet.tacticalRetreat) : planet.tacticalRetreat != null)
            return false;
        if (botPlanetID != null ? !botPlanetID.equals(planet.botPlanetID) : planet.botPlanetID != null) return false;
        if (moon != null ? !moon.equals(planet.moon) : planet.moon != null) return false;
        if (resources != null ? !resources.equals(planet.resources) : planet.resources != null) return false;
        if (planetImageURL != null ? !planetImageURL.equals(planet.planetImageURL) : planet.planetImageURL != null)
            return false;
        if (queueManager != null ? !queueManager.equals(planet.queueManager) : planet.queueManager != null)
            return false;
        if (allBuildables != null ? !allBuildables.equals(planet.allBuildables) : planet.allBuildables != null)
            return false;
        if (buildings != null ? !buildings.equals(planet.buildings) : planet.buildings != null) return false;
        if (facilities != null ? !facilities.equals(planet.facilities) : planet.facilities != null) return false;
        if (defense != null ? !defense.equals(planet.defense) : planet.defense != null) return false;
        if (ships != null ? !ships.equals(planet.ships) : planet.ships != null) return false;
        if (metalStorageString != null ? !metalStorageString.equals(planet.metalStorageString) : planet.metalStorageString != null)
            return false;
        if (crystalStorageString != null ? !crystalStorageString.equals(planet.crystalStorageString) : planet.crystalStorageString != null)
            return false;
        if (dueteriumStorageString != null ? !dueteriumStorageString.equals(planet.dueteriumStorageString) : planet.dueteriumStorageString != null)
            return false;
        if (coordinates != null ? !coordinates.equals(planet.coordinates) : planet.coordinates != null) return false;
        if (currentFacilityBeingBuild != null ? !currentFacilityBeingBuild.equals(planet.currentFacilityBeingBuild) : planet.currentFacilityBeingBuild != null)
            return false;
        if (currentBuildingBeingBuild != null ? !currentBuildingBeingBuild.equals(planet.currentBuildingBeingBuild) : planet.currentBuildingBeingBuild != null)
            return false;
        if (currentShipyardBeingBuild != null ? !currentShipyardBeingBuild.equals(planet.currentShipyardBeingBuild) : planet.currentShipyardBeingBuild != null)
            return false;
        if (lastUpdate != null ? !lastUpdate.equals(planet.lastUpdate) : planet.lastUpdate != null) return false;
        return lastMerchantItem != null ? lastMerchantItem.equals(planet.lastMerchantItem) : planet.lastMerchantItem == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = planetSize != null ? planetSize.hashCode() : 0;
        result = 31 * result + (planetName != null ? planetName.hashCode() : 0);
        result = 31 * result + (webElement != null ? webElement.hashCode() : 0);
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (className != null ? className.hashCode() : 0);
        result = 31 * result + (link != null ? link.hashCode() : 0);
        result = 31 * result + (tacticalRetreat != null ? tacticalRetreat.hashCode() : 0);
        result = 31 * result + (botPlanetID != null ? botPlanetID.hashCode() : 0);
        result = 31 * result + (moon != null ? moon.hashCode() : 0);
        result = 31 * result + (resources != null ? resources.hashCode() : 0);
        result = 31 * result + (planetImageURL != null ? planetImageURL.hashCode() : 0);
        result = 31 * result + (queueManager != null ? queueManager.hashCode() : 0);
        result = 31 * result + (allBuildables != null ? allBuildables.hashCode() : 0);
        result = 31 * result + (buildings != null ? buildings.hashCode() : 0);
        result = 31 * result + (facilities != null ? facilities.hashCode() : 0);
        result = 31 * result + (defense != null ? defense.hashCode() : 0);
        result = 31 * result + (ships != null ? ships.hashCode() : 0);
        temp = Double.doubleToLongBits(metalProduction);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(crystalProduction);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(dueteriumProduction);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) (metalStorage ^ (metalStorage >>> 32));
        result = 31 * result + (int) (crystalStorage ^ (crystalStorage >>> 32));
        result = 31 * result + (int) (dueteriumStorage ^ (dueteriumStorage >>> 32));
        result = 31 * result + (metalStorageString != null ? metalStorageString.hashCode() : 0);
        result = 31 * result + (crystalStorageString != null ? crystalStorageString.hashCode() : 0);
        result = 31 * result + (dueteriumStorageString != null ? dueteriumStorageString.hashCode() : 0);
        result = 31 * result + (int) (energyAvailable ^ (energyAvailable >>> 32));
        result = 31 * result + (int) (energyProduction ^ (energyProduction >>> 32));
        result = 31 * result + (int) (energyConsuption ^ (energyConsuption >>> 32));
        result = 31 * result + (coordinates != null ? coordinates.hashCode() : 0);
        result = 31 * result + (currentFacilityBeingBuild != null ? currentFacilityBeingBuild.hashCode() : 0);
        result = 31 * result + (currentBuildingBeingBuild != null ? currentBuildingBeingBuild.hashCode() : 0);
        result = 31 * result + (currentShipyardBeingBuild != null ? currentShipyardBeingBuild.hashCode() : 0);
        result = 31 * result + (lastUpdate != null ? lastUpdate.hashCode() : 0);
        result = 31 * result + (lastMerchantItem != null ? lastMerchantItem.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Planet{" +
                "planetSize=" + planetSize +
                ", planetName='" + planetName + '\'' +
                ", webElement='" + webElement + '\'' +
                ", attribute='" + attribute + '\'' +
                ", value='" + value + '\'' +
                ", id='" + id + '\'' +
                ", className='" + className + '\'' +
                ", link='" + link + '\'' +
                ", moon=" + moon +
                ", resources=" + resources +
                ", planetImageURL='" + planetImageURL + '\'' +
                ", metalProduction=" + metalProduction +
                ", crystalProduction=" + crystalProduction +
                ", dueteriumProduction=" + dueteriumProduction +
                ", coordinates=" + coordinates +
                ", currentFacilityBeingBuild=" + currentFacilityBeingBuild +
                ", currentBuildingBeingBuild=" + currentBuildingBeingBuild +
                ", currentShipyardBeingBuild=" + currentShipyardBeingBuild +
                '}';
    }
}
