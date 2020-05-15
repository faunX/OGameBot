package ogame.objects.game;
import ogame.pages.Facilities;
import ogame.pages.Research;
import ogame.pages.Resources;
import org.apache.regexp.RE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

public class Resource implements Comparable<Resource> {

    public static void main(String[] args)throws Exception {
//        System.out.println(((6400-212)+(1920-63)*2+3200*3)/14.25);

//        Buildable r = Buildable.getBuildableByName(Facilities.ROBOTICS_FACTORY);
//        long cum = 0;
//        for (int i = 0; i < 10; i++) {
//            long dm = r.getNextLevelCost().getDarkMatterCost();
//            System.out.println((i+1)+": "+dm);
//            r.incrementLevel();
//            cum += dm;
//        }
//        System.out.println("Total: "+cum);
//        System.out.println(Buildable.getBuildableByName(Facilities.ROBOTICS_FACTORY).getLevelCost(5)
//                .subtract(new Resource(348,104,0)).getDarkMatterCost());

        System.out.println(Buildable.getBuildableByName(Ship.SMALL_CARGO).getLevelCost(3)
                .subtract(new Resource(2243,51,7)).getDarkMatterCost());
    }

    public static final String METAL = "Metal";
    public static final String CRYSTAL = "Crystal";
    public static final String DEUTERIUM = "Deuterium";
    public static final String ENERGY = "energy";

    public long metal = 0;
    public long crystal = 0;
    public long deuterium = 0;
    public long energy = 0;

    private static List<String> names;
    /**
     * implementation:
     *  ownedResources.getDarkMatterCost(wantResource)
     */
    double darkMatterFactor = 14.25;
    public long getDarkMatterCost(Resource r){
        Resource rr = r.subtract(this);
        long res = (long) Math.ceil(((rr.getMetal() < 0 ? 0 : rr.getMetal()) + (rr.getCrystal() < 0 ? 0 : rr.getCrystal()) * 2 + (rr.getDeuterium() < 0 ? 0 : rr.getDeuterium()) * 3) / darkMatterFactor);
        return res < 500 ? 500 : res;
    }

    /**
     * get dark matter cost of this resource
     * @return
     */
    public long getDarkMatterCost(){
        return new Resource().getDarkMatterCost(this);
    }

    public Resource() {
        this.metal = 0;
        this.crystal = 0;
        this.deuterium = 0;
    }

    public Resource(Resource initialResources) {
        this.metal = initialResources.metal;
        this.crystal = initialResources.crystal;
        this.deuterium = initialResources.deuterium;
        this.energy = initialResources.energy;
    }

//    public static Resource getCost(String name) throws IOException{
//        return getCost(name, 1);
//    }

    public static Resource getMonoResource(String type, int amount){
        if(type == METAL || type == Resources.METAL_MINE){
            return new Resource(amount,0,0);
        }
        if(type == CRYSTAL || type == Resources.CRYSTAL_MINE){
            return new Resource(0,amount,0);
        }
        if(type == DEUTERIUM || type == Resources.DUETERIUM_SYNTHESIZER){
            return new Resource(0,0,amount);
        }
        if(type.equals(ENERGY)){
            return new Resource(0,0,0,amount);
        }
        return null;
    }

    private static Resource getCumulativeCost(Resource base, int min, int max, double power){
        min--;
        Resource cost = base.multiply((Math.pow(power, max)-Math.pow(power, min))/(power-1));
        if(cost.energy > 0){
            cost.energy = (long) Math.ceil(base.energy*(max*Math.pow(1.1, max) - Math.pow(1.1, min)*min));
        }
        return cost;
    }

    private static int getIndexOf(String name){
        for(int i =0;i< names.size();i++){
            if(names.get(i).equals(name)){
                return i;
            }
        }
        return -1;

    }

    public static Resource[] convertCosts(long[] costs){
        Resource[] resources = new Resource[costs.length/4];
        for(int i=0;i<costs.length;i+=4){
            resources[i/4] = new Resource(costs[i], costs[i+1], costs[i+2], costs[i+3]);
        }
        return resources;
    }


    public Resource(long metal, long crystal, long deuterium){
        this.metal = metal;
        this.crystal = crystal;
        this.deuterium = deuterium;
    }public Resource(long metal, long crystal, long deuterium, long energy){
        this.metal = metal;
        this.crystal = crystal;
        this.deuterium = deuterium;
        this.energy = energy;
    }

    public long getTimeUntilCanAfford(double metalProduction, double crystalProduction, double deuteriumProduction){
        long tm = (long)(getMetal()/metalProduction), tc = (long)(getCrystal()/crystalProduction), td = (long)(getDeuterium()/deuteriumProduction);
        return Math.max(Math.max(tm,tc),td);
    }

    public Resource add(Resource other){
        return new Resource(metal + other.metal, crystal + other.crystal, deuterium + other.deuterium, energy + other.energy);
    }

