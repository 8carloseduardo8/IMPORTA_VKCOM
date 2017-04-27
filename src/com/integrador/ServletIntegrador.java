package com.integrador;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ServletIntegrador {
	public static final String RECEBE_PEDIDOS = "0";
	public static final String ENVIA_PEDIDOS = "1";
	public static final String RECEBE_CLIENTES = "2";
	public static final String RECEBE_TITULOS = "3";

	public void executa() {
		int p = 100;
		int t = 100;

		while (true) {
			p++;
			t++;
			String comando = "";
			try {

				comando = "" + new File("").getAbsolutePath()
						+ "\\importaVKCOM.jar";
				new EXECUTAJAR(comando, RECEBE_CLIENTES).start();
				// System.out.println(comando);
				System.out.println("IMPORTANDO CLIENTES "
						+ new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
								.format(new Date()));

			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				comando = "" + new File("").getAbsolutePath()
						+ "\\importaVKCOM.jar ";
				new EXECUTAJAR(comando, ENVIA_PEDIDOS).start();
				// System.out.println(comando);
				System.out.println("ENVIANDO PEDIDOS "
						+ new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
								.format(new Date()));
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (p >= 20) { // AGUARDA 20 MINUTOS
				try {
					p = 0;
					comando = "" + new File("").getAbsolutePath()
							+ "\\importaVKCOM.jar ";
					new EXECUTAJAR(comando, RECEBE_PEDIDOS).start();
					// System.out.println(comando);
					System.out.println("RECEBENDO PEDIDOS "
							+ new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
									.format(new Date()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (t >= (60 * 6)) { // AGUARDA 6 HORAS
				try {
					t = 0;
					comando = "" + new File("").getAbsolutePath()
							+ "\\importaVKCOM.jar ";
					new EXECUTAJAR(comando, RECEBE_TITULOS).start();
					// System.out.println(comando);
					System.out.println("RECEBENDO TITULOS "
							+ new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
									.format(new Date()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			try {
				Thread.sleep(1000 * 60 * 1); // ESPERA UM MINUTO
			} catch (Exception e1) {
				System.out.println("ERRO AO AGUARDAR!" + e1.getMessage());
				e1.printStackTrace();
			}
		}
	}

	public static void recebeClientes() {
		try {
			new com.integracao.unidrogas.Comunicador()
					.recebeClientes();
		} catch (Exception e7) {
			// TODO Auto-generated catch block
			e7.printStackTrace();
		}
		try {
			new com.integracao.grbsp.Comunicador().recebeClientes();
		} catch (Exception e6) {
			// TODO Auto-generated catch block
			e6.printStackTrace();
		}
		try {
			new com.integracao.grbrj.Comunicador().recebeClientes();
		} catch (Exception e5) {
			// TODO Auto-generated catch block
			e5.printStackTrace();
		}
		try {
			new com.integracao.minasgerais.Comunicador().recebeClientes();
		} catch (Exception e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		try {
			new com.integracao.orgafarma.Comunicador().recebeClientes();
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		try {
			new com.integracao.medchap.Comunicador().recebeClientes();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			new com.integracao.focofarma.Comunicador().recebeClientes();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void recebePedidos() {
		try {
			new com.integracao.unidrogas.Comunicador().recebePedidos();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			new com.integracao.grbsp.Comunicador().recebePedidos();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			new com.integracao.grbrj.Comunicador().recebePedidos();
		} catch (Exception e5) {
			// TODO Auto-generated catch block
			e5.printStackTrace();
		}
		try {
			new com.integracao.minasgerais.Comunicador().recebePedidos();
		} catch (Exception e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		try {
			new com.integracao.orgafarma.Comunicador().recebePedidos();
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		try {
			new com.integracao.medchap.Comunicador().recebePedidos();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			new com.integracao.focofarma.Comunicador().recebePedidos();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void recebeTitulos() {
		try {
			new com.integracao.unidrogas.Comunicador().recebeTitulos();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			new com.integracao.grbsp.Comunicador().recebeTitulos();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			new com.integracao.grbrj.Comunicador().recebeTitulos();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			new com.integracao.minasgerais.Comunicador().recebeTitulos();
		} catch (Exception e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		try {
			new com.integracao.orgafarma.Comunicador().recebeTitulos();
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		try {
			new com.integracao.medchap.Comunicador().recebeTitulos();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			new com.integracao.focofarma.Comunicador().recebeTitulos();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void enviaPedidos() {
		try {
			new com.integracao.unidrogas.Comunicador().enviaPedidos();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			new com.integracao.grbsp.Comunicador().enviaPedidos();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			new com.integracao.grbrj.Comunicador().enviaPedidos();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			new com.integracao.minasgerais.Comunicador().enviaPedidos();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		try {
			new com.integracao.orgafarma.Comunicador().enviaPedidos();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			new com.integracao.medchap.Comunicador().enviaPedidos();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			new com.integracao.focofarma.Comunicador().enviaPedidos();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			new com.integracao.millenium.Comunicador().enviaPedidos();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String argv[]) {
		recebeTitulos();

		//
		// try {
		// System.out.println("CRIANDO CONEXÃO SOCKT");
		//
		// String porta = "900";
		// if (argv.length > 0)
		// porta += argv[0];
		//
		// ServerSocket serverSocket = new ServerSocket(
		// Integer.parseInt(porta));
		//
		// System.out.println("SOCKET CRIADO COM SUCESSO: PORTA " + porta);
		// } catch (IOException e) {
		// System.out.println("NAO FOI POSSIVEL CRIAR SOCKET! "
		// + e.getMessage());
		// System.exit(0);
		// }
		//
		// if (argv.length == 0)
		// new ServletIntegrador().executa();
		// else if (argv[0].equals(ENVIA_PEDIDOS)) {
		// enviaPedidos();
		// } else if (argv[0].equals(RECEBE_TITULOS)) {
		// recebeTitulos();
		// } else if (argv[0].equals(RECEBE_CLIENTES)) {
		// recebeClientes();
		// } else if (argv[0].equals(RECEBE_PEDIDOS)) {
		// recebePedidos();
		// }
		// if (argv.length > 0)
		// System.exit(Integer.parseInt(argv[0]));
	}

	private class EXECUTAJAR extends Thread {

		String comando;
		String argumento1;

		public EXECUTAJAR(String comando, String argumento1) {
			this.comando = comando;
			this.argumento1 = argumento1;
		}

		public void run() {
			try {

				List<String> command = new ArrayList<String>();
				command.add("java");
				command.add("-jar");
				command.add(comando);
				command.add(argumento1);

				ProcessBuilder builder = new ProcessBuilder(command);
				Map<String, String> environ = builder.environment();

				final Process process = builder.start();
				InputStream is = process.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				while ((line = br.readLine()) != null) {
					// System.out.println(line);
				}

				String exec = "";
				if (process.exitValue() == Integer.parseInt(ENVIA_PEDIDOS))
					exec = "ENVIA PEDIDOS";
				else if (process.exitValue() == Integer
						.parseInt(RECEBE_CLIENTES))
					exec = "RECEBE CLIENTES";
				else if (process.exitValue() == Integer
						.parseInt(RECEBE_PEDIDOS))
					exec = "RECEBE PEDIDOS";
				else if ((process.exitValue() == Integer
						.parseInt(RECEBE_TITULOS)))
					exec = "RECEBE TITULOS";

				System.out.println("Program terminated! >> " + exec);

				// Runtime rt = Runtime.getRuntime();
				// Process proc = rt.exec(comando);
				// int exitVal = proc.waitFor();
				// System.out.println("Process exitValue: " + exitVal);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
}
