package TestGlue;

import cucumber.api.Scenario;
import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Created by Martin on 10/11/2015.
 */
public class GlueTest {

    @Test
    public void testValueToInt() throws Throwable {
        Glue glue = new Glue();
        glue.BeforeHook(createScenario());
        glue.i_load_labels("src/test/resources/start.lbl");

        for (int j = 0; j < 10; j++) {
            System.out.println(j);
            for (int i = 0; i < 100; i++) {
                int ret = Glue.valueToInt("1 + " + i + " + start");
                assertThat(ret, is(equalTo(1 + i + 1024)));
            }
        }
    }

    @Test
    public void testLoadingLabelsTrimsSpace() throws Throwable {
        Glue glue = new Glue();
        glue.BeforeHook(createScenario());
        glue.i_load_labels("src/test/resources/with-spaces.lbl");

        assertThat(Glue.valueToInt("v1"), is(equalTo(101)));
        assertThat(Glue.valueToInt("v2"), is(equalTo(102)));
        assertThat(Glue.valueToInt("v3"), is(equalTo(103)));
        assertThat(Glue.valueToInt("v4"), is(equalTo(104)));
    }

    @Test
    public void testSkipsBlankLines() throws Throwable {
        Glue glue = new Glue();
        glue.BeforeHook(createScenario());
        glue.i_load_labels("src/test/resources/blank-lines.inc");

        assertThat(Glue.valueToInt("b"), is(equalTo(2)));
    }

    private static Scenario createScenario() {
        return new Scenario() {
            public Collection<String> getSourceTagNames() { return null;}
            public String getStatus() { return null; }
            public boolean isFailed() { return false; }
            public void embed(byte[] bytes, String s) {}
            public void write(String s) {}
            public String getName() { return null; }
            public String getId() { return null; }
        };
    }

}
