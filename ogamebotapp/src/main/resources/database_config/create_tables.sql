SET SEARCH_PATH TO OGAME;

-- DROP TABLE SERVER CASCADE;
-- DROP TABLE ALLIANCE CASCADE;
-- DROP TABLE PLAYER CASCADE;
-- DROP TABLE PLANET CASCADE;
-- DROP TABLE ALLIANCE_HIGHSCORE CASCADE;
-- DROP TABLE PLAYER_HIGHSCORE CASCADE;

------------------------------------------------------------------------------------------------------------------------
------------------------------- XML API TABLES
------------------------------------------------------------------------------------------------------------------------


CREATE TABLE IF NOT EXISTS SERVER(
  SERVER_ID                       INTEGER NOT NULL,
  SERVER_NAME                     VARCHAR(20),
  LANGUAGE                        CHAR(2),
  TIMEZONE                        VARCHAR(15),
  TIMEZONE_OFFSET                 CHAR(6),
  DOMAIN                          VARCHAR(30),
  VERSION                         VARCHAR(20),
  SPEED                           smallint,
  SPEED_FLEET                     smallint,
  GALAXIES                        smallint,
  SYSTEMS                         smallint,
  ACS                             smallint,
  RAPIDFIRE                       smallint,
  DEFTOTF                         smallint,
  DEBRIS_FACTOR                   decimal,
  DEBRIS_FACTOR_DEF               decimal,
  REPAIR_FACTOR                   decimal,
  NEWBIE_PROTECTION_LIMIT         integer,
  NEWBIE_PROTECTION_HIGH          integer,
  TOP_SCORE                       bigint,
  BONUS_FIELDS                    smallint,
  DONUT_GALAXY                    smallint,
  DONUT_SYSTEM                    smallint,
  WF_ENABLED                      INTEGER,
  WF_MINIMUM_RESS_LOST            INTEGER,
  WF_MINIMUM_LOSS_PERCENTAGE      smallint,
  WF_BASIC_PERCENTAGE_REPAIRABLE  smallint,
  GLOBAL_DEUTERIUM_SAVE_FACTOR    DECIMAL,
  TIMESTAMP                       BIGINT NOT NULL,--timestamp default current_timestamp,
  PRIMARY KEY (SERVER_ID,TIMESTAMP)
);


CREATE TABLE IF NOT EXISTS ALLIANCE(
  ALLIANCE_ID INTEGER NOT NULL,
  SERVER_ID   INTEGER,
  NAME        VARCHAR(100),
  TAG         VARCHAR(20),
  HOMEPAGE    VARCHAR(250),
  LOGO        VARCHAR(250),
  OPEN        CHAR(1),
  TIMESTAMP   BIGINT,
  SERVER_T    BIGINT,
  PRIMARY KEY (ALLIANCE_ID,SERVER_ID,TIMESTAMP),
  FOREIGN KEY (SERVER_ID,SERVER_T) REFERENCES SERVER(SERVER_ID,TIMESTAMP)
);

CREATE TABLE IF NOT EXISTS PLAYER(
  PLAYER_ID   INTEGER NOT NULL,
  SERVER_ID   INTEGER,
  NAME        VARCHAR(100),
  STATUS      VARCHAR(3),
  ALLIANCE_ID INTEGER,-- REFERENCES ALLIANCE(ALLIANCE_ID),
  TIMESTAMP                       BIGINT,
  AllIANCE_T  BIGINT,
  PRIMARY KEY (PLAYER_ID,SERVER_ID,TIMESTAMP),
  FOREIGN KEY (ALLIANCE_ID,AllIANCE_T,SERVER_ID) REFERENCES ALLIANCE(ALLIANCE_ID,TIMESTAMP,SERVER_ID)
);

