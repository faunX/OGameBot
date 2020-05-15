package ogame.objects.game;

import ogame.pages.Resources;
import utilities.database.Database;
import utilities.fileio.FileOptions;
import utilities.fileio.JarUtility;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Created by jarndt on 5/8/17.
 */
public class Buildable {
    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException, InterruptedException {
//        Ship.getAllShips().forEach(a->System.out.println(a.getName()+Buildable.getBuildableRequirements(a.getName())));
//        Buildable.getDefense().forEach(System.out::println);
//        Buildable crystalMine = Buildable.getBuildableByName(Resources.CRYSTAL_MINE);
//        for (int i = 0; i < 34; i++) {
//            System.out.println((crystalMine.getCurrentLevel()+1)+"="+crystalMine.getNextLevelCost());
//            crystalMine.setCurrentLevel(crystalMine.getCurrentLevel()+1);
//        }
//        try {
//            getInstance().executor.awaitTermination(1L, TimeUnit.MINUTES);
//        }finally {
//            getInstance().executor.shutdownNow();
//        }//        }//            while (!getInstance().executor.isTerminated() && !getInstance().executor.isShutdown())
//                Thread.sleep(1000);
//        }
//        Database.newDatabaseConnection().executeQuery("select * from buildable;")
//            .forEach(System.out::println);

//        "metalProduction": 0.37566070571306,
//                "crystalProduction": 0.30075214701031,
//                "dueteriumProduction": 0.16254376566924,

        System.out.println(
                utilities.Timer.getTime(
                    Buildable.getBuildableByName(Resources.SOLAR_PLANT)
                        .setCurrentLevel(8).getNextLevelCost()
                        .subtract(new Resource(659,2302,1161))
                        .getTimeUntilCanAfford(0.37566070571306, 0.30075214701031, 0.16254376566924)
                        * 1e9
                )
        );

//        System.out.println(new Gson().toJson(Buildable.getBuildableByID(0)));
    }

    public static final String
            RESOURCE_DIR    = FileOptions.cleanFilePath(JarUtility.getResourceDir()+"ogame/buildable_files/"),
            PROFILE_DIR     = RESOURCE_DIR+"/profile/",
            SHIP_INFO       = RESOURCE_DIR+"ogame_ship_info.csv",
            BATTLE_INFO     = RESOURCE_DIR+"battle_info",
            RESEARCH_INFO   = RESOURCE_DIR+"research_info.csv",
            FACILITIES_INFO = RESOURCE_DIR+"facilities_info.csv",
            BUILDING_INFO   = RESOURCE_DIR+"building_info.csv",
            SHIPYARD_INFO   = RESOURCE_DIR+"shipyard_info.csv",
            DEFENSE_INFO    = RESOURCE_DIR+"defense_info.csv",
            MAPPINGS        = RESOURCE_DIR+"mapper.csv",
            LAST_UPDATE     = RESOURCE_DIR+"last_update";

    private String name, webName, line, type, requires, cssSelector, quickBuildLink, ref;
    private int id, levelNeeded, currentLevel;
    private double costMultiplier, productionMultiplier, productionPower, consuptionMultiplier, consuptionPower;

    private Resource baseCost, currentProduction, baseProduction, baseConsuption, currentConsuption;

    public int getCurrentLevel() {
        return currentLevel;
    }

