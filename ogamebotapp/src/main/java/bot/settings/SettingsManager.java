package bot.settings;

import ogame.objects.game.planet.Planet;
import utilities.database.Database;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jarndt on 6/9/17.
 */
public class SettingsManager {
    public static final String
            DELETE_MESSAGES                 = "DELETE_MESSAGES".toLowerCase(),
            AUTO_BUILD_METAL_STORAGE        = "AUTO_BUILD_METAL_STORAGE".toLowerCase(),
            AUTO_BUILD_CRYSTAL_STORAGE      = "AUTO_BUILD_CRYSTAL_STORAGE".toLowerCase(),
            AUTO_BUILD_DEUTERIUM_STORAGE    = "AUTO_BUILD_DEUTERIUM_STORAGE".toLowerCase(),
            AUTO_BUILD_SOLAR                = "AUTO_BUILD_SOLAR".toLowerCase(),
            AUTO_BUILD_SOLAR_PERCENT        = "AUTO_BUILD_SOLAR_PERCENT".toLowerCase(),
            AUTO_BUILD_ESPIONAGE_PROBES     = "AUTO_BUILD_ESPIONAGE_PROBES".toLowerCase(),
            AUTO_BUILD_SMALL_CARGOS         = "AUTO_BUILD_SMALL_CARGOS".toLowerCase(),
            AUTO_BUILD_LARGE_CARGOS         = "AUTO_BUILD_LARGE_CARGOS".toLowerCase(),
            SIMULATE_QUEUE_ON_EMPTY         = "SIMULATE_QUEUE_ON_EMPTY".toLowerCase();

    public static final List<String> SETTINGS = Arrays.asList(
            DELETE_MESSAGES, AUTO_BUILD_CRYSTAL_STORAGE,AUTO_BUILD_DEUTERIUM_STORAGE,AUTO_BUILD_METAL_STORAGE,
            AUTO_BUILD_SOLAR,AUTO_BUILD_SOLAR_PERCENT,SIMULATE_QUEUE_ON_EMPTY,AUTO_BUILD_ESPIONAGE_PROBES,AUTO_BUILD_SMALL_CARGOS,
            AUTO_BUILD_LARGE_CARGOS
    );


    private transient Database d;
    private Database getDatabase() throws SQLException, IOException, ClassNotFoundException {
        if(d == null)
            d = Database.newDatabaseConnection();
        return d;
    }

    private HashMap<String,String> settings;
    private transient Planet planet;
    private String planetId;

    public SettingsManager(Planet planet) {
        this.planet = planet;
        this.planetId = planet.getBotPlanetID();
    }

    public HashMap<String, String> getSettings() throws SQLException, IOException, ClassNotFoundException {
        if(settings == null)
            settings = new HashMap<>();
        List<Map<String, Object>> v = getDatabase().executeQuery("select * from config where bot_planets_id = " + planetId);
        if(v != null && v.size() > 0 && v.get(0) != null && v.get(0).size() > 0)
            v.get(0).forEach((a, b)->settings.put(a,b!=null?b.toString():null));
        fillInDefaults();
        return settings;
    }

    private void fillInDefaults() {
        HashMap<String,String> defaults = new HashMap<>();
        defaults.put(DELETE_MESSAGES,"true");
        defaults.put(AUTO_BUILD_METAL_STORAGE,"false");
        defaults.put(AUTO_BUILD_CRYSTAL_STORAGE,"false");
        defaults.put(AUTO_BUILD_DEUTERIUM_STORAGE,"false");
        defaults.put(AUTO_BUILD_SOLAR,"false");
        defaults.put(AUTO_BUILD_SOLAR_PERCENT,"80");
        defaults.put(SIMULATE_QUEUE_ON_EMPTY,"false");
        defaults.put(AUTO_BUILD_ESPIONAGE_PROBES,"true");
        defaults.put(AUTO_BUILD_SMALL_CARGOS,"true");
        defaults.put(AUTO_BUILD_LARGE_CARGOS,"true");

        SETTINGS.forEach(a->{
            if(!(settings.containsKey(a) && settings.get(a) != null))
                settings.put(a,defaults.get(a));
        });
    }

    public void setSettings(HashMap<String, String> settings) {
        this.settings = settings;
    }

    public String getPlanetId() {
        return planetId;
    }

    public void setPlanetId(String planetId) {
        this.planetId = planetId;
    }

    public SettingsManager setPlanet(Planet planet) {
        this.planet = planet;
        return this;
    }
}
