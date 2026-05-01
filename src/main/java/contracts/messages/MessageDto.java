package contracts.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import persistence.entities.Message;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class MessageDto {
    private String senderUsername;
    private String payload;
    private String sentAt;

    public void mapFromMessage(Message m) {
        this.senderUsername = m.getSender().getUsername();
        this.payload = m.getPayload();
        this.sentAt = m.getSentAt().toString();
    }
}
