package bot.queue;

import bot.settings.SettingsManager;
import ogame.objects.game.BuildTask;
import ogame.objects.game.Buildable;
import ogame.objects.game.planet.Planet;
import ogame.pages.Resources;
import ogame.simulators.BuildingSimulator;
import utilities.database.Database;
import utilities.fileio.FileOptions;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Created by jarndt on 5/31/17.
 */
public class QueueManager {
//    public static int MAX_METAL = 22, MAX_CRYSTAL = 18, MAX_DEUTERIUM = 18, MAX_ROBOTICS = 10;

    private String planetId;
    private transient Planet planet;

    private transient List<BuildTask> queue = new ArrayList<>();
    private transient HashMap<String,Integer> research;

    public QueueManager(Planet planet,HashMap<String,Integer> research) {
        //TODO auto build;
        this.planet = planet;
        this.planetId = planet.getBotPlanetID();
        this.research = research;
    }

    private transient Database d;
    private Database getDatabase() throws SQLException, IOException, ClassNotFoundException {
        if(d == null)
            d = Database.newDatabaseConnection();
        return d;
    }

    public QueueManager setResearch(HashMap<String,Integer> research){
        this.research = research;
        return this;
    }

    public String getPlanetId() {
        return planetId;
    }

    public void setPlanetId(String planetId) {
        this.planetId = planetId;
    }

    public List<BuildTask> getQueue(int id) throws SQLException, IOException, ClassNotFoundException {
        List<Map<String, Object>> vv = getDatabase().executeQuery(
                "select p.id p_id,b.id b_id,* from bot_profile b, profile p \n" +
                        "\twhere b.profile_id = p.id \n" +
                        "    \t\tand done = 'N' \n" +
                        "    \t\tand bot_id = '"+id+"'\n" +
                        "            and (p.buildable_id,p.build_level) \n" +
                        "            \tnot in \n" +
                        "                \t(select buildable_id, build_level from planet_queue);"
        );
        List<Map<String, Object>> v = getDatabase().executeQuery(
                "select * from planet_queue where done = 'N' and bot_planets_id = " + planetId+"" +
                        " order by build_priority desc, build_timestamp;"
        );
        List<BuildTask> profile = new ArrayList<>(), queueList = new ArrayList<>();
        if(vv != null && vv.size() > 0 && vv.get(0) != null && vv.get(0).size() > 0)
            profile = vv.stream()
                    .map(a-> new BuildTask(a).setBuildPriority((int)a.get("build_priority")+(int)(a.get("priority"))))
                    .collect(Collectors.toList());


        if(v != null && v.size() > 0 && v.get(0) != null && v.get(0).size() > 0)
            queueList = v.stream().map(a -> new BuildTask(a)).collect(Collectors.toList());

        List<BuildTask> diff = new ArrayList<>(profile);
        diff.removeAll(queueList);
        insertBuildTasks(diff);

        Set<BuildTask> allQueue = new HashSet<>();
        allQueue.addAll(queueList);
        allQueue.addAll(profile);
        queue = new ArrayList<>();
        queue.addAll(allQueue);

        autoBuild();

        return queue;
    }

    public int getMaxPriority(){
        ArrayList<BuildTask> v = (ArrayList<BuildTask>) new ArrayList<>(queue).clone();
        Collections.sort(v,(a,b)->new Integer(b.getBuildPriority()).compareTo(a.getBuildPriority()));
        return v.size() == 0 ? 0 : v.get(0).getBuildPriority();
    }

//    public Planet getPlanet() {
//        if(planet == null)
//            planet = b
//        return planet;
//    }

