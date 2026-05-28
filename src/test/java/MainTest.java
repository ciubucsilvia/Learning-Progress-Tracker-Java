import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.stage2.Main;

import static org.junit.jupiter.api.Assertions.*;

public class MainTest {

    @ParameterizedTest
    @CsvSource({"John", "Jean-Clause", "Mary Luise"})
    void testValidateFirstNameTrue(String name) {
        assertTrue(Main.validateName(name));
    }

    @ParameterizedTest
    @CsvSource({"J.", "陳 港 生", "n"})
    void testValidateFirstNameFalse(String name) {
        assertFalse(Main.validateName(name));
    }

    @ParameterizedTest
    @CsvSource({"Doe", "van Helsing", "Johnson"})
    void testValidateLastNameTrue(String name) {
        assertTrue(Main.validateName(name));
    }

    @ParameterizedTest
    @CsvSource({"D."})
    void testValidateLastNameFalse(String name) {
        assertFalse(Main.validateName(name));
    }

    @ParameterizedTest
    @CsvSource({"jdoe@mail.net",
            "jane.doe@yahoo.com",
            "name@domain.com",
            "jc@google.it",
            "125367at@zzz90.z9"})
    void testValidateEmailTrue(String email) {
        assertTrue(Main.validateEmail(email));
    }

    @ParameterizedTest
    @CsvSource({"email"})
    void testValidateEmailFalse(String email) {
        assertFalse(Main.validateEmail(email));
    }
}
