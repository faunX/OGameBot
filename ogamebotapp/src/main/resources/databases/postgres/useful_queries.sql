select bot_planets_id, build_priority, build_level, buildable, type
from planet_queue p, buildable b
where p.buildable_id = b.id
and p.done = 'N'
order by build_priority;

select * from profile where id = 1;

insert into profile(id,name,buildable_id,build_level,build_priority)
values(1,'attack bot',13,1,5);

select * from buildable;

select bot_planets_id, build_priority, build_level, buildable, type, done  from planet_queue p, buildable b where p.buildable_id = b.id;

select * from messages order by message_date desc;
select * from config;

update config set AUTO_BUILD_LARGE_CARGOS = 'true';

select * from profile where id = 1;

select * from buildable;

select * from espionage_messages order by message_date desc;
select * from espionage_messages
where max_info >= 2 and server_id = 117
      and (message_date, coordinates) in (
  select max(message_date), coordinates from espionage_messages
  where max_info >= 2 and server_id = 117
  group by coordinates
)

select * from combat_messages order by message_date desc;

select * from targets;

select * from targets where coordinates = '8:295:10';

select * from bot_planets;

select * from information_schema.table_constraints where table_name = 'planet_queue';

delete from planet_queue where buildable_id = 6 and build_level = 6 and build_priority = 18;
select * from planet_queue where buildable_id = 6 and build_level = 6;
alter table planet_queue add constraint planet_queue_bot_planets_id_buildable_id_build_level_key UNIQUE (BOT_PLANETS_ID,BUILDABLE_ID,BUILD_LEVEL,DONE);

select * from planet where server_id = 117 and (coords,timestamp) in (
  select coords,max(timestamp)
  from planet where server_id = 117 and player_id in (
    select p.player_id from player p, player_highscore h
    where p.player_id = h.player_id and p.timestamp = h.player_t
          and h.type = '0'
          and status in ('I','i')
          and p.server_id = 117
          and p.timestamp = (select timestamp from player where server_id = 117 order by timestamp desc limit 1)
          and score > 281/5 and score < 281 * 5
    order by h.position desc
  ) group by coords)
                           and coords not in (select defender_planet_coords from combat_messages where attacker_status in ('draw','defeated') and server_id = 117)
order by coords;


SELECT oid, relname FROM pg_class WHERE oid=36182


alter table CONFIG add column AUTO_BUILD_LARGE_CARGOS         BOOLEAN DEFAULT FALSE;


