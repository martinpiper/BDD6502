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
public class GlueTest
{

	@Test
	public void testValueToInt() throws Throwable
	{
		Glue glue = new Glue();
		glue.BeforeHook(new Scenario()
		{
			public Collection<String> getSourceTagNames()
			{
				return null;
			}

			public String getStatus()
			{
				return null;
			}

			public boolean isFailed()
			{
				return false;
			}

			public void embed(byte[] bytes, String s)
			{

			}

			public void write(String s)
			{

			}

			public String getName()
			{
				return null;
			}

			public String getId()
			{
				return null;
			}
		});


		glue.i_load_labels("src/test/resources/test.lbl");

		for (int j = 0; j < 10; j++)
		{
			System.out.println(j);
			for (int i = 0; i < 100; i++)
			{
				int ret = glue.valueToInt("1 + " + i + " + start");
				assertThat(ret, is(equalTo(1 + i + 1024)));
			}
		}


		glue = null;
	}
}