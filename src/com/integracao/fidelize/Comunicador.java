package com.integracao.fidelize;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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

import br.com.core.util.LogUtil;
import br.com.core.util.TextUtil;
import br.com.smp.vk.venda.model.Pedido;
import br.com.smp.vk.venda.model.PedidoItem;
import br.com.smp.vk.venda.model.Prazo;
import br.com.smp.vk.venda.model.Produto;
import br.com.smp.vk.venda.model.StatusPedido;
import vendas.dao.PedidoDao;
import vendas.dao.PedidoItemDao;
import vendas.dao.PrazoDao;
import vendas.dao.ProdutoDao;
import vendas.dao.StatusPedidoDao;

public class Comunicador extends Integrador {

	private static final int canal = 20;

	private static FTPClient ftp = new FTPClient();

	private static final String host = "ftp.laborsil.com.br";
	private static final String usuario = "laborsil";
	private static final String senha = "Tec_1010";
	private static final int porta = 21;

	private static final String cnpjLaborsil = "02484348000127";
	private static final String cnpjCifarma = "17562075000169 ";

	private static final String pastaPedido = "/www/pedidos";
	// private static final String pastaNFe = "/RETORNO";
	private static final String pastaRetorno = "/www/respostas";
	// private static final String pastaBackup = "/BACKUP";

