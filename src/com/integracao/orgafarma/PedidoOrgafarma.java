package com.integracao.orgafarma;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

import com.util.Str;

import br.com.smp.vk.venda.model.CanalProduto;
import br.com.smp.vk.venda.model.CanalSetor;
import br.com.smp.vk.venda.model.Condicao;
import br.com.smp.vk.venda.model.PedidoItem;
import br.com.smp.vk.venda.model.Prazo;
import br.com.smp.vk.venda.model.Produto;
import vendas.dao.CanalProdutoDao;
import vendas.dao.CanalSetorDao;
import vendas.dao.CondicaoDao;
import vendas.dao.PrazoDao;
import vendas.dao.ProdutoDao;

public class PedidoOrgafarma {

	public static void main(String argv[]) {
		new PedidoOrgafarma().toString();
	}

	public Capa capa;
	public List<Item> itens;

	public PedidoOrgafarma() {
		capa = new Capa();
		itens = new ArrayList<Item>();
	}

	public class Capa {
		public String Numero;
		public String cobranca;
		public String codPedidoEdi;
		public String prazo;
		public String situacao;
		public String condicao;
		public String urgente;
		public String TC;
		public String percTC;
		public String tipoTC;
		public String ObsNF;
		public String ObsEntrega;
		public String CNPJCliente;
		public String CODCLIENTE;
		public String CNPJFornecedor;
		public String codFornecedor;
		public String codFilialFaturamento;
		public String CodVEndedor;
		public String Programado;
	}

	public static class Item {
		public String Codigo;
		public String Quantidade;
		public String Preco;
		public String PercDesconto;
		public String QuantidadeBonif;
		public String PercDescFin;
		public String LimiteDescFin;
		public String Prazo;
		public String EAN;
	}

	public static PedidoOrgafarma convert(br.com.smp.vk.venda.model.Pedido pedido) throws Exception {
		PedidoOrgafarma ped = new PedidoOrgafarma();

		Prazo prazo = new PrazoDao().getPrazo(pedido.prazo);
		Condicao condicao = new CondicaoDao().getCondicao(pedido.condicao);
		CanalSetor canSetor = new CanalSetorDao().getCanalSetor(pedido.canal, pedido.setorVendedor);

		ped.capa.Numero = pedido.numero + "";
		ped.capa.cobranca = "0";
		ped.capa.codPedidoEdi = pedido.numero + "";

		ped.capa.prazo = prazo.codigoExporta + "";
		ped.capa.situacao = "0";
		ped.capa.condicao = condicao.codigoExporta + "";
		ped.capa.urgente = "0";
		ped.capa.TC = "0";
		ped.capa.percTC = "0";
		ped.capa.tipoTC = "0";
		ped.capa.CNPJCliente = pedido.cnpj;
		ped.capa.CodVEndedor = canSetor.setorExporta;
		ped.capa.Programado = "01/01/1900";
		ped.capa.ObsEntrega = Str.removeAcento(pedido.getObs());

		for (PedidoItem i : pedido.itens) {
			PedidoOrgafarma.Item i2 = new PedidoOrgafarma.Item();

			CanalProduto canProd = new CanalProdutoDao().getCanalProduto(pedido.canal, i.produto);
			Produto prod = new ProdutoDao().getProduto(i.produto);

			i2.Codigo = canProd.produtoExporta;
			i2.Quantidade = (i.qntBonificacao + i.qntVenda) + "";
			i2.Preco = new DecimalFormat("0.00").format(i.valorBruto);
			i2.PercDesconto = new DecimalFormat("0.00")
					.format(((((i.qntBonificacao + i.qntVenda) * i.valorBruto) - (i.qntVenda * i.valorUnitario))
							/ ((i.qntBonificacao + i.qntVenda) * i.valorBruto)) * 100);
			i2.QuantidadeBonif = "0";
			i2.PercDescFin = "0";
			i2.LimiteDescFin = "01/01/1900";
			i2.Prazo = "0";
			i2.EAN = prod.ean;

			ped.itens.add(i2);
		}
		return ped;
	}

