package ogame.objects;

import utilities.password.Password;
import utilities.password.PasswordEncryptDecrypt;
import utilities.database.Database;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Created by jarndt on 5/25/17.
 */
public class User {
    Email email;
    String username,password,universe, lastLogin, ID, emailID;
    boolean verified = false, created = false;

    public User(String username, String password, String universe, String emailID) throws Exception {
        this.username = username;
        this.password = password;
        this.universe = universe;
        this.emailID = emailID;

        init();
    }
    public User(String username, String password, String universe) throws Exception {
        this.username = username;
        this.password = password;
        this.universe = universe;

        init();
    }public User(String username, String universe) throws Exception {
        this.username = username;
        this.universe = universe;

        init();
    }public User(Email email, String username, String password, String universe) throws Exception {
        this.universe = universe;
        this.username = username;
        this.password = password;
        this.email = email;
        this.emailID = email.getID();

        init();
    }


    private transient Database d;

    private Database getDatabaseConnection() throws SQLException, IOException, ClassNotFoundException {
        if(d == null)
            d = Database.newDatabaseConnection();
        return d;
    }

    private void init() throws Exception {
        List<Map<String, Object>> email = getDatabaseConnection().executeQuery("select * from ogame_user where username = '" + username + "' and universe = '"+universe+"';");
        if(email != null && email.size() != 0 && email.get(0) != null && email.get(0).size() > 1){
            Map<String, Object> emailInfo = email.get(0);
            String p = PasswordEncryptDecrypt.decrypt(emailInfo.get("password").toString());
//            if(password == null)
            password = p;
//            else if(!p.equals(password)) //password in database differs from one passed in, of of them is wrong i'll use the database password
            ID = emailInfo.get("id").toString();
            String v = emailInfo.get("verified").toString();
            verified = v.equals("Y") ? true : false;
            emailID = emailInfo.get("email_id").toString();
            if(email != null)
                this.email = Email.getEmailFromID(emailID);
            lastLogin = emailInfo.get("last_login").toString();
            created = emailInfo.get("created").equals("Y") ? true : false;
            return;
        }
        if(password == null) //and no record in database.
            throw new Exception("Invalid username and universe combination: "+username+":"+universe+", supply a password if it is a combination");

        emailID = emailID == null ? Email.getNullEmailID() : emailID;
        getDatabaseConnection().executeQuery(
          "insert into ogame_user(email_id,username,password,universe) " +
                  "values('"+emailID+"','"+username+"','"+PasswordEncryptDecrypt.encrypt(password)+"','"+universe+"');"
        );
    }

    public Email getEmail() {
        return email;
    }

    public User setEmail(Email email) {
        this.email = email;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUniverse() {
        return universe;
    }

    public void setUniverse(String universe) {
        this.universe = universe;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(String lastLogin) throws SQLException, IOException, ClassNotFoundException {
        getDatabaseConnection().executeQuery(
                "update ogame_user set last_login = '"+lastLogin+"' where id = "+ID+";"
        );
        this.lastLogin = lastLogin;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getEmailID() {
        return emailID;
    }

    public void setEmailID(String emailID) throws SQLException, IOException, ClassNotFoundException {
        getDatabaseConnection().executeQuery(
                "update ogame_user set email_id = '"+emailID+"' where id = "+ID+";"
        );
        this.emailID = emailID;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) throws SQLException, IOException, ClassNotFoundException {
        getDatabaseConnection().executeQuery(
                "update ogame_user set verified = '"+(verified?"Y":"N")+"' where id = "+ID+";"
        );
        this.verified = verified;
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) throws SQLException, IOException, ClassNotFoundException {
        if(created != this.created)
            getDatabaseConnection().executeQuery("update ogame_user set created = 'Y' where id = '"+ID+"';");
        this.created = created;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (verified != user.verified) return false;
        if (created != user.created) return false;
        if (email != null ? !email.equals(user.email) : user.email != null) return false;
        if (username != null ? !username.equals(user.username) : user.username != null) return false;
        if (password != null ? !password.equals(user.password) : user.password != null) return false;
        if (universe != null ? !universe.equals(user.universe) : user.universe != null) return false;
        if (lastLogin != null ? !lastLogin.equals(user.lastLogin) : user.lastLogin != null) return false;
        if (ID != null ? !ID.equals(user.ID) : user.ID != null) return false;
        if (emailID != null ? !emailID.equals(user.emailID) : user.emailID != null) return false;
        return d != null ? d.equals(user.d) : user.d == null;
    }

    @Override
    public int hashCode() {
        int result = email != null ? email.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (universe != null ? universe.hashCode() : 0);
        result = 31 * result + (lastLogin != null ? lastLogin.hashCode() : 0);
        result = 31 * result + (ID != null ? ID.hashCode() : 0);
        result = 31 * result + (emailID != null ? emailID.hashCode() : 0);
        result = 31 * result + (verified ? 1 : 0);
        result = 31 * result + (created ? 1 : 0);
        result = 31 * result + (d != null ? d.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "User{" +
                "email=" + email +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", universe='" + universe + '\'' +
                ", lastLogin='" + lastLogin + '\'' +
                ", ID='" + ID + '\'' +
                ", emailID='" + emailID + '\'' +
                ", verified=" + verified +
                ", created=" + created +
                ", d=" + d +
                '}';
    }

    public static User newRandomUser(String universe) throws Exception {
        Email randomEmail = Email.getUnusedEmail();
        return new User(randomEmail,randomEmail.getUsername(), Password.getRandomPassword(20),universe);
    }
}
