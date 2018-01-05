package tests;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = {"src/test/resources/features"},
        plugin = {"io.qameta.allure.cucumber2jvm.AllureCucumber2Jvm"})
public class Cucumber2JvmTest {
}