package com.util;

import java.text.SimpleDateFormat;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

public class Email {

	String emailOrigem, senha;

	public Email(String from, String pwd) {
		this.emailOrigem = from;
		this.senha = pwd;
		// final String from = "cmc@cifarma.ind.br";
		// final String pwd = "@cifa123";
	}

	public void envia(String titulo, String arquivo, String destinatario) {
		envia(titulo, arquivo,
				"RELATÓRIO DE VISITAS " + new SimpleDateFormat("dd-MM-yyyy HH").format(new Date()) + "h.xls",
				destinatario);
	}

	public void envia(String titulo, String arquivo, String nomeArquivo, String destinatario) {

		String to = destinatario;

		String filename = arquivo;
		String msgText1 = "Olá Srs,\n\nSegue em anexo o Relatório de Visitas Diário, atualizado até a data: "
				+ new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
		String subject = titulo;

		Properties props = new Properties();
		/** Parâmetros de conexão com servidor Gmail */
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");

		Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(emailOrigem, senha);
			}
		});

		/** Ativa Debug para sessão */
		session.setDebug(true);

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("seuemail@gmail.com")); // Remetente

			Address[] toUser = InternetAddress // Destinatário(s)
					.parse(to);

			message.setRecipients(Message.RecipientType.TO, toUser);
			message.setSubject(subject);// Assunto
			message.setContent(msgText1, "text/plain");

			MimeBodyPart messageBodyPart = new MimeBodyPart();
			Multipart multipart = new MimeMultipart();

			if (filename != null && filename.equals("") == false) {
				DataSource source = new FileDataSource(filename);
				messageBodyPart.setDataHandler(new DataHandler(source));
				messageBodyPart.setFileName(nomeArquivo);
				multipart.addBodyPart(messageBodyPart);
				message.setContent(multipart);
			}

			/** Método para enviar a mensagem criada */
			Transport.send(message);

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}

	}

	public void enviaSemAnexo(String subject, String mensagem, String destinatario) {

		String to = destinatario;

		Properties props = new Properties();
		/** Parâmetros de conexão com servidor Gmail */
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");

		Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(emailOrigem, senha);
			}
		});

		/** Ativa Debug para sessão */
		session.setDebug(true);

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("seuemail@gmail.com")); // Remetente

			Address[] toUser = InternetAddress // Destinatário(s)
					.parse(to);

			message.setRecipients(Message.RecipientType.TO, toUser);
			message.setSubject(subject);// Assunto
			// message.setContent(msgText1, "text/plain");
			// message.setContent(mensagem, "text/html; charset=utf-8");
			message.setContent(mensagem, "text/html");

			// if (filename != null && filename.equals("") == false) {
			// DataSource source = new FileDataSource(filename);
			// messageBodyPart.setDataHandler(new DataHandler(source));
			// messageBodyPart.setFileName("RELATÓRIO DE VISITAS "
			// + new SimpleDateFormat("dd-MM-yyyy HH")
			// .format(new Date()) + "h.xls");
			// multipart.addBodyPart(messageBodyPart);
			// message.setContent(multipart);
			// }

			/** Método para enviar a mensagem criada */
			Transport.send(message);

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}

	}

}
