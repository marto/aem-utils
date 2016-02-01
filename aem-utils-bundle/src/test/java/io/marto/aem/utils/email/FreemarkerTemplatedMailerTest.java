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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import io.marto.aem.utils.freemarker.BaseFreemarkerTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;

@RunWith(MockitoJUnitRunner.class)
public class FreemarkerTemplatedMailerTest extends BaseFreemarkerTest {
	
	@Mock
	private MessageGatewayService messageGatewayService;
	
	@Mock
	private MessageGateway<HtmlEmail> emailGateway;
	
	private AtomicReference<HtmlEmail> sentEmail = new AtomicReference<HtmlEmail>();
	
	private FreemarkerTemplatedMailer mailer;
	
	@Before
	public void setup() {
		super.setup();
		when(messageGatewayService.getGateway(HtmlEmail.class)).thenReturn(emailGateway);
		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				sentEmail.set(invocation.getArgumentAt(0, HtmlEmail.class));
				return null;
			}
			
		}).when(emailGateway).send(any(HtmlEmail.class));
		mailer = new FreemarkerTemplatedMailer(messageGatewayService, bundle);
		
	}
	
	@Test
	public void testGatewayFormatsAndSendsMessage() throws EmailException, IOException, MessagingException {
		Map<String, Object> model = createModel();
		
		String recipients[] = new String[] { "joe@me.com", "jack@test.com" };
		String subject = "Test Email";
		String sender = "admin@marto.io";
		String template = "/templates/helloworld.ftl";
		
		
		mailer.sendEmail(recipients, sender, subject, template, model);
		mailer.clear();
		
		HtmlEmail htmlMail = sentEmail.get();
		
		assertEquals(subject, htmlMail.getSubject());
		assertEquals(sender, htmlMail.getFromAddress().getAddress());
		assertEquals(recipients.length, htmlMail.getToAddresses().size());
		assertEquals(recipients[0],((InternetAddress)htmlMail.getToAddresses().get(0)).getAddress());
		assertEquals(recipients[1],((InternetAddress)htmlMail.getToAddresses().get(1)).getAddress());
		
		String msg = getEmail(htmlMail);
		
        assertThat(msg, containsString("FreeMarker Template example: Hello World!"));
        assertThat(msg, containsString("1. India"));
        assertThat(msg, containsString("2. United States"));
        assertThat(msg, containsString("3. Germany"));
        assertThat(msg, containsString("4. France"));
        
        assertThat(msg, containsString("To: \"joe@me.com\" <joe@me.com>, \"jack@test.com\" <jack@test.com>"));
        assertThat(msg, containsString("From: \"admin@marto.io\" <admin@marto.io>"));
        assertThat(msg, containsString("Subject: Test Email"));
	}

	
	@Test(expected = EmailException.class)
	public void testExceptionIsThrownWhenEmailGatewayDNE() throws EmailException, IOException, MessagingException {
		when(messageGatewayService.getGateway(HtmlEmail.class)).thenReturn(null);
		Map<String, Object> model = createModel();
		
		String recipients[] = new String[] { "joe@me.com", "jack@test.com" };
		String subject = "Test Email";
		String sender = "admin@marto.io";
		String template = "/templates/helloworld.ftl";
		
		
		mailer.sendEmail(recipients, sender, subject, template, model);
	}
	
	
	@Test(expected = EmailException.class)
	public void testFromAddressIgnoredWhenNotSet() throws EmailException, IOException, MessagingException {
		Map<String, Object> model = createModel();
		
		String recipients[] = new String[] { "joe@me.com", "jack@test.com" };
		String subject = "Test Email";
		String sender = null;
		String template = "/templates/helloworld.ftl";
		
		mailer.sendEmail(recipients, sender, subject, template, model);
		
		mailer.clear();
		
		HtmlEmail htmlMail = sentEmail.get();

		getEmail(htmlMail);
	}

	@Test
	public void testSubjectIgnoredWhenNotSet() throws EmailException, IOException, MessagingException {
		Map<String, Object> model = createModel();
		
		String recipients[] = new String[] { "joe@me.com", "jack@test.com" };
		String subject = null;
		String sender = "admin@marto.io";
		String template = "/templates/helloworld.ftl";
		
		mailer.sendEmail(recipients, sender, subject, template, model);
		
		mailer.clear();
		
		HtmlEmail htmlMail = sentEmail.get();

		String msg = getEmail(htmlMail);
		
		assertThat(msg, not(containsString("Subject:")));
	}
	
	@Test(expected = EmailException.class)
	public void testExceptionIsThrownWhenEmailTemplateIsMissingDNE() throws EmailException, IOException, MessagingException {
		when(messageGatewayService.getGateway(HtmlEmail.class)).thenReturn(null);
		Map<String, Object> model = createModel();
		
		String recipients[] = new String[] { "joe@me.com", "jack@test.com" };
		String subject = "Test Email";
		String sender = "admin@marto.io";
		String template = "/templates/non-existing-template.ftl";
		
		
		mailer.sendEmail(recipients, sender, subject, template, model);
		
	}
	
	private String getEmail(HtmlEmail htmlMail) throws EmailException, IOException, MessagingException, UnsupportedEncodingException {
		htmlMail.setHostName("localhost");
		htmlMail.buildMimeMessage();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		htmlMail.getMimeMessage().writeTo(out);
		return out.toString("UTF-8");
	}

}
