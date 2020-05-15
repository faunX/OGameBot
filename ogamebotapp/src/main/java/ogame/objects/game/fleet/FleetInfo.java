package ogame.objects.game.fleet;

import java.io.IOException;
import java.util.*;

/**
 * Created by jarndt on 5/10/17.
 */
public class FleetInfo {
    private int fleetsTotal, fleetsUsed, fleetsRemaining, expeditionsUsed, expeditionsTotal;
    private Set<FleetObject> fleets = new HashSet<>();

    public FleetInfo(){}

    public void addFleet(FleetObject fleet){
        fleets.add(fleet);
    }
    public void removeFleet(FleetObject fleet){
        fleets.remove(fleet);
    }

    public int getFleetsTotal() {
        return fleetsTotal;
    }

    public void setFleetsTotal(int fleetsTotal) {
        this.fleetsTotal = fleetsTotal;
    }

    public int getFleetsUsed() {
        return fleetsUsed;
    }

    public void setFleetsUsed(int fleetsUsed) {
        this.fleetsUsed = fleetsUsed;
    }

    public int getFleetsRemaining() {
        return getFleetsTotal()-getFleetsUsed();
    }

    public void setFleetsRemaining(int fleetsRemaining) {
        this.fleetsRemaining = fleetsRemaining;
    }

    public List<FleetObject> getFleets() {
        return new ArrayList<>(fleets);
    }

    public void setFleets(Set<FleetObject> fleets) {
        this.fleets = fleets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FleetInfo fleetInfo = (FleetInfo) o;

        if (fleetsTotal != fleetInfo.fleetsTotal) return false;
        if (fleetsUsed != fleetInfo.fleetsUsed) return false;
        if (fleetsRemaining != fleetInfo.fleetsRemaining) return false;
        return fleets != null ? fleets.equals(fleetInfo.fleets) : fleetInfo.fleets == null;
    }

    @Override
    public int hashCode() {
        int result = fleetsTotal;
        result = 31 * result + fleetsUsed;
        result = 31 * result + fleetsRemaining;
        result = 31 * result + (fleets != null ? fleets.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FleetInfo{" +
                "fleetsTotal=" + fleetsTotal +
                ", fleetsUsed=" + fleetsUsed +
                ", fleetsRemaining=" + fleetsRemaining +
                ", fleets=" + fleets +
                '}';
    }

}