    public Resource subtractR(Resource other){
        return new Resource(metal - other.metal, crystal - other.crystal, deuterium - other.deuterium, energy - other.energy);
    }public Resource subtract(Resource other){
        return new Resource(subNonZero(metal,other.metal), subNonZero(crystal,other.crystal), subNonZero(deuterium,other.deuterium), energy - other.energy);
    }
    private long subNonZero(long l1, long l2){
        long l3 = l1-l2;
        return l3 < 0 ? 0 : l3;
    }

    public Resource getDeficit(Resource goal){
        return goal.removeNegative().subtract(this.removeNegative()).removeNegative();
    }


    private Resource removeNegative(){
        return new Resource(Math.max(0, metal), Math.max(0, crystal), Math.max(0, deuterium), Math.max(0, energy));
    }

    public boolean isZero(){
        return metal == 0 && crystal == 0 && deuterium == 0 && energy == 0;
    }

    public boolean canAfford(Resource other){
        return other.getDeficit(this).isZero();
    }
    public boolean canAfford(long darkMatter, Resource resources) {
        if(canAfford(resources))
            return true;
        return this.subtract(resources).getDarkMatterCost() <= darkMatter;
    }

    public Resource multiply(int multiple){
        return multiply((double) multiple);
    }

    public Resource geometricPower(double multiplier, int nextLevel){
        return new Resource(
                (long)(metal*Math.pow(multiplier,nextLevel-1)),
                (long)(crystal*Math.pow(multiplier,nextLevel-1)),
                (long)(deuterium*Math.pow(multiplier,nextLevel-1)),
                (long)(energy*Math.pow(multiplier,nextLevel-1))
        );
    }


    public Resource multiply(double multiple) {
        return new Resource((long)(metal * multiple),(long)(crystal * multiple),(long)(deuterium * multiple), (long)(energy*multiple));
    }

    @Override
    public String toString() {
        return "Resource{" +
                "metal=" + metal +
                ", crystal=" + crystal +
                ", deuterium=" + deuterium +
                ", energy=" + energy +
                '}';
    }

    public long numAffordable(Resource cost){
        long metalAmount = cost.metal == 0? Long.MAX_VALUE : metal/cost.metal;
        long crystalAmount = cost.crystal == 0? Long.MAX_VALUE : crystal/cost.crystal;
        long deuteriumAmount = cost.deuterium == 0? Long.MAX_VALUE : deuterium/cost.deuterium;
        long energyAmount = cost.energy == 0? Long.MAX_VALUE : energy/cost.energy;
        return Math.min(energyAmount, Math.min(deuteriumAmount, Math.min(metalAmount, crystalAmount)));
    }

    public Resource clone(){
        return new Resource(metal, crystal, deuterium, energy);
    }

    private double multiplier = 2;

    public Resource setMultiplier(double multiplier){
        this.multiplier = multiplier;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Resource resource = (Resource) o;

        if (metal != resource.metal) return false;
        if (crystal != resource.crystal) return false;
        if (deuterium != resource.deuterium) return false;
        return energy == resource.energy;

    }

    @Override
    public int hashCode() {
        int result = (int) (metal ^ (metal >>> 32));
        result = 31 * result + (int) (crystal ^ (crystal >>> 32));
        result = 31 * result + (int) (deuterium ^ (deuterium >>> 32));
        result = 31 * result + (int) (energy ^ (energy >>> 32));
        return result;
    }

    public long getMetal() {
        return metal;
    }

    public void setMetal(long metal) {
        this.metal = metal;
    }

    public long getCrystal() {
        return crystal;
    }

    public void setCrystal(long crystal) {
        this.crystal = crystal;
    }

    public long getDeuterium() {
        return deuterium;
    }

    public void setDeuterium(long deuterium) {
        this.deuterium = deuterium;
    }

    public long getEnergy() {
        return energy;
    }

    public void setEnergy(long energy) {
        this.energy = energy;
    }

    public boolean lessThan(Resource r) {
        return metal < r.getMetal() && crystal < r.getCrystal() && deuterium < r.getDeuterium();
    }

    public Resource normalize() {
        return new Resource(metal == 0? metal : 1, crystal == 0? crystal:1, deuterium==0?deuterium:1,energy==0?energy:1);
    }

    public void setResources(Resource resources) {
        this.metal = resources.metal;
        this.crystal = resources.crystal;
        this.deuterium = resources.deuterium;
        this.energy = resources.energy;
    }

    public static double metalFactor = 1, crystalFactor = 2, deuteriumFactor = 3;
    @Override
    public int compareTo(Resource o) {
        Long l  = (long)(metal*metalFactor+crystal*crystalFactor+deuterium*deuteriumFactor),
             ll = (long)(o.metal*metalFactor+o.crystal*crystalFactor+o.deuterium*deuteriumFactor);
        return l.compareTo(ll);
    }

    public long getValue() {
        return (long) (getMetal()*metalFactor+getCrystal()*crystalFactor+getDeuterium()*deuteriumFactor);
    }

    public long getTotal(){
        return getMetal()+getCrystal()+getDeuterium();
    }
}
