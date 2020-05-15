package utilities.email;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

/**
 * Created by jarndt on 6/1/17.
 */
public class EmailUtility {
    String host,mailStoreType;

    public class EmailObject{
        private String emailNumber, subject,from,text;

        public EmailObject(String emailNumber, String subject, String from, String text) {
            this.emailNumber = emailNumber;
            this.subject = subject;
            this.from = from;
            this.text = text;
        }
        public String getEmailNumber() {
            return emailNumber;
        }

        public void setEmailNumber(String emailNumber) {
            this.emailNumber = emailNumber;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return "EmailObject{" +
                    "emailNumber='" + emailNumber + '\'' +
                    ", subject='" + subject + '\'' +
                    ", from='" + from + '\'' +
                    ", text='" + text + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EmailObject that = (EmailObject) o;

            if (emailNumber != null ? !emailNumber.equals(that.emailNumber) : that.emailNumber != null) return false;
            if (subject != null ? !subject.equals(that.subject) : that.subject != null) return false;
            if (from != null ? !from.equals(that.from) : that.from != null) return false;
            return text != null ? text.equals(that.text) : that.text == null;
        }

        @Override
        public int hashCode() {
            int result = emailNumber != null ? emailNumber.hashCode() : 0;
            result = 31 * result + (subject != null ? subject.hashCode() : 0);
            result = 31 * result + (from != null ? from.hashCode() : 0);
            result = 31 * result + (text != null ? text.hashCode() : 0);
            return result;
        }
    }

    List<EmailObject> emails = new ArrayList<>();
    String emailAddress,password;

    public EmailUtility(String emailAddress, String password) {
        this.emailAddress = emailAddress;
        this.password = password;


        host = "imap.one.com";// change accordingly
        mailStoreType = "pop3";
//        String username = "bc3ew9p4yh9qdv8wvj1h@michaelgutin.one";// change accordingly
//        String password = "bc3ew9p4yh9qdv8wvj1h";// change accordingly
    }

    public String getOgameVerifyLink(){
        if(emails.size() == 0)
            check();

        Document doc = emails.stream().filter(a -> a.getSubject().equals("Welcome to OGame.org"))
                .map(a -> Jsoup.parse(a.getText())).collect(Collectors.toList()).get(0);
        return doc.select("body > table > tbody > tr > td > table > tbody > tr:nth-child(5) > td > table > tbody > tr > td > table > tbody > tr:nth-child(3) > td > table > tbody > tr > td > font > a").attr("href").trim();
    }


    public List<EmailObject> check(){
        try {

            //create properties field
            Properties properties = new Properties();

            properties.put("mail.pop3.host", host);
            properties.put("mail.pop3.port", "993");
            properties.put("mail.pop3.starttls.enable", "true");
            Session emailSession = Session.getDefaultInstance(properties);

            //create the POP3 store object and connect with the pop server
            Store store = emailSession.getStore("pop3s");

            store.connect(host, emailAddress, password);

            //create the folder object and open it
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            // retrieve the messages from the folder in an array and print it
            Message[] messages = emailFolder.getMessages();
//            System.out.println("messages.length---" + messages.length);

            for (int i = 0, n = messages.length; i < n; i++) {
                Message message = messages[i];
                emails.add(new EmailObject((i+1)+"",message.getSubject(),message.getFrom()[0].toString(),message.getContent().toString()));
//                System.out.println("---------------------------------");
//                System.out.println("Email Number " + (i + 1));
//                System.out.println("Subject: " + message.getSubject());
//                System.out.println("From: " + message.getFrom()[0]);
//                System.out.println("Text: " + message.getContent().toString());

            }

            //close the store and folder objects
            emailFolder.close(false);
            store.close();

        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return emails;
    }

    public static void main(String[] args) {
//        check(host, mailStoreType, username, password);

        String verify = new EmailUtility("bc3ew9p4yh9qdv8wvj1h@michaelgutin.one", "bc3ew9p4yh9qdv8wvj1h")
                .getOgameVerifyLink();
        System.out.println(verify);
    }

}
