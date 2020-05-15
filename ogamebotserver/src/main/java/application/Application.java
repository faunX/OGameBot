package application;

import org.quartz.SchedulerException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import runner.Runner;
import utilities.FileOptions;
import utilities.database.Database;
import utilities.fileio.JarUtility;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jarndt on 4/13/17.
 */
@SpringBootApplication
@EnableScheduling
public class Application {
    public static void main(String[] args) throws Exception {
        FileOptions.setLogger(FileOptions.DEFAULT_LOGGER_STRING);
//        Database.DATABASE_TYPE = Database.HSQL;
        Runner.run(args);
        SpringApplication.run(Application.class,args);
    }

    public static final String STATIC_DIR = FileOptions.cleanFilePath(FileOptions.DEFAULT_DIR+"/ogamebotserver/src/main/resources/static");

    public static void init() throws IOException, InterruptedException {
        File bowerComponents = new File(FileOptions.cleanFilePath(STATIC_DIR+"/bower_components"));
        if(!(bowerComponents.exists() && bowerComponents.isDirectory())) {
            FileOptions.runSystemProcess("./bower.sh", STATIC_DIR);
            System.out.println("Bower Components installed.  Please rerun Application");
            System.exit(0);
        }
    }
}
