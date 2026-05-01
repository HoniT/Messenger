import org.junit.jupiter.api.Test;
import util.MessageValidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessageValidatorTest {
    private final MessageValidator validator = MessageValidator.getInstance();

    @Test
    public void testValidMessage() {
        assertTrue(validator.isValid("Hello, how are you?"));
    }

    @Test
    public void testEmptyMessageIsInvalid() {
        assertFalse(validator.isValid(""));
        assertFalse(validator.isValid("   "));
    }

    @Test
    public void testNullMessageIsInvalid() {
        assertFalse(validator.isValid(null));
    }
}
