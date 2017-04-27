package conect;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import br.com.importa.vkcom.core.LogUtil;
import conect.Conexao;

public class Conector {

	private static Conexao conUnidrogas;
	private static Conexao conMinasGerais;
	private static Conexao conCifarmaGO;
	private static Conexao conMabra;
	private static Conexao conGRBRJ;
	private static Conexao conGRBSP;
	private static Conexao conVendas;
	private static Conexao conMig;
	private static Conexao conSicom;

	public static Conexao getConexaoUnidrogas() {

		try {
			if (conUnidrogas != null && conUnidrogas.connection.isClosed()) {
				conUnidrogas = null;
			}
		} catch (SQLException e1) {
			conUnidrogas = null;
		}

		if (conUnidrogas != null)
			return conUnidrogas;

		try {
			Class.forName("oracle.jdbc.OracleDriver");

			Properties props = new Properties();
			props.setProperty("user", "geral");
			props.setProperty("password", "senhas");
			// props.setProperty(
			// OracleConnection.CONNECTION_PROPERTY_THIN_NET_CONNECT_TIMEOUT,
			// "60000");

			String strConexao = "jdbc:oracle:thin:@10.5.101.225:1521:DADOUNDG";
			Connection con = DriverManager.getConnection(strConexao, props);
			conUnidrogas = new Conexao(con, Conexao.UNIDROGAS);

			return conUnidrogas;
		} catch (Exception e) {
			LogUtil.error(Conector.class, "Erro ao criar conexao: " + e.getMessage());
		}
		return null;
	}

