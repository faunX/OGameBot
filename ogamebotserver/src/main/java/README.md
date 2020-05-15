# Ogame Bot Server #
The server consists of a java [Spring Boot](https://spring.io/docs) server that serves an [angularjs](https://angularjs.org/) website as well as holds the api for webrequests made from the website.<br/>
If `.src/main/resources/static/bower_components` doesn't exist (it won't on initial git download), then if you run Application it will first download the bower components, then kill the program.  Just simply run the application again, or run `bower install` command in the `.src/main/resources/static/` directory. 

