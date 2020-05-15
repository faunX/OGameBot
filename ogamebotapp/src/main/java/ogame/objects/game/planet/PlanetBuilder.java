package ogame.objects.game.planet;

import ogame.objects.game.BuildTask;
import ogame.objects.game.Buildable;
import ogame.objects.game.Coordinates;
import ogame.objects.game.Resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by jarndt on 5/29/17.
 */
public class PlanetBuilder {
    private PlanetProperties planetSize;
    private String planetName, webElement, attribute, value, id, className, link, tacticalRetreat, botPlanetID;
    private Planet moon;
    private Resource resources;
    private String planetImageURL;
    private long metalProduction, crystalProduction, dueteriumProduction;

    private Coordinates coordinates;

    private BuildTask currentFacilityBeingBuild, currentBuildingBeingBuild;
    private Set<BuildTask> currentShipyardBeingBuild = new HashSet<>();


    private HashMap<String, Integer>
            buildings   = new HashMap<>(), //building name, level, facilities are included
            facilities  = new HashMap<>(),
            defense     = new HashMap<>(), //defense name, count
            ships       = new HashMap<>(); //ship name, count

    public PlanetBuilder() throws IOException {
        Buildable.getResources().forEach(a->buildings.put(a.getName(),0));
        Buildable.getFacilities().forEach(a->facilities.put(a.getName(),0));
        Buildable.getDefense().forEach(a->defense.put(a.getName(),0));
        Buildable.getShipyard().forEach(a->ships.put(a.getName(),0));
    }

    public PlanetBuilder setPlanetSize(PlanetProperties planetSize) {
        this.planetSize = planetSize;
        return this;
    }

    public PlanetBuilder setPlanetName(String planetName) {
        this.planetName = planetName;
        return this;
    }

    public PlanetBuilder setWebElement(String webElement) {
        this.webElement = webElement;
        return this;
    }

    public PlanetBuilder setAttribute(String attribute) {
        this.attribute = attribute;
        return this;
    }

    public PlanetBuilder setValue(String value) {
        this.value = value;
        return this;
    }

    public PlanetBuilder setId(String id) {
        this.id = id;
        return this;
    }

    public PlanetBuilder setClassName(String className) {
        this.className = className;
        return this;
    }

    public PlanetBuilder setMoon(Planet moon) {
        this.moon = moon;
        return this;
    }

    public PlanetBuilder setResources(Resource resources) {
        this.resources = resources;
        return this;
    }

    public PlanetBuilder setMetal(long metal){
        if(this.resources == null)
            this.resources = new Resource();
        this.resources.metal = metal;
        return this;
    }

    public PlanetBuilder setCrystal(long crystal){
        if(this.resources == null)
            this.resources = new Resource();
        this.resources.crystal = crystal;
        return this;
    }

    public PlanetBuilder setDeuterium(long deuterium){
        if(this.resources == null)
            this.resources = new Resource();
        this.resources.deuterium = deuterium;
        return this;
    }

    public PlanetBuilder setEnergy(long energy){
        if(this.resources == null)
            this.resources = new Resource();
        this.resources.energy = energy;
        return this;
    }

    public PlanetBuilder setPlanetImageURL(String planetImageURL) {
        this.planetImageURL = planetImageURL;
        return this;
    }

    public PlanetBuilder setMetalProduction(long metalProduction) {
        this.metalProduction = metalProduction;
        return this;
    }

    public PlanetBuilder setCrystalProduction(long crystalProduction) {
        this.crystalProduction = crystalProduction;
        return this;
    }

    public PlanetBuilder setDueteriumProduction(long dueteriumProduction) {
        this.dueteriumProduction = dueteriumProduction;
        return this;
    }

    public PlanetBuilder setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
        return this;
    }

    public PlanetBuilder setCurrentFacilityBeingBuild(BuildTask currentFacilityBeingBuild) {
        this.currentFacilityBeingBuild = currentFacilityBeingBuild;
        return this;
    }

    public PlanetBuilder setCurrentBuildingBeingBuild(BuildTask currentBuildingBeingBuild) {
        this.currentBuildingBeingBuild = currentBuildingBeingBuild;
        return this;
    }

    public PlanetBuilder setCurrentShipyardBeingBuild(Set<BuildTask> currentShipyardBeingBuild) {
        this.currentShipyardBeingBuild = currentShipyardBeingBuild;
        return this;
    }

    public PlanetBuilder setBuildings(HashMap<String, Integer> buildings) {
        this.buildings = buildings;
        return this;
    }
    public PlanetBuilder addBuildingLevel(String buildingName, Integer level){
        if(buildings.containsKey(buildingName))
            buildings.put(buildingName,level);
        return this;
    }

    public PlanetBuilder setFacilities(HashMap<String, Integer> facilities) {
        this.facilities = facilities;
        return this;
    }
    public PlanetBuilder addFacilitiesLevel(String facilitiesName, Integer level){
        if(facilities.containsKey(facilitiesName))
            facilities.put(facilitiesName,level);
        return this;
    }

    public PlanetBuilder setDefense(HashMap<String, Integer> defense) {
        this.defense = defense;
        return this;
    }
    public PlanetBuilder addDefenseCount(String defenseName, Integer count){
        if(defense.containsKey(defenseName))
            defense.put(defenseName,count);
        return this;
    }

    public PlanetBuilder setShips(HashMap<String, Integer> ships) {
        this.ships = ships;
        return this;
    }
    public PlanetBuilder addShipCount(String shipName, Integer count){
        if(ships.containsKey(shipName))
            ships.put(shipName,count);
        return this;
    }

    public PlanetBuilder setLink(String link){
        this.link = link;
        return this;
    }

    public PlanetBuilder setTacticalRetreat(String tacticalRetreat) {
        this.tacticalRetreat = tacticalRetreat;
        return this;
    }

    public PlanetBuilder setBotPlanetID(String botPlanetID) {
        this.botPlanetID = botPlanetID;
        return this;
    }

    public Planet build() throws IOException {
        return new Planet(link,planetSize, planetName, webElement, attribute, value, id, className, moon, resources, planetImageURL,
                buildings, facilities, defense, ships, metalProduction, crystalProduction, dueteriumProduction,
                coordinates, currentFacilityBeingBuild, currentBuildingBeingBuild, currentShipyardBeingBuild,tacticalRetreat,botPlanetID);
    }
}
