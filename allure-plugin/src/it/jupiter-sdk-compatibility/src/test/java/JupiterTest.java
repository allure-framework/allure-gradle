import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JupiterTest {
    @Test
    void recordsEvidence() {
        assertEquals("<p>HELLO</p>", recordEvidence());
    }

    @Step("step")
    String recordEvidence() {
        return attachment();
    }

    @Attachment(value = "proof", type = "text/plain")
    String attachment() {
        return "<p>HELLO</p>";
    }
}
