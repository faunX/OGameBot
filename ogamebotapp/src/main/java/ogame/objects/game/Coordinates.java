package ogame.objects.game;

import utilities.database.Database;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jarndt on 5/8/17.
 */
public class Coordinates implements Comparable<Coordinates>{
    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
        String query = "" +
                "select * from planet where server_id = 135 and (coords,timestamp) in (\n" +
                "    select coords,max(timestamp)\n" +
                "    \tfrom planet where server_id = 135 and player_id in (\n" +
                "            select p.player_id from player p, player_highscore h\n" +
                "                where p.player_id = h.player_id and p.timestamp = h.player_t\n" +
                "                    and h.type = '0'\n" +
                "                    and status in ('I','i')\n" +
                "                    and p.server_id = 135\n" +
                "                    and p.timestamp = (select timestamp from player where server_id = 135 order by timestamp desc limit 1)\n" +
                "                    and score > 709/5 and score < 709 * 5\n" +
                "                order by h.position desc\n" +
                "    \t\t) group by coords)\n" +
                "order by coords;";

        Coordinates mainPlanet = new Coordinates("8:307:8");
        List<Map<String, Object>> v = Database.getExistingDatabaseConnection().executeQuery(query);
        if(v!=null && v.size() >0 && v.get(0)!=null && v.get(0).size()>0) {
            List<Coordinates> coords = v.stream().map(a -> new Coordinates(a.get("coords").toString())).collect(Collectors.toList());
            Collections.sort(coords,(a,b)->new Integer(a.getDistance(mainPlanet)).compareTo(b.getDistance(mainPlanet)));
//            System.out.println(coords.get(0));
            coords.forEach(System.out::println);
        }

//        Random r = new Random();
//        List<Coordinates> coordinates = new ArrayList<>();
//        for(int i = 0; i<100; i++)
//            coordinates.add(new Coordinates(r.nextInt(9)+1,r.nextInt(499)+1,r.nextInt(15)+1));
//        System.out.println(coordinates);
//        Collections.sort(coordinates);
//        System.out.println(coordinates);
    }

    private String universe;
    private int galaxy;
    private int system;
    private int planet;
    private int type = PLANET;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Coordinates that = (Coordinates) o;

        if (galaxy != that.galaxy) return false;
        if (system != that.system) return false;
        if (planet != that.planet) return false;
        if (type != that.type) return false;
        return universe != null ? universe.equals(that.universe) : that.universe == null;

    }

    @Override
    public int hashCode() {
        int result = universe != null ? universe.hashCode() : 0;
        result = 31 * result + galaxy;
        result = 31 * result + system;
        result = 31 * result + planet;
        result = 31 * result + type;
        return result;
    }

    public Coordinates(String universe, int galaxy, int system, int planet, int type) {
        this.universe = universe;
        this.galaxy = galaxy;
        this.system = system;
        this.planet = planet;
        this.type = type;


    }

    public int getType() {
        return type;
    }

    public Coordinates setType(int type) {
        this.type = type;
        return this;
    }

    public static final int PLANET = 0, MOON = 1, DEBRIS_FIELD = 2;

    public Coordinates(String coordinates) {
        coordinates = coordinates.replace("[","").replace("]","");
        String[] split = coordinates.split(":");
        galaxy = Integer.parseInt(split[0]);
        system = Integer.parseInt(split[1]);
        planet = Integer.parseInt(split[2]);
    }
    public Coordinates(String coordinates, String universe) {
        coordinates = coordinates.replace("[","").replace("]","");
        String[] split = coordinates.split(":");
        galaxy = Integer.parseInt(split[0]);
        system = Integer.parseInt(split[1]);
        planet = Integer.parseInt(split[2]);
        this.universe = universe;
    }

    public Coordinates(Coordinates coordinates) {
        this.universe = coordinates.getUniverse();
        this.galaxy = coordinates.getGalaxy();
        this.system = coordinates.getSystem();
        this.planet = coordinates.getPlanet();
        this.type = coordinates.getType();
    }

    public Coordinates clone(){
        return new Coordinates(this);
    }

    @Override
    public String toString() {
        return "{\"Coordinates\":{" +
                "\"universe\":\"" + universe + '\"' +
                ", \"galaxy\":" + galaxy +
                ", \"system\":" + system +
                ", \"planet\":" + planet +
                ", \"type\":" + type +
                "}}";
    }

    public Coordinates(String universe, int galaxy, int system, int planet) {

        this.universe = universe;
        this.galaxy = galaxy;
        this.system = system;
        this.planet = planet;
    }

    public Coordinates(int galaxy, int system, int planet) {
        this.galaxy = galaxy;
        this.system = system;
        this.planet = planet;
    }

    public String getUniverse() {
        return universe;
    }

    public Coordinates setUniverse(String universe) {
        this.universe = universe;
        return this;
    }
    public String getStringValue(){
        return galaxy+":"+system+":"+planet;
    }

    public int getGalaxy() {
        return galaxy;
    }

    public void setGalaxy(int galaxy) {
        this.galaxy = galaxy;
    }

    public int getSystem() {
        return system;
    }

    public void setSystem(int system) {
        this.system = system;
    }

    public int getPlanet() {
        return planet;
    }

    public void setPlanet(int planet) {
        this.planet = planet;
    }

    public String getCoordinates() {
        return galaxy+":"+system+":"+planet;
    }

    @Override
    public int compareTo(Coordinates o) {
        Coordinates first = new Coordinates(1,1,1);
        int v = Integer.compare(this.getDistance(first),o.getDistance(first));
        return v == 0 ? Integer.compare(this.getFullDistance(first),o.getFullDistance(first)) : v;
    }

    public int getDistance(Coordinates other){
        if(galaxy != other.galaxy)
            return getMinDistance(galaxy, other.galaxy, 9)*20000;
        if(system != other.system)
            return 2700 + getMinDistance(system, other.system, 499)*95;
        if(planet != other.planet)
            return 1000 + Math.abs(planet - other.planet)*5;
        return 5;
    }

    public int getFullDistance(Coordinates other){
        int v = 0;
        if(galaxy != other.galaxy)
            v+=getMinDistance(galaxy, other.galaxy, 9)*20000;
        if(system != other.system)
            v+= 2700 + getMinDistance(system, other.system, 499)*95;
        if(planet != other.planet)
            v+= 1000 + Math.abs(planet - other.planet)*5;
        return v+5;
    }

    public static int getTime(int distance, int speed){
        return 10 + (int)(3500*Math.sqrt(10*distance/speed));
    }

    public int getTime(Coordinates other, int speed){
        return Coordinates.getTime(getDistance(other), speed);
    }

    private int getMinDistance(int one, int two, int loop){
        int distance = Math.abs(one - two);
        return Math.min(distance, loop - distance);
    }

    public boolean isMoon() {
        return type == MOON;
    }
}