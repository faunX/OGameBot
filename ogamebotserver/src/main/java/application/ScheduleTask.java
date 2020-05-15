package application;

import org.apache.commons.codec.binary.Base64;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import utilities.webdriver.DriverController;
import utilities.webdriver.DriverControllerBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * Created by jarndt on 5/8/17.
 */
@Service
public class ScheduleTask {
    DriverController driverController;
    List<String> webpages = Arrays.asList(
            "http://google.com",
            "http://stackoverflow.com",
            "https://www.w3schools.com/angular/ng_ng-src.asp"
    );
    public ScheduleTask(){
        driverController = new DriverControllerBuilder().setName("Application Bot").build();
        new Thread(()->{
            int i = 0;
            while (true) {
                driverController.getDriver().navigate().to(webpages.get((i++) % webpages.size()));
//                t = new Thread(()->{
//                    driverController.getDriver().navigate().to(webpages.get((i++) % webpages.size()));
//                }).start();
                try {
                    Thread.sleep(5000);
//                    t.interrupt();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    @Autowired
    private SimpMessagingTemplate template;

    // this will send a message to an endpoint on which a client can subscribe
    @Scheduled(fixedRate = 5000)
    public void trigger() {
//        this.template.convertAndSend("/topic/message", "Date: " + new Date());
        byte[] bytesArr = ((TakesScreenshot) driverController.getDriver()).getScreenshotAs(OutputType.BYTES);
        String b64 = Base64.encodeBase64String(bytesArr);
        this.template.convertAndSend("/topic/message", b64);
    }

}