CREATE TABLE IF NOT EXISTS PLANET(
  PLANET_ID     INTEGER NOT NULL,
  SERVER_ID     INTEGER,
  PLAYER_ID     INTEGER,
  NAME          VARCHAR(100),
  COORDS        VARCHAR(8),
  MOON_ID       INTEGER,
  MOON_NAME     VARCHAR(100),
  MOON_SIZE     INTEGER,
  TIMESTAMP     BIGINT,
  PLAYER_T      BIGINT,
  PRIMARY KEY (PLANET_ID,SERVER_ID,TIMESTAMP),
  FOREIGN KEY (PLAYER_ID,PLAYER_T,SERVER_ID) REFERENCES PLAYER(PLAYER_ID,TIMESTAMP,SERVER_ID)
);

/*
TYPES
0 Total
1 Economy
2	Research
3	Military
5	Military Built
6	Military Destroyed
4	Military Lost
7	Honor
 */
CREATE TABLE IF NOT EXISTS PLAYER_HIGHSCORE(
  PLAYER_ID   INTEGER,
  SERVER_ID   INTEGER,
  POSITION    INTEGER,
  SCORE       BIGINT,
  SHIPS		  BIGINT,
  TYPE        CHAR(1), --SEE TABLE ABOVE
  TIMESTAMP                       BIGINT,
  PLAYER_T    BIGINT,
  PRIMARY KEY (PLAYER_ID,SERVER_ID,TIMESTAMP),
  FOREIGN KEY (PLAYER_ID,SERVER_ID,PLAYER_T) REFERENCES PLAYER(PLAYER_ID,SERVER_ID,TIMESTAMP)
);
CREATE TABLE IF NOT EXISTS ALLIANCE_HIGHSCORE(
  ALLIANCE_ID INTEGER,
  SERVER_ID   INTEGER,
  POSITION    INTEGER,
  SCORE       BIGINT,
  TYPE        CHAR(1), --SEE TABLE ABOVE
  TIMESTAMP                       BIGINT,
  ALLIANCE_T  BIGINT,
  PRIMARY KEY (ALLIANCE_ID,SERVER_ID,TIMESTAMP),
  FOREIGN KEY (ALLIANCE_ID,SERVER_ID,ALLIANCE_T) REFERENCES ALLIANCE(ALLIANCE_ID,SERVER_ID,TIMESTAMP)
);



------------------------------------------------------------------------------------------------------------------------
------------------------------- USER TABLES
------------------------------------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS USERS(
  id          SERIAL PRIMARY KEY,
  USERNAME    VARCHAR(100) NOT NULL UNIQUE,
  PASSWORD    VARCHAR(100) NOT NULL,
  FIRST_NAME  VARCHAR(100) NOT NULL,
  LAST_NAME   VARCHAR(100) NOT NULL,
  USER_TYPE   CHAR(1) DEFAULT 'N' CHECK(USER_TYPE in ('A'/*ADMIN*/, 'N' /*Normal User*/)),
  ACTIVE      CHAR(1) DEFAULT 'A' CHECK(ACTIVE in ('A'/*ACTIVE*/, 'N' /*Not Active*/))
);

CREATE TABLE IF NOT EXISTS TOKENS(
  USERS_ID          INTEGER REFERENCES USERS(id) PRIMARY KEY,
  TOKEN             VARCHAR(100) NOT NULL UNIQUE,
  TIMESTAMP         timestamp DEFAULT current_timestamp,
  EXPIRE_TIMESTAMP  TIMESTAMP DEFAULT current_timestamp-- + (1 || ' days')::INTERVAL
);



------------------------------------------------------------------------------------------------------------------------
------------------------------- BOT TABLES
------------------------------------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS PROXY(
  ID          SERIAL PRIMARY KEY,
  PROXY_IP    VARCHAR(100) NOT NULL,
  PROXY_PORT  INT NOT NULL,
  REAL_IP     VARCHAR(100),
  TIMESTAMP   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  SPEED       BIGINT,
  WORKED      CHAR(1) DEFAULT 'N' CHECK(WORKED in ('Y' /*YES*/, 'N' /*NO*/))
);

