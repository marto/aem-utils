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
package io.marto.aem.utils.email;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.join;
import io.marto.aem.utils.freemarker.FreemarkerTemplateFactory;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.osgi.framework.Bundle;

import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;

import freemarker.template.TemplateException;

/**
 * A helper class used to send HTML formatted emails based on freemarker templates stored as resources in an OSGi bundle.
 */
public class FreemarkerTemplatedMailer {
    private final MessageGatewayService messageGatewayService;
    private final FreemarkerTemplateFactory templateFactory;

    public FreemarkerTemplatedMailer(MessageGatewayService messageGatewayService, Bundle bundle) {
        this.templateFactory = new FreemarkerTemplateFactory(bundle);
        this.messageGatewayService = messageGatewayService;
    }

    /**
     * @param recipients    list of recipients who will be sent the email
     * @param sender        the from email address
     * @param subject       the subject line
     * @param template      the path to the template (the view)
     * @param model         the detail body of the email (the model)
     *
     * @throws EmailException when an email can't be sent because of email server issues or if the message can't be rendered for what ever reason.
     */
    public void sendEmail(final String[] recipients, String sender, final String subject, String template, Object model) throws EmailException {
        try {
            final HtmlEmail email = constructEmail(recipients, sender, subject, template, model);
            final MessageGateway<HtmlEmail> gateway = messageGatewayService.getGateway(HtmlEmail.class);
            if (gateway != null) {
                gateway.send(email);
            } else {
                throw new EmailException("Could not obtain message gateway for html emails");
            }
        } catch (EmailException e) {
            throw new EmailException(format("Fatal error trying to send user feedback to %s", join(recipients)), e);
        }
    }

    private HtmlEmail constructEmail(final String[] recipients, String sender, final String subject, String template, Object model) throws EmailException {
        final HtmlEmail email = new HtmlEmail();

        email.setMsg(renderBody(template, model));
        if (subject != null) {
        	email.setSubject(subject);
        }
        if (sender != null) {
        	email.setFrom(sender);
        }

        for(String recipient : recipients) {
            email.addTo(recipient);
        }

        return email;
    }

    private String renderBody(String template, Object model) throws EmailException {
        try {
            StringWriter writer = new StringWriter();
            templateFactory.render(template, model, writer);
            return writer.toString();
        } catch (TemplateException|IOException e) {
            throw new EmailException(format("Failed to render email template '%s'", template), e);
        }
    }

    /**
     * Clear the template cache
     */
    public void clear() {
        templateFactory.clear();
    }
}
