package com.integracao.medicamental;

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

import br.com.javac.v300.procnfe.TNFe.InfNFe.Det;
import br.com.smp.vk.venda.model.Canal;
import br.com.smp.vk.venda.model.CanalSetor;
import br.com.smp.vk.venda.model.Pedido;
import br.com.smp.vk.venda.model.PedidoItem;
import br.com.smp.vk.venda.model.Prazo;
import br.com.smp.vk.venda.model.Produto;
import br.com.javac.v300.procnfe.TNfeProc;
import conect.Conector;
import conect.Oracle;
import vendas.dao.CanalDao;
import vendas.dao.CanalProdutoDao;
import vendas.dao.CanalSetorDao;
import vendas.dao.PedidoDao;
import vendas.dao.PrazoDao;
import vendas.dao.ProdutoDao;

public class Comunicador extends Integrador {

	private int canal = 21;

	private FTPClient ftp = new FTPClient();

	public String host = "edi.medicamental.com.br";
	public String usuario = "cifarma";
	public String senha = "cifarma2017";

	public String pastaPedido = "/pedido";
	public String pastaNFe = "/retorno";
	public String pastaRetornoPedido = "/retorno";

	private HashMap<String, String> motivos = new HashMap<String, String>();

	public static void main(String argv[]) {
		try {
			new Comunicador().enviaPedidos();
			new Comunicador().recebePedidos();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Comunicador(Finaliza f, ServletIntegradorNormal integrador) {
		super(f, integrador, "MEDICAMENTAL");
		iniciar();
	}

	private Comunicador() {
		iniciar();
	}

	public void iniciar() {
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

	private void conectar() {
		try {
			ftp.connect(host);

			// verifica se conectou com sucesso!
			if (FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
				ftp.login(usuario, senha);
			} else {
				// erro ao se conectar
				ftp.disconnect();
				System.out.println("Conexão recusada");
			}
			System.out.println("CONECTADO...");
		} catch (Exception e) {
			System.out.println("Ocorreu um erro: " + e);
			e.printStackTrace();
		}
	}

	public File geraArquivoPedido(Pedido pedido) throws Exception {

		Canal canal = new CanalDao().getCanal(this.canal);

		String codigoProjeto = "00";

		File file = new File("C:\\Atua\\PHL" + codigoProjeto + canal.cnpj + "_"
				+ new SimpleDateFormat("YYYYMMDDHHMMSS").format(new Date()) + ".txt");
		BufferedWriter fw = new BufferedWriter(new FileWriter(file));

		CanalSetor canSet = new CanalSetorDao().getCanalSetor(this.canal, pedido.setorVendedor);

		String s;
		s = "1"; // IDENTIFICAÇÃO DO TIPO DE REGISTRO
		s += pedido.cnpj;
		s += "1";
		// s += Str.alinhaZeroDireita(Integer.parseInt(canSet.setorExporta) +
		// "", 13);
		s += Str.alinhaZeroDireita(Integer.parseInt("0") + "", 13);
		s += codigoProjeto;
		fw.write(s);
		fw.newLine();

		s = "2";
		s += "0";
		s += Str.alinhaZeroDireita(String.valueOf(pedido.numero), 7);
		s += canal.cnpj;
		s += "00000000";
		fw.write(s);
		fw.newLine();

		Prazo prazo = new PrazoDao().getPrazo(pedido.prazo);

		String codigoPrazo = prazo.codigoExporta;
		s = "3";
		s += "3";
		s += Str.alinhaZeroDireita(codigoPrazo, 4);
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
		System.out.println("Arquivo " + nomeArquivo + " enviado com sucesso!");
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
		System.out.println("ENVIANDO PEDIDOS PARA DISTRIBUIDORA: " + pedidos.size());
		for (Pedido ped : pedidos) {
			try {
				// com.util.Email
				// .enviaSemAnexo("PEDIDO DE VENDA CIFARMA (" + ped.numero
				// + ")", com.integracao.focofarma.Comunicador
				// .montaEmail(ped),
				// "simone.nazaria@hotmail.com");
				// System.out.println("PEDIDO ENVIADO COM SUCESSO");
				// ped.status = Pedido.EXPORTADO_FATURAMENTO;
				// new PedidoDao().atualizaStatus(ped);

				File f = geraArquivoPedido(ped);
				enviaPedido(f);
				System.out.println("PEDIDO ENVIADO COM SUCESSO: " + ped.numero);
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

		String pasta = pastaNFe;
		System.out.println("BAIXANDO NOTAS FISCAIS MEDICAMENTAL");
		ftp.changeWorkingDirectory(pasta);
		FTPFile[] files = ftp.listFiles();
		System.out.println("ARQUIVOS A SEREM BAIXADOS: " + files.length);

		new File("C:\\VK-FARMA\\INTEGRACAO\\MEDICAMENTAL").mkdirs();

		Canal canal = new CanalDao().getCanal(this.canal);

		for (FTPFile f : files) {

			System.out.println(canal.cnpj);
			System.out.println(f.getName().substring(5, 19));

			if (f.isFile() && f.getName().toUpperCase().endsWith("NOT")
					&& f.getName().substring(5, 19).equals(canal.cnpj)) {
				System.out.println("DOWNLOAD NOTA::: " + f.getName());
				String arqDown = "C:\\VK-FARMA\\INTEGRACAO\\MEDICAMENTAL\\" + f.getName();
				FileOutputStream fos = new FileOutputStream(arqDown);
				boolean download = ftp.retrieveFile(f.getName(), fos);
				fos.flush();
				fos.close();
				if (download) {
					try {
						// abre o pedido
						BufferedReader br = new BufferedReader(new FileReader(arqDown));
						String s;

						Pedido ped = null;
						int pedido = 0;
						double totalFaturado = 0;
						while ((s = br.readLine()) != null) {
							if (s.startsWith("1")) {
								pedido = Integer.parseInt(s.substring(30, 37));
								ped = new PedidoDao().getPedido(pedido);
								System.out.println(pedido);
							} else if (s.startsWith("2")) {
								ped.dataFaturamento = new SimpleDateFormat("ddMMyyyy").parse(s.substring(15, 23));
								ped.numeroNota = Integer.parseInt(s.substring(37, 43));

							} else if (s.startsWith("5")) {
								String ean = s.substring(1, 14);

								int qtdeAtendida = Integer.parseInt(s.substring(21, 25));
								Double valorUnitario = Double.parseDouble(s.substring(60, 68)) / 100;

								System.out.println(valorUnitario);

								Produto produto = new ProdutoDao().getProdutoEAN(ean);
								for (PedidoItem it : ped.itens) {
									if (it.produto.equals(produto.codigo)) {
										it.qntFaturada = qtdeAtendida;
										it.valorFaturado = qtdeAtendida * valorUnitario;
										totalFaturado += it.valorFaturado;
									}
								}
							}
						}

						if (ped != null) {
							ped.setValorFaturado(totalFaturado);
							new PedidoDao().Salvar(ped);
							ped.status = Pedido.FATURADO;
							new PedidoDao().atualizaStatus(ped);
							ftp.deleteFile(f.getName());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("Error in downloading file !");
				}
			}
		}
	}

	public void recebeRetornoPedido() throws Exception {
		ftp.changeWorkingDirectory(pastaRetornoPedido);

		FTPFile[] files = ftp.listFiles();
		System.out.println("ARQUIVOS A SEREM BAIXADOS: " + files.length);
		new File("C:\\VK-FARMA\\INTEGRACAO\\MEDICAMENTAL\\RETORNOPEDIDO").mkdirs();

		Canal canal = new CanalDao().getCanal(this.canal);

		for (FTPFile f : files) {

			System.out.println(f.getName().substring(5, 19));

			if (f.isFile() && f.getName().toUpperCase().endsWith("RET")
					&& f.getName().substring(5, 19).equals(canal.cnpj)) {
				System.out.println("DOWNLOAD RETORNO REDIDO::: " + f.getName());
				String arqDown = "C:\\VK-FARMA\\INTEGRACAO\\MEDICAMENTAL\\RETORNOPEDIDO\\" + f.getName();
				FileOutputStream fos = new FileOutputStream(arqDown);
				boolean download = ftp.retrieveFile(f.getName(), fos);
				fos.flush();
				fos.close();
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
								System.out.println(pedido);
							} else if (s.startsWith("2")) {

							} else if (s.startsWith("3")) {
								String ean = s.substring(1, 14);

								int qtdeAtendida = Integer.parseInt(s.substring(14, 18));
								qntAtendidaTotal += qtdeAtendida;
								int qtdeNaoAtendida = Integer.parseInt(s.substring(18, 22));
								String motivo = s.substring(22, 24);
								String retorno = s.substring(24, 25);
								System.out.println(retorno);

								Produto produto = new ProdutoDao().getProdutoEAN(ean);
								for (PedidoItem it : ped.itens) {
									if (it.produto.equals(produto.codigo)) {
										it.qntFaturada = qtdeAtendida;
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
							new PedidoDao().Salvar(ped);
							new PedidoDao().atualizaStatus(ped);
						}
						ftp.deleteFile(f.getName());
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("Error in downloading file !");
				}
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

	public void processaNFE(String arq) throws Exception {

		TNfeProc nfe = lerXML_JAXB.getNFe(arq);
		if (nfe != null) {
			// BUSCA O PEDIDO ORIGEM - SISTEMA VENDAS
			int numeroPedido = 0;
			try {
				System.out.println("XPED: " + nfe.getNFe().getInfNFe().getDet().get(0).getProd().getXPed());
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

	@Override
	public void recebeEstoque() throws Exception {
		// TODO Auto-generated method stub

	}
}