CREATE TABLE IF NOT EXISTS WEBDRIVER(
  ID          SERIAL PRIMARY KEY,
  name        VARCHAR(100) UNIQUE,
  ACTIVE      CHAR(1)  DEFAULT 'N' CHECK(ACTIVE in ('A'/*ACTIVE*/, 'N' /*Not Active*/)),
  start_date  TIMESTAMP DEFAULT current_timestamp,
  DRIVER_TYPE VARCHAR(50),
  PROXY       VARCHAR(100),
  WINDOW_WIDTH  INT,
  WINDOW_HEIGHT INT,
  WINDOW_POSITION_X INT,
  WINDOW_POSITION_Y INT
);

CREATE TABLE IF NOT EXISTS EMAIL(
  ID          SERIAL PRIMARY KEY,
  EMAIL       VARCHAR(100) UNIQUE,
  PASSWORD    VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS OGAME_USER(
  EMAIL_ID    INT REFERENCES EMAIL(ID),
  ID          SERIAL PRIMARY KEY,
  USERNAME    VARCHAR(100) NOT NULL,
  PASSWORD    VARCHAR(100) NOT NULL,
  UNIVERSE    VARCHAR(100) NOT NULL,
  VERIFIED    CHAR(1) DEFAULT 'N' CHECK(VERIFIED in ('Y' /*YES*/, 'N' /*NO*/)),
  CREATED     CHAR(1) DEFAULT 'N' CHECK(CREATED in ('Y' /*YES*/, 'N' /*NO*/)),
  LAST_LOGIN  TIMESTAMP DEFAULT current_timestamp,
  UNIQUE (USERNAME,UNIVERSE)
);

CREATE TABLE IF NOT EXISTS BOT(
  OGAME_USER_ID   INT REFERENCES OGAME_USER(ID),
  WEBDRIVER_ID    INT REFERENCES WEBDRIVER(ID),
  ID              SERIAL PRIMARY KEY,
  name            VARCHAR(100) UNIQUE,
  ACTIVE          CHAR(1)  DEFAULT 'N' CHECK(ACTIVE in ('A'/*ACTIVE*/, 'N' /*Not Active*/)),
  start_date      TIMESTAMP DEFAULT current_timestamp
);

CREATE TABLE IF NOT EXISTS JSON_DATA(
  BOT_ID          INT REFERENCES BOT(ID) PRIMARY KEY,
  JSON_DATA       TEXT
);

CREATE TABLE IF NOT EXISTS BUILDABLE(
  ID                INT PRIMARY KEY,
  BUILDABLE         VARCHAR(100),
  TYPE              VARCHAR(100) /*RESEARCH,FACILITY,ECT*/
);

CREATE TABLE IF NOT EXISTS PROFILE(
  ID                INT PRIMARY KEY,
  NAME              VARCHAR(100), /*THE PROFILE's NAME*/
  BUILDABLE_ID      INT REFERENCES BUILDABLE(ID),
  BUILD_LEVEL       INT DEFAULT -1,
  BUILD_PRIORITY    INT DEFAULT 0, /*Build priority Higher priority things done first*/
  BUILD_TIMESTAMP   TIMESTAMP DEFAULT current_timestamp  /*THE TIMESTAMP TO START BUILDING THIS BUILDABLE IF NULL OR PAST TIME THEN BUILD IT IF YOU CAN*/
);

CREATE TABLE IF NOT EXISTS BOT_PROFILE(
  ID                SERIAL PRIMARY KEY, /*allows multiple profiles*/
  BOT_ID            INT REFERENCES BOT(ID),
  PROFILE_ID        INT REFERENCES PROFILE(ID),
  PRIORITY			    INT DEFAULT 0, /*Highest priority done first*/
  DONE          	  CHAR(1)  DEFAULT 'N' CHECK(DONE in ('Y'/*Yes*/, 'N' /*No*/))
);

CREATE TABLE IF NOT EXISTS RESEARCH_DATA(
  OGAME_USER_ID       INT REFERENCES OGAME_USER(ID) PRIMARY KEY,
  ID                  SERIAL,
  ESPIONAGE_LEVEL     INT DEFAULT 0,
  COMPUTER_LEVEL      INT DEFAULT 0,
  WEAPON_LEVEL        INT DEFAULT 0,
  SHIELDING_LEVEL     INT DEFAULT 0,
  ARMOUR_LEVEL        INT DEFAULT 0,
  ENERGY_LEVEL        INT DEFAULT 0,
  HYPERSPACE_LEVEL    INT DEFAULT 0,
  COMBUSTION_D_LEVEL  INT DEFAULT 0,
  IMPULSE_D_LEVEL     INT DEFAULT 0,
  HYPERSPACE_D_LEVEL  INT DEFAULT 0,
  LASER_LEVEL         INT DEFAULT 0,
  ION_LEVEL           INT DEFAULT 0,
  PLASMA_LEVEL        INT DEFAULT 0,
  IRN_LEVEL           INT DEFAULT 0,
  ASTROPHYSICS_LEVEL  INT DEFAULT 0,
  GRAVITON_LEVEL      INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS BOT_PLANETS(
  OGAME_USER_ID     INT REFERENCES OGAME_USER(ID),
  ID                SERIAL PRIMARY KEY,
  NAME              VARCHAR(100),
  COORDS            VARCHAR(8),
  METAL             BIGINT,
  CRYSTAL           BIGINT,
  DUETERIUM         BIGINT,
  SOLAR_TOTAL       INT,
  SOLAR_REMAINING   INT,
  TOTAL_FIELDS      INT,
  AVAILABLE_FIELDS  INT,
  MIN_TEMP          INT,
  MAX_TEMP          INT,
  UNIQUE (OGAME_USER_ID,COORDS)
);

CREATE TABLE IF NOT EXISTS CONFIG(
  BOT_PLANETS_ID                  INT REFERENCES BOT_PLANETS(ID) PRIMARY KEY,
  DELETE_MESSAGES                 BOOLEAN DEFAULT TRUE,
  AUTO_BUILD_METAL_STORAGE        BOOLEAN DEFAULT FALSE,
  AUTO_BUILD_CRYSTAL_STORAGE      BOOLEAN DEFAULT FALSE,
  AUTO_BUILD_DEUTERIUM_STORAGE    BOOLEAN DEFAULT FALSE,
  AUTO_BUILD_SOLAR                BOOLEAN DEFAULT FALSE,
  AUTO_BUILD_SOLAR_PERCENT		    INT DEFAULT 80,
  AUTO_BUILD_ESPIONAGE_PROBES     BOOLEAN DEFAULT FALSE,
  AUTO_BUILD_SMALL_CARGOS         BOOLEAN DEFAULT FALSE,
  SIMULATE_QUEUE_ON_EMPTY         BOOLEAN DEFAULT FALSE,
  AUTO_BUILD_LARGE_CARGOS         BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS MESSAGES(
  OGAME_USER_ID     INT REFERENCES OGAME_USER(ID),
  MESSAGE_ID        INT NOT NULL,
  TAB_ID            INT NOT NULL,
  MESSAGE_STATUS    VARCHAR(100),
  MESSAGE_TITLE     VARCHAR(100),
  MESSAGE_DATE      TIMESTAMP DEFAULT current_timestamp,
  MESSAGE_FROM      VARCHAR(100),
  MESSAGE_CONTENT   TEXT,
  PRIMARY KEY (OGAME_USER_ID,MESSAGE_ID)
);

CREATE TABLE IF NOT EXISTS ESPIONAGE_MESSAGES(
  SERVER_ID           INT NOT NULL,
  MESSAGE_ID        	INT NOT NULL,
  LOOT					      BIGINT,
  COUNTER_ESP_PERCENT	INT,
  SMALL_CARGO_NEEDED	INT,
  LARGE_CARGO_NEEDED	INT,
  LOOT_PERCENT			  INT,
  MAX_INFO				    INT,
  MESSAGE_DATE        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PLANET_NAME			    VARCHAR(100),
  PLAYER_NAME			    VARCHAR(100),
  STATUS				      VARCHAR(100),
  ACTIVITY				    VARCHAR(100),
  API					        TEXT,
  COORDINATES			    VARCHAR(8),
  IS_HONORABLE			  BOOLEAN,
  METAL					      BIGINT,
  CRYSTAL				      BIGINT,
  DUETERIUM				    BIGINT,
  SOLAR					      INT,
  JSON_LEVELS			    TEXT,
  JSON_ACTIVE_REPAIR	TEXT,
  JSON_ESP_OBJECT     TEXT,
  PRIMARY KEY (SERVER_ID,MESSAGE_ID,MESSAGE_DATE)
);

CREATE TABLE IF NOT EXISTS COMBAT_MESSAGES(
  SERVER_ID           INT NOT NULL,
  MESSAGE_ID        	INT NOT NULL,
  MESSAGE_DATE        TIMESTAMP,
  ATTACKER_GAINS      INT,
  DEFENDER_GAINS      INT,
  DEBRIS_SIZE         INT,
  ACTUALLY_REPAIRED   INT,
  ATTACKER_HONOR      INT,
  DEFENDER_HONOR      INT,
  RECYCLER_COUNT      INT,
  MOON_CHANGE_PERCENT INT,
  ATTACKER_WEAPONS    INT,
  ATTACKER_SHIELDS    INT,
  ATTACKER_ARMOUR     INT,
  DEFENDER_WEAPONS    INT,
  DEFENDER_SHIELDS    INT,
  DEFENDER_ARMOUR     INT,
  LOOT_METAL          BIGINT,
  LOOT_CRYSTAL        BIGINT,
  LOOT_DEUETERIUM     BIGINT,
  DEBRIS_METAL        BIGINT,
  DEBRIS_CYRSTAL      BIGINT,
  ATTACKER_NAME       VARCHAR(100),
  DEFENDER_NAME       VARCHAR(100),
  API                 VARCHAR(500),
  ATTACKER_STATUS     VARCHAR(12),
  DEFENDER_STATUS     VARCHAR(12),
  ATTACKER_PLANET_COORDS  VARCHAR(8),
  DEFENDER_PLANET_COORDS  VARCHAR(8),
  JSON_ATTACKER_SHIPS     TEXT,
  JSON_ATTACKER_SHIPS_LOST  TEXT,
  JSON_DEFENDER_SHIPS     TEXT,
  JSON_DEFENDER_SHIPS_LOST  TEXT,
  PRIMARY KEY (SERVER_ID,MESSAGE_ID,MESSAGE_DATE)
);

CREATE TABLE IF NOT EXISTS PLANET_QUEUE(
  BOT_PLANETS_ID    INT REFERENCES BOT_PLANETS(ID),
  BUILDABLE_ID      INT REFERENCES BUILDABLE(ID),
  BUILD_LEVEL       INT DEFAULT -1, /*level less than one means build next level*/
  BUILD_PRIORITY    INT DEFAULT 0, /*Build priority Higher priority things done first*/
  BUILD_TIMESTAMP   TIMESTAMP DEFAULT current_timestamp,   /*THE TIMESTAMP TO START BUILDING THIS BUILDABLE IF NULL OR PAST TIME THEN BUILD IT IF YOU CAN*/
  DONE          	  CHAR(1)  DEFAULT 'N' CHECK(DONE in ('Y'/*Yes*/, 'N' /*No*/)),
  UNIQUE (BOT_PLANETS_ID,BUILDABLE_ID,BUILD_LEVEL,DONE)
);

CREATE TABLE IF NOT EXISTS RESOURCES_DATA(
  BOT_PLANETS_ID                INT REFERENCES BOT_PLANETS(ID) PRIMARY KEY,
  ID                            SERIAL,
  METAL_MINE_LEVEL              INT DEFAULT 0,
  CRYSTAL_MINE_LEVEL            INT DEFAULT 0,
  DEUTERIUM_SYNTHESIZER_LEVEL   INT DEFAULT 0,
  METAL_STORAGE_LEVEL           INT DEFAULT 0,
  CRYSTAL_STOREAGE_LEVEL        INT DEFAULT 0,
  DEUTERIUM_TANK_LEVEL          INT DEFAULT 0,
  FUSION_REACTOR_LEVEL          INT DEFAULT 0,
  SOLAR_PLANET_LEVEL            INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS SHIPS_DATA (
  BOT_PLANETS_ID                INT REFERENCES BOT_PLANETS(ID) PRIMARY KEY,
  ID                            SERIAL,
  SMALL_CARGO_SHIPS             INT DEFAULT 0,
  LARGE_CARGO_SHIPS             INT DEFAULT 0,
  LIGHT_FIGHTERS                INT DEFAULT 0,
  HEAVY_FIGHTERS                INT DEFAULT 0,
  CRUISERS                      INT DEFAULT 0,
  BATTLESHIPS                   INT DEFAULT 0,
  BATTLECRUISERS                INT DEFAULT 0,
  DESTROYERS                    INT DEFAULT 0,
  DEATHSTARS                    INT DEFAULT 0,
  BOMBERS                       INT DEFAULT 0,
  RECYCLERS                     INT DEFAULT 0,
  ESPIONAGE_PROBES              INT DEFAULT 0,
  SOLAR_SATELLITES              INT DEFAULT 0,
  COLONY_SHIPS                  INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS DEFENSE_DATA (
  BOT_PLANETS_ID                INT REFERENCES BOT_PLANETS(ID) PRIMARY KEY,
  ID                            SERIAL,
  ROCKET_LAUNCHERS              INT DEFAULT 0,
  LIGHT_LASERS                  INT DEFAULT 0,
  HEAVY_LASERS                  INT DEFAULT 0,
  ION_CANNONS                   INT DEFAULT 0,
  GAUSS_CANNONS                 INT DEFAULT 0,
  PLASMA_TURRETS                INT DEFAULT 0,
  SMALL_SHIELD_DOME             INT DEFAULT 0,
  LARGE_SHIELD_DOME             INT DEFAULT 0,
  ANTI_BALLISTIC_MISSILES       INT DEFAULT 0,
  INTERPLANETARY_MISSILES       INT DEFAULT 0
);


CREATE TABLE IF NOT EXISTS FACILITIES_DATA(
  BOT_PLANETS_ID            INT REFERENCES BOT_PLANETS(ID) PRIMARY KEY,
  ID                        SERIAL,
  ROBOTICS_FACTORY_LEVEL    INT DEFAULT 0,
  SHIPYARD_LEVEL            INT DEFAULT 0,
  RESEARCH_LAB_LEVEL        INT DEFAULT 0,
  ALLIANCE_DEPOT_LEVEL      INT DEFAULT 0,
  MISSILE_SILO_LEVEL        INT DEFAULT 0,
  NANITE_FACTORY_LEVEL      INT DEFAULT 0,
  TERRAFORMER_LEVEL         INT DEFAULT 0,
  SPACE_DOCK_LEVEL          INT DEFAULT 0,
  LUNAR_BASE_LEVEL          INT DEFAULT 0,
  SENSOR_PHALANX_LEVEL      INT DEFAULT 0,
  JUMP_GATE_LEVEL           INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS TARGETS(
  SERVER_ID                 INT NOT NULL,
  JSON_DATA                 TEXT NOT NULL,
  COORDINATES               VARCHAR(8),
  PRIMARY KEY (SERVER_ID,COORDINATES)
);