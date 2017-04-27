package com.integracao.baratela;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import com.integrador.Integrador;
import com.integrador.ServletIntegradorNormal;
import com.integrador.ServletIntegradorNormal.Finaliza;
import com.util.Str;
import com.util.lerXML_JAXB;

import br.com.core.util.LogUtil;
import br.com.javac.v300.procnfe.TNFe.InfNFe.Det;
import br.com.smp.vk.venda.model.Canal;
import br.com.smp.vk.venda.model.CanalProduto;
import br.com.smp.vk.venda.model.Pedido;
import br.com.smp.vk.venda.model.PedidoIntegracao;
import br.com.smp.vk.venda.model.PedidoItem;
import br.com.smp.vk.venda.model.Prazo;
import br.com.smp.vk.venda.model.Produto;
import br.com.javac.v300.procnfe.TNfeProc;
import conect.Conector;
import conect.Oracle;
import vendas.dao.CanalDao;
import vendas.dao.CanalProdutoDao;
import vendas.dao.PedidoDao;
import vendas.dao.PedidoIntegracaoDao;
import vendas.dao.PrazoDao;
import vendas.dao.ProdutoDao;

public class Comunicador extends Integrador {

	private final int canal = 25;

	private static FTPClient ftp = new FTPClient();

	public static String host = "177.99.238.72";
	public static String usuario = "cifarma";
	public static String senha = "123@cifarma";
	private static final int porta = 21;

	public static String pastaPedido = "/PEDIDO";
	public static String pastaNFe = "/NF";
	public static String pastaRetornoPedido = "/RETORNO";
	public static String pastaEstoque = "/ESTOQUE";

	private HashMap<String, String> motivos = new HashMap<String, String>();

	public Comunicador(Finaliza f, ServletIntegradorNormal integrador) {
		super(f,integrador,"BARATELA");
		uniciar();
	}

	public Comunicador() {
		uniciar();
	}

	public void uniciar() {
		motivos.put("01", "PROBLEMAS CADATRAIS");
		motivos.put("02", "LIMITE DE CRÉDITO");
		motivos.put("03", "VALOR MÍNIMO");
		motivos.put("04", "LAYOUT INCORRETO");
		motivos.put("05", "PRODUTO NÃO CADASTRADO");
		motivos.put("06", "FALTA DE ESTOQUE");
		motivos.put("07", "ESTOQUE INSUFICIENTE");
		motivos.put("08", "ALVARÁ VENCIDO");
		motivos.put("09", "CLIENTE INVÁLIDO");
		motivos.put("10", "PRODUTO BLOQUEADO");
		motivos.put("11", "PRODUTO NÃO CADASTRADO");
		motivos.put("12", "PEDIDO JÁ ENVIADO");
		motivos.put("13", "CLIENTE BLOQUEADO");
		motivos.put("14", "PENDENCIA FINANCEIRA OU CREDITO");
		motivos.put("51", "HORARIO ULTRAPASSADO");
		motivos.put("83", "CD INCORRETO");
		motivos.put("99", "NÃO ESPECIFICADO");

		conectar();
	}

