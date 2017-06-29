package com.integracao.one;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import com.integrador.Integrador;
import com.integrador.ServletIntegradorNormal;
import com.integrador.ServletIntegradorNormal.Finaliza;
import com.util.Str;
import com.util.lerXML_JAXB;

import br.com.core.util.TextUtil;
import br.com.importa.vkcom.core.LogUtil;
import br.com.javac.v300.procnfe.TNFe.InfNFe.Det;
import br.com.javac.v300.procnfe.TNfeProc;
import br.com.smp.vk.venda.model.CanalProduto;
import br.com.smp.vk.venda.model.CanalSetor;
import br.com.smp.vk.venda.model.Pedido;
import br.com.smp.vk.venda.model.PedidoIntegracao;
import br.com.smp.vk.venda.model.PedidoItem;
import br.com.smp.vk.venda.model.Prazo;
import br.com.smp.vk.venda.model.Produto;
import vendas.dao.CanalProdutoDao;
import vendas.dao.CanalSetorDao;
import vendas.dao.PedidoDao;
import vendas.dao.PedidoIntegracaoDao;
import vendas.dao.PedidoItemDao;
import vendas.dao.PrazoDao;
import vendas.dao.ProdutoDao;

/**
 * Interface de integração com a distribuidora ONE.
 * 
 * 
 * @author marce_000
 *
 */
public class Comunicador extends Integrador {

	private static final int canal = 22;

	private static FTPClient ftp = new FTPClient();

	private static final String host = "sistema.onemedicamentos.com.br";
	private static final String usuario = "cifarma";
	private static final String senha = "L8R7ZMAN29";
	private static final int porta = 21;

	private static final String cnpjOne = "00006010000134 ";
	private static final String cnpjCifarma = "17562075000169 ";

	private static final String pastaPedido = "/pedido";
	private static final String pastaNFe = "/xml";
	private static final String pastaRetorno = "/retorno";
	private static final String pastaBackup = "/bkp";

