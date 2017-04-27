package com.integracao.poupafarma;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.net.ftp.FTPClient;
import com.integrador.Integrador;

import br.com.smp.vk.venda.model.Pedido;

public class Comunicador extends Integrador {

	private final int canal = 8;

	private FTPClient ftp = new FTPClient();

	private String host;// = "187.4.255.101";
	private String usuario;// = "Cifarma";
	private String senha;// = "cf1557";

	private String pastaPedido = "/cif_envio";
	private String pastaNFe = "/cif_retorno";

	public Comunicador() {
		conectar();
	}

	private void conectar() {
		// try {
		// ftp.connect(host);
		//
		// // verifica se conectou com sucesso!
		// if (FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
		// ftp.login(usuario, senha);
		// } else {
		// // erro ao se conectar
		// ftp.disconnect();
		// System.out.println("Conexão recusada");
		// System.exit(1);
		// }
		// System.out.println("CONECTADO...");
		// } catch (Exception e) {
		// System.out.println("Ocorreu um erro: " + e);
		// e.printStackTrace();
		// System.exit(1);
		// }
	}

	private File geraArquivoPedido(Pedido pedido) throws Exception {
		return null;
	}

	private void enviaPedido(File arquivo) throws IOException {
		enviaArquivo(pastaPedido, arquivo.getAbsolutePath());
	}

	private void enviaArquivo(String pasta, String arquivo) throws IOException {
		// para cada arquivo informado...
		// abre um stream com o arquivo a ser enviado
		InputStream is = new FileInputStream(arquivo);
		// pega apenas o nome do arquivo
		int idx = arquivo.lastIndexOf(File.separator);
		if (idx < 0)
			idx = 0;
		else
			idx++;
		String nomeArquivo = arquivo.substring(idx, arquivo.length());

		System.out.println("ENVIANDO ARQUIVO TEXTO");
		ftp.setFileType(FTPClient.ASCII_FILE_TYPE);

		// ajusta o tipo do arquivo a ser enviado
		// if (arquivo.endsWith(".txt")) {
		// System.out.println("ENVIANDO ARQUIVO TEXTO");
		// ftp.setFileType(FTPClient.ASCII_FILE_TYPE);
		// } else if (arquivo.endsWith(".jpg")) {
		// ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
		// } else {
		// ftp.setFileType(FTPClient.ASCII_FILE_TYPE);
		// }
		System.out.println("Pasta " + pasta);
		System.out.println("Enviando arquivo " + nomeArquivo + "...");

		// faz o envio do arquivo
		ftp.mkd(pasta);
		ftp.changeWorkingDirectory(pasta);
		String dirs[] = pasta.split("/");
		for (String dir : dirs) {
			System.out.println("mkdir: " + dir);
			ftp.mkd(dir);
			ftp.changeWorkingDirectory(dir);
		}

		ftp.storeFile(nomeArquivo, is);
		is.close();
		System.out.println("Arquivo " + nomeArquivo + " enviado com sucesso!");
	}

	@Override
	public void enviaPedidos() throws Exception {

	}

	@Override
	public void recebePedidos() throws Exception {
		String arquivo = "F:\\00000031.RMP";
		recebePedido(arquivo);
	}

	public void recebePedido(String arquivo) throws IOException {
		BufferedReader file = new BufferedReader(new FileReader(arquivo));
		String l;
		while ((l = file.readLine()) != null) {

			if (l.startsWith("A")) {
				String codigoCliente = l.substring(1, 9);
				String tipoVenda = l.substring(9, 11);
				String tipoFaturamento = l.substring(11, 12);
				String versaoSistema = l.substring(12, 13);
				String tipoRetorno = l.substring(13, 14);

				System.out.println("CÓDIGO CLIENTE  : " + codigoCliente);
				System.out.println("TIPO VENDA      : " + tipoVenda);
				System.out.println("TIPO FATURAMENTO: " + tipoFaturamento);
				System.out.println("VERSAO SISTEMA  : " + versaoSistema);
				System.out.println("TIPO RETORNO    : " + tipoRetorno);

			} else if (l.startsWith("B")) {
				String codigoProduto = l.substring(1, 7);
				String quantidadePedida = l.substring(7, 14);
				System.out.println("CÓDIGO PRODUTO  : " + codigoProduto);
				System.out.println("QTDE PEDIDA     : " + quantidadePedida);
			} else if (l.startsWith("C")) {
				String obs = l.substring(1, l.length());
				System.out.println("OBSERVAÇÃO      : " + obs);
			} else if (l.startsWith("D")) {
				String numeroPedido = l.substring(1, 9);
				System.out.println("NÚMERO PEDIDO   : " + numeroPedido);
			}

			System.out.println("");

		}

		// file.readLine();
	}

	@Override
	public void recebeTitulos() throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public void recebeClientes() throws Exception {
		// TODO Auto-generated method stub
	}

	public static void main(String argv[]) {
		Comunicador con = new Comunicador();
		try {
			con.recebePedidos();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void recebeDevolucoes() throws Exception {

		return;
	}

	@Override
	public void recebeEstoque() throws Exception {
		// TODO Auto-generated method stub
		
	}

}
