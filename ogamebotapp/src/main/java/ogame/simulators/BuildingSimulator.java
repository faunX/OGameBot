package ogame.simulators;

import ogame.objects.game.Buildable;
import ogame.objects.game.Resource;
import ogame.pages.Facilities;
import ogame.pages.Research;
import ogame.pages.Resources;
import utilities.database.Database;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jarndt on 5/26/17.
 */
public class BuildingSimulator {
    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        List<Buildable> v = new BuildingSimulator().simulate();
//        v.forEach(System.out::println);
        Collections.reverse(v);
        StringBuilder b = new StringBuilder("");
        int i = 1;
        for(Buildable a : v){
            b.append("insert into profile(id,name,buildable_id,build_level,build_priority) " +
                    "   values(0,'farmingBot',"+a.getId()+","+a.getCurrentLevel()+","+(i++)+");");
        };
        Database.newDatabaseConnection().executeQuery(b.toString());

    }

    /*
    SUDO CODE:
        goal_buildings
        start():
            get starting resources
            for buildings you can build right now:
                if building is less than goal_buildings level:
                    tree.build_it()

        build_it():
            subtract required resources for it
            advance_timestep()

        advance_timestep():
            increase time by time it takes to build it.
            increase resources based off mine levels and time pasted

     */


    HashMap<String,Buildable> buildables = new HashMap<>(),
                                goalState = new HashMap<>();

    int temperature = 61;

    public BuildingSimulator() throws IOException {
        init();
    }public BuildingSimulator(List<Buildable> buildables) {
        buildables.forEach(a->this.buildables.put(a.getName(),a));
    }public BuildingSimulator(HashMap<String,Buildable> currentLevels) throws IOException {
        buildables.putAll(currentLevels);
    }private void init() throws IOException {
        Buildable.getBuildableObjects().stream().filter(a->!buildables.containsKey(a)).forEach(a->buildables.put(a.getName(),a.setCurrentLevel(0)));

        goalState.put(Resources.METAL_MINE,Buildable.getBuildableByName(Resources.METAL_MINE).setCurrentLevel(22));
        goalState.put(Resources.CRYSTAL_MINE,Buildable.getBuildableByName(Resources.CRYSTAL_MINE).setCurrentLevel(18));
        goalState.put(Resources.DUETERIUM_SYNTHESIZER,Buildable.getBuildableByName(Resources.DUETERIUM_SYNTHESIZER).setCurrentLevel(18));
        goalState.put(Facilities.ROBOTICS_FACTORY,Buildable.getBuildableByName(Facilities.ROBOTICS_FACTORY).setCurrentLevel(10));
    }
    public List<Buildable> simulate() {
        return simulate(new Resource(500,500,0));
    }public List<Buildable> simulate(Resource initialResources){
        Simulation simulation = new Simulation();
        _simulate(initialResources,buildables,goalState,0,simulation,0);
        Simulation bestTime = null;
        while (true) {
            List<Simulation> leaves = simulation.getRoot().getLeafNodes().stream().filter(a->a.depth==maxDepth).collect(Collectors.toList());
            Collections.sort(leaves, (a, b) -> {
                int res = new Long(a.endTime).compareTo(new Long(b.endTime));
                if(res != 0)
                    return res;
                return b.finalResources.compareTo(a.finalResources);
            });
            if(leaves.size() == 0) {
                List<Simulation> v = simulation.getRoot().getLeafNodes();
                Collections.sort(v,(a,b)->new Integer(b.depth).compareTo(a.depth));
                bestTime = v.get(0);
            }else
                bestTime = leaves.get(0);
            if(bestTime.done)
                break;
            maxDepth+=maxDepthInterval;
            _simulate(bestTime.finalResources, bestTime.finalBuildables, bestTime.goalState, bestTime.endTime, bestTime, ++bestTime.depth);
        }
        List<Simulation> leaves = bestTime.getParents().stream().filter(a->a.goal!=null).collect(Collectors.toList());
        Collections.sort(leaves,(a,b)->new Integer(a.order).compareTo(b.order));
//        leaves.forEach(a-> System.out.println("Build: "+a.goal.getName()+" level: "+a.goal.getCurrentLevel()));
//        System.out.println("Build: "+bestTime.goal.getName()+" level: "+bestTime.goal.getCurrentLevel());
//        System.out.println("TIME: "+bestTime.endTime);
        HashMap<Integer,Buildable> values = new HashMap<>();
        List<Buildable> buildables = new ArrayList<>();
        for (int i = 0; i < leaves.size(); i++)
            if(!values.containsValue(leaves.get(i).goal)) {
                values.put(i, leaves.get(i).goal);
                buildables.add(leaves.get(i).goal);
            }
        buildables.forEach(a-> System.out.println("Build: "+a.getName()+" level: "+a.getCurrentLevel()));
        System.out.println("Build: "+bestTime.goal.getName()+" level: "+bestTime.goal.getCurrentLevel());
        System.out.println("TIME: "+bestTime.endTime);
        return buildables;
    }

    int maxDepth = 14, maxDepthInterval = maxDepth;

    public class Simulation{
        private Resource initialResources, finalResources, initalProduction, finalProduction;
        private HashMap<String,Buildable> initialBuildables, finalBuildables, goalState;
        long startTime, endTime;
        Buildable goal;
        boolean done = false;
        int depth, order;

        Simulation parent;
        List<Simulation> children = new ArrayList<>();
        public Simulation(){}
        public Simulation(int depth, int order,Buildable goal,Resource initialResources, Resource finalResources, Resource initalProduction, HashMap<String, Buildable> initialBuildables, HashMap<String, Buildable> finalBuildables, HashMap<String, Buildable> goalState, long startTime, long endTime, Simulation parent) {
            this.goal = goal;
            this.order = order;
            this.depth = depth;
            this.initialResources = initialResources;
            this.finalResources = finalResources;
            this.initalProduction = initalProduction;
            this.initialBuildables = initialBuildables;
            this.finalBuildables = finalBuildables;
            this.goalState = goalState;
            this.startTime = startTime;
            this.endTime = endTime;
            this.parent = parent;
        }

        public List<Simulation> getLeafNodes() {
            List<Simulation> leaves = new ArrayList<>();
            return _getLeafNodes(leaves,this);
        }

        private List<Simulation> _getLeafNodes(List<Simulation> leaves, Simulation simulation) {
            if(simulation.children.size() == 0)
                leaves.add(simulation);
            for(Simulation s : simulation.children)
                _getLeafNodes(leaves,s);
            return leaves;
        }

        public List<Simulation> getParents(){
            List<Simulation> leaves = new ArrayList<>();
            return _getParents(leaves,this);
        }

        private List<Simulation> _getParents(List<Simulation> leaves, Simulation simulation) {
            if(simulation.parent==null)
                return leaves;
            leaves.add(simulation.parent);
            return _getParents(leaves,simulation.parent);
        }

        public Simulation getRoot() {
            if(this.parent == null)
                return this;
            return this.parent.getRoot();
        }
    }

    public void _simulate(Resource initialResources,
                          HashMap<String, Buildable> buildables,
                          HashMap<String, Buildable> goalState,
                          long time,
                          Simulation simulation,
                          int depth){
        //want to minimize time to get to goal state
        HashMap<String, Buildable> newGoalState = getDiff(buildables,goalState);
        if(newGoalState.size() == 0)
            simulation.done = true;


        for(String goal : newGoalState.keySet()){
            long totalTime = time;
            HashMap<String,Buildable> newBuildables = new HashMap<>();
            buildables.forEach((a,b)->newBuildables.put(a,b.clone()));

            Resource resources = new Resource(initialResources);

            Buildable b = newBuildables.get(goal);
            Resource cost = b.getNextLevelCost();

            Buildable   mm = newBuildables.get(Resources.METAL_MINE),
                        cm = newBuildables.get(Resources.CRYSTAL_MINE),
                        ds = newBuildables.get(Resources.DUETERIUM_SYNTHESIZER),
                        sp = newBuildables.get(Resources.SOLAR_PLANT),
                        fr = newBuildables.get(Resources.FUSION_REACTOR),
                        ms = newBuildables.get(Resources.METAL_STORAGE),
                        cs = newBuildables.get(Resources.CRYSTAL_STORAGE),
                        dt = newBuildables.get(Resources.DUETERIUM_TANK);

            long    solarConsumption = mm.getCurrentConsuption().add(cm.getCurrentConsuption()).add(ds.getCurrentConsuption()).getEnergy(),
                    solarTotal       = sp.getCurrentProduction()
                                        .add(fr.getCurrentProduction(temperature,newBuildables.get(Research.ENERGY).getCurrentLevel())).getEnergy(),
                    maxMetal         = Resources.getStorageCapacityLevel(ms.getCurrentLevel()),
                    maxCrystal       = Resources.getStorageCapacityLevel(cs.getCurrentLevel()),
                    maxDueterium     = Resources.getStorageCapacityLevel(dt.getCurrentLevel());

            double d = solarTotal/(solarConsumption == 0?1:(double)solarConsumption),
                    productionFactor = d > 1 ? 1 : d;


            double  metalProduction        = productionFactor*mm.getCurrentProduction().getMetal(), //metal/second
                    crystalProduction      = productionFactor*cm.getCurrentProduction().getCrystal(),
                    dueteriumProduction    = productionFactor*ds.getCurrentProduction(temperature).getDeuterium()
                                                - fr.getCurrentConsuption().getDeuterium();
            Resource production = new Resource((long)metalProduction,(long)crystalProduction,(long)dueteriumProduction);

            //build solar plant
            if(productionFactor < .8) {
                totalTime += build(sp, resources, production,newBuildables);
                Simulation simChild = new Simulation(++simulation.order,depth,sp,initialResources, resources, production, buildables, newBuildables, goalState, time, totalTime, simulation);
                simulation.children.add(simChild);
                simulation.finalBuildables = null;
                simulation.initialBuildables = null;
                simulation = simChild;
            }
            //build storage if you need it
            if(cost.getMetal() >= maxMetal) {
                totalTime += build(ms, resources, production, newBuildables);
                Simulation simChild = new Simulation(++simulation.order,depth,ms,initialResources, resources, production, buildables, newBuildables, goalState, time, totalTime, simulation);
                simulation.children.add(simChild);
                simulation.finalBuildables = null;
                simulation.initialBuildables = null;
                simulation = simChild;
            }if(cost.getCrystal() >= maxCrystal) {
                totalTime += build(cs, resources, production, newBuildables);
                Simulation simChild = new Simulation(++simulation.order,depth,cs,initialResources, resources, production, buildables, newBuildables, goalState, time, totalTime, simulation);
                simulation.children.add(simChild);
                simulation.finalBuildables = null;
                simulation.initialBuildables = null;
                simulation = simChild;
            }if(cost.getDeuterium() >= maxDueterium) {
                totalTime += build(dt, resources, production, newBuildables);
                Simulation simChild = new Simulation(++simulation.order,depth,dt,initialResources, resources, production, buildables, newBuildables, goalState, time, totalTime, simulation);
                simulation.children.add(simChild);
                simulation.finalBuildables = null;
                simulation.initialBuildables = null;
                simulation = simChild;
            }

            totalTime+=build(b,resources,production, newBuildables);
            Simulation simChild = new Simulation(++simulation.order,depth,b,initialResources, resources, production, buildables, newBuildables, goalState, time, totalTime, simulation);
            simulation.children.add(simChild);
            simulation.finalBuildables = null;
            simulation.initialBuildables = null;
            if(depth < maxDepth)
                _simulate(resources,newBuildables,newGoalState,totalTime,simChild,++depth);
        }
    }
    private double build(Buildable ms, Resource resources, Resource production, HashMap<String, Buildable> newBuildables){
        Resource msNLCost = ms.getNextLevelCost();

        double addTime = 0;
        if(resources.lessThan(msNLCost)){ //can't afford right now, wait until you can afford
            //add time it takes to get enough resources
            long    mt = msNLCost.getMetal()        - resources.metal,
                    ct = msNLCost.getCrystal()      - resources.crystal,
                    ddt = msNLCost.getDeuterium()   - resources.deuterium;
            //m = x*m/s => m/(m/s) = x
            double  metalTime       = mt/(double)production.getMetal(),
                    crystalTime     = ct/(double)production.getCrystal(),
                    duetTime        = ddt/(double)(production.getDeuterium() == 0?Double.MIN_VALUE:production.getDeuterium());

            addTime = Math.max(Math.max(metalTime,crystalTime),duetTime);

            resources.setResources(resources.add(new Resource((long)(production.getMetal()*addTime),(long)(production.getCrystal()*addTime),(long)(production.getDeuterium()*addTime))));
        }
        resources.setResources(resources.subtract(msNLCost));
        ms.incrementLevel();
        addTime += ms.getCurrentBuildTime(newBuildables.get(Facilities.ROBOTICS_FACTORY).getCurrentLevel(),newBuildables.get(Facilities.NANITE_FACTORY).getCurrentLevel());
        return addTime;
    }

    private HashMap<String, Buildable> getDiff(HashMap<String, Buildable> buildables, HashMap<String, Buildable> goalState) {
        HashMap<String,Buildable> diff = (HashMap<String, Buildable>) goalState.clone();
        goalState.forEach((a,b)->{
            if(buildables.containsKey(a) && buildables.get(a).getCurrentLevel() >= b.getCurrentLevel())
               diff.remove(a);
        });
        return diff;
    }
}
