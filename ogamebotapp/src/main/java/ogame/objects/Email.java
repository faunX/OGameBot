package ogame.objects;

import utilities.password.PasswordEncryptDecrypt;
import utilities.database.Database;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Created by jarndt on 5/25/17.
 */
public class Email {
    public static void main(String[] args) throws Exception {
        System.out.println(Email.getUnusedEmail());
    }

    private String emailAddress, password, ID;
    private String username, domain;

    public Email(String emailAddress, String password, String ID) throws Exception {
        this.emailAddress = emailAddress;
        this.password = password;
        this.ID = ID;

        init();
    }

    public Email(String emailAddress, String password) throws Exception {
        this.emailAddress = emailAddress;
        this.password = password;

        init();
    }

    public Email(String emailAddress) throws Exception {
        this.emailAddress = emailAddress;

        init();
    }

    private void init() throws Exception {
        String[] split = this.emailAddress.split("@");
        this.username = split[0];
        if(split[0].equals("None Given"))
            this.domain = split[0];
        else
            this.domain = split[1];

        List<Map<String, Object>> email = getDatabaseConnection().executeQuery("select * from email where email = '" + emailAddress + "';");
        if(email != null && email.size() > 0 && email.get(0) != null && email.get(0).size() > 0){
            Map<String, Object> emailInfo = email.get(0);
            String p = PasswordEncryptDecrypt.decrypt(emailInfo.get("password").toString());
//            if(password == null)
            password = p;
//            else if(!p.equals(password)) //password in database differs from one passed in, of of them is wrong i'll use the database password
            ID = emailInfo.get("id").toString();
            return;
        }
        if(password == null) //and no record in database.
            throw new Exception("Invalid email address: "+emailAddress+", supply a password if it is a valid email");
    }

    private static Database d;
    public static Database getDatabaseConnection() throws SQLException, IOException, ClassNotFoundException {
        if(d == null)
            d = Database.newDatabaseConnection();
        return d;
    }

    public static Email getEmailFromID(String emailID) throws Exception {
        List<Map<String, Object>> email = getDatabaseConnection().executeQuery("select * from email where id = '" + emailID + "';");
        if(email != null && email.size() == 1 && email.get(0) != null && email.get(0).size() != 0 )
            return new Email(
                        email.get(0).get("email").toString(),
                        PasswordEncryptDecrypt.decrypt(email.get(0).get("password").toString()),
                        emailID
                    );
        return null;
    }

    public static final String NO_EMAIL = "None Given";

    public static int recurseCount = 0;
    public static String getNullEmailID() throws SQLException, IOException, ClassNotFoundException {
        List<Map<String, Object>> email = getDatabaseConnection().executeQuery("select * from email where email = '" + NO_EMAIL + "';");
        if(email != null && email.size() == 1 && email.get(0) != null && email.get(0).size() != 0)
            return email.get(0).get("id").toString();
        getDatabaseConnection().executeQuery("insert into email(email,password) values('" + NO_EMAIL + "','" + NO_EMAIL + "');");
        if(recurseCount++ < 10)
            return getNullEmailID();
        return null;
    }

    public static Email getUnusedEmail() throws Exception {
        List<Map<String, Object>> email = getDatabaseConnection().executeQuery(
                "select * from email where " +
                        "id not in (select email_id from ogame_user)" +
                        "and email != '"+NO_EMAIL+"';");
        if(email != null && email.size() > 0 && email.get(0) != null && email.get(0).size() != 0 ) {
            String pass = email.get(0).get("password").toString();
            return new Email(
                    email.get(0).get("email").toString(),
                    pass.equals(NO_EMAIL)?pass:PasswordEncryptDecrypt.decrypt(pass),
                    email.get(0).get("id").toString()
            );
        }
        return null;
    }

    public String getUsername() {
        return username;
    }

    public String getDomain() {
        return domain;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Email email = (Email) o;

        if (emailAddress != null ? !emailAddress.equals(email.emailAddress) : email.emailAddress != null) return false;
        if (password != null ? !password.equals(email.password) : email.password != null) return false;
        return ID != null ? ID.equals(email.ID) : email.ID == null;
    }

    @Override
    public int hashCode() {
        int result = emailAddress != null ? emailAddress.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (ID != null ? ID.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Email{" +
                "emailAddress='" + emailAddress + '\'' +
                ", password='" + password + '\'' +
                ", ID='" + ID + '\'' +
                '}';
    }
}
