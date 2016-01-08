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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;

@RunWith(MockitoJUnitRunner.class)
public abstract class BaseFreemarkerTest {

	@Mock 
	protected Bundle bundle;
	protected final AtomicInteger loadCount = new AtomicInteger(); 
	
	@Before
	public void setup() {
		
		when(bundle.getEntry(anyString())).then(new Answer<URL>() {

			@Override
			public URL answer(InvocationOnMock invocation) throws Throwable {
				// delegate to normal class loader
				final String path = invocation.getArgumentAt(0, String.class);
				loadCount.incrementAndGet();
				return FreemarkerTemplateFactoryTest.class.getResource("/" + path);
			}
			
		});
	}
	
	protected Map<String, Object> createModel() {
		// Build the data-model
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("message", "Hello World!");

        //List parsing 
        List<String> countries = new ArrayList<String>();
        countries.add("India");
        countries.add("United States");
        countries.add("Germany");
        countries.add("France");
         
        model.put("countries", countries);
		return model;
	}
}
