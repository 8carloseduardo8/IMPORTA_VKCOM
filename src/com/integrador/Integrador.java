package com.integrador;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JFrame;

import com.integrador.ServletIntegradorNormal.Finaliza;

import conect.Conector;
import conect.Conexao;
import conect.Oracle;

public abstract class Integrador extends Thread {

	Finaliza f;
	JButton botao;
	String texto;

	public Integrador() {
	}

	public Integrador(Finaliza f, ServletIntegradorNormal integrador, String texto) {
		this.f = f;
		this.texto = texto;
		if (integrador == null) {
			this.botao = new JButton("");
		} else {
			this.botao = integrador.adicionaBotao(texto);
		}
	}

	@Override
	public void run() {

		if (f == null)
			f = new Finaliza();

		f.qtdeExecucoes++;

		botao.setText(texto + " - ENVIANDO PEDIDOS");
		try {
			enviaPedidos();
		} catch (Exception e) {
			e.printStackTrace();
		}
		botao.setText(texto + " - RECEBENDO CLIENTES");
		try {
			recebeClientes();
		} catch (Exception e) {
			e.printStackTrace();
		}
		botao.setText(texto + " - RECEBENDO PEDIDOS");
		try {
			recebePedidos();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		botao.setText(texto + " - RECEBENDO TITULOS");
		try {
			recebeTitulos();
		} catch (Exception e) {
			e.printStackTrace();
		}
		botao.setText(texto + " - RECEBENDO DEVOLUÇÕES");
		try {
			recebeDevolucoes();
		} catch (Exception e) {
			e.printStackTrace();
		}
		botao.setText(texto + " - RECEBENDO ESTOQUE");
		try {
			recebeEstoque();
		} catch (Exception e) {
			e.printStackTrace();
		}

		botao.setText(texto + " - FINALIZADO!");
		botao.setBackground(Color.GREEN);
		f.qtdeExecucoes--;

		return;
		//
		// int p = 100;
		// int t = 100;
		//
		// while (true) {
		// p++;
		// t++;
		//
		// try {
		// recebeClientes();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		//
		// try {
		// enviaPedidos();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		//
		// if (p >= 20) { // AGUARDA 20 MINUTOS
		// try {
		// p = 0;
		// recebePedidos();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// }
		// if (t >= (60 * 6)) { // AGUARDA 6 HORAS
		// try {
		// t = 0;
		// recebeTitulos();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// }
		// try {
		// sleep(1000 * 60 * 1); // ESPERA UM MINUTO
		// } catch (Exception e1) {
		// System.out.println("ERRO AO AGUARDAR!" + e1.getMessage());
		// e1.printStackTrace();
		// }
		// }
	}

	public abstract void enviaPedidos() throws Exception;

	public abstract void recebePedidos() throws Exception;

	public abstract void recebeTitulos() throws Exception;

	public abstract void recebeClientes() throws Exception;

	public abstract void recebeDevolucoes() throws Exception;

	public abstract void recebeEstoque() throws Exception;

	public void salvaRegistroLogExecucao(int canal) {
		Conexao con = Conector.getConexaoVK();
		con.executarNoEx(
				"UPDATE VEN_CANALINTEGRACAO SET ultimoacesso = SYSDATE WHERE CANAL = " + Oracle.strInsert(canal));
	}

}