	public static void main(String[] args) {
		try {
			// new Comunicador().enviaPedidos();
			new Comunicador().recebePedidos();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public Comunicador() {
	}

	public Comunicador(Finaliza f, ServletIntegradorNormal integrador) {
		super(f, integrador, "LABORSIL");
	}

	private static void conectar() {

		if (!ftp.isConnected()) {
			try {
				ftp.connect(host, porta);

				// verifica se conectou com sucesso!
				if (FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
					LogUtil.info(Comunicador.class, "LABORSIL: Conectando ao FTP");
					ftp.login(usuario, senha);
					LogUtil.info(Comunicador.class, "LABORSIL: Diretorio atual: " + ftp.printWorkingDirectory());
				} else {
					// erro ao se conectar
					ftp.disconnect();
					LogUtil.info(Comunicador.class, "LABORSIL: Conexão recusada");
				}
				ftp.enterLocalActiveMode();
			} catch (Exception e) {
				LogUtil.error(Comunicador.class, "LABORSIL: Ocorreu um erro inesperado.... " + e.getMessage());
			}
		}
	}

	private static File geraArquivoPedido(Pedido pedido) throws Exception {

		LogUtil.info(Comunicador.class, "LABORSIL: Gerando arquivo de pedido");

		LocalDate data = LocalDate.now();
		DateTimeFormatter formatData = DateTimeFormatter.ofPattern("yyyyMMdd");

		LocalTime hora = LocalTime.now();
		DateTimeFormatter formatHora = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);

		File file = new File("C:\\ATUA\\LABORSIL\\PEDIDO_" + TextUtil.padLeft(String.valueOf(pedido.numero), 10, "0")
				+ "_" + cnpjCifarma + "_CIF" + ".PED");
		BufferedWriter fw = new BufferedWriter(new FileWriter(file));

		// L1 - fixo 1
		String s;
		s = "1;";
		// fw.write(s);
		// fw.newLine();

		// L2 - cnpj ou cpf do cliente
		s += TextUtil.padLeft(pedido.cnpj, 14, "0") + ";";
		// fw.write(s);
		// fw.newLine();

		// L3 - email
		s += ";";
		// fw.write(s);
		// fw.newLine();

		// L4 - cnpj distribuidor
		s += cnpjLaborsil + ";";
		// fw.write(s);
		// fw.newLine();

		// L5 - prazo negociado
		Prazo prazo = new PrazoDao().getPrazo(pedido.prazo);
		if (prazo != null)
			s += prazo.codigoExporta;
		s += ";";
		// fw.write(s);
		// fw.newLine();

		// L6 - tipo de venda
		s += ";";
		// fw.write(s);
		// fw.newLine();

		// L7 - codigo pedido do cliente
		s += ";";
		// fw.write(s);
		// fw.newLine();

		// L8 - codigo pedido
		s += String.valueOf(pedido.numero) + ";";
		// fw.write(s);
		// fw.newLine();

		// L9 - margem
		s += ";";
		// fw.write(s);
		// fw.newLine();

		// L10 - flag de pedido ("B" se Bonificado e vazio se não Bonificado)
		s += ";";
		// fw.write(s);
		// fw.newLine();

		// L11 - vesao do layout
		s += "2.1" + ";";
		// fw.write(s);
		// fw.newLine();

		// L12 - vesao do layout
		s += "CIF" + ";";
		// fw.write(s);
		// fw.newLine();

		// L13 - flag recálculo desconto ("R" se sim, vazio se não)
		s += ";";
		// fw.write(s);
		// fw.newLine();

		// L14 - flag CNPJ ou CPF ("PF" se enviarmos CPF e “PJ” se enviarmos
		// CNPJ)
		s += "PJ";
		fw.write(s);
		fw.newLine();

		int totalItens = 0;
		int totalItensVendidos = 0;
		for (PedidoItem i : pedido.itens) {
			Produto prod = new ProdutoDao().getProduto(i.produto);

			// Registro tipo "2" – Identificacao do Pedido
			// L1 - fixo "2"
			s = "2" + ";";
			// fw.write(s);
			// fw.newLine();

			// L2 - codigo EAN
			s += prod.ean + ";";
			// fw.write(s);
			// fw.newLine();

			// L3 - quantidade pedida
			s += String.valueOf(i.qntVenda) + ";";
			// fw.write(s);
			// fw.newLine();

			// L4 - percentual unitario de desconto
			String percDesconto = new DecimalFormat("0.00")
					.format(((((i.qntBonificacao + i.qntVenda) * i.valorBruto) - (i.qntVenda * i.valorUnitario))
							/ ((i.qntBonificacao + i.qntVenda) * i.valorBruto)) * 100);
			s += percDesconto.replace(",", ".") + ";";
			// fw.write(percDesconto.replace(",", "."));
			// fw.newLine();

			// L5 - valor unitario liquido
			s += ";";
			// fw.write(s);
			// fw.newLine();

			// L6 - prazo produtos
			s += ";";
			// fw.write(s);
			// fw.newLine();

			// L7 - flag produto monitorado ("M" para monitorado e “L” para
			// Liberado)
			s += "L";
			fw.write(s);
			fw.newLine();

			totalItens++;
			totalItensVendidos = totalItensVendidos + i.qntVenda;

		}

		// Registro Tipo "9" – Finalizador do Pedido
		// L1 - fixo "9"
		s = "9" + ";";
		// fw.write(s);
		// fw.newLine();

		// L2 - total de produtos
		s += String.valueOf(totalItensVendidos);
		fw.write(s);

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
			LogUtil.info(Comunicador.class, "LABORSIL: Enviando arquivo texto. Pasta: " + pasta);
			ftp.setFileType(FTPClient.ASCII_FILE_TYPE);

			LogUtil.info(Comunicador.class, "LABORSIL: Enviando arquivo " + nomeArquivo + "...");
			ftp.changeWorkingDirectory(pasta);
			LogUtil.info(Comunicador.class, "LABORSIL: Alterando pasta de envio");
			ftp.storeFile(nomeArquivo, is);
			is.close();
			LogUtil.info(Comunicador.class, "LABORSIL: Arquivo " + nomeArquivo + " enviado com sucesso!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void enviaPedidos() throws Exception {

		conectar();

		// CARREGA TODOS OS PEDIDO QUE ESTÃO LIBERADOS PARA EXPORTAÇÃO
		PedidoDao pedDao = new PedidoDao();
		List<Pedido> pedidos = pedDao.getPedidosPendenteExportacao(canal);
		LogUtil.info(Comunicador.class, "LABORSIL: Enviando pedidos para distribuidora: " + pedidos.size());
		for (Pedido ped : pedidos) {
			try {
				File f = geraArquivoPedido(ped);
				enviaPedido(f);
				LogUtil.info(Comunicador.class, "LABORSIL: Pedido enviado com sucesso: " + ped.numero);
				ped.status = Pedido.EXPORTADO_FATURAMENTO;
				ped.dataEnvio = new Date();
				new PedidoDao().atualizaStatus(ped);
				new PedidoDao().atualizadaDataEnvio(ped);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private static void processaArquivoNota(String arquivo) throws Exception {
		LogUtil.info(Comunicador.class, "LABORSIL: Lendo arquivos de nota");
		Scanner scanner = null;
		try {
			scanner = new Scanner(new FileReader(arquivo)).useDelimiter("\\n");
		} catch (FileNotFoundException e) {
			throw new Exception("LABORSIL: Arquivo não encontrato " + arquivo);
		}
		Pedido pedido = new Pedido();
		File f = new File(arquivo);
		int numeroPedido = Integer.parseInt(f.getName().substring(5, 15));
		PedidoDao pedidoDao = new PedidoDao();
		pedido = pedidoDao.getPedido(numeroPedido);
		
		if (pedido == null) {
			throw new Exception("LABORSIL: Pedido não encontrato");
		}
		Double valorFaturado = new Double(0);
		Produto produto;
		PedidoItem pedidoItem;
		while (scanner.hasNext()) {
			try {
				String dados = scanner.next().trim();
				String[] retorno;
				LogUtil.info(Comunicador.class, "LABORSIL: Linha " + dados.substring(0, 1) + ": " + dados);
				retorno = dados.split(";", dados.length());
				switch (retorno[0]) {
				// CABEÇALHO DO PEDIDO
				case "1":
					retorno = dados.split(";", dados.length());
					dados = retorno[1];

					// 2 - Código do Pedido no Fornecedor
					pedido.setNumeroDistribuidor(new Integer(dados));

					// 3 - Data de Processamento
					dados = retorno[2];
					pedido.setDataFaturamento(new SimpleDateFormat("yyyy-MM-dd").parse(dados));

					// 4 - Hora Processamento
					dados = retorno[3];

					// 5 - Data de Emissão da Nota
					dados = retorno[4];

					// 6 - CNPJ do Fornecedor
					dados = retorno[5];

					// 7 - Sigla Indústria
					dados = retorno[6];

					// 8 - CNPJ Faturado*
					dados = retorno[7];

					break;
				case "2":
					retorno = dados.split(";", dados.length());

					// 2 - Nº da Nota
					dados = retorno[1];
					pedido.setNumeroNota(new Integer(dados));

					// 3 - Chave DANFe (Somente números)
					dados = retorno[6];

					break;
				case "3":
					retorno = dados.split(";", dados.length());

					// 2 - Valor da Nota
					dados = retorno[1];
					dados = dados.replace(".", "");
					dados = dados.replace(",", ".");
					pedido.setValorFaturado(new Double(dados));
					break;
				case "4":
					retorno = dados.split(";", dados.length());

					// 2 - Código EAN do produto
					ProdutoDao produtoDao = new ProdutoDao();
					dados = retorno[1];
					produto = produtoDao.getProdutoEAN(dados);
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

						// 3 - Quantidade Atendida
						dados = retorno[2];
						pedidoItem.setQntFaturada(new Integer(dados));

						// 6 - Valor Unitário Líquido do Produto
						dados = retorno[5];
						dados = dados.replace(".", "");
						dados = dados.replace(",", ".");
						pedidoItem.setValorUnitario(new Double(dados));

						// Valor Faturado
						pedidoItem.setValorFaturado(pedidoItem.getQntFaturada() * pedidoItem.getValorUnitario());
						// valorFaturado = valorFaturado +
						// pedidoItem.getValorFaturado();
						new PedidoItemDao().salvar(pedidoItem, false, pedido);
					}

					break;
				case "4.1":
					break;
				case "5":
					break;
				case "6":
					break;
				case "9":
					LogUtil.info(Comunicador.class, "LABORSIL: Atualizando data Faturamento e valor faturado.");
					pedido.setStatus("FATURADO");
					pedidoDao.atualizaStatus(pedido);
					pedidoDao.Salvar(pedido);
					break;
				default:
					throw new Exception("LABORSIL: Dados inválidos!");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new Exception("LABORSIL: Erro ao processar retorno do pedido!");
			}
		}
	}

	private static void processaArquivoRetorno(String arquivo) throws Exception {
		LogUtil.info(Comunicador.class, "LABORSIL: Lendo arquivos de retorno");
		Scanner scanner = null;
		try {
			scanner = new Scanner(new FileReader(arquivo)).useDelimiter("\\n");
		} catch (FileNotFoundException e) {
			throw new Exception("LABORSIL: Arquivo não encontrato " + arquivo);
		}
		Pedido pedido = new Pedido();
		pedido.setNumero(new Integer(arquivo.substring(40, 50)));
		PedidoDao pedidoDao = new PedidoDao();
		Double valorFaturado = new Double(0);
		Produto produto;
		PedidoItem pedidoItem;
		int[] statusPedido = new int[20];
		int[] quantidadeFaturada = new int[20];
		int i = 0;
		while (scanner.hasNext()) {
			try {
				String dados = scanner.next().trim();
				String[] retorno;
				LogUtil.info(Comunicador.class, "LABORSIL: Linha " + dados.substring(0, 1) + ": " + dados);

				switch (dados.substring(0, 1)) {
				// CABEÇALHO DO PEDIDO
				case "1":

					pedido = pedidoDao.getPedido(pedido.getNumero());
					if (pedido == null) {
						throw new Exception("LABORSIL: Pedido não encontrato");
					}

					// CNPJ da farmácia - Preencher com zero a esquerda
					retorno = dados.split(";", dados.length());

					// 2 - Código do Pedido no Fornecedor
					dados = retorno[1];
					if (!dados.isEmpty()) {
						pedido.setNumeroDistribuidor(new Integer(dados));
					}

					// 3 - Data Hora de Processamento (do pedido)
					dados = retorno[2];

					// 4 - Filler
					dados = retorno[3];

					// 5 - Prazo Faturado (0 para à vista ou numero de dias)
					dados = retorno[4];

					// 6 - Motivo do Pedido
					dados = retorno[5];

					// 7 - Flag Tipo de Pedido**
					dados = retorno[6];

					// 8 - Versão do Layout (Fixo 2.1)
					dados = retorno[7];

					// 9 - Sigla Indústria
					dados = retorno[8];

					pedido.setDataRecebimento(new Date());
					// pedidoDao.atualizaStatus(pedido);
					pedidoDao.atualizadaDataRecebimento(pedido);
					break;
				// RETORNO DOS ITENS DO PEDIDO
				case "2":
					retorno = dados.split(";", dados.length());

					// 2 - Código EAN
					dados = retorno[1];
					ProdutoDao produtoDao = new ProdutoDao();
					produto = produtoDao.getProdutoEAN(dados);
					if (produto == null) {
						throw new Exception("LABORSIL: Código EAN não cadastrado");
					}

					String codigo = produto.getCodigo();
					List<PedidoItem> itens = pedido.getItens().stream().filter((p) -> p.getProduto().equals((codigo)))
							.collect(Collectors.toList());
					if (itens.size() > 0) {
						pedidoItem = new PedidoItem();
						pedidoItem = itens.get(0);

						// 3 - Quantidade Atendida
						dados = retorno[2];
						quantidadeFaturada[i] = new Integer(dados);

						// pedidoItem.setQntFaturada(new Integer(dados));
						// pedidoItem.setValorFaturado(pedidoItem.getQntFaturada());
						// valorFaturado = pedidoItem.getValorFaturado();

						// 4 - Percentual Unitário de Desconto do Produto
						dados = retorno[3];

						// 5 - Valor Unitário do Desconto
						dados = retorno[4];

						// 6 - Valor Unitário Líquido do Produto
						dados = retorno[5];
						// pedidoItem.setValorUnitario(new Double(dados));

						// 7 - Motivo
						dados = retorno[6];
						statusPedido[i] = new Integer(dados.isEmpty() ? "0" : dados);

						// 8 - EAN Faturado
						dados = retorno[7];

						// 9 - Flag produto Monitorado
						dados = retorno[8];

						// 10 - Motivo do Distribuidor
						dados = retorno[9];
						// new PedidoItemDao().salvar(pedidoItem, false,
						// pedido);
					}
					i++;

					break;
				// TRAILER
				case "9":
					LogUtil.info(Comunicador.class, "LABORSIL: Atualizando data Faturamento e valor faturado.");
					retorno = dados.split(";", dados.length());

					StatusPedidoDao statusPedidoDao = new StatusPedidoDao();
					String stp = TextUtil.padLeft(String.valueOf(statusPedido[0]), 3, "0");
					StatusPedido st = statusPedidoDao.getStatusPedido(stp);
					if (st == null) {
						throw new Exception("ONE: Status do pedido não encontrato");
					}
					pedido.setStatus(st.getDescricao());
					int somaQtd = 0;
					for (int j = 0; j < quantidadeFaturada.length; j++) {
						somaQtd = somaQtd + quantidadeFaturada[j];
					}
					if (somaQtd > 0) {
						pedido.setStatus(Pedido.RECEBIDO_FATURAMENTO);
					}
					pedidoDao.atualizaStatus(pedido);
					pedidoDao.Salvar(pedido);
					break;
				default:
					throw new Exception("LABORSIL: Dados inválidos!");
				}

			} catch (Exception ex) {
				throw new Exception("LABORSIL: Erro ao processar retorno do pedido!");
			}
		}
	}

	private void processaArquivos() throws Exception {
		conectar();

		LogUtil.info(Comunicador.class, "LABORSIL: Alterando pasta: " + pastaRetorno);
		ftp.changeWorkingDirectory(pastaRetorno);

		FTPFile[] files = ftp.listFiles();
		LogUtil.info(Comunicador.class, "LABORSIL: Total de arquivos: " + files.length);
		new File("C:\\VK-FARMA\\INTEGRACAO\\LABORSIL").mkdirs();

		for (FTPFile f : files) {
			if (f.getType() == 0) {
				LogUtil.info(Comunicador.class, "LABORSIL: Lendo arquivo: " + f.getName());
				String arqDown = "C:\\VK-FARMA\\INTEGRACAO\\LABORSIL\\" + f.getName();
				FileOutputStream fos = new FileOutputStream(arqDown);
				boolean download = ftp.retrieveFile(f.getName(), fos);
				if (download) {
					try {
						if (f.getName().contains("RETORNO")) {
							processaArquivoRetorno(arqDown);
						}

						if (f.getName().contains("NOTA")) {
							processaArquivoNota(arqDown);
						}

						ftp.deleteFile(f.getName());
						LogUtil.info(Comunicador.class, "LABORSIL: Arquivo: " + f.getName() + " lido com sucesso");
					} catch (Exception e) {
						LogUtil.error(Comunicador.class, "LABORSIL: " + e.getMessage());
					}
				} else {
					LogUtil.error(Comunicador.class, "LABORSIL: Error in downloading file! " + f.getName());
				}
				fos.flush();
				fos.close();
			}
		}
		LogUtil.info(Comunicador.class, "LABORSIL: Desconectando FTP");
		ftp.disconnect();
	}

	@Override
	public void recebePedidos() throws Exception {
		processaArquivos();
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

	}

}
