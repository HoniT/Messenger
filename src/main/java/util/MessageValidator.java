package util;

public class MessageValidator {
    private static MessageValidator instance;
    private MessageValidator() {}

    public static synchronized MessageValidator getInstance() {
        if (instance == null) {
            instance = new MessageValidator();
        }
        return instance;
    }

    public static final String errorMessage = "Message must be between 0-100 characters";
    public boolean isValid(String messagePayload) {
        if (messagePayload == null || messagePayload.trim().isEmpty()) {
            return false;
        }
        return messagePayload.length() <= 100;
    }
}
