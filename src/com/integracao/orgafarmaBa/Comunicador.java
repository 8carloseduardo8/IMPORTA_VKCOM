package com.integracao.orgafarmaBa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import com.integrador.Integrador;
import com.integrador.ServletIntegradorNormal;
import com.integrador.ServletIntegradorNormal.Finaliza;
import com.util.lerXML_JAXB;

import br.com.javac.v300.procnfe.TNFe.InfNFe.Det;
import br.com.smp.vk.venda.model.CanalProduto;
import br.com.smp.vk.venda.model.Pedido;
import br.com.smp.vk.venda.model.PedidoIntegracao;
import br.com.smp.vk.venda.model.PedidoItem;
import br.com.javac.v300.procnfe.TNfeProc;
import vendas.dao.CanalProdutoDao;
import vendas.dao.PedidoDao;
import vendas.dao.PedidoIntegracaoDao;

public class Comunicador extends Integrador {

	private final int canal = 31;

	public FTPClient ftp = new FTPClient();

	public String unidade = "3";
	public String pastaInterna = "C:\\ATUA";

	public String host = "";
	public String usuario = "";
	public String senha = "";

	public String pastaPedido = "\\entrada";
	public String pastaNFe = "\\saida\\nota";
	public String pastaRetorno = "\\saida\\retorno";

	Logger logger = Logger.getLogger(Comunicador.class);

	private boolean conectado;

	public static void main(String argv[]) {
		try {
			 new Comunicador().enviaPedidos();
//			new Comunicador().recebePedidos();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Comunicador(Finaliza f, ServletIntegradorNormal integrador) {
		super(f, integrador, "ORGAFARMA");
		iniciar();
	}

	public Comunicador() {
		iniciar();
	}

	public void iniciar() {
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
			conectado = true;
		} catch (Exception e) {
			System.out.println("Ocorreu um erro: " + e);
			e.printStackTrace();
		}
	}

