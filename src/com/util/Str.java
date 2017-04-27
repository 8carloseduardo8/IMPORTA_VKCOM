package com.util;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;

public class Str {

	public static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

	public static String[] split(String texto, String regex) {
		try {
			List<String> ss = new ArrayList<String>();

			String c = "";
			for (int i = 0; i < texto.length(); i++) {
				String x = texto.substring(i, i + 1);

				if (x.equals(regex)) {
					ss.add(c);
					c = "";
				} else {
					c += x;
				}
			}
			ss.add(c);

			String ret[] = new String[ss.size()];
			for (int i = 0; i < ss.size(); i++)
				ret[i] = ss.get(i);

			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return new String[] { "" };
		}
	}

	public static List<String> getDiasSemana() {
		List<String> lista = new ArrayList<String>();

		lista.add("SEGUNDA FEIRA");
		lista.add("TERÇA FEIRA");
		lista.add("QUARTA FEIRA");
		lista.add("QUINTA FEIRA");
		lista.add("SEXTA FEIRA");
		lista.add("SABADO");

		return lista;
	}

	public static int getStringDIA(String diaSemana) {
		if (diaSemana.equals("SEGUNDA FEIRA"))
			return 2;
		if (diaSemana.equals("TERÇA FEIRA"))
			return 3;
		if (diaSemana.equals("QUARTA FEIRA"))
			return 4;
		if (diaSemana.equals("QUINTA FEIRA"))
			return 5;
		if (diaSemana.equals("SEXTA FEIRA"))
			return 6;
		if (diaSemana.equals("SABADO"))
			return 7;
		return 0;
	}

	public static String getStringDIA(int diaSemana) {
		String retorno = "";
		switch (diaSemana) {
		case 2:
			retorno = "SEGUNDA FEIRA";
			break;
		case 3:
			retorno = "TERÇA FEIRA";
			break;
		case 4:
			retorno = "QUARTA FEIRA";
			break;
		case 5:
			retorno = "QUINTA FEIRA";
			break;
		case 6:
			retorno = "SEXTA FEIRA";
			break;
		case 7:
			retorno = "SABADO";
			break;
		}
		return retorno;
	}

	public static String alinhaZeroDireita(String texto, int size) {
		String ret = "";

		int qntZeros = size - texto.length();

		for (int i = 0; i < qntZeros; i++)
			ret += "0";

		ret += texto;
		return ret;
	}

	public static String repeat(String texto, int size) {
		String ret = "";

		for (int i = 1; i <= size; i++)
			ret += texto;

		return ret;
	}

	public static double arredondar2(double valor) {
		double arredondado = valor;
		arredondado *= (Math.pow(10, 2));
		arredondado = Math.floor(arredondado);
		arredondado /= (Math.pow(10, 2));
		return arredondado;
	}

	public static String removeAcento(String texto) {
		CharSequence cs = new StringBuilder(texto == null ? "" : texto);
		return Normalizer.normalize(cs, Normalizer.Form.NFKD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}
}
