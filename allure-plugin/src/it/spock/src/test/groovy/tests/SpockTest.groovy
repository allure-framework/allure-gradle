package tests

import io.qameta.allure.Attachment
import io.qameta.allure.Step
import spock.lang.Specification

class SpockTest extends Specification {
    def "Example of Spock Test"() {
        when:
        stepMethod()
        then:
        true
    }

    @Step("step")
    void stepMethod() {
        attachment()
    }

    @Attachment(value = "attachment", type = "text/plain")
    String attachment() {
        return "<p>HELLO</p>"
    }
}
