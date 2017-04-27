package com.integracao.nortefarma;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import com.integrador.Integrador;

import br.com.core.util.TextUtil;
import br.com.importa.vkcom.core.LogUtil;
import br.com.smp.vk.venda.model.Cliente;
import br.com.smp.vk.venda.model.Pedido;
import br.com.smp.vk.venda.model.PedidoItem;
import br.com.smp.vk.venda.model.Produto;
import vendas.dao.ClienteDao;
import vendas.dao.PedidoDao;
import vendas.dao.ProdutoDao;

public class Comunicador extends Integrador {

	private static final int canal = 26;

	private static FTPClient ftp = new FTPClient();

	private static final String host = "";
	private static final String usuario = "";
	private static final String senha = "";
	private static final int porta = 21;

	private static final String cnpjNorteFarma = "00006010000134 ";
	private static final String cnpjCifarma = "17562075000169 ";

	private static final String pastaPedido = "/";
	private static final String pastaNFe = "/";
	private static final String pastaRetorno = "/";
	private static final String pastaBackup = "/";

	public static void main(String argv[]) {
		try {
			new Comunicador().enviaPedidos();
			// new Comunicador().processaArquivos();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Comunicador() {
	}

	private static void conectar() {

		if (!ftp.isConnected()) {
			try {
				ftp.connect(host, porta);

				// verifica se conectou com sucesso!
				if (FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
					LogUtil.info(Comunicador.class, "NORTEFARMA: Conectando ao FTP");
					ftp.login(usuario, senha);
					LogUtil.info(Comunicador.class, "NORTEFARMA: Diretorio atual: " + ftp.printWorkingDirectory());
				} else {
					// erro ao se conectar
					ftp.disconnect();
					LogUtil.info(Comunicador.class, "NORTEFARMA: Conexão recusada");
				}
				ftp.enterLocalPassiveMode();
			} catch (Exception e) {
				LogUtil.error(Comunicador.class, "NORTEFARMA: Ocorreu um erro inesperado.... " + e.getMessage());
			}
		}
	}

	private static File geraArquivoPedido(Pedido pedido) throws Exception {

		LogUtil.info(Comunicador.class, "NORTEFARMA: Gerando arquivo de pedido");

		LocalDate data = LocalDate.now();
		DateTimeFormatter formatData = DateTimeFormatter.ofPattern("yyyyMMdd");

		LocalTime hora = LocalTime.now();
		DateTimeFormatter formatHora = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);

		File file = new File("C:\\ATUA\\NORTEFARMA\\CIF_" + data.format(formatData) + "_"
				+ TextUtil.padLeft(String.valueOf(pedido.numero), 8, "0") + ".PED");
		BufferedWriter fw = new BufferedWriter(new FileWriter(file));

		// Busca dados do cliente.
		Cliente cli = new ClienteDao().getCliente(pedido.cnpj);
		if (cli == null) {
			// LogUtil.error(Comunicador.class, "NORTEFARMA: Cliente não
			// encontrado.... " + pedido.cnpj);
			fw.flush();
			fw.close();
			throw new Exception("NORTEFARMA: Cliente não encontrado.... " + pedido.cnpj);
		}

		// HEADER CABECALHO DO PEDIDO - 70
		String s;
		s = "1";
		s += "V";
		s += TextUtil.padRight("", 9, " ");
		s += TextUtil.padLeft(cli.cnpj, 14, " ");
		s += TextUtil.padLeft(cli.razao, 50, " ");
		s += TextUtil.padLeft("", 6, " "); // codigo vendedor
		s += TextUtil.padLeft(pedido.vendedorNome, 30, " "); // nome vendedor
		s += new SimpleDateFormat("ddMMyyyy").format(pedido.data);
		s += new SimpleDateFormat("ddMMyyyy").format(pedido.dataCriacao);
		s += TextUtil.padLeft(cli.telefone1, 15, " "); // telefone cliente
		s += TextUtil.padLeft(pedido.clienteUF, 2, " "); // uf cliente
		s += TextUtil.padLeft(pedido.clienteCidade, 40, " "); // municipio
																// cliente
		s += TextUtil.padLeft(cli.endereco + " " + cli.bairro + " ", 50, " "); // endereco
																				// cliente
		s += TextUtil.padLeft(cli.cep, 8, "0"); // cep cliente
		s += TextUtil.padLeft(String.valueOf(pedido.prazo), 5, " "); // cod da
																		// cond
																		// pgto
		s += TextUtil.padLeft(pedido.prazoNome, 35, " "); // descricao da cond
															// pgto
		s += TextUtil.padLeft("BOLETO", 20, " "); // descricao forma de pgto
		s += TextUtil.padLeft(cnpjNorteFarma, 14, " "); // cnpj operador
														// logistico
		s += TextUtil.padLeft(pedido.obs, 86, " "); // observacao cliente
		s += TextUtil.padLeft(String.valueOf(pedido.numero), 15, " "); // numero
																		// do
																		// pedido
		fw.write(s);
		fw.newLine();

		// DETALHE ITENS DO PEDIDO
		int linhas = 0;
		for (PedidoItem i : pedido.itens) {
			Produto prod = new ProdutoDao().getProduto(i.produto);

			s = "2";
			s += TextUtil.padLeft(prod.ean, 13, "0");
			s += TextUtil.padLeft(prod.codigo, 6, "0");
			s += TextUtil.padLeft(prod.descricao, 40, " ");
			s += " ";
			s += TextUtil.padLeft(prod.unidade, 3, " ");
			s += " ";
			s += TextUtil.padLeft(String.valueOf(i.qntVenda).replace(",", "").replace(".", ""), 12, "0");
			s += " ";
			String vlrUnitario = new DecimalFormat("0.00").format(i.valorUnitario);
			s += TextUtil.padLeft(vlrUnitario.replace(",", "").replace(".", ""), 12, "0");
			s += " ";
			String vlrTotal = new DecimalFormat("0.00").format(i.qntVenda * i.valorUnitario);
			s += TextUtil.padLeft(vlrTotal.replace(",", "").replace(".", ""), 12, "0");
			s += " ";
			String vlrBruto = new DecimalFormat("0.00").format(i.valorBruto);
			s += TextUtil.padLeft(vlrBruto.replace(",", "").replace(".", ""), 12, "0");
			s += " ";
			String percDesconto = new DecimalFormat("0.00")
					.format(((((i.qntBonificacao + i.qntVenda) * i.valorBruto) - (i.qntVenda * i.valorUnitario))
							/ ((i.qntBonificacao + i.qntVenda) * i.valorBruto)) * 100);

			s += TextUtil.padLeft(String.valueOf(percDesconto).replace(",", "").replace(".", ""), 12, "0");
			s += " ";
			s += TextUtil.padLeft(String.valueOf(pedido.numero), 9, " ");
			s += TextUtil.padLeft("", 13, " ");
			s += TextUtil.padLeft("", 250, " ");

			fw.write(s);
			linhas++;
			fw.newLine();
		}

		// TRAILER
		s = "9";
		linhas = linhas + 2;
		s += TextUtil.padLeft(String.valueOf(linhas), 5, "0");
		s += TextUtil.padLeft("", 393, " ");

		fw.write(s);
		fw.newLine();

		fw.flush();
		fw.close();

		return file;
	}

