package conect;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SQLServer {
	public static String strInsert(String campo) {
		if (campo == null) {
			return "''";
		} else {
			return "'" + campo + "'";
		}
	}

	public static String strInsert(int campo) {
		return String.valueOf(campo);
	}

	public static String strInsert(Date campo) {
		if (campo == null) {
			return "null";
		} else {
			return "'" + new SimpleDateFormat("dd/MM/yyyy").format(campo) + "'";
		}
	}

	public static String strInsert(Double campo) {
		if (campo == null) {
			return "null";
		} else {
			return campo + "";
		}
	}
}