	public static void main(String argv[]) {
		try {
			new Comunicador().enviaPedidos();
			new Comunicador().recebePedidos();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void conectar() {

		if (!ftp.isConnected()) {
			try {
				ftp.connect(host, porta);

				// verifica se conectou com sucesso!
				if (FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
					LogUtil.info(Comunicador.class, "BARATELA: Conectando ao FTP");
					ftp.login(usuario, senha);
					LogUtil.info(Comunicador.class, "BARATELA: Diretorio atual: " + ftp.printWorkingDirectory());
				} else {
					// erro ao se conectar
					ftp.disconnect();
					LogUtil.info(Comunicador.class, "BARATELA: Conexão recusada");
				}
				ftp.enterLocalActiveMode();
			} catch (Exception e) {
				LogUtil.error(Comunicador.class, "BARATELA: Ocorreu um erro inesperado.... " + e.getMessage());
			}
		}
	}

	public File geraArquivoPedido(Pedido pedido) throws Exception {

		Canal can = new CanalDao().getCanal(canal);

		File file = new File("C:\\Atua\\PHL##" + can.cnpj + "_"
				+ new SimpleDateFormat("YYYYMMDDHHMMSS").format(new Date()) + ".txt");
		BufferedWriter fw = new BufferedWriter(new FileWriter(file));

		String s;
		s = "1"; // IDENTIFICAÇÃO DO TIPO DE REGISTRO
		s += pedido.cnpj;
		s += "1";
		s += "0000000000000";
		s += "00";
		fw.write(s);
		fw.newLine();

		s = "2";
		s += "0";
		s += Str.alinhaZeroDireita(String.valueOf(pedido.numero), 7);
		s += "07222185000209";
		s += "00000000";
		fw.write(s);
		fw.newLine();

		Prazo prazo = new PrazoDao().getPrazo(pedido.prazo);

		s = "3";
		s += "2";
		s += Str.alinhaZeroDireita(String.valueOf(prazo.codigoExporta), 4);
		s += "000";
		s += "0000000";
		s += "000000000000000";
		fw.write(s);
		fw.newLine();

		s = "4";
		s += "000000000000000"; // NÚMERO DO PEDIDO CLIENTE *
		s += Str.alinhaZeroDireita("", 15);
		fw.write(s);
		fw.newLine();

		s = "5";
		s += new SimpleDateFormat("ddMMyyyy").format(pedido.data);
		s += Str.alinhaZeroDireita("", 22);
		fw.write(s);
		fw.newLine();

		s = "6";
		s += "000000"; // HORA DO PEDIDO
		s += Str.alinhaZeroDireita("", 24);
		fw.write(s);
		fw.newLine();

		for (PedidoItem i : pedido.itens) {
			Produto prod = new ProdutoDao().getProduto(i.produto);

			s = "7";
			s += Str.alinhaZeroDireita(prod.ean, 13);
			s += new DecimalFormat("0000").format(i.qntBonificacao + i.qntVenda);
			s += "00";
			s += Str.alinhaZeroDireita("", 7);

			// FAZ O ARREDONDAMENTO PARA BAIXO (2 CASAS DECIMAIS)
			double percDesconto = Math
					.floor(((((i.qntBonificacao + i.qntVenda) * i.valorBruto) - (i.qntVenda * i.valorUnitario))
							/ ((i.qntBonificacao + i.qntVenda) * i.valorBruto)) * 10000);

			s += new DecimalFormat("0000").format(percDesconto);
			fw.write(s);
			fw.newLine();
		}

		s = "8";
		s += new DecimalFormat("00").format(pedido.itens.size());
		s += Str.alinhaZeroDireita("", 28);
		fw.write(s);
		fw.newLine();

		fw.flush();
		fw.close();
		return file;
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

		LogUtil.info(Comunicador.class, "BARATELA: Enviando arquivo!");
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
		LogUtil.info(Comunicador.class, "BARATELA: Pasta " + pasta);
		LogUtil.info(Comunicador.class, "BARATELA: Enviando arquivo " + nomeArquivo + "...");

		// faz o envio do arquivo
		// ftp.mkd(pasta);
		ftp.changeWorkingDirectory(pasta);
		// String dirs[] = pasta.split("/");
		// for (String dir : dirs) {
		// System.out.println("mkdir: " + dir);
		// ftp.mkd(dir);
		// ftp.changeWorkingDirectory(dir);
		// }

		ftp.storeFile(nomeArquivo, is);
		is.close();
		LogUtil.info(Comunicador.class, "BARATELA: Arquivo " + nomeArquivo + " enviado com sucesso!");
	}

	@Override
	public void enviaPedidos() throws Exception {

		conectar();

		// Conector.getConexaoVK()
		// .executar(
		// "UPDATE VEN_PEDIDO P SET STATUS = 'PENDENTE EXPORTAÇÃO' WHERE STATUS
		// = 'BLOQUEIO COMERCIAL' AND CANAL = "
		// + canal);

		// CARREGA TODOS OS PEDIDO QUE ESTÃO LIBERADOS PARA EXPORTAÇÃO
		PedidoDao pedDao = new PedidoDao();
		List<Pedido> pedidos = pedDao.getPedidosPendenteExportacao(canal);
		LogUtil.info(Comunicador.class, "BARATELA: Enviando pedidos para distribuidora: " + pedidos.size());
		for (Pedido ped : pedidos) {
			try {
				File f = geraArquivoPedido(ped);
				enviaPedido(f);

				// GRAVA LOG DE INTEGRACAO
				PedidoIntegracao pedInt = new PedidoIntegracao();
				pedInt.pedido = ped.numero;
				pedInt.dataRecebimento = new Date();
				pedInt.tipoArquivo = PedidoIntegracao.ENVIO_PEDIDO;
				pedInt.nomeArquivo = f.getName();
				new PedidoIntegracaoDao().salvar(pedInt);

				LogUtil.info(Comunicador.class, "BARATELA: Pedido enviado com sucesso: " + ped.numero);
				ped.status = Pedido.EXPORTADO_FATURAMENTO;
				new PedidoDao().atualizaStatus(ped);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		salvaRegistroLogExecucao(canal);
	}

	@Override
	public void recebePedidos() throws Exception {

		recebeRetornoPedido();
		recebeEstoque();

		String pasta = pastaNFe;
		LogUtil.info(Comunicador.class, "BARATELA: Lendo notas fiscais...");

		Canal canal = new CanalDao().getCanal(this.canal);

		// ftp.mkd(pasta);
		ftp.changeWorkingDirectory(pasta);
		// String dirs[] = pasta.split("/");
		// for (String dir : dirs) {
		// System.out.println("mkdir: " + dir);
		// ftp.mkd(dir);
		// ftp.changeWorkingDirectory(dir);
		// }

		FTPFile[] files = ftp.listFiles();
		LogUtil.info(Comunicador.class, "BARATELA: Total de notas: " + files.length);
		new File("C:\\VK-FARMA\\INTEGRACAO\\BARATELA").mkdirs();

		for (FTPFile f : files) {
			LogUtil.info(Comunicador.class, "BARATELA: Lendo nota: " + f.getName());
			String arqDown = "C:\\VK-FARMA\\INTEGRACAO\\BARATELA\\" + f.getName();
			FileOutputStream fos = new FileOutputStream(arqDown);
			boolean download = ftp.retrieveFile(f.getName(), fos);
			if (download) {
				try {
					if (f.isFile() && f.getName().toUpperCase().endsWith("NOT")
							&& f.getName().substring(5, 19).equals(canal.cnpj)) {
						processaNOT(arqDown);
						ftp.deleteFile(f.getName());
					}
					// processaNFE(arqDown);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				LogUtil.error(Comunicador.class, "BARATELA: Erro ao tentar ler notas ");
			}
			fos.flush();
			fos.close();
		}
	}

	public void recebeRetornoPedido() throws Exception {
		ftp.changeWorkingDirectory(pastaRetornoPedido);

		FTPFile[] files = ftp.listFiles();
		LogUtil.info(Comunicador.class, "BARATELA: ARQUIVOS A SEREM BAIXADOS: " + files.length);
		new File("C:\\VK-FARMA\\INTEGRACAO\\BARATELA\\RETORNOPEDIDO\\").mkdirs();

		for (FTPFile f : files) {
			if (f.isFile()) {
				LogUtil.info(Comunicador.class, "BARATELA: DOWNLOAD NFE::: " + f.getName());
				String arqDown = "C:\\VK-FARMA\\INTEGRACAO\\BARATELA\\RETORNOPEDIDO\\" + f.getName();
				FileOutputStream fos = new FileOutputStream(arqDown);
				boolean download = ftp.retrieveFile(f.getName(), fos);
				if (download) {
					try {
						// abre o pedido
						BufferedReader br = new BufferedReader(new FileReader(arqDown));
						String s;

						Pedido ped = null;
						int pedido = 0;

						int qntAtendidaTotal = 0;

						while ((s = br.readLine()) != null) {
							if (s.startsWith("1")) {
								pedido = Integer.parseInt(s.substring(16, 23));
								ped = new PedidoDao().getPedido(pedido);
								LogUtil.info(Comunicador.class, "BARATELA: Pedido: " + pedido);
							} else if (s.startsWith("2")) {

							} else if (s.startsWith("3")) {
								String ean = s.substring(1, 14);
								int qtdeAtendida = Integer.parseInt(s.substring(14, 18));
								qntAtendidaTotal += qtdeAtendida;
								int qtdeNaoAtendida = Integer.parseInt(s.substring(18, 22));
								String motivo = s.substring(22, 24);
								String retorno = s.substring(24, 25);
								LogUtil.info(Comunicador.class, "BARATELA: " + retorno);

								Produto produto = new ProdutoDao().getProdutoEAN(ean);
								if (ped != null) {
									for (PedidoItem it : ped.itens) {
										if (it.produto.equals(produto.codigo)) {
											it.qntFaturada = qtdeAtendida;
										}
									}
								}

								if (ped != null && pedido != 0) {
									if (ped.status.equals(Pedido.FATURADO) == false) {
										if (motivo.equals("00") || motivo.equals("12")) {
											ped.status = Pedido.RECEBIDO_FATURAMENTO;
										} else {
											ped.status = Pedido.REJEITADO + " " + motivos.get(motivo);
										}
									}
								}
							}
						}

						if (ped != null) {
							if (ped.status.equals(Pedido.FATURADO) == false) {
								if (qntAtendidaTotal > 0) {
									ped.status = Pedido.RECEBIDO_FATURAMENTO;
								}
							}

							// GRAVA LOG DE INTEGRACAO
							PedidoIntegracao pedInt = new PedidoIntegracao();
							pedInt.pedido = ped.numero;
							pedInt.dataRecebimento = new Date();
							pedInt.tipoArquivo = PedidoIntegracao.RETORNO_PEDIDO;
							pedInt.nomeArquivo = f.getName();
							new PedidoIntegracaoDao().salvar(pedInt);

							new PedidoDao().Salvar(ped);
							new PedidoDao().atualizaStatus(ped);
						}

						ftp.deleteFile(f.getName());
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					LogUtil.info(Comunicador.class, "BARATELA: Error in downloading file !");
				}
				fos.flush();
				fos.close();
			}
		}

	}

	public void recebeEstoque() throws Exception {
		ftp.changeWorkingDirectory(pastaEstoque);

		FTPFile[] files = ftp.listFiles();
		LogUtil.info(Comunicador.class, "BARATELA: Recebendo estoque");
		LogUtil.info(Comunicador.class, "BARATELA: ARQUIVOS A SEREM BAIXADOS: " + files.length);
		new File("C:\\VK-FARMA\\INTEGRACAO\\BARATELA\\ESTOQUE\\").mkdirs();

		for (FTPFile f : files) {
			if (f.isFile()) {

				Date dataAlteracao = f.getTimestamp().getTime();
				
				LogUtil.info(Comunicador.class, "BARATELA: Arquivo estoque: " + f.getName());
				String arqDown = "C:\\VK-FARMA\\INTEGRACAO\\BARATELA\\ESTOQUE\\" + f.getName();
				FileOutputStream fos = new FileOutputStream(arqDown);
				boolean download = ftp.retrieveFile(f.getName(), fos);
				if (download) {
					try {
						// abre o pedido
						BufferedReader br = new BufferedReader(new FileReader(arqDown));
						String s;

						while ((s = br.readLine()) != null) {
							String produto;
							int estoque = 0;
							if (s.startsWith("80")) {
								produto = s.substring(2, 15);
								estoque = Integer.parseInt(s.substring(16, 24).replace(",", ""));
								LogUtil.info(Comunicador.class, "BARATELA: Estoque: " + estoque + " Produto: " + produto);
								CanalProduto canProd = new CanalProdutoDao().getCanalProdutoEAN(canal, produto);
								if (canProd != null) {
									LogUtil.info(Comunicador.class, "BARATELA: Atualizando estoque... " + canProd.produto);
									canProd.estoque = estoque;
									canProd.estoqueData = dataAlteracao;
									new CanalProdutoDao().salvar(canProd);
								}
							}
						}

						ftp.deleteFile(f.getName());
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					LogUtil.info(Comunicador.class, "BARATELA: Error in downloading file !");
				}
				fos.flush();
				fos.close();
			}
		}
	}

	@Override
	public void recebeTitulos() throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public void recebeClientes() throws Exception {
		// TODO Auto-generated method stub
	}

	public void processaNOT(String arq) throws Exception {
		// abre o pedido
		File f = new File(arq);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String s;

		Pedido ped = null;
		int pedido = 0;
		double totalFaturado = 0;
		while ((s = br.readLine()) != null) {
			if (s.startsWith("1")) {
				pedido = Integer.parseInt(s.substring(30, 37));
				ped = new PedidoDao().getPedido(pedido);
				if (ped != null) {
					for (PedidoItem it : ped.itens) {
						it.qntFaturada = 0;
					}
				}
				LogUtil.info(Comunicador.class, "BARATELA: Pedido: " + pedido);
			} else if (s.startsWith("2")) {
				ped.dataFaturamento = new SimpleDateFormat("ddMMyyyy").parse(s.substring(15, 23));
				ped.numeroNota = Integer.parseInt(s.substring(37, 43));

			} else if (s.startsWith("5")) {
				String ean = s.substring(1, 14);

				int qtdeAtendida = Integer.parseInt(s.substring(21, 25));
				Double valorUnitario = Double.parseDouble(s.substring(60, 68)) / 100;

				Produto produto = new ProdutoDao().getProdutoEAN(ean);
				if (ped != null) {
					for (PedidoItem it : ped.itens) {
						if (it.produto.equals(produto.codigo)) {
							it.qntFaturada = qtdeAtendida;
							it.valorFaturado = qtdeAtendida * valorUnitario;
							totalFaturado += it.valorFaturado;
						}
					}
				}
			}
		}

		if (ped != null) {
			ped.setValorFaturado(totalFaturado);
			new PedidoDao().Salvar(ped);
			ped.status = Pedido.FATURADO;
			new PedidoDao().atualizaStatus(ped);

			// GRAVA LOG DE INTEGRACAO
			PedidoIntegracao pedInt = new PedidoIntegracao();
			pedInt.pedido = ped.numero;
			pedInt.dataRecebimento = new Date();
			pedInt.tipoArquivo = PedidoIntegracao.RETORNO_NOTA;
			pedInt.nomeArquivo = f.getName();
			new PedidoIntegracaoDao().salvar(pedInt);
		}
	}

	public void processaNFE(String arq) throws Exception {

		TNfeProc nfe = lerXML_JAXB.getNFe(arq);
		if (nfe != null) {
			// BUSCA O PEDIDO ORIGEM - SISTEMA VENDAS
			int numeroPedido = 0;
			try {
				numeroPedido = Integer.parseInt(nfe.getNFe().getInfNFe().getDet().get(0).getProd().getXPed());
			} catch (Exception e) {
				throw new Exception(e);
				// NÃO É UM NÚMERO VÁLIDO
			}

			Pedido pedido = new Pedido();
			pedido.canal = canal;
			pedido.numero = numeroPedido;
			// pedido.numeroDistribuidor = Integer.parseInt(nfe.getNFe()
			// .getInfNFe().getIde().getNNF());
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

				// GRAVA NO BANCO DE DADOS O EAN E O CÓDIGO DA DISTRIBUIDORA
				try {
					String sql = "";
					sql += "INSERT INTO VEN_LOGPEDIDOITEM (codigo,ean,descricao,CANAL) values (";
					sql += Oracle.strInsert(nIt.getProd().getCProd()) + ",";
					sql += Oracle.strInsert(nIt.getProd().getCEAN()) + ",";
					sql += Oracle.strInsert(nIt.getProd().getXProd()) + ",";
					sql += Oracle.strInsert(canal) + ")";
					Conector.getConexaoVK().executar(sql);
				} catch (Exception e) {
					// NÃO PRECISA RETORNAR ERRO DE EXECUÇÃO DE SQL
					e.printStackTrace();
				}
			}
			pedido.valorBruto = totalBruto;
			pedido.valorFaturado = totalFaturado;
			pedido.valorLiquido = totalLiquido;

			new PedidoDao().Salvar(pedido);
			new PedidoDao().atualizaStatus(pedido);
		}
	}

	@Override
	public void recebeDevolucoes() throws Exception {

		return;
	}
}
