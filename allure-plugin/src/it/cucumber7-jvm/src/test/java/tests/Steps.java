package tests;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Attachment;

import org.junit.Assert;

public class Steps {

    @Given("step")
    public void stepMethod() {
        attachment();
    }

    @Attachment(value = "attachment", type = "text/plain")
    public String attachment() {
        return "<p>HELLO</p>";
    }

}
