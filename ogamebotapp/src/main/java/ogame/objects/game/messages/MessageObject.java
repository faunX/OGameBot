package ogame.objects.game.messages;

import bot.Bot;
import ogame.objects.game.fleet.Mission;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by jarndt on 6/6/17.
 */
public class MessageObject {
    public static final String ESPIONAGE = Mission.ESPIONAGE, COMBAT_REPORT = Mission.ATTACKING, OTHER ="other";

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private long messageId;
    private int tabId, pageNumber;
    private String messageStatus, messageTitle, from, type;
    private Elements messageContent;
    private LocalDateTime messageDate;
    private boolean isNewMessage = false;

    private EspionageMessage espionageMessage;
    private CombatMessage combatMessage;

    public MessageObject(long messageId, String messageStatus, String messageTitle, String messageDate, String from, Elements messageContent) {
        this.messageId = messageId;
        this.messageStatus = messageStatus;
        this.messageTitle = messageTitle;
        this.messageDate = LocalDateTime.parse(messageDate, FORMATTER);
        this.from = from;
        this.messageContent = messageContent;
    }

    public MessageObject(Element htmlLiElement) {
        messageId = Long.parseLong(htmlLiElement.attr("data-msg-id"));
        messageStatus = htmlLiElement.select("div.msg_status").text();
        messageTitle = htmlLiElement.select("span.msg_title").text();
        messageDate = LocalDateTime.parse(htmlLiElement.select("span.msg_date").text(), FORMATTER);
        from = htmlLiElement.select("span.msg_sender").text();
        messageContent = htmlLiElement.select("span.msg_content");
        isNewMessage = htmlLiElement.hasClass("msg_new");
    }

    public String getType() {
        if(type == null){
            if(tabId == 20 && messageTitle.contains("Espionage report from"))
                type = ESPIONAGE;
            else if(tabId == 21 && messageTitle.contains("Combat Report"))
                type = COMBAT_REPORT;
            else
                type = OTHER;
        }
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public String getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(String messageStatus) {
        this.messageStatus = messageStatus;
    }

    public String getMessageTitle() {
        return messageTitle;
    }

    public void setMessageTitle(String messageTitle) {
        this.messageTitle = messageTitle;
    }

    public LocalDateTime getMessageDate() {
        return messageDate;
    }

    public Timestamp getMessageTimestamp(){
        return Timestamp.valueOf(messageDate);
    }

    public void setMessageDate(LocalDateTime messageDate) {
        this.messageDate = messageDate;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public Elements getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(Elements messageContent) {
        this.messageContent = messageContent;
    }

    public int getTabId() {
        return tabId;
    }

    public MessageObject setTabId(int tabId) {
        this.tabId = tabId;
        return this;
    }

    public boolean isNewMessage() {
        return isNewMessage;
    }

    public void setNewMessage(boolean newMessage) {
        isNewMessage = newMessage;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public MessageObject setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
        return this;
    }

    public EspionageMessage getEspionageMessage(String server, String cookies) throws IOException {
        if(getType().equals(ESPIONAGE) && espionageMessage == null)
            espionageMessage = new EspionageMessage(this, server, cookies);
        return espionageMessage;
    }
    public CombatMessage getCombatMessage(String server, String cookies) throws IOException {
        if(getType().equals(COMBAT_REPORT) && combatMessage == null)
            combatMessage = new CombatMessage(this, server, cookies);
        return combatMessage;
    }

    public void setEspionageMessage(EspionageMessage espionageMessage) {
        this.espionageMessage = espionageMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageObject that = (MessageObject) o;

        return messageId == that.messageId;
    }

    @Override
    public int hashCode() {
        return (int) (messageId ^ (messageId >>> 32));
    }

    @Override
    public String toString() {
        return "MessageObject{" +
                "messageId=" + messageId +
                ", tabId=" + tabId +
                ", messageStatus='" + messageStatus + '\'' +
                ", messageTitle='" + messageTitle + '\'' +
                ", from='" + from + '\'' +
                ", messageContent='" + messageContent + '\'' +
                ", messageDate=" + messageDate +
                ", isNewMessage=" + isNewMessage +
                '}';
    }
}
