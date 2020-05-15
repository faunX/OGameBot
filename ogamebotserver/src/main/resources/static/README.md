# Ogame Bot Webpage #
This is the webpage for controller one or more bots<br/>
The starting point is index.html.  This will require a login, then once logged in you will proceed to the dashboard.
- - - -
### Structure ###
`index.html` is the starting point with <br/>
`<div ui-view></div>`<br/>
deciding what page is viewed.  It goes off of <br/>
`./common/common-router.js`

- - - -
### Setup ###
Run the Application class to start the server that will serve this webpage.  If `./bower_components` doesn't exist then the initial run of Application class will create them and then exit.  Simply run Application class again, or run `./bower.sh` or `bower install` in this directory before running Application class.