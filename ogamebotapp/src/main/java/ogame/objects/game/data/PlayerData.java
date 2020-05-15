package ogame.objects.game.data;

import utilities.database.Database;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Created by jarndt on 6/10/17.
 */
public class PlayerData {
    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
        Server server = new Server(Server.IZAR);
//        System.out.println(new PlayerData("unsupo", server));

        List<Map<String, Object>> v = Database.newDatabaseConnection().executeQuery(
                "select * from player p, player_highscore h " +
                        "   where p.player_id = h.player_id and p.timestamp = h.player_t" +
                        "       and name = 'unsupo'" +
                        "       and p.server_id = 135" +
                        "       and type = '0'" +
                        "   order by player_t"
        );

        v.forEach(System.out::println);
    }

    public static final String INACTIVE = "i", INACTIVE_30_DAYS = "I", VACTION_MODE = "v", SUSPENDED = "b", OUTLAW = "o";

    private Integer playerId, serverId, allianceId;
    private transient Server server;
    private String name, status;
    private LocalDateTime lastUpdate;

    public PlayerData(int playerID, Server server) throws SQLException, IOException, ClassNotFoundException {
        this.server = server;
        parse("select * from player where player_id = "+playerID+" and server_id = "+server.getServerID()+" order by timestamp desc limit 1;");
    }public PlayerData(String playerName, Server server) throws SQLException, IOException, ClassNotFoundException {
        this.server = server;
        parse("select * from player where name = '"+playerName+"' and server_id = "+server.getServerID()+" order by timestamp desc limit 1;");
    }public PlayerData(Server server){
        this.server = server;
    }

    public void parse(String query) throws SQLException, IOException, ClassNotFoundException {
        List<Map<String, Object>> results = Database.newDatabaseConnection().executeQuery(query);
        if(results != null && results.size() != 0 && results.get(0) != null && results.get(0).size() != 0){
            Map<String, Object> v = results.get(0);
            playerId = (int) v.get("player_id");
            serverId = (int) v.get("server_id");
            name = v.get("name").toString();
            status = v.get("status") == null ? "a" : v.get("status").toString();
            allianceId = v.get("alliance_id") == null ? null : (int)v.get("alliance_id");
            lastUpdate = LocalDateTime.ofInstant(Instant.ofEpochSecond((Long)v.get("timestamp")),server.getZoneId());
        }else
            throw new IOException("Invalid value given in constructor: "+query);
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public int getAllianceId() {
        return allianceId;
    }

    public void setAllianceId(int allianceId) {
        this.allianceId = allianceId;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayerData that = (PlayerData) o;

        if (playerId != that.playerId) return false;
        if (serverId != that.serverId) return false;
        if (allianceId != that.allianceId) return false;
        if (server != null ? !server.equals(that.server) : that.server != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        return lastUpdate != null ? lastUpdate.equals(that.lastUpdate) : that.lastUpdate == null;
    }

    @Override
    public int hashCode() {
        int result = playerId;
        result = 31 * result + serverId;
        result = 31 * result + allianceId;
        result = 31 * result + (server != null ? server.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (lastUpdate != null ? lastUpdate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "playerId=" + playerId +
                ", serverId=" + serverId +
                ", allianceId=" + allianceId +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", lastUpdate=" + lastUpdate +
                '}';
    }
}
