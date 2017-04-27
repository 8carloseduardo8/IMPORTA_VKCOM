package conect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import br.com.importa.vkcom.core.LogUtil;
import conect.Oracle;
import conect.Resultado;

public class Conexao {

	protected Connection connection;
	protected long transacoes = 0;
	protected boolean rollback = false;
	protected List<Resultado> resultados;
	protected String historico;
	protected boolean ocorreuerro = false;

	public static final int UNIDROGAS = 0;
	public static final int MINASGERAIS = 9;
	public static final int CIFARMAGO = 8;
	public static final int MABRA = 10;
	public static final int VENDAS = 1;
	public static final int GRBRJ = 2;
	public static final int GRBSP = 2;
	public static final int VISITALK = 2;
	public static final int MIG = 2;
	public static final int SICOM = 2;

	public static String FORMAT_DATA_BANCO = "dd-MM-yyyy";

	public int tipoConexao;

	public Conexao(Connection connection, int tipoConexao) {
		this.connection = connection;
		this.tipoConexao = tipoConexao;
	}

	public int executar(List<String> lista) throws Exception {
		Statement statement = null;
		int resultado = 0;

		String sql = "";

		try {
			connection.setAutoCommit(false);
			for (String s : lista) {
				sql = s;
				historico += "\nExecutar: " + sql;
				LogUtil.info(Oracle.class, sql);

				statement = connection.createStatement();
				// registraLogSQL(sql, "A");
				resultado = statement.executeUpdate(sql);
				// registraLogSQL(sql, "D");
			}

		} catch (Exception exception) {
			ocorreuerro = true;
			if (connection != null) {
				connection.rollback();
			}
			exception.printStackTrace();
			throw new Exception("Exception ao executar (" + sql + "), " + exception.getMessage(), exception);

		} finally {
			if (connection != null) {
				connection.commit();
			}
			if (statement != null) {
				statement.close();
			}
			connection.setAutoCommit(true);
		}

		return resultado;
	}

	public int executar(String sql) throws Exception {
		Statement statement = null;
		int resultado = 0;

		LogUtil.info(Conexao.class, sql);

		try {
			connection.setAutoCommit(true);
			historico += "\nExecutar: " + sql;

			statement = connection.createStatement();
			// registraLogSQL(sql, "A");
			resultado = statement.executeUpdate(sql);
			// registraLogSQL(sql, "D");

		} catch (Exception exception) {
			ocorreuerro = true;
			exception.printStackTrace();
			throw new Exception("Exception ao executar (" + sql + "), " + exception.getMessage(), exception);

		} finally {
			if (statement != null) {
				statement.close();
			}
		}

		return resultado;
	}

