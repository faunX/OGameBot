This is project is old, i plan to return to work on this in a while.  I'll be more interested in returning to it if others show interest in this.  The goal is to make a client that the user can either use on their own computer with a progressive web app UI or an app/website where users can create bots that will use a proxy and have one or more bots be utilized through the website.  Via electron and ionic, a native app for both mobile and other os can be made.

This is still a work in progress.  Currently it is only functional through an IDE.

This is the master branch, it will house the most functional version of the bot so far.  The experimental branch is ogame_3.0 where i'll be starting over with the information i've learned so far.  When i feel confortable with 3.0 i'll merge it into master branch and include a jar to download and run the bot.  If i have time I plan to complete this by the end of the year 2018.

- - - -
# Ogame BOT #
Once this is done, the server will be hosted on a website which will make a local run not required.

#### Requirements to run locally ####
Java 8 download it [here](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)<br/>
maven  download it [here](https://maven.apache.org/download.cgi)<br/>

- - - -
This is the parent folder for the ogame bot.  This is used to fully autonomize the game [Ogame.org](https://en.ogame.gameforge.com/).<br/>
Since I can't store jars larger than 100MB on git, you'll have to build the jar yourself.<br/>
Simply run in the root directory:<br/>
`mvn clean install -U`<br/><br/>

This will create and copy the jars for both projects into the `jarResources` directory.<br/><br/>
To Run the jars type:<br/>
`java -jar {{jarfile}}` where `{{jarfile}}` is the name of jar file you want to run.<br/>
You should get these jars in that directory:<br/>
`ogamebot-app-1.0-SNAPSHOT.jar` <br/>
`ogamebot-server-1.0-SNAPSHOT.jar`<br/><br/>
The description of each is below.

- - - -
### Ogame Bot App ###
This is the bot itself.  This uses the [Selenium Framework](http://www.seleniumhq.org/projects/webdriver/) with a [phantomjs headless browser](http://phantomjs.org/) to automate the game itself.

It also uses [Postgres](https://www.postgresql.org) to store data to be used by all bots in a cluster to communicate with each other.  Postgres is shipped with the code.


When postgres fails to load for whatever reason, it will fall back to [HSQLDB](http://hsqldb.org/) which shouldn't have any issues.  HSQL isn't as good as postgres however due to read/write speeds.

- - - -
### Ogame Bot Server ###
This is the website to control and monitor one or more ogame bots.<br/>
The server consists of a java [Spring Boot](https://spring.io/docs) server that serves an [angularjs](https://angularjs.org/) website as well as holds the api for webrequests made from the website.

The Server has a website that when run is located at [localhost:8080](localhost:8080).  It will then require a log in.  You may create a new one in the register page.
