package com.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import br.com.javac.v300.procnfe.TNfeProc;

public class lerXML_JAXB {

	@SuppressWarnings("unchecked")
	public static TNfeProc getNFe(String arquivo) throws Exception {

		String xml = lerXML(arquivo);

		try {
			JAXBContext context = JAXBContext.newInstance(TNfeProc.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			TNfeProc nfe = unmarshaller.unmarshal(
					new StreamSource(new StringReader(xml)), TNfeProc.class)
					.getValue();
			return nfe;
		} catch (JAXBException ex) {
			ex.printStackTrace();
			throw new Exception(ex.toString());
		}
	}

	// public static TNfeProc Ler_xml(String xml) throws Exception {
	// // JAXBContext context1 =
	// // JAXBContext.newInstance("com.smp.v310.EnviNFe");
	//
	// // BufferedReader br = new BufferedReader(new FileReader(new
	// // File(xml)));
	// // String w = "";
	// // String s = null;
	// // while ((s = br.readLine()) != null) {
	// // w += s.replaceAll("xmlns=\"http://www.portalfiscal.inf.br/nfe\"",
	// // "");
	// // }
	// // br.close();
	// //
	// // BufferedWriter bw = new BufferedWriter(new FileWriter(new
	// // File(xml)));
	// // bw.write(w);
	// // bw.flush();
	// // bw.close();
	//
	// JAXBContext context1 = JAXBContext.newInstance(TNfeProc.class);
	//
	// Unmarshaller unmarshaller1 = context1.createUnmarshaller();
	// JAXBElement<TNfeProc> element = (JAXBElement<TNfeProc>) unmarshaller1
	// .unmarshal(new File(xml));
	// return element.getValue();
	// }

	private static String lerXML(String fileXML) throws IOException {
		String linha = "";
		StringBuilder xml = new StringBuilder();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(fileXML)));
		while ((linha = in.readLine()) != null) {
			xml.append(linha);
		}
		in.close();

		return xml.toString();
	}

	private static void info(String log) {
		System.out.println("INFO: " + log);
	}

	private static void error(String log) {
		System.out.println("ERROR: " + log);
	}

}