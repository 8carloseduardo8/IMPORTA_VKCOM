package com.integrador;

import java.awt.GridLayout;
import java.io.FileNotFoundException;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;

public class ServletIntegradorNormal {

	public static Date timeInicial, timeFinal;

	public JFrame janela = new JFrame("INTEGRAÇÃO");

	public static void main(String argv[]) throws FileNotFoundException {

		ServletIntegradorNormal servletIntegradorNormal = new ServletIntegradorNormal();
		servletIntegradorNormal.iniciaInterfaceGrafica();

		timeInicial = new Date();

		new alteraSaidaPadrao().start();

		Finaliza f = new Finaliza();

		new com.integracao.mabra.Comunicador(f, servletIntegradorNormal).start();
		new com.integracao.cifarma.Comunicador(f, servletIntegradorNormal).start();
		new com.integracao.grbrj.Comunicador(f, servletIntegradorNormal).start();
		new com.integracao.grbsp.Comunicador(f, servletIntegradorNormal).start();
		// new com.integracao.millenium.Comunicador(f,
		// servletIntegradorNormal).start();
		new com.integracao.minasgerais.Comunicador(f, servletIntegradorNormal).start();
		new com.integracao.orgafarma.Comunicador(f, servletIntegradorNormal).start();
		new com.integracao.unidrogas.Comunicador(f, servletIntegradorNormal).start();
		new com.integracao.fidelize.Comunicador(f, servletIntegradorNormal).start();
		new com.integracao.metta.Comunicador(f, servletIntegradorNormal).start();
		new com.integracao.one.Comunicador(f, servletIntegradorNormal).start();
		new com.integracao.coremedic.Comunicador(f, servletIntegradorNormal).start();
		new com.integracao.baratela.Comunicador(f, servletIntegradorNormal).start();
		new com.integracao.ftb.Comunicador(f, servletIntegradorNormal).start();

		new com.integracao.jorgebatista.Comunicador(f, 12, servletIntegradorNormal).start();
		new com.integracao.jorgebatista.Comunicador(f, 13, servletIntegradorNormal).start();
		new com.integracao.jorgebatista.Comunicador(f, 14, servletIntegradorNormal).start();
		new com.integracao.jorgebatista.Comunicador(f, 15, servletIntegradorNormal).start();
		new com.integracao.jorgebatista.Comunicador(f, 16, servletIntegradorNormal).start();
		new com.integracao.jorgebatista.Comunicador(f, 17, servletIntegradorNormal).start();
		new com.integracao.jorgebatista.Comunicador(f, 18, servletIntegradorNormal).start();
		new com.integracao.jorgebatista.Comunicador(f, 19, servletIntegradorNormal).start();
		new com.integracao.jorgebatista.Comunicador(f, 23, servletIntegradorNormal).start();

		new com.integracao.medicamental.Comunicador(f, servletIntegradorNormal).start();
		new com.integracao.alfamed.Comunicador(f, servletIntegradorNormal).start();

		f.start();

	}

	public void iniciaInterfaceGrafica() {
		janela.setSize(800, 600);
		janela.setVisible(true);
		janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		GridLayout grid = new GridLayout(0, 2);
		janela.setLayout(grid);

	}

	public JButton adicionaBotao(String texto) {

		JButton botao = new JButton(texto);

		janela.add(botao);

		return botao;
	}

	public static class Finaliza extends Thread {

		public int qtdeExecucoes = 0;

		public void run() {
			while (true) {
				try {
					// espera 10 segundos
					sleep(1000 * 10);
					System.out.println("THREADS ATIVAS: " + qtdeExecucoes);
					if (qtdeExecucoes == 0) {

						timeFinal = new Date();

						System.out.println("TEMPO TOTAL DE EXECUÇÃO: " + (timeInicial.getTime() - timeFinal.getTime()));

						System.exit(1);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static class alteraSaidaPadrao extends Thread {

		public void run() {

			// while (true) {
			// try {
			// PrintStream out = new PrintStream(new FileOutputStream(
			// new File("C:\\VK-FARMA\\LOGINTEGRACAO\\"
			// + new SimpleDateFormat("yyyyMMddHHmmss")
			// .format(new Date()) + ".log")));
			// System.setOut(out);
			// } catch (FileNotFoundException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			//
			// Calendar c = Calendar.getInstance();
			// c.add(Calendar.DAY_OF_YEAR, 1);
			// c.set(Calendar.HOUR_OF_DAY, 0);
			// c.set(Calendar.MINUTE, 1);
			// c.set(Calendar.SECOND, 0);
			// c.set(Calendar.MILLISECOND, 0);
			//
			// long timeProximoDia = c.getTimeInMillis();
			// long timeAgora = new Date().getTime();
			//
			// try {
			// sleep(timeProximoDia - timeAgora);
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
			// }
		}
	}

}