    private void autoBuild() throws SQLException, IOException, ClassNotFoundException {
        List<BuildTask> buildTasks = new ArrayList<>();
        if(queue.isEmpty() &&
            planet.getSetting(SettingsManager.SIMULATE_QUEUE_ON_EMPTY,planetId).equalsIgnoreCase("true"))
            buildTasks.addAll(simulateQueue());
        if(planet.getSetting(SettingsManager.AUTO_BUILD_METAL_STORAGE,planetId).equalsIgnoreCase("true")
                && (planet.getMetalStorageString() == null || (planet.getMetalStorageString() != null && !planet.getMetalStorageString().isEmpty())))
            buildTasks.add(
                    new BuildTask().setBuildable(
                            Buildable.getBuildableByName(Resources.METAL_STORAGE)
                                    .setCurrentLevel(planet.getBuildable(Resources.METAL_STORAGE).getCurrentLevel()+1)
                    ).setBuildPriority(getMaxPriority()+1)
                    .setCountOrLevel(planet.getBuildable(Resources.METAL_STORAGE).getCurrentLevel()+1)
            );
        if(planet.getSetting(SettingsManager.AUTO_BUILD_CRYSTAL_STORAGE,planetId).equalsIgnoreCase("true")
                && (planet.getCrystalStorageString() == null || (planet.getCrystalStorageString() != null && !planet.getCrystalStorageString().isEmpty())))
            buildTasks.add(
                    new BuildTask().setBuildable(
                        Buildable.getBuildableByName(Resources.CRYSTAL_STORAGE)
                                .setCurrentLevel(planet.getBuildable(Resources.CRYSTAL_STORAGE).getCurrentLevel()+1)
                    ).setBuildPriority(getMaxPriority()+1)
                    .setCountOrLevel(planet.getBuildable(Resources.CRYSTAL_STORAGE).getCurrentLevel()+1)
            );
        if(planet.getSetting(SettingsManager.AUTO_BUILD_DEUTERIUM_STORAGE,planetId).equalsIgnoreCase("true")
                && (planet.getDueteriumStorageString() == null || (planet.getDueteriumStorageString() != null && !planet.getDueteriumStorageString().isEmpty())))
            buildTasks.add(
                    new BuildTask().setBuildable(
                            Buildable.getBuildableByName(Resources.DUETERIUM_TANK)
                                    .setCurrentLevel(planet.getBuildable(Resources.DUETERIUM_TANK).getCurrentLevel()+1)
                    ).setBuildPriority(getMaxPriority()+1)
                    .setCountOrLevel(planet.getBuildable(Resources.DUETERIUM_TANK).getCurrentLevel()+1)
            );
        if(planet.getSetting(SettingsManager.AUTO_BUILD_SOLAR,planetId).equalsIgnoreCase("true")
                && planet.getEnergyPercent()*100 < Integer.parseInt(planet.getSetting(SettingsManager.AUTO_BUILD_SOLAR_PERCENT,planetId)))
            buildTasks.add(
                    new BuildTask().setBuildable(
                            Buildable.getBuildableByName(Resources.SOLAR_PLANT)
                                    .setCurrentLevel(planet.getBuildable(Resources.SOLAR_PLANT).getCurrentLevel()+1)
                    ).setBuildPriority(getMaxPriority()+1)
                    .setCountOrLevel(planet.getBuildable(Resources.SOLAR_PLANT).getCurrentLevel()+1)
            );

        if(buildTasks.size() > 0)
            FileOptions.runConcurrentProcessNonBlocking((Callable)()->{insertBuildTasks(buildTasks); return null;});
    }

    private void insertBuildTasks(List<BuildTask> diff) throws SQLException, IOException, ClassNotFoundException {
        StringBuilder builder = new StringBuilder("");
        for(BuildTask b : diff)
            builder.append("insert into planet_queue(bot_planets_id,buildable_id,build_level,build_priority) " +
                    "   values("+planetId+","+b.getBuildable().getId()+","+b.getCountOrLevel()+","+b.getBuildPriority()+") ON CONFLICT DO NOTHING;");
        if(!builder.toString().isEmpty())
            getDatabase().executeQuery(builder.toString());
    }

    private List<BuildTask> simulateQueue(){
        try {
            HashMap<String, Buildable> q = planet.getAllBuildables();
            research.forEach((a,b)->q.put(a,Buildable.getBuildableByName(a).setCurrentLevel(b)));
            List<Buildable> v = new BuildingSimulator(q).simulate();
            Collections.reverse(v);
            List<BuildTask> buildTasks = new ArrayList<>();
            for (int i = 0; i < v.size(); i++)
                buildTasks.add(new BuildTask().setBuildable(v.get(i)).setBuildPriority(i));
            setQueue(buildTasks);
            return buildTasks;
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setQueue(List<BuildTask> queue) {
        this.queue = queue;
    }


    public QueueManager setPlanet(Planet planet) {
        this.planet = planet;
        return this;
    }

    @Override
    public String toString() {
        return "QueueManager{" +
                "planetId='" + planetId + '\'' +
                ", queue=" + queue +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueueManager that = (QueueManager) o;

        if (planetId != null ? !planetId.equals(that.planetId) : that.planetId != null) return false;
        return queue != null ? queue.equals(that.queue) : that.queue == null;
    }

    @Override
    public int hashCode() {
        int result = planetId != null ? planetId.hashCode() : 0;
        result = 31 * result + (queue != null ? queue.hashCode() : 0);
        return result;
    }
}