	@Override
	public void recebePedidos() throws Exception {
		if (conectado == false) {
			return;
		}

		recebeRetornoPedido();

		String pasta = pastaNFe;
		System.out.println("BAIXANDO NOTAS FISCAIS ORGAFARMA");
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
		new File("C:\\VK-FARMA\\INTEGRACAO\\ORGAFARMA").mkdirs();

		for (FTPFile f : files) {
			System.out.println("DOWNLOAD NFE::: " + f.getName());
			String arqDown = "C:\\VK-FARMA\\INTEGRACAO\\ORGAFARMA\\" + f.getName();
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
		logger.error("EXECUTADO");
	}

	private void recebeRetornoPedido() throws Exception {
		if (conectado == false) {
			return;
		}

		String pasta = pastaRetorno;
		System.out.println("BAIXANDO RETORNOS DOS PEDIDOS");
		// ftp.mkd(pasta);
		ftp.changeWorkingDirectory(pasta);
		System.out.println("DIRETÓRIO ATUAL: " + ftp.printWorkingDirectory());

		FTPFile[] files = ftp.listFiles();
		System.out.println("ARQUIVOS A SEREM BAIXADOS: " + files.length);
		new File("C:\\VK-FARMA\\INTEGRACAO\\ORGAFARMA\\RETORNO").mkdirs();

		for (FTPFile f : files) {
			System.out.println("DOWNLOAD RETORNO::: " + f.getName());
			String arqDown = "C:\\VK-FARMA\\INTEGRACAO\\ORGAFARMA\\RETORNO\\" + f.getName();
			FileOutputStream fos = new FileOutputStream(arqDown);
			boolean download = ftp.retrieveFile(f.getName(), fos);
			if (download) {
				try {

					SAXBuilder sb = new SAXBuilder();
					Document d = sb.build(arqDown);
					Element mural = d.getRootElement();
					List elements = mural.getChildren();
					Iterator i = elements.iterator();

					// Iteramos com os elementos filhos, e filhos do dos filhos
					while (i.hasNext()) {
						Element element = (Element) i.next();
						String numeroDistribuidor = element.getChild("cod_pedido").getValue();
						String numero = element.getChild("cod_pedido_edi").getValue();

						Pedido pedido = new PedidoDao().getPedido(Integer.parseInt(numero));
						if (pedido != null && pedido.numero > 0) {

							// GRAVA O ARQUIVO RECEBIDO
							PedidoIntegracao pedInt = new PedidoIntegracao();
							pedInt.pedido = pedido.numero;
							pedInt.dataRecebimento = new Date();
							pedInt.tipoArquivo = PedidoIntegracao.RETORNO_PEDIDO;
							pedInt.nomeArquivo = f.getName();
							new PedidoIntegracaoDao().salvar(pedInt);

							pedido.numeroDistribuidor = Integer.parseInt(numeroDistribuidor);
							pedido.status = Pedido.RECEBIDO_FATURAMENTO;

							new PedidoDao().Salvar(pedido);
							new PedidoDao().atualizaStatus(pedido);
							ftp.deleteFile(f.getName());
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("Error in downloading file !");
			}
			fos.flush();
			fos.close();
		}
		logger.error("EXECUTADO");
	}

	@Override
	public void recebeTitulos() throws Exception {
	}

	@Override
	public void recebeClientes() throws Exception {
	}

	@Override
	public void enviaPedidos() throws Exception {

		// // VERIFICA OS PEDIDOS QUE ESTÃO COMO 'EXPORTADO FATURAMENTO' A MAIS
		// DE
		// // UMA HORA
		// Calendar data = Calendar.getInstance();
		// data.add(Calendar.HOUR_OF_DAY, -1);
		//
		// Conexao con = Conector.getConexaoVendas();
		// con.executar("UPDATE VEN_PEDIDO SET STATUS = "
		// + Oracle.strInsert(Pedido.PENDENTE_EXPORTACAO)
		// + " where canal = " + canal + " and status = "
		// + Oracle.strInsert(Pedido.EXPORTADO_FATURAMENTO)
		// + " and dataAtualizacao <= " + Oracle.strInsert(data.getTime()));

		// CARREGA TODOS OS PEDIDO QUE ESTÃO LIBERADOS PARA EXPORTAÇÃO

		// Conector.getConexaoVK()
		// .executar(
		// "UPDATE VEN_PEDIDO P SET STATUS = 'PENDENTE EXPORTAÇÃO' WHERE STATUS
		// = 'BLOQUEIO COMERCIAL' AND CANAL = "
		// + canal);

		PedidoDao pedDao = new PedidoDao();
		List<Pedido> pedidos = pedDao.getPedidosPendenteExportacao(canal);

		for (Pedido ped : pedidos) {
			try {
				enviaPedido(ped);

				ped.status = Pedido.EXPORTADO_FATURAMENTO;
				new PedidoDao().atualizaStatus(ped);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		logger.error("EXECUTADO");
		// salvaRegistroLogExecucao(canal);
	}

	private void enviaPedido(Pedido pedido) throws Exception {

		String pasta = pastaPedido;

		com.integracao.orgafarma.PedidoOrgafarma pedOrgafarma = com.integracao.orgafarma.PedidoOrgafarma
				.convert(pedido);
		String arquivo = pedOrgafarma.getXMLFile().getAbsolutePath();

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

		// ajusta o tipo do arquivo a ser enviado
		if (arquivo.endsWith(".txt")) {
			System.out.println("ENVIANDO ARQUIVO TEXTO");
			ftp.setFileType(FTPClient.ASCII_FILE_TYPE);
		} else if (arquivo.endsWith(".jpg")) {
			ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
		} else {
			ftp.setFileType(FTPClient.ASCII_FILE_TYPE);
		}
		System.out.println("Pasta " + pasta);
		System.out.println("Enviando arquivo " + nomeArquivo + "...");

		// faz o envio do arquivo
		// ftp.mkd(pasta);
		ftp.changeWorkingDirectory(pasta);
		String dirs[] = pasta.split("/");
		for (String dir : dirs) {
			System.out.println("mkdir: " + dir);
			// ftp.mkd(dir);
			ftp.changeWorkingDirectory(dir);
		}

		ftp.storeFile(nomeArquivo, is);
		is.close();
		System.out.println("Arquivo " + nomeArquivo + " enviado com sucesso!");
		logger.error("EXECUTADO");

		// GRAVA O ARQUIVO RECEBIDO
		PedidoIntegracao pedInt = new PedidoIntegracao();
		pedInt.pedido = pedido.numero;
		pedInt.dataRecebimento = new Date();
		pedInt.tipoArquivo = PedidoIntegracao.ENVIO_PEDIDO;
		pedInt.nomeArquivo = nomeArquivo;
		new PedidoIntegracaoDao().salvar(pedInt);

	}

	public void processaNFE(String arq) throws Exception {

		TNfeProc nfe = lerXML_JAXB.getNFe(arq);
		if (nfe != null) {

			// BUSCA O PEDIDO ORIGEM - SISTEMA VENDAS
			Det it0 = nfe.getNFe().getInfNFe().getDet().get(0);
			int numeroPedido = 0;
			try {
				numeroPedido = Integer.parseInt(it0.getProd().getXPed());
			} catch (Exception e) {
				throw new Exception("NÚMERO DO PEDIDO NÃO ENCONTRADO!", e);
				// NÃO É UM NÚMERO VÁLIDO
			}

			Pedido pedido = new Pedido();
			pedido.canal = canal;
			pedido.numero = numeroPedido;
			pedido.numeroNota = Integer.parseInt(nfe.getNFe().getInfNFe().getIde().getNNF());
			pedido.cnpj = nfe.getNFe().getInfNFe().getDest().getCNPJ();
			pedido.clienteRazao = nfe.getNFe().getInfNFe().getDest().getXNome();
			pedido.clienteUF = nfe.getNFe().getInfNFe().getDest().getEnderDest().getUF().name();
			try {
				pedido.data = new SimpleDateFormat("yyyy-MM-dd").parse(nfe.getNFe().getInfNFe().getIde().getDhEmi());
				pedido.dataFaturamento = new SimpleDateFormat("yyyy-MM-dd")
						.parse(nfe.getNFe().getInfNFe().getIde().getDhEmi());
			} catch (Exception e) {
				throw new Exception("DATA DA NOTA NÃO LOCALIZADA!", e);
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
				CanalProduto canProd = new CanalProdutoDao().getCanalProdutoExporta(canal, nIt.getProd().getCProd());
				it.qntFaturada = (int) Double.parseDouble(nIt.getProd().getQCom());
				it.valorFaturado = Double.parseDouble(nIt.getProd().getVProd());
				// it.qntVenda = it.qntFaturada;
				// it.valorUnitario = it.valorFaturado / it.qntVenda;
				if (canProd != null) {
					it.produto = canProd.produto;
					it.valorBruto = canProd.valorBruto;
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

			// GRAVA O ARQUIVO RECEBIDO
			PedidoIntegracao pedInt = new PedidoIntegracao();
			pedInt.pedido = pedido.numero;
			pedInt.dataRecebimento = new Date();
			pedInt.tipoArquivo = PedidoIntegracao.RETORNO_NOTA;
			pedInt.nomeArquivo = new File(arq).getName();
			new PedidoIntegracaoDao().salvar(pedInt);

			new PedidoDao().Salvar(pedido);
			new PedidoDao().atualizaStatus(pedido);
		}
		logger.error("EXECUTADO");
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
