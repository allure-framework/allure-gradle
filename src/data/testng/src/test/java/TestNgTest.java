import org.testng.Assert;
import org.testng.annotations.Test;

public class TestNgTest {

    @Test
    public void testWithAttachment() {
        attachment();
        Assert.assertTrue(true);
    }

    public String attachment() {
        return "<p>HELLO</p>";
    }

}
