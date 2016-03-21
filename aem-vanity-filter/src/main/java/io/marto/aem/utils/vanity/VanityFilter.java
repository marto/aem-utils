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

import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.strip;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SlingFilter(
        label = "Vanity URL Filter",
        description = "Enables vanity paths to be hosted under certain root paths (eg: /content/geometrix) and to ease dispatcher configuration & access filter setup",
        metatype = true,
        generateComponent = true,
        order = 0,
        scope = SlingFilterScope.REQUEST)
public class VanityFilter implements Filter {
    private final AtomicReference<Map<String, String[]>> contentPaths = new AtomicReference<>();

    @Property(cardinality = 100, label = "Root Content Paths", description = "List of root content paths eg: [/content/geometrix/, /content/gemoetrix-media/] "
            + "that will be searched for. If there are multiple valid paths for one root path use the following syntax to specify so: "
            + "'/content/geometrix/=/content/geometrix/en,/content/geometrix/fr'. By default if you specify '/content/geometrix/' then this is interpreted as "
            + "'/content/geometrix/=/content/geometrix/'")
    public static final String PROPERTY_SERVICES = "rootPaths";

    @Activate
    @Modified
    protected void configure(final Map<String, ?> config) {
        final String[] values = PropertiesUtil.toStringArray(config.get(PROPERTY_SERVICES), new String[0]);
        final Map<String, String[]> newConfig = new LinkedHashMap<>();
        for (String val: values) {
            String[] args = split(val, "=", 2);
            final String[] paths;
            if (args.length > 1) {
                paths = split(args[1], ",");
                for (int i = 0; i < paths.length; i++) {
                    paths[i] = strip(paths[i]);
                }
            } else {
                paths = new String[] { args[0] };
            }
            newConfig.put(strip(args[0]), paths);
        }
        contentPaths.set(newConfig);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        doFilterImpl((SlingHttpServletRequest)request, (SlingHttpServletResponse)response, chain);
    }

    private void doFilterImpl(SlingHttpServletRequest req, SlingHttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (contentPaths.get() != null) {
            final RequestPathInfo pi = req.getRequestPathInfo();
            for (Entry<String, String[]> entry: contentPaths.get().entrySet()) {
                final RequestDispatcher dispatcher = evaluate(req, pi, entry.getKey(), entry.getValue());
                if (dispatcher != null) {
                    LOGGER.debug("Found vanity path at '{}'", entry.getKey());
                    dispatcher.forward(req, res);
                    return;
                }
            }
        }
        LOGGER.debug("Failed to find any vanity paths");
        chain.doFilter(req, res);
    }

    private RequestDispatcher evaluate(SlingHttpServletRequest req, final RequestPathInfo pi, String prefix, String[] allowedPaths) {
        final Resource resource = req.getResourceResolver().resolve(getVanityFromPrefixedPath(prefix, pi));
        if (pi.getResourcePath().startsWith(prefix)) {
            if (isVanity(resource, allowedPaths)) {
                return req.getRequestDispatcher(resource);
            }
        }
        return null;
    }

    private String getVanityFromPrefixedPath(String prefix, final RequestPathInfo pi) {
        final String vanity = removeStart(pi.getResourcePath(), prefix);
        return endsWith(prefix, "/") ? "/" + vanity : vanity;
    }

    /**
     * @return true if the resource is a vanity <code>redirect</code> and the target starts with <code>prefix</code>, false otherwise
     */
    private boolean isVanity(Resource resource, String[] allowedPaths) {
        if (!StringUtils.equals(resource.getResourceType(), SLING_REDIRECT)) {
            return false;
        }
        String target = resource.getValueMap().get(SLING_TARGET, String.class);
        for (String path : allowedPaths) {
            if (startsWith(target, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
    }

    private static final String SLING_TARGET = "sling:target";
    private static final String SLING_REDIRECT = "sling:redirect";
    private static final Logger LOGGER = LoggerFactory.getLogger(VanityFilter.class);
}
