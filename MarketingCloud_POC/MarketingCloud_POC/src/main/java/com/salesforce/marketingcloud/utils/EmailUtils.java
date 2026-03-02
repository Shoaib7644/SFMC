package com.salesforce.marketingcloud.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.salesforce.marketingcloud.managers.FileReaderManager;

/**
 * @Author: RafterOne QA
 */
public class EmailUtils {

	/**
	 * Method to Send Execution Report on Email
	 */
	public static void sendreport() {

		final String EMAILFROM =FileReaderManager.getInstance().getConfigReader().getEmailFrom();
		final String EMAILTO = FileReaderManager.getInstance ().getConfigReader ().getEmailTo();
		final String EMAILCC = FileReaderManager.getInstance ().getConfigReader ().getEmailCC();
		final String EMAILPWD = FileReaderManager.getInstance ().getConfigReader ().getEmailPassword();
		final String SMTP = FileReaderManager.getInstance ().getConfigReader ().getEmailSMTP();

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", SMTP);
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.ssl.protocols", "TLSv1.2");
		Session session = Session.getInstance(props,
				new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(EMAILFROM,EMAILPWD);
			}
		});

		try {
			String reportFileName = FileReaderManager.getInstance().getConfigReader().getEmailReportName();
			DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
			String datetext = formatter.format(new Date());
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(EMAILFROM));
			message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(EMAILTO));
			if(!(EMAILCC == null || EMAILCC == ""))
				message.setRecipients(Message.RecipientType.CC,InternetAddress.parse(EMAILCC));
			message.setSubject(FileReaderManager.getInstance().getConfigReader().getEmailSubject() +" "+ datetext);
			String htmlMessage = "<html>"+FileReaderManager.getInstance().getConfigReader().getEmailInitial()+"<br><br>"
			+ FileReaderManager.getInstance().getConfigReader().getEmailMessage()+"<br><br>"
			+ FileReaderManager.getInstance().getConfigReader().getEmailRegards()+"<br>"
			+ FileReaderManager.getInstance().getConfigReader().getEmailSender()+"<br>"
			+"<img src=\"cid:AbcXyz123\" /></html>";
			MimeBodyPart messageBodyPart = new MimeBodyPart();
		    messageBodyPart.setContent(htmlMessage, "text/html");
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);
			String[] attachFiles = new String[2];
	        attachFiles[0] = System.getProperty("user.dir") + FileReaderManager.getInstance().getConfigReader().getEmailSignatureLogo();         
	        attachFiles[1] = FileReaderManager.getInstance().getConfigReader().getPdfReportFilePath();
			MimeBodyPart imagePart = new MimeBodyPart();
			imagePart.setHeader("Content-ID", "AbcXyz123");
			imagePart.setDisposition(MimeBodyPart.INLINE);
			imagePart.attachFile(attachFiles[0]);
			multipart.addBodyPart(imagePart);
			MimeBodyPart messageBodyPart2 = new MimeBodyPart();      
			String filename =  attachFiles[1];     
			DataSource source = new FileDataSource(filename);    
			messageBodyPart2.setDataHandler(new DataHandler(source));    
			messageBodyPart2.setFileName(reportFileName); 
			multipart.addBodyPart(messageBodyPart2);  
			message.setContent(multipart);
			Transport.send(message);
			System.out.println("Email Sent successfully!!");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
