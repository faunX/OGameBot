# Ogame Bot App #
This is the bot itself.  This uses the [Selenium Framework](http://www.seleniumhq.org/projects/webdriver/) with a [phantomjs headless browser](http://phantomjs.org/) to automate the game itself.

The bot is working and will build and maintain itself based on a queue.  

It is a long way from done.

Next major hurdle is farming inactives.<br/>
The difficulties in this include determining the best targets to attack
based off the database from ogame xml data, and the already gathered espionage 
and combat reports.

Attacking:

query database for espionage and combat reports where no defense was present
and player is inactive get only latest based on date and coordinates.
Sort this by resources either gathered last or in the espionage report.
make sure espionage report shows defense and fleets if it can.

if no results come back then query ogame xml players for inactive
and points less than or equal to current points.

if no probes then send 1 small cargo into the closest planet in the 
above list.

else send 1 probe. at some point you send more probes to get more info
if you don't have defense and ship info.

get number of small/largo cargos preferably small and send it to the planet
if you don't have enough then build more in the queue and send what you have.


#### STILL TO DO ####
- Clean up Bot class. Way too big <br/>
- Better way to get settings from SettingManager class.<br/>
- Improve BuildingSimulator class, include research in simulation as well<br/>
- Combat report parsing (espionage report parsing complete)
- Finding targets, this includes blind attacking, calculating correct number of probes needed based on previous data.  
Calculating best targets based on previous data, ect.
- when to research more levels of computer, astrophysics, espionage, weapons, armour and shielding technologies
- Fleet sending
- Rocket sending
- fleet saving, how?, when?, ect
- recalling fleet
- response to being attacked
- multiple bots at once
- dealing with the human chat (responding and talking to human players).
- buddy system
- colonization, either allowing any number of fields, proper place to put colony, how to send out a colinization mission.
- dealing with other colonies like what to do with them
- getting graviton (use a colony close to planet slot 1?), when to build solar satellites?
- Is there ever a good time to build defenses?
- fix daily merchant item
- finding human targets
- moons, how to create them, how to move between them
- alliance stuff, should bots be in alliance with each other, could allow for wars
- Fix ogame logging me out for no reason (might have been fixed due to refreshing page instead of clicking on the link to the page again)
- add more info to database
- add a column to the database, planet_queue and profile for maintain, where if true it will make sure the planet has that many ships/defense always on the planet, this can never be set to done
- will it ever be an issue to worry about fields remaining on planet
- when to think about IRN
- TODO FIX BUILDING SIMULATOR, it fails a lot and isn't very accurate and doesn't take into account researches and any building goal state
