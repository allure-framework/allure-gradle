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

    @Given("step")
    public void stepMethod() {
        attachment();
    }

    @Attachment(value = "attachment", type = "text/plain")
    public String attachment() {
        return "<p>HELLO</p>";
    }

}
