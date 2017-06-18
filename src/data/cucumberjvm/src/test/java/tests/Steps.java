package tests;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.qameta.allure.Attachment;
import org.apache.commons.io.IOUtils;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

import org.junit.Assert;

public class Steps {

    private static int a, b, c;

    @Given("^a is (\\d+)$")
    public void a_is(int arg1) throws Throwable {
        this.a = arg1;
    }

    @Given("^b is (\\d+)$")
    public void b_is(int arg1) throws Throwable {
        this.b = arg1;
    }

    @When("^I add a to b$")
    public void i_add_a_to_b() throws Throwable {
        this.c = this.a + this.b;
    }

    @Then("^result is (\\d+)$")
    public void result_is(int arg1) throws Throwable {
        attach();
        Assert.assertEquals(this.c, arg1);
    }

    @Attachment(type = "image/png", fileExtension = "png", value = "att")
    public byte[] attach() {
        try {
            return IOUtils.toByteArray(ClassLoader.getSystemResourceAsStream("cucumber.png"));
        } catch (IOException e) {
            return null;
        }
    }
}
