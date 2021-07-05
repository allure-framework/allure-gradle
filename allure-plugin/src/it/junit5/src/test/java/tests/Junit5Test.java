package tests;

import io.qameta.allure.Attachment;
import io.qameta.allure.Step;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Junit5Test {

    @Test
    public void testWithAttachment() {
        stepMethod();
        assertTrue(true);
    }

    @Step("step")
    public void stepMethod() {
        attachment();
    }

    @Attachment(value = "attachment", type = "text/plain")
    public String attachment() {
        return "<p>HELLO</p>";
    }

}