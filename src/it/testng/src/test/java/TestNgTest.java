import org.testng.Assert;
import org.testng.annotations.Test;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;

public class TestNgTest {

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
