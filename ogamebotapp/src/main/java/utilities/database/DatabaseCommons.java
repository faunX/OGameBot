package utilities.database;

import utilities.webdriver.DriverController;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Created by jarndt on 5/8/17.
 */
public class DatabaseCommons {

    public static int registerDriver(DriverController driverController) throws SQLException, IOException, ClassNotFoundException {
        Database d = new Database(Database.DATABASE,Database.USERNAME,Database.PASSWORD);
        List<Map<String, Object>> v = d.executeQuery("select * from webdriver where name = '" + driverController.getDriverName() + "'");
        if(v != null && v.size() == 1) {
            d.executeQuery("update webdriver set " +
                    "active = 'A', " +
                    "driver_type = '" + driverController.getDriverType() + "', " +
                    "proxy = '" + driverController.getProxy() + "', " +
                    "window_width = '" + (int) driverController.getWindowWidth() + "', " +
                    "window_height = '" + (int) driverController.getWindowHeight() + "', " +
                    "window_position_x = '" + (int) driverController.getWindowPositionX() + "', " +
                    "window_position_y = '" + (int) driverController.getWindowPositionY() + "' " +
                    "where id = '" + v.get(0).get("id") + "';");
            return Integer.parseInt(v.get(0).get("id").toString());
        }
        d.executeQuery("insert into WEBDRIVER(name,active,driver_type,proxy,window_width,window_height,window_position_x,window_position_y) " +
                "values('"+driverController.getDriverName()+"','A','"+driverController.getDriverType()+"'," +
                "'"+driverController.getProxy()+"','"+(int)driverController.getWindowWidth()+"','"+(int)driverController.getWindowHeight()+"'," +
                "'"+driverController.getWindowPositionX()+"','"+driverController.getWindowPositionY()+"');");

        v = d.executeQuery("select * from webdriver where name = '" + driverController.getDriverName() + "'");
        return Integer.parseInt(v.get(0).get("id").toString());
    }


    public static void deregisterDriver(DriverController driverController) throws SQLException, IOException, ClassNotFoundException {
        new Database(Database.DATABASE,Database.USERNAME,Database.PASSWORD)
            .executeQuery("update webdriver set active = 'N' where name = '" + driverController.getDriverName() + "'");
    }
}
