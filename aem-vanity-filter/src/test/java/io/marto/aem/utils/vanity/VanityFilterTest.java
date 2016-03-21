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
package io.marto.aem.utils.vanity;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class VanityFilterTest {

    private VanityFilter filter = new VanityFilter();

    @Mock
    private FilterChain chain;

    @Mock
    private SlingHttpServletResponse response;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private RequestDispatcher dispatcher;

    private String forwardVanityResourceRedirectPath;

    @Before
    public void setup() {
        when(request.getResourceResolver()).thenReturn(resolver);
        when(resolver.resolve(anyString())).thenAnswer(new Answer<Resource>() {
            @Override
            public Resource answer(InvocationOnMock invocation) throws Throwable {
                return new NonExistingResource(resolver, invocation.getArgumentAt(0, String.class));
            }
        });
        when(request.getRequestDispatcher(any(Resource.class))).thenAnswer(new Answer<RequestDispatcher>() {
            @Override
            public RequestDispatcher answer(InvocationOnMock invocation) throws Throwable {
                forwardVanityResourceRedirectPath = invocation.getArgumentAt(0, Resource.class).getValueMap().get("sling:target", String.class);
                return dispatcher;
            }
        });
    }

    @Test
    public void testVanityIsResolved() throws IOException, ServletException {
        givenVanity("MyVanity", "/content/geometrix/vanity-target-page");

        whenFilterSetupWith("/content/geometrix/")
            .andCalledWith("/content/geometrix/MyVanity");

        thenInternalyForwardedToVanityTarget("/content/geometrix/vanity-target-page");
    }

    @Test
    public void testVanityIsResolvedOnAllowedPaths() throws IOException, ServletException {
        givenVanity("MyVanity", "/content/geometrix-media/vanity-target-page");

        whenFilterSetupWith("/content/geometrix/=/content/geometrix/,/content/geometrix-media/")
            .andCalledWith("/content/geometrix/MyVanity");

        thenInternalyForwardedToVanityTarget("/content/geometrix-media/vanity-target-page");
    }


    @Test
    public void testVanityIsResolvedRootDoesNotContainEndSlash() throws IOException, ServletException {
        givenVanity("MyVanity", "/content/geometrix/vanity-target-page");

        whenFilterSetupWith("/content/geometrix")
            .andCalledWith("/content/geometrix/MyVanity");

        thenInternalyForwardedToVanityTarget("/content/geometrix/vanity-target-page");
    }

    @Test
    public void testVanityIsNotResolvedWhenServletSetupWithIncorectRootPaths() throws IOException, ServletException {
        givenVanity("MyVanity", "/content/geometrix/vanity-target-page");

        whenFilterSetupWith("/content/geometrix-media/")
            .andCalledWith("/content/geometrix/MyVanity");

        thenRequestIsPassedThrough();
    }

    @Test
    public void testSimplePassThrough() throws IOException, ServletException {
        whenFilterSetupWith("/content/geometrix/")
            .andCalledWith("/content/geometrix/some-path");

        thenRequestIsPassedThrough();
    }

    @Test
    public void testPassThroughOnNullConfiguration() throws IOException, ServletException {
        whenFilterSetupWith()
            .andCalledWith("/content/geometrix/some-path");

        thenRequestIsPassedThrough();
    }

    @Test
    public void testVanityIsNotResolvedWhenVanityIsOnOtherRootPath() throws IOException, ServletException {
        givenVanity("MyVanity", "/content/geometrix-other/vanity-target-page");

        whenFilterSetupWith("/content/geometrix/")
            .andCalledWith("/content/geometrix/MyVanity");

        thenRequestIsPassedThrough();
    }

    private void thenRequestIsPassedThrough() throws IOException, ServletException {
        verify(chain, times(1)).doFilter(request, response);
    }

    private void thenInternalyForwardedToVanityTarget(String target) throws ServletException, IOException {
        verify(dispatcher).forward(eq(request), eq(response));
        assertEquals(forwardVanityResourceRedirectPath, target);

        // And we stop processing the request
        verify(chain, times(0)).doFilter(request, response);
    }

    private VanityFilterTest whenFilterSetupWith(String ... rootPaths) {
        if (rootPaths.length > 0) {
            filter.configure(Collections.singletonMap("rootPaths", rootPaths));
        }
        return this;
    }

    private void andCalledWith(String path) throws IOException, ServletException {
        RequestPathInfo pi = mock(RequestPathInfo.class);
        when(request.getRequestPathInfo()).thenReturn(pi);
        when(pi.getResourcePath()).thenReturn(path);

        filter.doFilter(request, response, chain);
    }

    private void givenVanity(String vanityPath, String target) {
        final Resource vanityResource = mock(Resource.class);
        when(vanityResource.getResourceType()).thenReturn("sling:redirect");
        ValueMap valueMap = valueMap("sling:target", target);
        when(vanityResource.getValueMap()).thenReturn(valueMap);
        when(resolver.resolve(eq("/"+vanityPath))).thenReturn(vanityResource);
    }

    private ValueMap valueMap(String key, String target) {
        ValueMap vals = mock(ValueMap.class);
        when(vals.get(eq(key), eq(String.class))).thenReturn(target);
        return vals;
    }
}
