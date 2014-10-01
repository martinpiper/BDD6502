import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(/*monochrome = true , */ glue = "TestGLue" , format = {"pretty" , "html:target/cucumber"} , features = "features")
public class RunCukesTest {
}
