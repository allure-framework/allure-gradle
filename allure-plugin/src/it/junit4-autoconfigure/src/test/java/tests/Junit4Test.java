package tests;

import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.junit.Assert;
import org.junit.Test;

public class Junit4Test {

    @Test
    public void testWithAttachment() {
        stepMethod();
        Assert.assertTrue(true);
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