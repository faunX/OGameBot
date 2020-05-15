package utilities.database;

import bot.Bot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ogame.objects.game.Coordinates;
import ogame.objects.game.planet.Planet;
import ogame.pages.Research;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import utilities.data.XMLAPIDownloader;
import utilities.fileio.FileOptions;
import utilities.fileio.JarUtility;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Created by jarndt on 5/29/17.
 */
public class JsonPlanetData {
    public static void main(String[] args) throws IOException {
        new JsonPlanetData().dataToDatabase();
//        dataJson.forEach(System.out::println);
    }

    private static Scheduler scheduler;
    private static Scheduler getScheduler() throws SchedulerException {
        if(scheduler == null){
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
        }
        return scheduler;
    }
    public static void scheduleJob(String cronSchedule) throws SchedulerException {
        String group = "group3";
        if(getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(group)).stream().filter(a->a.getName().equals("dummyJobName")).collect(Collectors.toList()).size() != 0)
            return;
        JobDetail job = JobBuilder.newJob(JsonPlanetData.JsonToDatabaseJob.class)
                .withIdentity("dummyJobName", group).build();

        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity("dummyTriggerName", group)
                .withSchedule(
                        CronScheduleBuilder.cronSchedule(cronSchedule))
                .build();

        //schedule it
        getScheduler().scheduleJob(job, trigger);
    }
    public static class JsonToDatabaseJob implements Job{
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            System.out.println("Sending Json Data to Database");
            try {
                new JsonPlanetData().dataToDatabase();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Done sending json data to database");
        }
    }
    public static void startJsonToDatabaseThread() throws SchedulerException {
        scheduleJob("0 0/5 * * * ?"); //run download job every 1 minutes
    }



    private String dataDirectory;

    public JsonPlanetData(){
        dataDirectory = JarUtility.getResourceDir();
    }
    public JsonPlanetData(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public List<Bot> getBotData() throws IOException {
        return FileOptions.getAllFilesEndsWith(dataDirectory, "_data.json")
                .stream().map(a -> {
                    try {
                        return FileOptions.readFileIntoString(a.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).filter(a -> a != null)
                .map(a -> (Bot)new Gson().fromJson(a, new TypeToken<Bot>(){}.getType()))
                .collect(Collectors.toList());
    }

    public void dataToDatabase() throws IOException {
        FileOptions.runConcurrentProcess(getBotData().stream().map(a->(Callable)()->{
            try {
                StringBuilder builder = new StringBuilder("");
                builder.append(writeJSONData(a));
                builder.append(writePlanetsData(a));
                getConnection().executeQuery(builder.toString());
                builder = new StringBuilder("");
                builder.append(writeResearchData(a));
                builder.append(writeResourceData(a));
                builder.append(writeDefenseData(a));
                builder.append(writeFacilitiesData(a));
                builder.append(writeShipsData(a));
                getConnection().executeQuery(builder.toString());
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList()));
    }

    private Database d;
    private Database getConnection() throws SQLException, IOException, ClassNotFoundException {
        if(d == null)
            d = Database.newDatabaseConnection();
        return d;
    }


    private String writeJSONData(Bot a) throws SQLException, IOException, ClassNotFoundException {
        StringBuilder builder = new StringBuilder("");
        String botJson = new Gson().toJson(a);
        List<Map<String, Object>> v = getConnection().executeQuery("select * from JSON_DATA where bot_id = " + a.getId());
        if(v != null && v.size() > 0 && v.get(0) != null && v.get(0).size() > 0) {
            String json = v.get(0).get("json_data").toString();
            if(json.equals(botJson))
                return "";
            builder.append("update json_data set json_data = '"+botJson+"' where bot_id = "+a.getId()+" ; ");
        }else
            builder.append("insert into json_data(bot_id,json_data) values("+a.getId()+",'"+botJson+"') ON CONFLICT DO NOTHING;");
        return builder.toString();
    }
    private String writePlanetsData(Bot a) throws SQLException, IOException, ClassNotFoundException {
        String tableName = "bot_planets";
        StringBuilder builder = new StringBuilder("");
        HashMap<String, Planet> planets = a.getPlanets();
        List<String> planetNames = new ArrayList<>(planets.keySet()), removePlanetNames = new ArrayList<>();
        List<Map<String, Object>> v = getConnection().executeQuery("select * from bot_planets where ogame_user_id = " + a.getOgameUserId());
        if(v != null && v.size() > 0 && v.get(0) != null && v.get(0).size() > 0) {
            builder.append("");
            for(Map<String,Object> column : v){
                String name = column.get("coords").toString();
                if(planets.containsKey(name)){
                    //update the planet
                    Planet p = planets.get(name);
                    removePlanetNames.add(name);
                    builder.append("update "+tableName+" set name = '"+p.getPlanetName()+"', coords = '"+p.getCoordinates().getStringValue()+"'," +
                            " metal = "+p.getMetal()+", crystal = "+p.getCrystal()+", dueterium = "+p.getDueterium()+", solar_total = "+p.getEnergyProduction()+
                            ", solar_remaining = "+p.getEnergyAvailable()+", total_fields = "+p.getPlanetSize().getTotalFields()+", " +
                                    "available_fields = "+(p.getPlanetSize().getTotalFields()-p.getPlanetSize().getUsedFields())+", " +
                                    "min_temp = "+p.getPlanetSize().getMinTemp()+", max_temp = "+p.getPlanetSize().getMaxTemp()+"" +
                            " where ogame_user_id = "+a.getOgameUserId()+" and id = "+column.get("id")+"; "
                    );
                }else //delete the planet
                    builder.append("delete from "+tableName+" where ogame_user_id = "+a.getOgameUserId()+" and id = "+column.get("id")+";");
            }

        }
        planetNames.removeAll(removePlanetNames);
        //insert the remaining planets
        for(String name : planetNames) {
            Planet p = planets.get(name);
            builder.append("insert into "+tableName+"(ogame_user_id,name,coords,metal,crystal,dueterium,solar_total," +
                    "solar_remaining,total_fields,available_fields," +
                    "min_temp,max_temp) " +
                    "values("+a.getOgameUserId()+",'"+p.getPlanetName()+"','"+p.getCoordinates().getStringValue()+"',"+p.getMetal()+","+p.getCrystal()+","+p.getDueterium()+","+p.getEnergyProduction()+"," +
                    p.getEnergyAvailable()+","+p.getPlanetSize().getTotalFields()+","+(p.getPlanetSize().getTotalFields()-p.getPlanetSize().getUsedFields())+"," +
                    p.getPlanetSize().getMinTemp()+","+p.getPlanetSize().getMaxTemp()+") ON CONFLICT DO NOTHING; ");
        }

        return builder.toString();
    }

    private String writeResearchData(Bot a) throws SQLException, IOException, ClassNotFoundException {
        List<String> tableResearchNames = Arrays.asList(
                "ESPIONAGE_LEVEL","COMPUTER_LEVEL","WEAPON_LEVEL","SHIELDING_LEVEL",
                "ARMOUR_LEVEL","ENERGY_LEVEL","HYPERSPACE_LEVEL","COMBUSTION_D_LEVEL","IMPULSE_D_LEVEL",
                "HYPERSPACE_D_LEVEL","LASER_LEVEL","ION_LEVEL","PLASMA_LEVEL",
                "IRN_LEVEL","ASTROPHYSICS_LEVEL","GRAVITON_LEVEL"
        ),
        researchNames = Arrays.asList(
                "Espionage Technology","Computer Technology","Weapons Technology","Shielding Technology",
                "Armour Technology","Energy Technology","Hyperspace Technology","Combustion Drive","Impulse Drive",
                "Hyperspace Drive","Laser Technology","Ion Technology","Plasma Technology",
                "Intergalactic Research Network","Astrophysics","Graviton Technology"
        );
        StringBuilder builder = new StringBuilder("");
        HashMap<String, Integer> researches = a.getResearch();
        List<Map<String, Object>> v = getConnection().executeQuery("select * from research_data where ogame_user_id = " + a.getOgameUserId());
        if(v != null && v.size()>0 && v.get(0) != null && v.get(0).size() > 0){
            builder.append("update research_data set ");
            boolean diff = false;
            Map<String, Object> databaseLevels = v.get(0);
            for (int i = 0; i < tableResearchNames.size(); i++) {
                int actualLevel = researches.get(researchNames.get(i));
                int databaseLevel = (int)databaseLevels.get(tableResearchNames.get(i).toLowerCase());
                if(actualLevel!=databaseLevel){
                    diff = true;
                    builder.append(tableResearchNames.get(i)+" = "+actualLevel+", ");
                }
            }
            String update = builder.toString();
            builder = new StringBuilder(update.substring(0,update.length()-", ".length()));
            if(!diff)
                return "";

            builder.append(" where ogame_user_id = "+a.getOgameUserId()+" and id = "+databaseLevels.get("id")+"; ");
        }else {
            builder.append("insert into research_data(ogame_user_id,");
            StringBuilder values = new StringBuilder(" values("+a.getOgameUserId()+",");
            for (int i = 0; i < tableResearchNames.size(); i++) {
                String sep = (i == tableResearchNames.size()-1 ? "" : ",");
                builder.append(tableResearchNames.get(i)+sep);
                Integer value = researches.get(researchNames.get(i));
                values.append((value==null?0:value)+sep);
            }
            builder.append(") "+values.toString()+") ON CONFLICT DO NOTHING; ");
        }

        return builder.toString();
    }

    private String writeResourceData(Bot a) throws SQLException, IOException, ClassNotFoundException {
        List<String> resourceNamesDatabase = Arrays.asList(
                "METAL_MINE_LEVEL","CRYSTAL_MINE_LEVEL","DEUTERIUM_SYNTHESIZER_LEVEL","METAL_STORAGE_LEVEL",
                "CRYSTAL_STOREAGE_LEVEL","DEUTERIUM_TANK_LEVEL","FUSION_REACTOR_LEVEL","SOLAR_PLANET_LEVEL"
        );
        List<String> resourceNames = Arrays.asList(
                "Metal Mine","Crystal Mine","Deuterium Synthesizer","Metal Storage",
                "Crystal Storage","Deuterium Tank","Fusion Reactor","Solar Plant"
        );
        return queryBuilder(a,"RESOURCES_DATA",resourceNamesDatabase,resourceNames);
    }

    private String writeShipsData(Bot a) throws SQLException, IOException, ClassNotFoundException {
        List<String> shipsNamesDatabase = Arrays.asList(
                "SMALL_CARGO_SHIPS","LARGE_CARGO_SHIPS","LIGHT_FIGHTERS","HEAVY_FIGHTERS","CRUISERS","BATTLESHIPS",
                "BATTLECRUISERS","DESTROYERS","DEATHSTARS","BOMBERS","RECYCLERS","ESPIONAGE_PROBES","SOLAR_SATELLITES","COLONY_SHIPS"
        );
        List<String> shipsNames = Arrays.asList(
                "Small Cargo","Large Cargo","Light Fighter","Heavy Fighter","Cruiser","Battleship",
                "Battlecruiser","Destroyer","Deathstar","Bomber","Recycler","Espionage Probe","Solar Satellite","Colony Ship"
        );
        return queryBuilder(a,"SHIPS_DATA",shipsNamesDatabase,shipsNames);
    }

    private String writeFacilitiesData(Bot a) throws SQLException, IOException, ClassNotFoundException {
        List<String> facilitiesNamesDatabase = Arrays.asList(
                "ROBOTICS_FACTORY_LEVEL","RESEARCH_LAB_LEVEL","SHIPYARD_LEVEL","ALLIANCE_DEPOT_LEVEL","MISSILE_SILO_LEVEL",
                "NANITE_FACTORY_LEVEL","TERRAFORMER_LEVEL","LUNAR_BASE_LEVEL","SENSOR_PHALANX_LEVEL","JUMP_GATE_LEVEL"
        );
        List<String> facilitiesNames = Arrays.asList(
                "Robotics Factory","Research Lab","Shipyard","Alliance Depot","Missile Silo",
                "Nanite Factory","Terraformer","Space Dock","Lunar Base","Sensor Phalanx","Jump Gate"
        );
        return queryBuilder(a,"FACILITIES_DATA",facilitiesNamesDatabase,facilitiesNames);
    }

    private String writeDefenseData(Bot a) throws SQLException, IOException, ClassNotFoundException {
        List<String> defenseNamesDatabase = Arrays.asList(
                "ROCKET_LAUNCHERS","LIGHT_LASERS","HEAVY_LASERS","GAUSS_CANNONS","ION_CANNONS","PLASMA_TURRETS",
                "SMALL_SHIELD_DOME","LARGE_SHIELD_DOME","ANTI_BALLISTIC_MISSILES","INTERPLANETARY_MISSILES"
        );
        List<String> defenseNames = Arrays.asList(
                "Rocket Launcher","Light Laser","Heavy Laser","Gauss Cannon","Ion Cannon","Plasma Turret",
                "Small Shield Dome","Large Shield Dome","Anti-Ballistic Missiles","Interplanetary Missiles"
        );
        return queryBuilder(a,"DEFENSE_DATA",defenseNamesDatabase,defenseNames);
    }

    private String queryBuilder(Bot a, String tableName, List<String> databaseNames, List<String> codeNames) throws SQLException, IOException, ClassNotFoundException {
        StringBuilder builder = new StringBuilder("");
        List<Map<String, Object>> v = getConnection().executeQuery(
                "select * from "+tableName+" r, bot_planets b where r.BOT_PLANETS_ID = b.id and OGAME_USER_ID = "+a.getOgameUserId()
                //this query gets a list of planets and it's resources buildings data belonging to this user
        );
        HashMap<String, Planet> botPlanets = a.getPlanets();
        List<String> botPlanetNames = new ArrayList<>(botPlanets.keySet()),
                removeList  = new ArrayList<>();
        HashMap<String,Integer> planetIds = new HashMap<>();
        if(v != null && v.size() > 0 && v.get(0) != null && v.get(0).size() > 0) {
            v.forEach(b -> planetIds.put(b.get("name").toString(), (int) b.get("bot_planets_id")));
            for(Map<String, Object> planet : v){
                String dbPlanetName = planet.get("coords").toString();
                if(botPlanetNames.contains(dbPlanetName)){
                    removeList.add(dbPlanetName);
                    Planet currentPlanet = botPlanets.get(dbPlanetName);
                    HashMap<String, Integer> buildables = new HashMap<>();
                    buildables.putAll(currentPlanet.getBuildings());
                    buildables.putAll(currentPlanet.getFacilities());
                    buildables.putAll(currentPlanet.getDefense());
                    buildables.putAll(currentPlanet.getShips());

                    boolean diff = false;
                    StringBuilder updateStatement = new StringBuilder("update "+tableName+" set ");
                    for (int i = 0; i < databaseNames.size(); i++) {
                        int value = (Integer) planet.get(databaseNames.get(i).toLowerCase());
                        if(buildables.get(codeNames.get(i))==null)
                            continue;
                        int existing = buildables.get(codeNames.get(i));
                        if(value!=existing){
                            diff = true;
                            updateStatement.append(databaseNames.get(i)+" = "+existing+", ");
                        }
                    }
                    String update = updateStatement.toString();
                    updateStatement = new StringBuilder(update.substring(0,update.length()-", ".length()));
                    updateStatement.append(" where bot_planets_id = "+planet.get("id")+"; ");
                    if(diff)
                        builder.append(updateStatement.toString());
                }else //the planet isn't in the json, must mean the user deleted the planet.  Thus delete the planet from database
                    builder.append("delete from "+tableName+" where bot_planets_id = "+planet.get("id")+"; ");
            }
        }else {
            v = getConnection().executeQuery(
                    "select * from bot_planets where OGAME_USER_ID = " + a.getOgameUserId()
                    //this query gets a list of planets
            );
            v.forEach(b -> planetIds.put(b.get("coords").toString(), (int) b.get("id")));
        }

        botPlanetNames.removeAll(removeList);
        for(String planet : botPlanetNames){
            Planet currentPlanet = botPlanets.get(planet);
            HashMap<String, Integer> buildables = new HashMap<>();
            buildables.putAll(currentPlanet.getBuildings());
            buildables.putAll(currentPlanet.getFacilities());
            buildables.putAll(currentPlanet.getDefense());
            buildables.putAll(currentPlanet.getShips());

            StringBuilder   insert = new StringBuilder("insert into "+tableName+"(BOT_PLANETS_ID,"),
                    values = new StringBuilder(" values("+planetIds.get(planet)+",");
            int size = databaseNames.size() < codeNames.size() ? databaseNames.size() : codeNames.size();
            for (int i = 0; i < size; i++) {
                String sep = i == size - 1 ? "" : ",";
                insert.append(databaseNames.get(i)+sep);
                Integer vv = buildables.get(codeNames.get(i));
                values.append((vv==null?0:vv)+sep);
            }

            builder.append(insert.toString()+")"+values.toString()+") ON CONFLICT DO NOTHING; ");
        }

        return builder.toString();
    }

}