	public static Conexao getConexaoCifarmaGO() {

		try {
			if (conCifarmaGO != null && conCifarmaGO.connection.isClosed()) {
				conCifarmaGO = null;
			}
		} catch (SQLException e1) {
			conCifarmaGO = null;
		}

		if (conCifarmaGO != null)
			return conCifarmaGO;

		try {
			Class.forName("oracle.jdbc.OracleDriver");

			Properties props = new Properties();
			props.setProperty("user", "geral");
			props.setProperty("password", "senhas");

			String strConexao = "jdbc:oracle:thin:@10.5.100.240:1521:DADOUNDG";
			Connection con = DriverManager.getConnection(strConexao, props);
			conCifarmaGO = new Conexao(con, Conexao.CIFARMAGO);

			return conCifarmaGO;
		} catch (Exception e) {
			System.err.println("Exception AO CRIAR CONEXAO: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	public static Conexao getConexaoMabra() {

		try {
			if (conMabra != null && conMabra.connection.isClosed()) {
				conMabra = null;
			}
		} catch (SQLException e1) {
			conMabra = null;
		}

		if (conMabra != null)
			return conMabra;

		try {
			Class.forName("oracle.jdbc.OracleDriver");

			Properties props = new Properties();
			props.setProperty("user", "geral");
			props.setProperty("password", "senhas");

			String strConexao = "jdbc:oracle:thin:@10.5.101.8:1521:DADOUNDG";
			Connection con = DriverManager.getConnection(strConexao, props);
			conMabra = new Conexao(con, Conexao.MABRA);

			return conMabra;
		} catch (Exception e) {
			System.err.println("Exception AO CRIAR CONEXAO: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	public static Conexao getConexaoMinasGerais() {

		try {
			if (conMinasGerais != null && (conMinasGerais.connection != null && conMinasGerais.connection.isClosed())) {
				conMinasGerais = null;
			}
		} catch (SQLException e1) {
			conMinasGerais = null;
		}

		if (conMinasGerais != null && conMinasGerais.connection != null)
			return conMinasGerais;

		try {
			Class.forName("oracle.jdbc.OracleDriver");

			Properties props = new Properties();
			props.setProperty("user", "geral");
			props.setProperty("password", "senhas");
			// props.setProperty(
			// OracleConnection.CONNECTION_PROPERTY_THIN_NET_CONNECT_TIMEOUT,
			// "60000");

			String strConexao = "jdbc:oracle:thin:@192.168.0.103:1521:DADOUNDG";
			Connection con = DriverManager.getConnection(strConexao, props);
			conMinasGerais = new Conexao(con, Conexao.MINASGERAIS);

			return conMinasGerais;
		} catch (Exception e) {
			LogUtil.error(Conector.class, "Erro ao criar conexao: " + e.getMessage());
		}
		return null;
	}

	public static Conexao getConexaoGRBRJ() {

		try {
			if (conGRBRJ != null && conGRBRJ.connection.isClosed()) {
				conGRBRJ = null;
			}
		} catch (SQLException e1) {
			conGRBRJ = null;
		}

		if (conGRBRJ != null)
			return conGRBRJ;

		try {
			Class.forName("oracle.jdbc.OracleDriver");

			String strConexao = "jdbc:oracle:thin:@10.3.100.240:1521:DADOUNDG";

			Connection con = DriverManager.getConnection(strConexao, "geral", "senhas");
			conGRBRJ = new Conexao(con, Conexao.GRBRJ);

			return conGRBRJ;
		} catch (Exception e) {
			LogUtil.error(Conector.class, "Erro ao criar conexao: " + e.getMessage());
		}
		return null;
	}

	public static Conexao getConexaoGRBSP() {

		try {
			if (conGRBSP != null && (conGRBSP.connection != null && conGRBSP.connection.isClosed())) {
				conGRBSP = null;
			}
		} catch (SQLException e1) {
			conGRBSP = null;
		}

		if (conGRBSP != null && conGRBSP.connection != null)
			return conGRBSP;

		try {
			Class.forName("oracle.jdbc.OracleDriver");

			String strConexao = "jdbc:oracle:thin:@10.5.101.228:1521:DADOUNDG";

			Connection con = DriverManager.getConnection(strConexao, "geral", "senhas");
			conGRBSP = new Conexao(con, Conexao.GRBSP);

			return conGRBSP;
		} catch (Exception e) {
			LogUtil.error(Conector.class, "Erro ao criar conexao: " + e.getMessage());
		}
		return null;
	}

	public static Conexao getConexaoVK() {
		try {
			if (conVendas != null && conVendas.connection.isClosed()) {
				conVendas = null;
			}
		} catch (SQLException e1) {
			conVendas = null;
		}

		if (conVendas != null)
			return conVendas;

		try {
			Class.forName("oracle.jdbc.OracleDriver");

			String strConexao = "jdbc:oracle:thin:@10.5.101.29:1521:DADOUNDG";
			// String strConexao = "jdbc:oracle:thin:@7.222.12.11:1521:XE";

			Connection con = DriverManager.getConnection(strConexao, "visitacao", "visitacao");
			conVendas = new Conexao(con, Conexao.VENDAS);

			return conVendas;
		} catch (Exception e) {
			LogUtil.error(Conector.class, "Erro ao criar conexao: " + e.getMessage());
		}
		return null;
	}

	// public static Conexao getConexaoVisitalk() {
	//
	// if (conVisitalk != null)
	// return conVisitalk;
	//
	// try {
	// Class.forName("oracle.jdbc.OracleDriver");
	//
	// String strConexao = "jdbc:oracle:thin:@10.5.101.29:1521:XE";
	//
	// Connection con = DriverManager.getConnection(strConexao,
	// "visitacao", "visitacao");
	// conVisitalk = new Conexao(con, Conexao.VISITALK);
	//
	// return conVisitalk;
	// } catch (Exception e) {
	// System.err.println("Exception AO CRIAR CONEXAO: " + e.getMessage());
	// e.printStackTrace();
	// }
	// return null;
	// }

	public static Conexao getConexaoMig() {

		if (conMig != null)
			return conMig;

		try {
			Class.forName("oracle.jdbc.OracleDriver");

			String strConexao = "jdbc:oracle:thin:@10.5.101.110:1521:BDMIG";

			Connection con = DriverManager.getConnection(strConexao, "dbreport", "dbreport");
			conMig = new Conexao(con, Conexao.MIG);

			return conMig;
		} catch (Exception e) {
			LogUtil.error(Conector.class, "Erro ao criar conexao: " + e.getMessage());
		}
		return null;
	}

	public static Conexao getConexaoSicom() throws Exception {

		if (conSicom != null)
			return conSicom;

		// string de conexão...usando Windows Authentication
		String connectionUrl = "jdbc:sqlserver://10.5.100.171:1433" + ";databaseName=SicomNet";

		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver").newInstance();
			// Abre a conexão
			Connection conn = DriverManager.getConnection(connectionUrl, "sa", "Qazwsx3s");
			// imprime na tela
			return new Conexao(conn, Conexao.SICOM);
		} catch (SQLException ex) {
			// se ocorrem erros de conexão
			LogUtil.info(Conector.class, "SQLException: " + ex.getMessage());
			LogUtil.info(Conector.class, "SQLState: " + ex.getSQLState());
			LogUtil.info(Conector.class, "VendorError: " + ex.getErrorCode());
			throw new Exception(ex);
		} catch (Exception e) {
			// se ocorrerem outros erros
			LogUtil.info(Conector.class, "Problemas ao tentar conectar com o banco de dados: " + e);
			throw new Exception(e);
		}
	}
}