	public int executarNoEx(String sql) {
		Statement statement = null;
		int resultado = 0;

		LogUtil.info(Conexao.class, sql);

		try {
			connection.setAutoCommit(true);

			historico += "\nExecutar: " + sql;

			statement = connection.createStatement();
			// registraLogSQL(sql, "A");
			resultado = statement.executeUpdate(sql);
			// registraLogSQL(sql, "D");

		} catch (Exception exception) {
			ocorreuerro = true;
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
				}
			}
		}

		return resultado;
	}

	public Resultado consultar(String sql) throws Exception {
		Statement statement = null;
		ResultSet resultset = null;

		try {
			historico += "\nConsultar: " + sql;
			LogUtil.info(Conexao.class, sql);
			// statement = connection.createStatement();
			statement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			resultset = statement.executeQuery(sql);

			if (resultados == null) {
				resultados = new ArrayList<Resultado>();
			}

			Resultado resultado = new Resultado(this, statement, resultset);
			resultados.add(resultado);

			return resultado;

		} catch (Exception exception) {
			ocorreuerro = true;

			// if (statement == null || resultset == null || connection == null
			// || statement.isClosed() || resultset.isClosed()
			// || connection.isClosed()) {
			// switch (tipoConexao) {
			// case UNIDROGAS:
			// return Conector.getConexaoUnidrogas().consultar(sql);
			// }
			// }

			if (resultset != null) {
				resultset.close();
			}

			if (statement != null) {
				statement.close();
			}
			exception.printStackTrace();
			throw new Exception("Exception ao executar (" + sql + "), " + exception.getMessage(), exception);
		}
	}

	public Resultado consultarSQLServer(String sql) throws Exception {
		PreparedStatement pstatement = null;
		ResultSet rs = null;

		try {
			historico += "\nConsultar: " + sql;
			LogUtil.info(Conexao.class, historico);
			pstatement = connection.prepareStatement(sql);
			// pstatement.setString(1, search);
			rs = pstatement.executeQuery();

			if (resultados == null) {
				resultados = new ArrayList<Resultado>();
			}

			Resultado resultado = new Resultado(this, pstatement, rs);
			resultados.add(resultado);

			return resultado;

		} catch (Exception exception) {
			ocorreuerro = true;
			if (rs != null) {
				rs.close();
			}

			if (pstatement != null) {
				pstatement.close();
			}

			throw new Exception("Exception ao executar (" + sql + "), " + exception.getMessage(), exception);
		}
	}

	public void iniciar() throws Exception {
		Connection conn = connection;

		try {
			if (conn.getAutoCommit()) {
				historico += "\nBegin Transaction";
				LogUtil.info(Conexao.class, historico);
				conn.setAutoCommit(false);
			}
		} catch (Exception exception) {
			ocorreuerro = true;
			throw new Exception("Exception ao iniciar transação.", exception);
		}

		transacoes += 1;
	}

	public void concluir() throws Exception {
		if (!rollback) {
			if (transacoes > 0) {
				transacoes -= 1;

				if (transacoes == 0) {
					Connection conn = connection;
					try {
						historico += "\nCommit";
						LogUtil.info(Conexao.class, historico);
						conn.commit();
						conn.setAutoCommit(true);

					} catch (Exception exception) {
						ocorreuerro = true;
						throw new Exception("Exception ao finalizar transação.", exception);
					}
				}
			} else {
				throw new Exception("Não há transações ativas.");
			}
		} else {
			throw new Exception("A transação atual está em situação de rollback.");
		}
	}

	public void cancelar() throws Exception {
		if (transacoes > 0) {
			transacoes -= 1;

			if (transacoes == 0) {
				Connection conn = connection;

				try {
					historico += "\nCommit";
					LogUtil.info(Conexao.class, historico);
					conn.rollback();
					conn.setAutoCommit(true);

					rollback = false;
				} catch (Exception exception) {
					ocorreuerro = true;
					throw new Exception("Exception ao finalizar transação.", exception);
				}
			} else {
				rollback = true;
			}
		} else {
			throw new Exception("Não há transações ativas.");
		}
	}

	public void close() {
		historico += "\nFechar";
		if (resultados != null) {
			while (!resultados.isEmpty()) {
				try {
					resultados.get(0).close();
				} catch (Exception exception) {
					ocorreuerro = true;
				}
			}
			resultados = null;
		}

		if (connection != null) {
			try {
				connection.close();
			} catch (Exception exception) {
				ocorreuerro = true;
			}
			connection = null;

			if (ocorreuerro) {
				LogUtil.error(Conexao.class, historico);
			}
		}
	}

	public void close(Resultado resultado) throws Exception {
		if (resultados.contains(resultado)) {
			resultados.remove(resultado);
			resultado.close();
		}
	}

	// public void registraLogSQL(String linha, String sit) {
	//
	// String sql = "insert into ven_logsql (time, sql,sit) values (sysdate,"
	// + Oracle.strInsert(linha.replaceAll("'", "''")) + ","
	// + Oracle.strInsert(sit) + ")";
	// try {
	// Statement statement = null;
	// statement = connection.createStatement();
	// statement.executeUpdate(sql);
	// statement.close();
	// } catch (Exception e) {
	// }
	// }
}
