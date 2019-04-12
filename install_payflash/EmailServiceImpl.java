package com.sap.sme.payment.service.impl;

import com.sap.sme.common.logger.LightLogger;
import com.sap.sme.payment.service.EmailService;
import com.sap.sme.payment.service.model.EmailAttachment;
import com.sap.sme.payment.service.model.EmailEntity;
import com.sap.sme.payment.service.model.SMTP;

import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.*;

import org.apache.commons.mail.EmailConstants;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;

/**
 * Created by i065037 on 2019/3/14.
 */
@Service
public class EmailServiceImpl implements EmailService{
    private final LightLogger LOGGER = LightLogger.getLogger(this);

    private final static int SOCKET_TIMEOUT_MS = 10000;
    private final static int MAIL_SMTP_TIMEOUT = 20000;


    @Override
    public boolean send(EmailEntity entity) {
        try {
            LOGGER.info("Start to sending via SMTP mail ...");

            SMTP smtp = this.getDefaultSMTP();
            JavaMailSenderImpl sender = this.createMailSender(smtp);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = null;
            if (entity.getAttachments().size() > 0) {
                helper = new MimeMessageHelper(message, true);
            } else {
                helper = new MimeMessageHelper(message);
            }

            helper.setSentDate(new Date());

            if (StringUtils.isEmpty(entity.getFromDisplayName())) {
                helper.setFrom(entity.getFrom());
            } else {
                helper.setFrom(entity.getFrom(), entity.getFromDisplayName());
            }

            String[] to = entity.getTo().split(",");
            String[] cc = null;
            String[] bcc = null;

            helper.setTo(to);
            if (!StringUtils.isEmpty(entity.getBcc())) {
                bcc = entity.getBcc().split(",");
                helper.setBcc(bcc);
            }

            if(!StringUtils.isEmpty(entity.getCc())){
                cc = entity.getCc().split(",");
                helper.setCc(cc);
            }

            if((null == to || 0 == to.length)
                    && (null == cc || 0 == cc.length)
                    && (null == bcc || 0 == bcc.length)) {
                LOGGER.info("After being filtered by the blacklist, this email via SMTP has no one to send to. "
                        + "Therefore, consider it has been sent successfully.");
                return true;
            }

            helper.setSubject(entity.getSubject());
            helper.setText(entity.getBody(), entity.isHtml());

            for (EmailAttachment attachment : entity.getAttachments()) {
                InputStream inputStream = new ByteArrayInputStream(attachment
                        .getContent().getBytes());
                InputStream base64Stream = MimeUtility.decode(inputStream,
                        "base64");

                if (this.isInline(attachment)) {
                    helper.addInline(
                            MimeUtility.encodeText(attachment.getFileName(), "UTF-8", "B"),
                            new ByteArrayDataSource(base64Stream, attachment
                                    .getContentType()));
                } else {
                    helper.addAttachment(
                            MimeUtility.encodeText(attachment.getFileName(), "UTF-8", "B"),
                            new ByteArrayDataSource(base64Stream, attachment
                                    .getContentType()));
                }

                inputStream.close();
                base64Stream.close();
            }


            sender.send(message);
            LOGGER.info("Sending mail via SMTP successfully.");
            return true;
        } catch (Exception ex) {
            LOGGER.error("failed to send mail via SMTP", ex);
            needToRetry(ex, entity);
        }
        return false;
    }


    private SMTP getDefaultSMTP(){
        SMTP smtp = new SMTP();
        smtp.setSecurtiyLevel("SSL");
        smtp.setServer("email-smtp.eu-west-1.amazonaws.com");
        smtp.setPort(465);
        smtp.setUserName("AKIA32RQNNLMD6JV2ZJK");
        smtp.setPassword("BL622q1XuVpkDeF64O9STi8bjhKTF38YEFrttEBSfMS/");
        return smtp;
    }


    private void needToRetry(Exception e, EmailEntity entity) {

        if(e instanceof MailSendException) {
            entity.setNeedToRetry(true);
            return;
        }

        if(e instanceof MailAuthenticationException) {
            Throwable t = e.getCause();
            int depth = 0;
            while(depth < 10 && null != t) {
                if(t instanceof SocketTimeoutException) {
                    entity.setNeedToRetry(true);
                    return;
                }
                else {
                    t = t.getCause();
                    depth = depth + 1;
                }
            }
        }

        entity.setNeedToRetry(false);
    }

    private JavaMailSenderImpl createMailSender(SMTP smtp) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtp.getServer());
        sender.setPort(smtp.getPort());
        sender.setDefaultEncoding(EmailConstants.UTF_8);
        if (!StringUtils.isEmpty(smtp.getUserName())
                && !StringUtils.isEmpty(smtp.getPassword())) {
            sender.setUsername(smtp.getUserName());
            sender.setPassword(smtp.getPassword());
        }

        Properties properties = new Properties();
        properties.setProperty(EmailConstants.MAIL_DEBUG, "false");
        if (smtp.getSecurtiyLevel() == "SSL") {
            properties.setProperty(
                    EmailConstants.MAIL_TRANSPORT_STARTTLS_ENABLE, "false");
            properties.setProperty(EmailConstants.MAIL_SMTP_SSL_ENABLE, "true");
        } else if (smtp.getSecurtiyLevel() =="STARTTLS") {
            properties.setProperty(
                    EmailConstants.MAIL_TRANSPORT_STARTTLS_ENABLE, "true");
            properties
                    .setProperty(EmailConstants.MAIL_SMTP_SSL_ENABLE, "false");
        } else {
            properties.setProperty(
                    EmailConstants.MAIL_TRANSPORT_STARTTLS_ENABLE, "false");
            properties
                    .setProperty(EmailConstants.MAIL_SMTP_SSL_ENABLE, "false");
        }

        properties.setProperty(EmailConstants.MAIL_SMTP_TIMEOUT,
                Integer.toString(MAIL_SMTP_TIMEOUT));
        properties.setProperty(EmailConstants.MAIL_SMTP_CONNECTIONTIMEOUT,
                Integer.toString(SOCKET_TIMEOUT_MS));

        properties.put("mail.smtp.auth", "true");
        sender.setJavaMailProperties(properties);

        return sender;
    }

    private boolean isInline(EmailAttachment emailAttachment) {
        if (emailAttachment.getInline() == null) {
            return this.isPic(emailAttachment.getContentType()) ? true : false;
        } else {
            if (!this.isPic(emailAttachment.getContentType())) {
                return false;
            } else {
                return emailAttachment.getInline();
            }
        }
    }

    private boolean isPic(String contentType) {
        return StringUtils.equalsIgnoreCase(contentType, "image/jpeg")
                || StringUtils.equalsIgnoreCase(contentType, "image/png");
    }

}