	public static void main(String argv[]) {
		try {
			new Comunicador().enviaPedidos();
			new Comunicador().recebePedidos();
			new Comunicador().processaArquivos();
			new Comunicador().processaArquivosNota();
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
					LogUtil.info(Comunicador.class, "ONE: Conectando ao FTP");
					ftp.login(usuario, senha);
					LogUtil.info(Comunicador.class, "ONE: Diretorio atual: " + ftp.printWorkingDirectory());
				} else {
					// erro ao se conectar
					ftp.disconnect();
					LogUtil.info(Comunicador.class, "ONE: Conexão recusada");
				}
				ftp.enterLocalPassiveMode();
			} catch (Exception e) {
				LogUtil.error(Comunicador.class, "ONE: Ocorreu um erro inesperado.... " + e.getMessage());
			}
		}
	}

	public Comunicador(Finaliza f, ServletIntegradorNormal integrador) {
		super(f, integrador, "ONE");
	}

	private static File geraArquivoPedido(Pedido pedido) throws Exception {

		LogUtil.info(Comunicador.class, "ONE: Gerando arquivo de pedido");

		LocalDate data = LocalDate.now();
		DateTimeFormatter formatData = DateTimeFormatter.ofPattern("yyyyMMdd");

		LocalTime hora = LocalTime.now();
		DateTimeFormatter formatHora = DateTimeFormatter.ofPattern("HH:mm");

		File file = new File("C:\\ATUA\\ONE\\PEDems_" + pedido.cnpj.trim() + "_" + data.format(formatData)
				+ hora.format(formatHora).replace(":", "") + ".TXT");
		BufferedWriter fw = new BufferedWriter(new FileWriter(file));

		// HEADER DO ARQUIVO IDENTIFICACAO DO ARQUIVO - 62
		String s;
		s = "0";
		s += "PEDIDO OPER.LOG";
		s += TextUtil.padLeft(cnpjOne, 15, "0");
		s += data.format(formatData);
		s += hora.format(formatHora).replace(":", "").concat("00");
		s += TextUtil.padLeft(cnpjCifarma, 15, "0");
		s += TextUtil.padRight(pedido.setorVendedor.replace(".", ""), 20, " ");
		fw.write(s);
		fw.newLine();

		// HEADER CABECALHO DO PEDIDO - 70
		s = "1";
		s += TextUtil.padLeft(pedido.cnpj, 15, "0");
		s += TextUtil.padRight(String.valueOf(pedido.numero), 12, " ");
		s += new SimpleDateFormat("ddMMyyyy").format(pedido.data);
		s += "0";
		s += "0";
		s += TextUtil.padLeft("0", 5, "0");
		s += TextUtil.padRight("", 15, " ");

		/*
		 * Prazo prazo = new PrazoDao().getPrazo(pedido.prazo);
		 * 
		 * if (prazo != null) { s +=
		 * Str.alinhaZeroDireita(String.valueOf(prazo.codigoExporta), 3); } else
		 * { s += Str.alinhaZeroDireita("0", 3); }
		 */

		CanalSetor canalSetor = new CanalSetorDao().getCanalSetor(canal, pedido.setorVendedor);
		if (canalSetor == null) {
			fw.flush();
			fw.close();
			throw new Exception("ONE: Código exportação não encontrado");
		}
		// Código do Representante
		s += Str.alinhaZeroDireita("0", 9);

		fw.write(s);
		fw.newLine();

		// CONDIÇÃO DE PAGAMENTO
		s = "4";

		Prazo prazo = new PrazoDao().getPrazo(pedido.prazo);
		if (prazo != null) {
			// Código da Condição de Pagamento no Operador Logístico
			s += TextUtil.padLeft(String.valueOf(prazo.codigoExporta), 5, "0");
		} else {
			// Código da Condição de Pagamento no Operador Logístico
			s += TextUtil.padLeft("0", 5, "0");
		}

		// Descrição da Condição de Pagamento
		s += TextUtil.padRight(pedido.prazoNome, 30, " ");

		// Quantidade de Parcelas da Condição de Pagamento
		if (prazo != null) {
			s += TextUtil.padLeft(String.valueOf(prazo.qtdParcelas), 3, "0");
		} else {
			s += TextUtil.padLeft("0", 3, "0");
		}

		// Percentual de Desconto Financeiro da Condição de Pagamento
		s += TextUtil.padLeft("0", 5, "0");

		String[] dias = prazo.descricao.replaceAll("[DIAS dias]", "").split("/");
		Integer quantidadeDias = dias.length;
		Long percentualPag = (long) (100 / quantidadeDias);

		// Numero de dias da Parcela x
		for (int j = 0; j < 6; j++) {
			if (j < quantidadeDias) {
				s += TextUtil.padLeft(dias[j], 3, "0");
			} else {
				s += TextUtil.padLeft("0", 3, "0");
			}
		}

		// Percentual de Pagamento da Parcela x
		for (int j = 0; j < 6; j++) {
			// Trata os percentuais até a quantidade de dias do prazo.
			if (j < quantidadeDias) {
				s += TextUtil.padLeft(String.valueOf(percentualPag).replace("[,.]", ""), 5, "0");
			} else {
				s += TextUtil.padLeft("0", 5, "0");
			}
		}

		fw.write(s);
		fw.newLine();

		// DETALHE ITENS DO PEDIDO - 41
		int totalItens = 0;
		int totalItensVendidos = 0;
		for (PedidoItem i : pedido.itens) {
			Produto prod = new ProdutoDao().getProduto(i.produto);

			s = "2";
			s += TextUtil.padRight(String.valueOf(i.pedido), 12, " ");
			s += Str.alinhaZeroDireita(prod.ean, 13);
			s += Str.alinhaZeroDireita(String.valueOf(i.qntVenda + i.qntBonificacao), 5);

			String percDesconto = new DecimalFormat("0.00")
					.format(((((i.qntBonificacao + i.qntVenda) * i.valorBruto) - (i.qntVenda * i.valorUnitario))
							/ ((i.qntBonificacao + i.qntVenda) * i.valorBruto)) * 100);

			s += Str.alinhaZeroDireita(
					String.valueOf(new DecimalFormat("0000").format(Double.parseDouble(percDesconto.replace(",", "")))),
					5);

			/*
			 * if (prazo != null) { s +=
			 * Str.alinhaZeroDireita(String.valueOf(prazo.codigoExporta), 3); }
			 * else { s += Str.alinhaZeroDireita("0", 3); }
			 */

			s += Str.alinhaZeroDireita("0", 3);

			s += "0";
			s += "Z";

			totalItens++;
			totalItensVendidos = totalItensVendidos + i.qntVenda;

			fw.write(s);
			fw.newLine();
		}

		// TRAILER - 28
		s = "3";
		s += TextUtil.padRight(String.valueOf(pedido.numero), 12, " ");
		s += Str.alinhaZeroDireita(String.valueOf(totalItens), 5);
		s += Str.alinhaZeroDireita(String.valueOf(totalItensVendidos), 10);

		fw.write(s);
		fw.newLine();

		fw.flush();
		fw.close();

		return file;
	}

	private static void enviaPedido(File arquivo) throws Exception {
		enviaArquivo(pastaPedido, arquivo.getAbsolutePath());
	}

	private static void enviaArquivo(String pasta, String arquivo) throws Exception {

		InputStream is = null;
		is = new FileInputStream(arquivo);

		int idx = arquivo.lastIndexOf(File.separator);
		if (idx < 0) {
			idx = 0;
		} else {
			idx++;
		}
		String nomeArquivo = arquivo.substring(idx, arquivo.length());
		LogUtil.info(Comunicador.class, "ONE: Enviando arquivo texto. Pasta: " + pasta);
		ftp.setFileType(FTPClient.ASCII_FILE_TYPE);

		LogUtil.info(Comunicador.class, "ONE: Enviando arquivo " + nomeArquivo + "...");
		ftp.changeWorkingDirectory(pasta);
		LogUtil.info(Comunicador.class, "ONE: Alterando pasta de envio");
		ftp.storeFile(nomeArquivo, is);
		is.close();
		LogUtil.info(Comunicador.class, "ONE: Arquivo " + nomeArquivo + " enviado com sucesso!");
	}

	@Override
	public void enviaPedidos() throws Exception {

		conectar();

		// CARREGA TODOS OS PEDIDO QUE ESTÃO LIBERADOS PARA EXPORTAÇÃO
		PedidoDao pedDao = new PedidoDao();
		List<Pedido> pedidos = pedDao.getPedidosPendenteExportacao(canal);
		LogUtil.info(Comunicador.class, "ONE: Enviando pedidos para distribuidora: " + pedidos.size());
		for (Pedido ped : pedidos) {
			try {
				File f = geraArquivoPedido(ped);
				enviaPedido(f);
				LogUtil.info(Comunicador.class, "ONE: Pedido enviado com sucesso: " + ped.numero);
				ped.status = Pedido.EXPORTADO_FATURAMENTO;
				ped.dataEnvio = new Date();

				// GRAVA O ARQUIVO RECEBIDO
				PedidoIntegracao pedInt = new PedidoIntegracao();
				pedInt.pedido = ped.numero;
				pedInt.dataRecebimento = new Date();
				pedInt.tipoArquivo = PedidoIntegracao.ENVIO_PEDIDO;
				pedInt.nomeArquivo = f.getName();
				new PedidoIntegracaoDao().salvar(pedInt);

				new PedidoDao().atualizaStatus(ped);
				new PedidoDao().atualizadaDataEnvio(ped);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// salvaRegistroLogExecucao(canal);
	}

	@Override
	public void recebePedidos() throws Exception {
		processaArquivos();
	}

	private void processaArquivosNota() throws Exception {
		conectar();

		LogUtil.info(Comunicador.class, "ONE: Alterando pasta: " + pastaNFe);
		ftp.changeWorkingDirectory(pastaNFe);

		FTPFile[] files = ftp.listFiles();
		LogUtil.info(Comunicador.class, "ONE: Total de arquivos: " + files.length);
		new File("C:\\VK-FARMA\\INTEGRACAO\\ONE").mkdirs();

		for (FTPFile f : files) {
			LogUtil.info(Comunicador.class, "ONE: Lendo arquivo: " + f.getName());
			String arqDown = "C:\\VK-FARMA\\INTEGRACAO\\ONE\\" + f.getName();
			FileOutputStream fos = new FileOutputStream(arqDown);
			boolean download = ftp.retrieveFile(f.getName(), fos);
			if (download) {
				try {
					if (f.getName().contains("3517")) {
						processaNFE(arqDown);
					}
					ftp.deleteFile(f.getName());
					LogUtil.info(Comunicador.class, "ONE: Arquivo: " + f.getName() + " lido com sucesso");
				} catch (Exception e) {
					LogUtil.error(Comunicador.class, "ONE: " + e.getMessage());
				}
			} else {
				LogUtil.error(Comunicador.class, "ONE: Error in downloading file! " + f.getName());
			}
			fos.flush();
			fos.close();
		}
		LogUtil.info(Comunicador.class, "ONE: Desconectando FTP");
		ftp.disconnect();
	}

	private void processaArquivos() throws Exception {
		conectar();

		LogUtil.info(Comunicador.class, "ONE: Alterando pasta: " + pastaRetorno);
		ftp.changeWorkingDirectory(pastaRetorno);

		FTPFile[] files = ftp.listFiles();
		LogUtil.info(Comunicador.class, "ONE: Total de arquivos: " + files.length);
		new File("C:\\VK-FARMA\\INTEGRACAO\\ONE").mkdirs();

		for (FTPFile f : files) {
			LogUtil.info(Comunicador.class, "ONE: Lendo arquivo: " + f.getName());
			String arqDown = "C:\\VK-FARMA\\INTEGRACAO\\ONE\\" + f.getName();
			FileOutputStream fos = new FileOutputStream(arqDown);
			boolean download = ftp.retrieveFile(f.getName(), fos);
			if (download) {
				try {
					if (f.getName().contains("RETems")) {
						processaArquivoRetorno(arqDown);
					}

					if (f.getName().contains("estems")) {
						processaArquivoEstoque(arqDown);
					}

					if (f.getName().contains("NOTems")) {
						processaArquivoNFE(arqDown);
					}

					if (f.getName().contains("3517")) {
						processaNFE(arqDown);
					}
					ftp.deleteFile(f.getName());
					LogUtil.info(Comunicador.class, "ONE: Arquivo: " + f.getName() + " lido com sucesso");
				} catch (Exception e) {
					LogUtil.error(Comunicador.class, "ONE: " + e.getMessage());
				}
			} else {
				LogUtil.error(Comunicador.class, "ONE: Error in downloading file! " + f.getName());
			}
			fos.flush();
			fos.close();
		}
		LogUtil.info(Comunicador.class, "ONE: Desconectando FTP");
		ftp.disconnect();
	}

	public void processaArquivoEstoque(String arquivo) throws Exception {
		LogUtil.info(Comunicador.class, "ONE: Lendo arquivos de estoque");
		Produto produto;
		CanalProduto canalProduto;
		Scanner scanner = null;
		try {
			scanner = new Scanner(new FileReader(arquivo)).useDelimiter("\\n");
		} catch (FileNotFoundException e) {
			throw new Exception("ONE: Arquivo não encontrato " + arquivo);
		}
		while (scanner.hasNext()) {
			String dados = scanner.next().trim();
			String retorno = "";
			LogUtil.info(Comunicador.class, "ONE: Linha " + dados.substring(0, 2) + ": " + dados);

			switch (dados.substring(0, 2)) {
			// HEADER DO ARQUIVO
			case "80":

				// Código EAN do produto
				retorno = dados.substring(2, 15);
				ProdutoDao produtoDao = new ProdutoDao();
				produto = produtoDao.getProdutoEAN(retorno);
				if (produto == null) {
					LogUtil.error(Comunicador.class, "ONE: Código EAN não cadastrado");
				} else {
					CanalProdutoDao canalProdutoDao = new CanalProdutoDao();
					canalProduto = canalProdutoDao.getCanalProdutoEAN(canal, produto.getEan());
					if (canalProduto == null) {
						canalProduto = new CanalProduto();
					}

					canalProduto.setCanal(canal);
					canalProduto.setProduto(produto.getCodigo());
					canalProduto.setProdutoExporta(produto.getCodigo());
					canalProduto.estoqueData = new Date();

					// Unidades do produto em estoque
					retorno = dados.substring(15, 24);

					canalProduto.setEstoque(new Integer(retorno));
					canalProdutoDao.salvar(canalProduto);
				}

			}

		}

	}

	private static void processaArquivoRetorno(String arquivo) throws Exception {
		LogUtil.info(Comunicador.class, "ONE: Lendo arquivos de retorno");
		Scanner scanner = null;
		try {
			scanner = new Scanner(new FileReader(arquivo)).useDelimiter("\\n");
		} catch (FileNotFoundException e) {
			throw new Exception("ONE: Arquivo não encontrato " + arquivo);
		}
		Pedido pedido = new Pedido();
		String dataHoraRecebimento = new String();
		PedidoDao pedidoDao = new PedidoDao();
		Produto produto;
		String data = arquivo.substring(49, 57);
		String hora = arquivo.substring(57, 63);
		dataHoraRecebimento = data.concat(" ").concat(hora);
		while (scanner.hasNext()) {
			try {
				String dados = scanner.next().trim();
				String retorno = "";
				LogUtil.info(Comunicador.class, "ONE: Linha " + dados.substring(0, 1) + ": " + dados);
				switch (dados.substring(0, 1)) {
				// HEADER DO ARQUIVO
				case "0":
					break;
				// CABEÇALHO DO PEDIDO
				case "1":
					// CNPJ da farmácia - Preencher com zero a esquerda
					retorno = dados.substring(1, 16);

					// Número do Pedido na Indústria
					retorno = dados.substring(16, 28).trim();
					pedido = pedidoDao.getPedido(new Integer(retorno));
					if (pedido == null) {
						throw new Exception("ONE: Pedido não encontrato");
					}

					// Data em que o pedido foi faturado pelo Distribuidor
					retorno = dados.substring(28, 36);

					// Hora em que o pedido foi faturado pelo Distribuidor
					retorno = dados.substring(36, 44);

					// Número do Pedido no OL
					retorno = dados.substring(44, 56);
					pedido.setNumeroDistribuidor(new Integer(retorno));
					pedidoDao.atualizaNumeroDistribuidor(pedido);

					// Motivo pelo qual o pedido não foi atendido
					retorno = dados.substring(56, 58);
					// StatusPedidoDao statusPedidoDao = new StatusPedidoDao();
					// StatusPedido statusPedido =
					// statusPedidoDao.getStatusPedido(retorno);
					/*
					 * if (statusPedido == null) { throw new
					 * Exception("ONE: Status do pedido não encontrato"); }
					 */
					pedido.setStatus(Pedido.EXPORTADO_FATURAMENTO);
					if (!retorno.equals("50")) {
						pedido.setStatus(Pedido.REJEITADO);
					}

					pedido.setDataRecebimento(new SimpleDateFormat("yyyyMMdd HHmmss").parse(dataHoraRecebimento));
					pedidoDao.atualizaStatus(pedido);
					pedidoDao.atualizadaDataRecebimento(pedido);
					break;
				// RETORNO DOS ITENS DO PEDIDO
				case "2":
					// Código EAN do produto
					retorno = dados.substring(1, 14);
					ProdutoDao produtoDao = new ProdutoDao();
					produto = produtoDao.getProdutoEAN(retorno);
					if (produto == null) {
						throw new Exception("ONE: Código EAN não cadastrado");
					}

					// Atualiza quantidade e valor faturado
					/*
					 * String codigo = produto.getCodigo(); List<PedidoItem>
					 * itens = pedido.getItens().stream() .filter((p) ->
					 * p.getProduto().equals((codigo)))
					 * .collect(Collectors.toList()); if (itens.size() > 0) {
					 * pedidoItem = new PedidoItem(); pedidoItem = itens.get(0);
					 * 
					 * // Quantidade do produto que foi atendida retorno =
					 * dados.substring(27, 32);
					 * 
					 * pedidoItem.setQntFaturada(new Integer(retorno)); // Valor
					 * Faturado pedidoItem.setValorFaturado(pedidoItem.
					 * getQntFaturada() pedidoItem.getValorUnitario());
					 * valorFaturado = pedidoItem.getValorFaturado(); new
					 * PedidoItemDao().salvar(pedidoItem, false, pedido); }
					 */

					// Número do Pedido na Indústria
					retorno = dados.substring(14, 26);

					// Indica qual foi à condição de pagamento utilizada
					// pelo OL
					retorno = dados.substring(26, 27);

					// Desconto aplicado pelo Operador Logístico
					retorno = dados.substring(32, 35);

					// Prazo médio concedido pelo Operador Logístico (em
					// dias)
					retorno = dados.substring(35, 40);

					// Quantidade do produto que não foi atendida
					retorno = dados.substring(40, 45);

					// Código do Motivo pelo qual o produto não foi vendido
					// para
					// o cliente.
					retorno = dados.substring(45, 47);

					// Descrição Motivo pelo qual o produto não foi vendido
					// para
					// o cliente.
					retorno = dados.substring(47, dados.length());
					break;
				// TRAILER
				case "3":
					// Número do Pedido na Indústria
					retorno = dados.substring(1, 13);

					// Quantidade de Linhas do Arquivo de Retorno
					retorno = dados.substring(13, 18);

					// Quantidade de linhas de Itens atendidos
					retorno = dados.substring(18, 23);

					// Quantidade de linhas de Itens não atendidos
					retorno = dados.substring(23, dados.length());
					break;
				default:
					throw new Exception("ONE: Pedido é inválido");
				}

			} catch (Exception ex) {
				throw new Exception("ONE: Erro ao processar retorno do pedido!");
			}
		}
	}

	private static void processaArquivoNFE(String arquivo) throws Exception {
		LogUtil.info(Comunicador.class, "ONE: Lendo arquivos retorno de notas");
		Scanner scanner = null;
		try {
			scanner = new Scanner(new FileReader(arquivo)).useDelimiter("\\n");
		} catch (FileNotFoundException e) {
			throw new Exception("ONE: Arquivo não encontrato " + arquivo);
		}
		Pedido pedido = new Pedido();
		PedidoDao pedidoDao = new PedidoDao();
		Double valorFaturado = new Double(0);
		PedidoItem pedidoItem;
		Produto produto;
		while (scanner.hasNext()) {
			try {
				String dados = scanner.next().trim();
				String retorno = "";
				LogUtil.info(Comunicador.class, "ONE: Linha " + dados.substring(0, 1) + ": " + dados);
				switch (dados.substring(0, 1)) {
				// HEADER DO ARQUIVO
				case "0":
					String cnpf = dados.substring(16, 31);
					String data = dados.substring(31, 39);
					String hora = dados.substring(39, 47);
					break;
				// CABEÇALHO DO PEDIDO
				case "1":
					// CNPJ da farmácia - Preencher com zero a esquerda
					retorno = dados.substring(1, 16);

					// CNPJ do Distribuidor Emissor da Nota
					retorno = dados.substring(16, 31);
					/*
					 * pedido = pedidoDao.getPedido(new Integer(retorno)); if
					 * (pedido == null) { throw new Exception(
					 * "ONE: Pedido não encontrato"); }
					 */

					// Número do Pedido na Indústria
					retorno = dados.substring(39, 51).trim();
					pedido = pedidoDao.getPedido(new Integer(retorno));
					if (pedido == null) {
						throw new Exception("ONE: Pedido não encontrato");
					}

					// Número da nota fiscal
					retorno = dados.substring(31, 39);
					pedido.setNumeroNota(new Integer(retorno));

					// Data em que o pedido foi faturado pelo Distribuidor
					// (DDMMAAAA)
					retorno = dados.substring(51, 59);
					pedido.setDataFaturamento(new SimpleDateFormat("ddMMyyyy").parse(retorno));

					// Hora em que o pedido foi faturado pelo Distribuidor
					// (HHMMSSSS)
					retorno = dados.substring(59, 67);

					/*
					 * StatusPedidoDao statusPedidoDao = new StatusPedidoDao();
					 * StatusPedido statusPedido = statusPedidoDao
					 * .getStatusPedido(retorno); if (statusPedido == null) {
					 * throw new Exception(
					 * "ONE: Status do pedido não encontrato"); }
					 */
					break;
				// RETORNO DOS ITENS DO PEDIDO
				case "2":
					// Código EAN do produto
					retorno = dados.substring(25, 38).trim();
					ProdutoDao produtoDao = new ProdutoDao();
					produto = produtoDao.getProdutoEAN(retorno);
					if (produto == null) {
						throw new Exception("ONE: Código EAN não cadastrado");
					}

					// Atualiza quantidade e valor faturado
					String codigo = produto.getCodigo();
					List<PedidoItem> itens = pedido.getItens().stream().filter((p) -> p.getProduto().equals((codigo)))
							.collect(Collectors.toList());
					if (itens.size() > 0) {
						pedidoItem = new PedidoItem();
						pedidoItem = itens.get(0);

						// Quantidade do produto que foi atendida
						retorno = dados.substring(51, 56);

						pedidoItem.setQntFaturada(new Integer(retorno));
						// Valor Faturado
						pedidoItem.setValorFaturado(pedidoItem.getQntFaturada() * pedidoItem.getValorUnitario());
						valorFaturado = valorFaturado + pedidoItem.getValorFaturado();
						new PedidoItemDao().salvar(pedidoItem, false, pedido);
					}

					// Número do Pedido na Indústria
					retorno = dados.substring(38, 50);

					// Indica qual foi à condição de pagamento utilizada
					// pelo OL
					retorno = dados.substring(50, 51);

					// Desconto aplicado pelo Operador Logístico
					retorno = dados.substring(56, 59);

					// Prazo médio concedido pelo Operador Logístico (em
					// dias)
					retorno = dados.substring(59, 62);

					// Quantidade do produto que não foi atendida
					// retorno = dados.substring(40, 45);

					// Código do Motivo pelo qual o produto não foi vendido
					// para
					// o cliente.
					// retorno = dados.substring(45, 47);

					// Descrição Motivo pelo qual o produto não foi vendido
					// para
					// o cliente.
					// retorno = dados.substring(47, dados.length());

					// Atualiza data do faturamento e valor faturado no
					// pedido
					LogUtil.info(Comunicador.class, "ONE: Atualizando data Faturamento e valor faturado.");
					pedido.setValorFaturado(valorFaturado);
					pedido.setStatus("FATURADO");
					pedidoDao.atualizaStatus(pedido);
					pedidoDao.Salvar(pedido);

					break;
				// TRAILER
				case "3":
					// Número do Pedido na Indústria
					retorno = dados.substring(1, 13);

					// Quantidade de Linhas do Arquivo de Retorno
					retorno = dados.substring(13, 18);

					// Quantidade de linhas de Itens atendidos
					retorno = dados.substring(18, 23);

					// Quantidade de linhas de Itens não atendidos
					retorno = dados.substring(23, dados.length());
					break;
				default:
					throw new Exception("ONE: Pedido é inválido");
				}
			} catch (Exception ex) {
				throw new Exception("ONE: Erro ao processar nota!");
			}
		}
	}

	private void processaNFE(String arq) throws Exception {

		TNfeProc nfe = lerXML_JAXB.getNFe(arq);
		if (nfe != null) {
			LogUtil.info(Comunicador.class, "ONE: Lendo nota fiscal.");
			// BUSCA O PEDIDO ORIGEM - SISTEMA VENDAS
			int numeroPedido = 0;
			try {
				LogUtil.info(Comunicador.class,
						"ONE: Buscando pedido: " + nfe.getNFe().getInfNFe().getDet().get(0).getProd().getXPed());
				numeroPedido = Integer.parseInt(nfe.getNFe().getInfNFe().getDet().get(0).getProd().getXPed());
			} catch (Exception e) {
				throw new Exception("ONE: Erro ao processar nota!");
			}

			Pedido pedido = new Pedido();
			PedidoDao pedidoDao = new PedidoDao();
			pedido = pedidoDao.getPedido(numeroPedido);
			pedido.canal = canal;
			pedido.numero = numeroPedido;
			// pedido.numeroDistribuidor =
			// Integer.parseInt(nfe.getNFe().getInfNFe().getIde().getNNF());
			pedido.numeroNota = Integer.parseInt(nfe.getNFe().getInfNFe().getIde().getNNF());
			pedido.cnpj = nfe.getNFe().getInfNFe().getDest().getCNPJ();
			pedido.clienteRazao = nfe.getNFe().getInfNFe().getDest().getXNome();
			pedido.clienteUF = nfe.getNFe().getInfNFe().getDest().getEnderDest().getUF().name();
			try {
				pedido.data = new SimpleDateFormat("yyyy-MM-dd").parse(nfe.getNFe().getInfNFe().getIde().getDhEmi());
				pedido.dataFaturamento = new SimpleDateFormat("yyyy-MM-dd")
						.parse(nfe.getNFe().getInfNFe().getIde().getDhEmi());
			} catch (Exception e) {
				throw new Exception(e);
			}
			pedido.status = Pedido.FATURADO;

			double totalBruto = 0;
			double totalLiquido = 0;
			double totalFaturado = 0;
			pedido.itens = new ArrayList<PedidoItem>();

			for (Det nIt : nfe.getNFe().getInfNFe().getDet()) {
				PedidoItem it = new PedidoItem();
				it.pedido = numeroPedido;

				// CARREGA O DE-PARA DO PRODUTO
				Produto prod = new ProdutoDao().getProdutoEAN(nIt.getProd().getCEAN());

				it.qntFaturada = (int) Double.parseDouble(nIt.getProd().getQCom());
				it.valorFaturado = Double.parseDouble(nIt.getProd().getVProd());
				it.qntVenda = it.qntFaturada;
				it.valorUnitario = it.valorFaturado / it.qntVenda;
				if (prod != null) {
					it.produto = prod.codigo;
					try {
						it.valorBruto = new CanalProdutoDao().getCanalProduto(canal, prod.codigo).valorBruto;
					} catch (Exception e) {
					}
				} else {
					it.produto = nIt.getProd().getCProd();
				}

				pedido.itens.add(it);

				totalBruto += (it.valorBruto * it.qntFaturada);
				totalLiquido += (it.valorUnitario * it.qntFaturada);
				totalFaturado += it.valorFaturado;

			}
			pedido.valorBruto = totalBruto;
			pedido.valorFaturado = totalFaturado;
			pedido.valorLiquido = totalLiquido;

			new PedidoDao().Salvar(pedido);
			new PedidoDao().atualizaStatus(pedido);
		}
	}

	@Override
	public void recebeTitulos() throws Exception {
	}

	@Override
	public void recebeClientes() throws Exception {
	}

	@Override
	public void recebeDevolucoes() throws Exception {
	}

	@Override
	public void recebeEstoque() throws Exception {
		// TODO Auto-generated method stub

	}
}
