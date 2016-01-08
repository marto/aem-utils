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

import static java.lang.String.format;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import org.apache.commons.codec.CharEncoding;
import org.osgi.framework.Bundle;

import freemarker.cache.URLTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;


/**
 * A freemarker template processing factory that loads templates (stored as resources in an OSGi bundle) and renders them.
 */
public class FreemarkerTemplateFactory {

    private final Configuration config;

    /**
     * Create an instance that loads templates from an OSGi <tt>bundle</tt>.
     *
     * @param bundle  the OSGi bundle used to load all templates from
     */
    public FreemarkerTemplateFactory(final Bundle bundle) {
       this.config = new Configuration();
       config.setObjectWrapper(new DefaultObjectWrapper());
       config.setTemplateUpdateDelay(Integer.MAX_VALUE);
       config.setLocalizedLookup(false);
       config.setTemplateLoader(new URLTemplateLoader() {
          @Override
          protected URL getURL(String url) {
             return bundle.getEntry(url);
          }
       });
    }

    /**
     * Clear the template cache
     */
    public void clear() {
       this.config.clearTemplateCache();
       this.config.clearEncodingMap();
       this.config.clearSharedVariables();
    }

    /**
     * @param templatePath  the template to render (view)
     * @param model         the model
     * @param writer        the writer
     * @throws TemplateException  on template error
     * @throws IOException        on any other write error
     */
    public void render(String templatePath, Object model, Writer writer) throws TemplateException, IOException {
       final Template template;
       try {
          template = config.getTemplate(templatePath, CharEncoding.UTF_8);
       } catch (IOException e) {
          throw new TemplateException(format("Failed to render template '%s'", templatePath), e, null);
       }
       template.process(model, writer);
    }

    /**
     * Same as {@link #render(String, Object, Writer)} but instead returns a StringBuffer of the rendered result instead of writing it to a writer.
     *
     * @param templatePath  the template to render (view)
     * @param model         the model
     * @return a StringBuffer that contains the rendered template.
     *
     * @throws TemplateException  on template error
     * @throws IOException        on any other write error
     */
    public StringBuffer renderToStringBuffer(String templatePath, Object model) throws TemplateException, IOException {
        final StringWriter writer = new StringWriter();
        render(templatePath, model, writer);
        return writer.getBuffer();
    }

}
