import io.karatelabs.core.Runner;
import io.qameta.allure.karate.AllureKarate;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KarateTest {
    @Test
    public void runsFeature() {
        var results = Runner.builder().path("classpath:evidence.feature")
            .listener(new AllureKarate())
            .outputHtmlReport(false).parallel(1);
        assertEquals(1, results.getScenarioCount());
        assertEquals(0, results.getScenarioFailedCount());
    }
}