	public File getXMLFile() {

		Element XMLped = new Element("Pedido");
		Element XMLitens = new Element("Itens");

		XMLped.addContent(new Element("Numero").setText(capa.Numero));
		XMLped.addContent(new Element("cobranca").setText(capa.cobranca));
		XMLped.addContent(new Element("codPedidoEdi").setText(capa.codPedidoEdi));
		XMLped.addContent(new Element("prazo").setText(capa.prazo));
		XMLped.addContent(new Element("situacao").setText(capa.situacao));
		XMLped.addContent(new Element("condicao").setText(capa.condicao));
		XMLped.addContent(new Element("urgente").setText(capa.urgente));
		XMLped.addContent(new Element("TC").setText(capa.TC));
		XMLped.addContent(new Element("percentTC").setText(capa.percTC));
		XMLped.addContent(new Element("tipoTC").setText(capa.tipoTC));
		XMLped.addContent(new Element("ObsNF").setText(capa.ObsNF));
		XMLped.addContent(new Element("ObsEntrega").setText(capa.ObsEntrega));
		XMLped.addContent(new Element("CNPJCliente").setText(capa.CNPJCliente));
		XMLped.addContent(new Element("CNPJFornecedor").setText(capa.CNPJFornecedor));
		XMLped.addContent(new Element("codFornecedor").setText(capa.codFornecedor));
		XMLped.addContent(new Element("codFilialFaturamento").setText(capa.codFilialFaturamento));
		XMLped.addContent(new Element("CodVendedor").setText(capa.CodVEndedor));
		XMLped.addContent(new Element("Programado").setText(capa.Programado));

		for (Item it : itens) {
			Element XMLit = new Element("Item");

			XMLit.addContent(new Element("Codigo").setText(it.Codigo));
			XMLit.addContent(new Element("Quantidade").setText(it.Quantidade.replace(",", ".")));
			XMLit.addContent(new Element("Preco").setText(it.Preco.replace(",", ".")));
			XMLit.addContent(new Element("PercDesconto").setText(it.PercDesconto.replace(",", ".")));
			XMLit.addContent(new Element("QuantidadeBonif").setText(it.QuantidadeBonif.replace(",", ".")));
			XMLit.addContent(new Element("PercDescFin").setText(it.PercDescFin.replace(",", ".")));
			XMLit.addContent(new Element("LimiteDescFin").setText(it.LimiteDescFin.replace(",", ".")));
			XMLit.addContent(new Element("Prazo").setText(it.Prazo));
			XMLit.addContent(new Element("EAN").setText(it.EAN));
			XMLitens.addContent(XMLit);
		}

		Element XMLcapa = new Element("Pedidos");
		XMLped.addContent(XMLitens);
		XMLcapa.addContent(XMLped);

		try {
			new File("C:\\Atua\\ORGAFARMA\\").mkdirs();
		} catch (Exception e) {
			e.printStackTrace();
		}

		File file = new File("C:\\Atua\\ORGAFARMA\\" + capa.Numero + ".txt");

		XMLOutputter xout = new XMLOutputter();
		try {
			// Criando o arquivo de saida
			FileWriter arquivo = new FileWriter(file);
			// Imprimindo o XML no arquivo
			xout.output(new Document(XMLcapa), arquivo);
			arquivo.flush();
			arquivo.close();

			// // ABRINDO O ARQUIVO E REMOVENDO OS CARACTERES ESPECIAIS
			// BufferedReader br = new BufferedReader(new FileReader(file));
			// BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			//
			// String linha;
			// while ((linha = br.readLine()) != null) {
			// bw.write(Str.removeAcento(linha));
			// }
			// br.close();
			// bw.flush();
			// bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return file;
	}
}