    public Buildable setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
        return this;
    }
    public Resource getCurrentLevelCost(){
        return getLevelCost(currentLevel);
    }
    public Resource getNextLevelCost() {
        if(currentLevel == 0)
            return baseCost;
        return baseCost.geometricPower(costMultiplier,currentLevel+1);
    }
    public Resource getLevelCost(int level){
        if(level == 0)
            return new Resource(0,0,0);
        if(costMultiplier == 1)
            return baseCost.multiply(level);
        return baseCost.geometricPower(costMultiplier,level);
    }

    public double getProductionMultiplier() {
        return productionMultiplier;
    }

    public void setProductionMultiplier(double productionMultiplier) {
        this.productionMultiplier = productionMultiplier;
    }

    public double getProductionPower() {
        return productionPower;
    }

    public void setProductionPower(double productionPower) {
        this.productionPower = productionPower;
    }

    public double getConsuptionMultiplier() {
        return consuptionMultiplier;
    }

    public void setConsuptionMultiplier(double consuptionMultiplier) {
        this.consuptionMultiplier = consuptionMultiplier;
    }

    public double getConsuptionPower() {
        return consuptionPower;
    }

    public void setConsuptionPower(double consuptionPower) {
        this.consuptionPower = consuptionPower;
    }

    public Resource getBaseProduction() {
        return baseProduction;
    }

    public void setBaseProduction(Resource baseProduction) {
        this.baseProduction = baseProduction;
    }

    public Resource getBaseConsuption() {
        return baseConsuption;
    }

    public void setBaseConsuption(Resource baseConsuption) {
        this.baseConsuption = baseConsuption;
    }

    public void setCssSelector(String cssSelector) {
        this.cssSelector = cssSelector;
    }

    public String getCssSelector() {
        return cssSelector;
    }

    public String getType() {
        return type;
    }

    public String getWebName() {
        return webName;
    }

    public int getLevelNeeded() {
        return levelNeeded;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String getRequiresString() {
        return requires;
    }

    public Buildable setLevelNeeded(int levelNeeded) {
        this.levelNeeded = levelNeeded;
        return this;
    }

    public String getQuickBuildLink() {
        return quickBuildLink;
    }

    public void setQuickBuildLink(String quickBuildLink) {
        this.quickBuildLink = quickBuildLink;
    }

    public String getRef() {
        return ref;
    }

    public void incrementLevel(){
        this.currentLevel++;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    @Override
    public String toString() {
        return "Buildable{" +
                "name='" + name + '\'' +
                ", webName='" + webName + '\'' +
                ", type='" + type + '\'' +
                ", requires='" + requires + '\'' +
                ", id=" + id +
                ", levelNeeded=" + levelNeeded +
                ", currentLevel=" + currentLevel +
                ", costMultiplier=" + costMultiplier +
                ", baseCost=" + baseCost +
                ", nextLevelCost=" + getNextLevelCost() +
                ", currentProduction=" + currentProduction +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Buildable buildable = (Buildable) o;

        if (id != buildable.id) return false;
        if (levelNeeded != buildable.levelNeeded) return false;
        if (currentLevel != buildable.currentLevel) return false;
        if (Double.compare(buildable.costMultiplier, costMultiplier) != 0) return false;
        if (Double.compare(buildable.productionMultiplier, productionMultiplier) != 0) return false;
        if (Double.compare(buildable.productionPower, productionPower) != 0) return false;
        if (Double.compare(buildable.consuptionMultiplier, consuptionMultiplier) != 0) return false;
        if (Double.compare(buildable.consuptionPower, consuptionPower) != 0) return false;
        if (name != null ? !name.equals(buildable.name) : buildable.name != null) return false;
        if (webName != null ? !webName.equals(buildable.webName) : buildable.webName != null) return false;
        if (line != null ? !line.equals(buildable.line) : buildable.line != null) return false;
        if (type != null ? !type.equals(buildable.type) : buildable.type != null) return false;
        if (requires != null ? !requires.equals(buildable.requires) : buildable.requires != null) return false;
        if (cssSelector != null ? !cssSelector.equals(buildable.cssSelector) : buildable.cssSelector != null)
            return false;
        if (quickBuildLink != null ? !quickBuildLink.equals(buildable.quickBuildLink) : buildable.quickBuildLink != null)
            return false;
        if (ref != null ? !ref.equals(buildable.ref) : buildable.ref != null) return false;
        if (baseCost != null ? !baseCost.equals(buildable.baseCost) : buildable.baseCost != null) return false;
        if (currentProduction != null ? !currentProduction.equals(buildable.currentProduction) : buildable.currentProduction != null)
            return false;
        if (baseProduction != null ? !baseProduction.equals(buildable.baseProduction) : buildable.baseProduction != null)
            return false;
        if (baseConsuption != null ? !baseConsuption.equals(buildable.baseConsuption) : buildable.baseConsuption != null)
            return false;
        return currentConsuption != null ? currentConsuption.equals(buildable.currentConsuption) : buildable.currentConsuption == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = name != null ? name.hashCode() : 0;
        result = 31 * result + (webName != null ? webName.hashCode() : 0);
        result = 31 * result + (line != null ? line.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (requires != null ? requires.hashCode() : 0);
        result = 31 * result + (cssSelector != null ? cssSelector.hashCode() : 0);
        result = 31 * result + (quickBuildLink != null ? quickBuildLink.hashCode() : 0);
        result = 31 * result + (ref != null ? ref.hashCode() : 0);
        result = 31 * result + id;
        result = 31 * result + levelNeeded;
        result = 31 * result + currentLevel;
        temp = Double.doubleToLongBits(costMultiplier);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(productionMultiplier);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(productionPower);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(consuptionMultiplier);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(consuptionPower);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (baseCost != null ? baseCost.hashCode() : 0);
        result = 31 * result + (currentProduction != null ? currentProduction.hashCode() : 0);
        result = 31 * result + (baseProduction != null ? baseProduction.hashCode() : 0);
        result = 31 * result + (baseConsuption != null ? baseConsuption.hashCode() : 0);
        result = 31 * result + (currentConsuption != null ? currentConsuption.hashCode() : 0);
        return result;
    }

    public List<Buildable> getRequires(){
        String[] split = requires.split("\\/");
        List<Buildable> buildables = new ArrayList<>();
        for(String s : split){
            if(s == null)
                continue;
            if(s.trim().isEmpty())
                continue;
            String[] subSplit = s.split("\\.");
            Buildable b = getBuildableByID(Integer.parseInt(subSplit[0])).clone();
            b.setLevelNeeded(Integer.parseInt(subSplit[1]));
            buildables.add(b);
        }
        return buildables;
    }

    public Buildable(String line) {
        this.line = line;
        String[] obj = line.split(",");
        if("ID".equals(obj[0])) return;
        id = Integer.parseInt(obj[0]);
        Ship s = null;
        if(obj[1].contains("/"))
            s = getObjName(obj[1]);
        name = s == null?obj[1]:s.getName();
        webName = obj[2];
        requires = obj.length <= 3 ? "" : obj[3];
        try {
            type = getType(id);
        } catch (IOException e) {
            e.printStackTrace();
        }
        exceptions(name);
        if(obj.length <= 8) {
            costMultiplier = 1;
            baseCost = s.getCost();
            return;
        }

        costMultiplier = Double.parseDouble(obj[4]);
        baseCost = new Resource(
            Long.parseLong(obj[5]),
            Long.parseLong(obj[6]),
            Long.parseLong(obj[7]),
            Long.parseLong(obj[8])
        );
    }

    public long getLevelBuildTime(int level, int roboticsFactoryLevel, int naniteFactoryLevel, double acceleration){
        Resource r = getLevelCost(level);
        return Math.round((r.metal+r.crystal)/2500./(1+roboticsFactoryLevel)/acceleration/Math.pow(2,naniteFactoryLevel) //in hours
                *60*60); //seconds
    }
    public long getCurrentBuildTime(int roboticsFactoryLevel, int naniteFactoryLevel, double acceleration){
        return getLevelBuildTime(currentLevel,roboticsFactoryLevel,naniteFactoryLevel,acceleration);
    }
    public long getCurrentBuildTime(int roboticsFactoryLevel, int naniteFactoryLevel){
        return getLevelBuildTime(currentLevel,roboticsFactoryLevel,naniteFactoryLevel,1);
    }

    public Resource getLevelConsumption(int currentLevel){
        if(baseConsuption == null)
            return null;
        Resource base = new Resource(0, 0, 0, 1);
        if(name.equals(Resources.FUSION_REACTOR))
            base = new Resource(0,0,1);
        return base.multiply(consuptionMultiplier).multiply(currentLevel).multiply(Math.pow(consuptionPower,currentLevel));
    }
    public Resource getCurrentConsuption(){
        return getLevelConsumption(currentLevel);
    }

    public Resource getLevelProduction(int currentLevel,double acceleration,
                                       int averageTemperature, int plasmaTechLevel,
                                       int energyTechLevel){
        if(baseProduction == null)
            return null;
        if(name.equals(Ship.SOLAR_SATELLITE))
            return new Resource(0,0,0,1)
                        .multiply(currentLevel)
                        .multiply(Math.floor((averageTemperature+160)/6));
        Resource base = new Resource(0,0,0,1)
                .multiply(productionMultiplier)
                .multiply(currentLevel);
        if(name.equals(Resources.FUSION_REACTOR))
            return base.multiply(Math.pow(1.05+energyTechLevel*productionMultiplier,currentLevel));
        if(name.equals(Resources.SOLAR_PLANT))
            return base.multiply(Math.pow(productionPower,currentLevel));
        base = baseProduction.normalize();
        if(name.equals(Resources.DUETERIUM_SYNTHESIZER))
            base = new Resource(0,0,1);
        Resource r = base
                .multiply(productionMultiplier)
                .multiply(currentLevel)
                .multiply(Math.pow(productionPower, currentLevel))
                .add(baseProduction)
                .multiply(acceleration)
                .multiply(1 + plasmaTechLevel / 100);
        if(name.equals(Resources.DUETERIUM_SYNTHESIZER))
            r.multiply(1.36-.004*averageTemperature);
        return r;
    }public Resource getLevelProduction(int level, int averageTemperature){
        return getLevelProduction(level,1,averageTemperature,0,0);
    }

    public Resource getCurrentProduction(double acceleration, int averageTemperature, int plasmaTechLevel, int energyTechLevel) {
        return getLevelProduction(currentLevel,acceleration,averageTemperature,plasmaTechLevel,energyTechLevel);
    } public Resource getCurrentProduction(double acceleration, int averageTemperature, int plasmaTechLevel) {
        return getLevelProduction(currentLevel,acceleration,averageTemperature,plasmaTechLevel,0);
    }public Resource getCurrentProduction(int averageTemperature){
        return getCurrentProduction(1,averageTemperature,0);
    }public Resource getCurrentProduction(int averageTemperature, int energyTechLevel){
        return getCurrentProduction(1,averageTemperature,0,energyTechLevel);
    }public Resource getCurrentProduction(){
        if(name.equals(Resources.DUETERIUM_SYNTHESIZER))
            throw new IllegalArgumentException("Must supply temperature");
        if(name.equals(Resources.FUSION_REACTOR))
            throw new IllegalArgumentException("Must supply temperature and energy tech level");
        return getCurrentProduction(0);
    }

    private void exceptions(String name) {
        //TODO
        double pm = 30, pp = 1.1, em = 10, ep = 1.1;
        Resource baseConsuption = new Resource();
        if(name.equals(Resources.METAL_MINE)){
            baseProduction = new Resource(30,0,0);
            this.baseConsuption = baseConsuption;
            productionMultiplier = pm;
            productionPower = pp; //per hour
            consuptionMultiplier = em;
            consuptionPower = ep;
        }else if(name.equals(Resources.CRYSTAL_MINE)){
            baseProduction = new Resource(0,15,0);
            this.baseConsuption = baseConsuption;
            productionMultiplier = 20;
            productionPower = pp;
            consuptionMultiplier = em;
            consuptionPower = ep;
        }else if(name.equals(Resources.DUETERIUM_SYNTHESIZER)){
            baseProduction = baseConsuption;
            this.baseConsuption = baseConsuption;
            productionMultiplier = 10;
            productionPower = pp;
            consuptionMultiplier = 20;
            consuptionPower = ep;
        }else if(name.equals(Resources.SOLAR_PLANT)){
            baseProduction = baseConsuption;
            productionMultiplier = 20;
            productionPower = pp;
        }else if(name.equals(Resources.FUSION_REACTOR)){
            baseProduction = baseConsuption;
            this.baseConsuption = baseConsuption;
            productionMultiplier = pm;
            productionPower = 0.01;
            consuptionPower = ep;
            consuptionMultiplier = 10;
        }else if(name.equals(Ship.SOLAR_SATELLITE))
            baseProduction = baseConsuption;
    }

    //if it's a ship then get the ship's name, else null
    private Ship getObjName(String s) {
        String[] split = s.split("\\/");
        try {
            return Ship.getShipByID(Integer.parseInt(split[1]),RESOURCE_DIR+split[0]);//.getName();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Buildable clone(){
        Buildable b = new Buildable();
        b.name = this.name;
        b.webName = this.webName;
        b.line = this.line;
        b.type = this.type;
        b.requires = this.requires;
        b.cssSelector = this.cssSelector;
        b.quickBuildLink = this.quickBuildLink;
        b.ref = this.ref;
        b.id = this.id;
        b.levelNeeded = this.levelNeeded;
        b.currentLevel = this.currentLevel;
        b.costMultiplier = this.costMultiplier;
        b.productionMultiplier = this.productionMultiplier;
        b.productionPower = this.productionPower;
        b.consuptionMultiplier = this.consuptionMultiplier;
        b.consuptionPower = this.consuptionPower;
        b.baseCost = this.baseCost;
        b.currentProduction = this.currentProduction;
        b.baseProduction = this.baseProduction;
        b.baseConsuption = this.baseConsuption;
        b.currentConsuption = this.currentConsuption;
        return b;
////        return new Buildable(this.line).setCurrentLevel(getCurrentLevel());
    }


    /*STATIC STUFF*/

    public static Map<String, Integer> getMissing(String goal, Map<String, Integer> current){
        return getMissing(getBuildableRequirements(goal), current);
    }

    public static Map<String, Integer> getMissing(Map<String, Integer> requirements, Map<String, Integer> current){
        HashMap<String, Integer> missingMap = new HashMap<String, Integer>();
        for(String requirement: requirements.keySet()){
            Integer cur = current.get(requirement);
            if(cur == null){
                cur = 0;
            }
            int missing = requirements.get(requirement) - cur;
            if(missing > 0){
                missingMap.put(requirement, missing);
            }
        }
        return missingMap;
    }
    public Buildable(){}

    private ExecutorService executor;
    private static HashMap<String,Integer> requirements;
    private static List<Buildable> buildables = new ArrayList<>();
    private static HashMap<String,List<Integer>> mappings = new HashMap<>();
    private Buildable(boolean b){
        try {
            loadFiles();

            executor = FileOptions.runConcurrentProcessNonBlocking((Callable)()->{sendBuildablesToDatabase(); return null; });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendBuildablesToDatabase() throws SQLException, IOException, ClassNotFoundException {
        Database d = Database.newDatabaseConnection();
        List<String> v = d.executeQuery("select id from buildable").stream().map(a -> a.get("id").toString()).collect(Collectors.toList());
        StringBuilder builder = new StringBuilder("");
        Buildable.getBuildableObjects().forEach(a->{
            if(!v.contains(a.getId()+""))
                builder.append("insert into buildable(id,buildable,type) values("+a.getId()+",'"+a.getName()+"','"+a.getType()+"'); ");
        });
        d.executeQuery(builder.toString());
    }

    private static Buildable instance;
    private static Buildable getInstance(){
        if(instance == null)
            instance = new Buildable(true);
        return instance;
    }
    private void loadFiles() throws IOException {
        addBuildingFromFile(BUILDING_INFO);
        addBuildingFromFile(RESEARCH_INFO);
        addBuildingFromFile(FACILITIES_INFO);
        addBuildingFromFile(SHIPYARD_INFO);
        addBuildingFromFile(DEFENSE_INFO);
    }private void addBuildingFromFile(String file) throws IOException {
        FileOptions.readFileIntoListString(file).stream().filter(a->!a.contains("ID")).forEach(a->buildables.add(new Buildable(a)));
    }
    public static HashMap<String,Integer> getBuildableRequirements(String buildableName){
        requirements = new HashMap<>();
        getAllRequirements(buildableName);
        return requirements;
    }private static void getAllRequirements(String buildable){
        for(Buildable b : getBuildableByName(buildable).getRequires()){
            if(!requirements.containsKey(b.getName()) ||
                    (requirements.containsKey(b.getName()) && requirements.get(b.getName()) < b.getLevelNeeded()))
                requirements.put(b.getName(),b.getLevelNeeded());
            getAllRequirements(b.getName());
        }
    }

    private Set<Buildable> facilitites, resources, research, shipyard, defense;

    private static Set<Buildable> getBuildableType(String type, Set<Buildable> buildables) throws IOException {
        if(buildables == null){
            buildables = new HashSet<>();
            List<Integer> fac = getMappings().get(type);
            buildables.addAll(
                    getBuildableObjects().stream()
                            .filter(a->a.getId() <= fac.get(1) && a.getId() >= fac.get(0))
                            .collect(Collectors.toList())
            );
        }
        return buildables;
    }

    public static Set<Buildable> getResources() throws IOException {
        return getBuildableType("resources",getInstance().resources);
    }
    public static Set<Buildable> getResearch() throws IOException {
        return getBuildableType("research",getInstance().research);
    }
    public static Set<Buildable> getShipyard() throws IOException {
        return getBuildableType("shipyard",getInstance().shipyard);
    }
    public static Set<Buildable> getDefense() throws IOException {
        return getBuildableType("defense",getInstance().defense);
    }
    public static Set<Buildable> getFacilities() throws IOException {
        return getBuildableType("facilities",getInstance().facilitites);
    }


    public static List<Buildable> getBuildableObjects(){
        return getInstance().buildables;
    }
    public static Buildable getBuildableByName(String name){
        return getBuildableObjects().stream().filter(a->name.equals(a.getName())).collect(Collectors.toList()).get(0).clone();
    } public static Buildable getBuildableByNameIgnoreCase(String name){
        return getBuildableObjects().stream().filter(a->name.equalsIgnoreCase(a.getName())).collect(Collectors.toList()).get(0).clone();
    }  public static Buildable getBuildableByID(int id) {
        return getBuildableObjects().stream().filter(a->a.getId() == id).collect(Collectors.toList()).get(0).clone();
    }public static String getType(String name) throws IOException{
        return getType(getBuildableByName(name).getId());
    }public static String getType(int id) throws IOException {
        for(String key : getMappings().keySet()){
            List<Integer> l = getMappings().get(key);
            Integer v1 = l.get(0), v2 = l.get(1);
            if(id <= v2 && id >= v1)
                return key;
        }
        return null;
    }public static HashMap<String, List<Integer>> getMappings() throws IOException {
        if(mappings.isEmpty()) {
            List<String> v = FileOptions.readFileIntoListString(MAPPINGS);
            for(String s : v){
                String[] split = s.split(",");
                mappings.put(split[0].toLowerCase(), Arrays.asList(Integer.parseInt(split[1]),Integer.parseInt(split[2])));
            }
        }
        return mappings;
    }
}
