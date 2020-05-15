package ogame.objects.game.planet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Created by jarndt on 5/10/17.
 */
public class PlanetProperties {
    private int totalFields, usedFields, size, minTemp, maxTemp;

    public PlanetProperties(int totalFields, int usedFields, int size, int minTemp, int maxTemp) {
        this.totalFields = totalFields;
        this.usedFields = usedFields;
        this.size = size;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
    }

    public PlanetProperties() {}

    public int getTotalFields() {
        return totalFields;
    }

    public void setTotalFields(int totalFields) {
        this.totalFields = totalFields;
    }

    public int getUsedFields() {
        return usedFields;
    }

    public void setUsedFields(int usedFields) {
        this.usedFields = usedFields;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getMinTemp() {
        return minTemp;
    }

    public void setMinTemp(int minTemp) {
        this.minTemp = minTemp;
    }

    public int getMaxTemp() {
        return maxTemp;
    }

    public void setMaxTemp(int maxTemp) {
        this.maxTemp = maxTemp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlanetProperties that = (PlanetProperties) o;

        if (totalFields != that.totalFields) return false;
        if (usedFields != that.usedFields) return false;
        if (size != that.size) return false;
        if (minTemp != that.minTemp) return false;
        return maxTemp == that.maxTemp;
    }

    @Override
    public int hashCode() {
        int result = totalFields;
        result = 31 * result + usedFields;
        result = 31 * result + size;
        result = 31 * result + minTemp;
        result = 31 * result + maxTemp;
        return result;
    }

    @Override
    public String toString() {
        return "PlanetProperties{" +
                "totalFields=" + totalFields +
                ", usedFields=" + usedFields +
                ", size=" + size +
                ", minTemp=" + minTemp +
                ", maxTemp=" + maxTemp +
                '}';
    }

    public static PlanetProperties parsePlanetProperties(String html){
        return parsePlanetProperties(Jsoup.parse(html));
    }
    public static PlanetProperties parsePlanetProperties(Document html) {
        return null;
    }
    public static PlanetProperties OLD_parsePlanetProperties(Document html){
        String diameterContentField = html.select("#diameterContentField").get(0).text();
        String[] split = diameterContentField.split("km \\(");
        int size = Integer.parseInt(split[0].replace(".",""));
        String[] subSplit = split[1].split("/");
        int usedFields = Integer.parseInt(subSplit[0]);
        int totalFields = Integer.parseInt(subSplit[1].replace(")",""));

        String temperatureContentField = html.select("#temperatureContentField").get(0).text();
        String degree = "°";
        String[] tempSplit = temperatureContentField.split(degree+"C to ");
        int minTemp = Integer.parseInt(tempSplit[0]);
        int maxTemp = Integer.parseInt(tempSplit[1].replace(degree+"C",""));

        return new PlanetProperties(totalFields,usedFields,size,minTemp,maxTemp);
    }
}
