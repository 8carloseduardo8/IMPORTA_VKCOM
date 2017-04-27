package conect;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

import conect.Conexao;

public class Resultado {

	private Conexao conexao;
	private Statement statement;
	private ResultSet resultset;

	public Resultado(Conexao conexao, Statement statement, ResultSet resultset) {
		this.conexao = conexao;
		this.statement = statement;
		this.resultset = resultset;
	}

	public Conexao getConexao() {
		return conexao;
	}

	public String getString(String nome) throws Exception {
		return resultset.getString(nome);
	}

	public String getString(int posicao) throws Exception {
		return resultset.getString(posicao);
	}

	public int getInt(int indice) throws Exception {
		return resultset.getInt(indice);
	}

	public int getInt(String nome) throws Exception {
		return resultset.getInt(nome);
	}

	public long getLong(String nome) throws Exception {
		return resultset.getLong(nome);
	}

	public Date getDate(String nome) throws Exception {
		// String s = resultset.getString(nome);
		// if (s != null && s.equals("") == false)
		// return resultset.getDate(nome);
		// return null;
		Timestamp timestamp = resultset.getTimestamp(nome);
		return timestamp == null ? null : new Date(timestamp.getTime());
	}

	public Date getDate(int indice) throws Exception {
		// String s = resultset.getString(nome);
		// if (s != null && s.equals("") == false)
		// return resultset.getDate(nome);
		// return null;
		Timestamp timestamp = resultset.getTimestamp(indice);
		return timestamp == null ? null : new Date(timestamp.getTime());
	}

	public double getDouble(String nome) throws Exception {
		return resultset.getDouble(nome) + 0.00000001d;
	}

	public boolean next() throws Exception {
		return resultset.next();
	}

	public boolean previous() throws Exception {
		return resultset.previous();
	}

	public boolean first() throws Exception {
		return resultset.first();
	}

	public void close() throws Exception {
		if (conexao != null) {
			conexao.close(this);

			if (resultset != null) {
				resultset.close();
			}

			if (statement != null) {
				statement.close();
			}

			conexao = null;
			resultset = null;
		}
	}
}