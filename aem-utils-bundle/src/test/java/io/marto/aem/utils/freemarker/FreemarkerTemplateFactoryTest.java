/*
Copyright (c) 2015-2016 "Martin Petrovsky"

This file is part of aem-utils (marto.io).

This is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package io.marto.aem.utils.freemarker;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import freemarker.template.TemplateException;

@RunWith(MockitoJUnitRunner.class)
public class FreemarkerTemplateFactoryTest extends BaseFreemarkerTest {

	protected FreemarkerTemplateFactory templateFactory;
	
	@Before
	public void setup() {
		super.setup();
		this.templateFactory = new FreemarkerTemplateFactory(bundle);
	}
	

	@Test
	public void testRendering() throws TemplateException, IOException {
        final Map<String, Object> model = createModel();
        
        String output = this.templateFactory.renderToStringBuffer("/templates/helloworld.ftl", model).toString();
        
        assertThat(output, containsString("FreeMarker Template example: Hello World!"));
        assertThat(output, containsString("1. India"));
        assertThat(output, containsString("2. United States"));
        assertThat(output, containsString("3. Germany"));
        assertThat(output, containsString("4. France"));
	}


	@Test
	public void testRenderThrowsTemlateExceptionWhenItCantFindTheTemplate() throws TemplateException, IOException {
		try {
			this.templateFactory.renderToStringBuffer("/templates/non-existing-template.ftl", new HashMap());
			fail("Expected TemplateException");
		} catch (TemplateException e) {
			assertThat(e.getMessage(), containsString("Failed to render template"));
			assertThat(e.getMessage(), containsString("/templates/non-existing-template.ftl"));
		}
	}

	@Test
	public void testClearClearsCacheAndReloadsTemplate() throws TemplateException, IOException {
		final Map<String, Object> model = createModel();
		assertThat(this.templateFactory.renderToStringBuffer("/templates/helloworld.ftl", model).toString(), containsString("FreeMarker Template example: Hello World!"));
		assertEquals(1, loadCount.get());
		assertThat(this.templateFactory.renderToStringBuffer("/templates/helloworld.ftl", model).toString(), containsString("FreeMarker Template example: Hello World!"));
		assertEquals(1, loadCount.get());
		
		this.templateFactory.clear();
		assertThat(this.templateFactory.renderToStringBuffer("/templates/helloworld.ftl", model).toString(), containsString("FreeMarker Template example: Hello World!"));
		assertEquals(2, loadCount.get());
	}
}
