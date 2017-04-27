package com.integracao.medchap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import vendas.dao.CanalProdutoDao;
import vendas.dao.PedidoDao;
import vendas.dao.ProdutoDao;
import br.com.javac.v300.procnfe.TNFe.InfNFe.Det;
import br.com.smp.vk.venda.model.Pedido;
import br.com.smp.vk.venda.model.PedidoItem;
import br.com.smp.vk.venda.model.Produto;
import br.com.javac.v300.procnfe.TNfeProc;

import com.integrador.Integrador;
import com.integrador.ServletIntegradorNormal;
import com.integrador.ServletIntegradorNormal.Finaliza;
import com.util.Str;
import com.util.lerXML_JAXB;

import conect.Conector;
import conect.Oracle;

public class Comunicador extends Integrador {

	private final int canal = 8;

	private FTPClient ftp = new FTPClient();

	private String host = "187.4.255.101";
	private String usuario = "Cifarma";
	private String senha = "cf1557";

	private String pastaPedido = "/cif_envio";
	private String pastaNFe = "/cif_retorno";

	public static void main(String argv[]) {
		try {
			new Comunicador().enviaPedidos();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	public Comunicador(Finaliza f, ServletIntegradorNormal integrador) {
		super(f, integrador, "MEDCHAP");
		try {
			conectar();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Comunicador() {
		try {
			conectar();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
				System.exit(1);
			}
			System.out.println("CONECTADO...");
		} catch (Exception e) {
			System.out.println("Ocorreu um erro: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	private File geraArquivoPedido(Pedido pedido) throws Exception {

		int contLinhas = 0;

		File file = new File("C:\\Atua\\" + pedido.numero + ".ped");
		FileWriter fw = new FileWriter(file);

		String s = "";
		s += "1"; // IDENTIFICAÇÃO DO TIPO DE REGISTRO
		s += new DecimalFormat("00000").format(++contLinhas);
		s += "PED";
		s += "OPE";
		s += "000000";
		s += "00000000";
		s += pedido.cnpj;
		s += new DecimalFormat("00000000").format(pedido.numero);
		String obs = "PRAZO: " + pedido.prazoNome + ", OBS: " + pedido.obs;

		if (obs.length() > 200)
			obs = obs.substring(200);
		else
			for (int i = obs.length(); i < 200; i++)
				obs += " ";

		s += obs;
		fw.write(s + "\n");

		int qntItens = 0;

		for (PedidoItem i : pedido.itens) {

			Produto prod = new ProdutoDao().getProduto(i.produto);

			qntItens += i.qntBonificacao + i.qntVenda;

			s = "";
			s += "2";
			s += new DecimalFormat("00000").format(++contLinhas);
			s += prod.ean;
			s += "000000";
			s += new DecimalFormat("00000").format(i.qntBonificacao
					+ i.qntVenda);
			s += new DecimalFormat("0000000").format(Str
					.arredondar2((i.qntVenda * i.valorUnitario)
							/ (i.qntVenda + i.qntBonificacao)) * 100);
			fw.write(s + "\n");
		}

		s = "";
		s += "3";
		s += new DecimalFormat("00000").format(++contLinhas);
		s += new DecimalFormat("00000").format(pedido.itens.size());
		s += new DecimalFormat("0000000").format(qntItens);
		fw.write(s + "\n");

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
		// // ftp.mkd(pasta);
		ftp.changeWorkingDirectory(pasta);
		String dirs[] = pasta.split("/");
		for (String dir : dirs) {
			// // ftp.mkd(dir);
			ftp.changeWorkingDirectory(dir);
		}

		ftp.storeFile(nomeArquivo, is);
		is.close();
		System.out.println("Arquivo " + nomeArquivo + " enviado com sucesso!");
	}

	@Override
	public void enviaPedidos() throws Exception {

		try {
			conectar();

			// Conector.getConexaoVK()
			// .executar(
			// "UPDATE VEN_PEDIDO P SET STATUS = 'PENDENTE EXPORTAÇÃO' WHERE STATUS = 'BLOQUEIO COMERCIAL' AND CANAL = "
			// + canal);

			// CARREGA TODOS OS PEDIDO QUE ESTÃO LIBERADOS PARA EXPORTAÇÃO
			PedidoDao pedDao = new PedidoDao();
			List<Pedido> pedidos = pedDao.getPedidosPendenteExportacao(canal);
			System.out.println("ENVIANDO PEDIDOS PARA DISTRIBUIDORA: "
					+ pedidos.size());
			for (Pedido ped : pedidos) {
				try {
					File f = geraArquivoPedido(ped);
					enviaPedido(f);
					System.out.println("PEDIDO ENVIADO COM SUCESSO: "
							+ ped.numero);
					ped.status = Pedido.EXPORTADO_FATURAMENTO;
					new PedidoDao().atualizaStatus(ped);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			salvaRegistroLogExecucao(canal);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void recebePedidos() throws Exception {
		String pasta = pastaNFe;
		System.out.println("BAIXANDO NOTAS FISCAIS MEDCHAP");
		// ftp.mkd(pasta);
		ftp.changeWorkingDirectory(pasta);
		String dirs[] = pasta.split("/");
		for (String dir : dirs) {
			System.out.println("mkdir: " + dir);
			// ftp.mkd(dir);
			ftp.changeWorkingDirectory(dir);
		}

		FTPFile[] files = ftp.listFiles();
		System.out.println("ARQUIVOS A SEREM BAIXADOS: " + files.length);
		new File("C:\\VK-FARMA\\INTEGRACAO\\MEDCHAP").mkdirs();

		for (FTPFile f : files) {
			if (f.isFile()) {
				System.out.println("DOWNLOAD NFE::: " + f.getName());
				String arqDown = "C:\\VK-FARMA\\INTEGRACAO\\MEDCHAP\\"
						+ f.getName();
				FileOutputStream fos = new FileOutputStream(arqDown);
				boolean download = ftp.retrieveFile(f.getName(), fos);
				if (download) {
					try {
						processaNFE(arqDown);
						ftp.deleteFile(f.getName());
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("Error in downloading file !");
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

	public void processaNFE(String arq) throws Exception {

		TNfeProc nfe = lerXML_JAXB.getNFe(arq);
		if (nfe != null) {

			// BUSCA O PEDIDO ORIGEM - SISTEMA VENDAS
			int numeroPedido = 0;
			try {
				System.out.println("XPED: "
						+ nfe.getNFe().getInfNFe().getCompra().getXPed());
				numeroPedido = Integer.parseInt(nfe.getNFe().getInfNFe()
						.getCompra().getXPed());
			} catch (Exception e) {
				// NÃO É UM NÚMERO VÁLIDO
				throw new Exception(e);
			}

			Pedido pedido = new Pedido();
			pedido.canal = canal;
			pedido.numero = numeroPedido;
			pedido.numeroDistribuidor = Integer.parseInt(nfe.getNFe()
					.getInfNFe().getIde().getNNF());
			pedido.numeroNota = Integer.parseInt(nfe.getNFe().getInfNFe()
					.getIde().getNNF());
			pedido.cnpj = nfe.getNFe().getInfNFe().getDest().getCNPJ();
			pedido.clienteRazao = nfe.getNFe().getInfNFe().getDest().getXNome();
			pedido.clienteUF = nfe.getNFe().getInfNFe().getDest()
					.getEnderDest().getUF().name();
			try {
				pedido.data = new SimpleDateFormat("yyyy-MM-dd").parse(nfe
						.getNFe().getInfNFe().getIde().getDhEmi());
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
				Produto prod = new ProdutoDao().getProdutoEAN(nIt.getProd()
						.getCEAN());

				it.qntFaturada = (int) Double.parseDouble(nIt.getProd()
						.getQCom());
				it.valorFaturado = Double.parseDouble(nIt.getProd().getVProd());
				it.qntVenda = it.qntFaturada;
				it.valorUnitario = it.valorFaturado / it.qntVenda;
				if (prod != null) {
					it.produto = prod.codigo;
					try {
						it.valorBruto = new CanalProdutoDao().getCanalProduto(
								canal, prod.codigo).valorBruto;
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
				String sql = "";
				sql += "INSERT INTO VEN_LOGPEDIDOITEM (codigo,ean,descricao,CANAL) values (";
				sql += Oracle.strInsert(nIt.getProd().getCProd()) + ",";
				sql += Oracle.strInsert(nIt.getProd().getCEAN()) + ",";
				sql += Oracle.strInsert(nIt.getProd().getXProd()) + ",";
				sql += Oracle.strInsert(canal) + ")";
				try {
					Conector.getConexaoVK().executar(sql);
				} catch (Exception e) {
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
