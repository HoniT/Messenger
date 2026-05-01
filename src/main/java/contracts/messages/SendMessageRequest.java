package contracts.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class SendMessageRequest {
    private UUID senderSessionId;

    private String targetUsername;
    private String message;
}
