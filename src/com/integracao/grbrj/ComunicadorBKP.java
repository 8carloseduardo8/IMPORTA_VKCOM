package com.integracao.grbrj;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import vendas.dao.CanalProdutoDao;
import vendas.dao.CanalSetorDao;
import vendas.dao.ClienteDao;
import vendas.dao.PedidoDao;
import vendas.dao.PrazoDao;
import vendas.dao.SetorClienteDao;

import com.integrador.Integrador;

import br.com.smp.vk.venda.model.CanalProduto;
import br.com.smp.vk.venda.model.CanalSetor;
import br.com.smp.vk.venda.model.Cliente;
import br.com.smp.vk.venda.model.Pedido;
import br.com.smp.vk.venda.model.PedidoItem;
import br.com.smp.vk.venda.model.Prazo;
import br.com.smp.vk.venda.model.SetorCliente;
import conect.Conector;
import conect.Conexao;
import conect.Oracle;
import conect.Resultado;
import conect.SQLServer;

import org.apache.log4j.Logger;

public class ComunicadorBKP extends Integrador {

	int canal = 3;

	public static void main(String[] args) {
		try {
			// new Comunicador().recebeClientes();
			// new Comunicador().enviaPedidos();
			new Comunicador().recebePedidos();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// CANAIS 3 E 10
	private final String empresa = "0023";
	private final String filial = "0002";
	Logger logger = Logger.getLogger(Comunicador.class);

	private void enviaPedido(Pedido pedido) throws Exception {

		Conexao con = Conector.getConexaoGRBRJ();

		List<String> sqlsExec = new ArrayList<String>();

		System.out.println("ENVIANDO PEDIDO PARA FATURAMENTO");

		if (pedido.numeroDistribuidor > 0) {
			logger.error("PEDIDO J� FOI EXPORTADO! n�m ped: " + pedido.numero + "; n�m distrib: "
					+ pedido.numeroDistribuidor);
		}

		int idPedido = 0;
		String sql = "select unisys.pedidovenda.nextval numero from dual";
		Resultado res = con.consultar(sql);
		if (res.next())
			idPedido = res.getInt("numero");
		res.close();

		pedido.numeroDistribuidor = idPedido;

		if (idPedido == 0)
			throw new Exception("ERRO AO CARREGAR N�MERO DO PEDIDO");

		// CARREGA OS C�DIGOS EXPORTA
		String vendedor = "";
		int negociacao = 2011;
		String cPrazo = "";
		String dPrazo = "";
		String cCliente = "";

		try {
			CanalSetor canSet = new CanalSetorDao().getCanalSetor(canal, pedido.getSetorVendedor());
			Prazo prazo = new PrazoDao().getPrazo(pedido.prazo);

			vendedor = canSet.setorExporta;
			cPrazo = prazo.codigoExporta;
			// CARREGA A DESCRI��O DO PRAZO
			sql = "select CD_PRAZO from unisys.tcadcdp0001 ";
			sql += "WHERE DDELETE IS NULL ";
			sql += "AND   CD_CODIGO = " + Oracle.strInsert(cPrazo);
			res = con.consultar(sql);
			if (res.next())
				dPrazo = res.getString("CD_PRAZO");
			res.close();

			sql = "select * from unisys.tcadcli0001 where cl_cnpj = " + Oracle.strInsert(pedido.cnpj);
			res = con.consultar(sql);
			if (res.next())
				cCliente = res.getString("CL_CODIGO");
			res.close();

			sql = "insert into pedido (empresa, filial, numero ) values (";
			sql += Oracle.strInsert(empresa) + ", " + Oracle.strInsert(filial) + ", " + Oracle.strInsert(idPedido)
					+ ")";
			sqlsExec.add(sql);

			sql = "update pedido set";
			sql += " vendedor = " + Oracle.strInsert(vendedor);
			sql += ", cliente = " + Oracle.strInsert(cCliente);
			sql += ", neg = " + Oracle.strInsert(negociacao);
			sql += ", condicao = " + Oracle.strInsert(cPrazo);
			sql += ", prazo = " + Oracle.strInsert(dPrazo);
			sql += ", observacao = " + Oracle.strInsert(pedido.obs);
			sql += ", data = " + Oracle.strInsert(pedido.data);
			sql += ", tipo_origem = " + Oracle.strInsert(1);
			sql += ", entrega = " + Oracle.strInsert(new Date());
			sql += " where " + "empresa = " + Oracle.strInsert(empresa);
			sql += " and filial = " + Oracle.strInsert(filial);
			sql += " and numero = " + idPedido;
			sqlsExec.add(sql);

			int sequencia = 0;
			for (PedidoItem it : pedido.itens) {
				sequencia += 1;
				sql = "insert into item (empresa,filial,pedido,sequencia,produto) values (";
				sql += Oracle.strInsert(empresa) + ",";
				sql += Oracle.strInsert(filial) + ",";
				sql += Oracle.strInsert(idPedido) + ",";
				sql += Oracle.strInsert(sequencia) + ",";
				sql += Oracle.strInsert(it.produto);
				sql += ")";
				sqlsExec.add(sql);

				sql = "update item set";
				sql += " bonificacao = 0";
				sql += ", quantidade = " + Oracle.strInsert(it.qntVenda + it.qntBonificacao);
				sql += ", preco = " + it.valorBruto;
				sql += ", precoFinal = " + (it.qntVenda * it.valorUnitario) / (it.qntVenda + it.qntBonificacao);
				sql += ", desconto_1 = " + Oracle.strInsert((double) ((it.valorBruto
						- ((it.qntVenda * it.valorUnitario) / (it.qntVenda + it.qntBonificacao))) / it.valorBruto)
						* 100);
				sql += ", desconto_2 = 0";
				sql += ", desconto_3 = 0";
				// sql += ", comissao = " + Oracle.strInsert(0);
				sql += ", situacao = 1";
				sql += ", atualizado = " + Oracle.strInsert(new Date());
				sql += ", bonus = null";
				sql += ", campanha = null";
				sql += ", comprimento = null";
				sql += ", largura = null";
				sql += ", altura = null";
				sql += " where empresa = " + empresa;
				sql += " and   filial  = " + filial;
				sql += " and   pedido  = " + idPedido;
				sql += " and   produto  = " + it.produto;
				sqlsExec.add(sql);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("ERRO AO CARREGAR INFORMA��ES ADICIONAIS, " + e.getMessage());
		}

		try {
			con.executar(sqlsExec);
		} catch (Exception e) {
			throw new Exception(e);
		}

		System.out.println("PEDIDO ENVIADO COM SUCESSO");

		// ATUALIZA STATUS DO PEDIDO PARA EXPORTADO
		pedido.status = Pedido.EXPORTADO_FATURAMENTO;
		// new PedidoDao().Salvar(pedido);
		new PedidoDao().atualizaNumeroDistribuidor(pedido);
		new PedidoDao().atualizaStatus(pedido);
		logger.error("EXECUTADO");
	}

	@Override
	public void recebePedidos() throws Exception {

		// if (true)
		// return;

		Conexao con = Conector.getConexaoGRBRJ();

		int qntPedidos = 0;

		// CARREGA A HORA DA ULTIMA SINCRONIZA��O
		Conexao conVendas = Conector.getConexaoVK();
		String sql = "SELECT * FROM VEN_CANALINTEGRACAO WHERE CANAL = " + canal;
		Resultado rs = conVendas.consultar(sql);
		Date data = new Date();
		if (rs.next())
			data = rs.getDate("ultimoacesso");
		rs.close();

		Calendar c = Calendar.getInstance();
		c.setTime(data);

		// SE FOR MEIA NOITE O SISTEMA BUSCA OS �LTIMOS PEDIDOS DA SEMANA
		if (c.get(Calendar.HOUR_OF_DAY) == 0)
			c.add(Calendar.DAY_OF_YEAR, -7);
		else
			c.add(Calendar.HOUR_OF_DAY, -1);

		data = c.getTime();

		salvaRegistroLogExecucao(canal);

		sql = "select p.ph_pedorigem, p.ph_origem, c.cl_cnpj, v.* from pedido_a v ";
		sql += " left join unisys.tpdhven0001 p on v.numero = p.ph_numero ";
		sql += " left join unisys.tcadcli0001 c on v.cliente = c.cl_codigo ";
		// sql += " where p.timeupd >= " + Oracle.strInsert(data);
		// sql += " where p.timeupd >= '01/07/2015'";
		sql += " order by v.data desc";
		rs = con.consultar(sql);
		try {
			while (rs.next()) {
				Pedido ped = new Pedido();
				ped.status = rs.getString("situacao");
				ped.numeroDistribuidor = rs.getInt("numero");

				ped.data = rs.getDate("data");
				ped.dataFaturamento = rs.getDate("data");
				ped.cnpj = rs.getString("cl_cnpj");

				CanalSetor canalSetor = new CanalSetorDao().getSetorExporta(rs.getString("vendedor"), canal);
				if (canalSetor != null)
					ped.setorVendedor = canalSetor.setor;
				else
					ped.setorVendedor = rs.getString("vendedor");
				ped.canal = canal;
				ped.condicao = 0;
				ped.forma = 0;
				ped.tipoPedido = "VENDA";
				ped.prazo = 0;
				ped.status = rs.getString("situacao");
				ped.origem = rs.getString("ph_origem");

				ped.valorBruto = 0;
				ped.valorLiquido = 0;
				ped.valorFaturado = 0;
				ped.coicidencia = 0;

				// CARREGANDO OS ITENS DO PEDIDO
				sql = "select * from gerente_item i where i.PEDIDO = " + ped.numeroDistribuidor;
				Resultado rsIt = con.consultar(sql);

				ArrayList<PedidoItem> lista = new ArrayList<PedidoItem>();

				while (rsIt.next()) {

					CanalProduto canProd = new CanalProdutoDao().getCanalProduto(canal, rsIt.getString("produto"));

					PedidoItem it = new PedidoItem();
					it.pedido = ped.numero;
					it.produto = rsIt.getString("produto");

					it.qntVenda = rsIt.getInt("qtd_solicitado");
					it.valorUnitario = (rsIt.getDouble("valor_pedido") / it.qntVenda);
					if (canProd != null)
						it.valorBruto = canProd.valorBruto;
					else
						it.valorBruto = it.valorUnitario;

					it.valorFaturado = rsIt.getDouble("valor_faturado");
					it.qntFaturada = rsIt.getInt("qtd_liberado");
					// ////////////////////////////////////////////////////////////////
					ped.valorBruto += it.valorBruto;
					ped.valorFaturado += it.valorFaturado;
					ped.valorLiquido += rsIt.getDouble("valor_pedido") * it.qntVenda;

					lista.add(it);
				}
				rsIt.close();

				ped.itens = lista;

				PedidoDao pedDao = new PedidoDao();
				pedDao.Salvar(ped);

				// SE O STATUS DO PEDIDO FOR DIFERENTE DO ATUAL ENT�O ATUALIZA O
				// STATUS
				Pedido pedAux = pedDao.getPedido(ped.numero);
				if (pedAux.status.equals(ped.status) == false) {
					pedDao.atualizaStatus(ped);
				}

				qntPedidos++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		rs.close();

		System.out.println("RECEBIDOS: (" + qntPedidos + ") PEDIDOS");
		logger.error("EXECUTADO");
	}

	@Override
	public void recebeTitulos() throws Exception {

		Conexao con2 = Conector.getConexaoVK();

		con2.executar("delete from VEN_TITULO WHERE CANAL = " + canal);

		String sql = "";
		sql += " insert into ven_titulo (cliente,numero,parcela,vendedor,tipo,vencimento,valor,status,canal) ";
		sql += " SELECT MAX(C.CL_CNPJ) CNPJ, T.NUMERO, T.PARCELA, T.VENDEDOR, MAX(T.tipo), MAX(T.VENCIMENTO), SUM(T.VALOR) VALOR, T.STATUS, "
				+ canal;
		sql += " FROM UNISYS.TCADCLI0001@DBL_RJ C, ";
		sql += " (SELECT * FROM TITULO_A@DBL_RJ ";
		sql += " UNION ALL ";
		sql += " SELECT * FROM TITULO_B@DBL_RJ ";
		sql += " UNION ALL ";
		sql += " SELECT * FROM TITULO_C@DBL_RJ ";
		sql += " UNION ALL ";
		sql += " SELECT * FROM TITULO_D@DBL_RJ ";
		sql += " ) T ";
		sql += " WHERE T.CLIENTE = C.CL_CODIGO ";
		sql += " AND   T.STATUS  <> 'B' ";
		sql += " GROUP BY T.NUMERO, T.PARCELA, T.VENDEDOR, T.STATUS ";
		con2.executar(sql);

		sql = "";
		sql += " UPDATE VEN_TITULO T SET VENDEDOR = (SELECT S.SETOR FROM VEN_CANALSETOR S  ";
		sql += " WHERE S.CANAL = " + canal + " AND S.SETOREXPORTA = T.VENDEDOR AND NOT S.SETOR IS NULL ";
		sql += " ) ";
		sql += " WHERE T.VENDEDOR in ( ";
		sql += " SELECT SETOREXPORTA FROM VEN_CANALSETOR F WHERE F.CANAL = " + canal;
		sql += " ) ";
		con2.executar(sql);
		logger.error("EXECUTADO");
	}

	@Override
	public void recebeClientes() throws Exception {
		Conexao con = Conector.getConexaoGRBRJ();

		// TODO Auto-generated method stub
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, -1);
		Date data = c.getTime();

		ClienteDao cliDao = new ClienteDao();
		SetorClienteDao setCliDao = new SetorClienteDao();

		String sql = "SELECT * FROM CLIENTE_A ";
		sql += " WHERE ESTADO IN ('RJ','ES') ";
		// sql += " AND VENDEDOR in ('2300039') ";
		sql += " AND ATUALIZADO >= '" + new SimpleDateFormat("dd/MM/yyyy").format(data) + "'";

		Resultado res = con.consultar(sql);
		while (res.next()) {
			try {
				Cliente cli = new Cliente();
				SetorCliente setCli = new SetorCliente();

				cli.cnpj = res.getString("documento");
				cli.razao = res.getString("razao").replace("'", "");

				if (res.getString("fantasia") != null)
					cli.fantasia = res.getString("fantasia").replace("'", "");
				else
					cli.fantasia = "";

				if (res.getString("endereco") != null)
					cli.endereco = res.getString("endereco").replace("'", "");
				if (res.getString("bairro") != null)
					cli.bairro = res.getString("bairro").replace("'", "");
				if (res.getString("cep") != null)
					cli.cep = res.getString("cep");
				if (res.getString("cidade") != null)
					cli.cidade = res.getString("cidade").replace("'", "");
				if (res.getString("estado") != null)
					cli.uf = res.getString("estado");
				if (res.getString("telefone") != null)
					cli.telefone1 = res.getString("telefone");

				cliDao.salvar(cli);

				if (res.getString("vendedor") != null) {
					CanalSetor canalSetor = new CanalSetorDao().getSetorExporta(res.getString("vendedor"), canal);
					if (canalSetor != null)
						setCli.setor = canalSetor.setor;
					setCli.cliente = cli.cnpj;
					if (setCli.setor != null && setCli.cliente != null) {
						System.out.println(">>>>>>>> VINCULANDO CLIENTE AO VENDEDOR");
						setCliDao.salvar(setCli);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		res.close();

		logger.error("EXECUTADO");
	}

	@Override
	public void enviaPedidos() throws Exception {

		// Conector.getConexaoVK()
		// .executar(
		// "UPDATE VEN_PEDIDO P SET STATUS = 'PENDENTE EXPORTA��O' WHERE STATUS
		// = 'BLOQUEIO COMERCIAL' AND CANAL = "
		// + canal);

		// CARREGA TODOS OS PEDIDO QUE EST�O LIBERADOS PARA EXPORTA��O
		PedidoDao pedDao = new PedidoDao();
		List<Pedido> pedidos = pedDao.getPedidosPendenteExportacao(canal);

		for (Pedido ped : pedidos) {
			try {
				enviaPedido(ped);
			} catch (Exception e) {
			}
		}
		logger.error("EXECUTADO");
		salvaRegistroLogExecucao(canal);
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