	private static void enviaPedido(File arquivo) throws IOException {
		enviaArquivo(pastaPedido, arquivo.getAbsolutePath());
	}

	private static void enviaArquivo(String pasta, String arquivo) {

		InputStream is = null;
		try {
			is = new FileInputStream(arquivo);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		int idx = arquivo.lastIndexOf(File.separator);
		if (idx < 0) {
			idx = 0;
		} else {
			idx++;
		}
		String nomeArquivo = arquivo.substring(idx, arquivo.length());
		try {
			LogUtil.info(Comunicador.class, "NORTEFARMA: Enviando arquivo texto. Pasta: " + pasta);
			ftp.setFileType(FTPClient.ASCII_FILE_TYPE);

			LogUtil.info(Comunicador.class, "NORTEFARMA: Enviando arquivo " + nomeArquivo + "...");
			ftp.changeWorkingDirectory(pasta);
			LogUtil.info(Comunicador.class, "NORTEFARMA: Alterando pasta de envio");
			ftp.storeFile(nomeArquivo, is);
			is.close();
			LogUtil.info(Comunicador.class, "NORTEFARMA: Arquivo " + nomeArquivo + " enviado com sucesso!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void enviaPedidos() throws Exception {
		// conectar();

		// CARREGA TODOS OS PEDIDO QUE ESTÃO LIBERADOS PARA EXPORTAÇÃO
		PedidoDao pedDao = new PedidoDao();
		List<Pedido> pedidos = pedDao.getPedidosPendenteExportacao(canal);
		LogUtil.info(Comunicador.class, "NORTEFARMA: Enviando pedidos para distribuidora: " + pedidos.size());
		for (Pedido ped : pedidos) {
			try {
				File f = geraArquivoPedido(ped);
				// enviaPedido(f);
				// LogUtil.info(Comunicador.class, "NORTEFARMA: Pedido enviado
				// com sucesso: " + ped.numero);
				// ped.status = Pedido.EXPORTADO_FATURAMENTO;
				// ped.dataEnvio = new Date();
				// new PedidoDao().atualizaStatus(ped);
				// new PedidoDao().atualizadaDataEnvio(ped);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void recebePedidos() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void recebeTitulos() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void recebeClientes() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void recebeDevolucoes() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void recebeEstoque() throws Exception {
		// TODO Auto-generated method stub

	}

}
