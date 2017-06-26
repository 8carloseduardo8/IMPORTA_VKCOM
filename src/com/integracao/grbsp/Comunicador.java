package com.integracao.grbsp;

import java.util.Calendar;
import java.text.SimpleDateFormat;
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
import com.integrador.ServletIntegradorNormal;
import com.integrador.ServletIntegradorNormal.Finaliza;

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

public class Comunicador extends Integrador {

	private final int canal = 4;
	private final String empresa = "0023";
	private final String filial = "0003";
	Logger logger = Logger.getLogger(Comunicador.class);

	Conexao con;

	public Comunicador(Finaliza f, ServletIntegradorNormal integrador) {
		super(f, integrador, "GRB-SP");
		con = Conector.getConexaoGRBSP();
	}

	public Comunicador() {
		con = Conector.getConexaoGRBSP();
	}

	public static void main(String[] args) {
		try {
			// new Comunicador().start();
			 new Comunicador().recebeClientes();
			// new Comunicador().recebeTitulos();
			// new Comunicador().enviaPedidos();
			// new Comunicador().recebePedidos();
//			new Comunicador().recebeEstoque();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void enviaPedido(Pedido pedido) throws Exception {

		List<String> sqlsExec = new ArrayList<String>();

		System.out.println("ENVIANDO PEDIDO PARA FATURAMENTO");

		if (pedido.numeroDistribuidor > 0)
			throw new Exception("PEDIDO JÁ FOI EXPORTADO! núm ped: " + pedido.numero + "; núm distrib: "
					+ pedido.numeroDistribuidor);

		int idPedido = 0;
		String sql = "select unisys.pedidovenda.nextval numero from dual";
		Resultado res = con.consultar(sql);
		if (res.next())
			idPedido = res.getInt("numero");
		res.close();

		pedido.numeroDistribuidor = idPedido;

		if (idPedido == 0)
			throw new Exception("ERRO AO CARREGAR NÚMERO DO PEDIDO");

		// CARREGA OS CÓDIGOS EXPORTA
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
			// CARREGA A DESCRIÇÃO DO PRAZO
			sql = "select CD_PRAZO from unisys.tcadcdp0001 ";
			sql += "WHERE DDELETE IS NULL ";
			sql += "AND   CD_CODIGO = " + Oracle.strInsert(cPrazo);
			res = con.consultar(sql);
			if (res.next())
				dPrazo = res.getString("CD_PRAZO");
			res.close();

			sql = "select * from unisys.tcadcli0001 where ddelete is null and cl_cnpj = "
					+ Oracle.strInsert(pedido.cnpj);
			res = con.consultar(sql);
			if (res.next())
				cCliente = res.getString("CL_CODIGO");
			res.close();

			sql = "insert into pedido (empresa, filial, numero, PEDIDO_ORIGEM ) values (";
			sql += Oracle.strInsert(empresa) + ", " + Oracle.strInsert(filial) + "," + Oracle.strInsert(idPedido) + ","
					+ Oracle.strInsert(pedido.numero) + ")";
			sqlsExec.add(sql);

			sql = "update pedido set";
			sql += " vendedor = " + Oracle.strInsert(vendedor);
			sql += ", cliente = " + Oracle.strInsert(cCliente);
			sql += ", neg = " + Oracle.strInsert(negociacao);
			sql += ", condicao = " + Oracle.strInsert(cPrazo);
			sql += ", prazo = " + Oracle.strInsert(dPrazo);
			sql += ", observacao = SUBSTR(" + Oracle.strInsert(((pedido.motivoData != null
					? "LIBERADO POR: " + pedido.motivoUsuario + "  / MOTIVO: " + pedido.motivo + " / OBS: " : "")
					+ pedido.obs)) + ", 1,1000)";
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

				if (it.qntVenda == 0)
					sql += ", precoFinal = " + it.valorBruto;
				else
					sql += ", precoFinal = " + (it.qntVenda * it.valorUnitario) / (it.qntVenda + it.qntBonificacao);
				if (it.qntVenda == 0)
					sql += ", desconto_1 = 0 ";
				else
					sql += ", desconto_1 = " + Oracle.strInsert((double) ((it.valorBruto
							- ((it.qntVenda * it.valorUnitario) / (it.qntVenda + it.qntBonificacao))) / it.valorBruto)
							* 100);
				sql += ", desconto_2 = 0";
				sql += ", desconto_3 = 0";
				sql += ", comissao = " + Oracle.strInsert(0);
				sql += ", situacao = 1";
				sql += ", atualizado = " + Oracle.strInsert(new Date());
				sql += ", bonus = null";
				sql += ", campanha = null";
				sql += ", comprimento = null";
				sql += ", largura = null";
				sql += ", altura = null";
				if (it.qntVenda == 0)
					sql += ", tpvenda = '301'";
				else
					sql += ", tpvenda = '101'";
				sql += " where empresa = " + empresa;
				sql += " and   filial  = " + filial;
				sql += " and   pedido  = " + idPedido;
				sql += " and   produto  = " + it.produto;
				sqlsExec.add(sql);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("ERRO AO CARREGAR INFORMAÇÕES ADICIONAIS, " + e.getMessage());
		}

		try {
			con.executar(sqlsExec);
		} catch (Exception e) {
			throw new Exception(e);
		}

		// DEPOIS DE ENVIADO VERIFICA A QUANTIDADE DE ITENS GRAVADOS NA
		// DISTRIBUIDORA COM A QUANTIDADE DE ITENS DO PEDIDO
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

		int qntPedidos = 0;

		// CARREGA A HORA DA ULTIMA SINCRONIZAÇÃO
		Conexao conVendas = Conector.getConexaoVK();
		String sql = "SELECT * FROM VEN_CANALINTEGRACAO WHERE CANAL = " + canal;
		Resultado rs = conVendas.consultar(sql);
		Date data = new Date();
		if (rs.next())
			data = rs.getDate("ultimoacesso");
		rs.close();

		Calendar c = Calendar.getInstance();
		c.setTime(data);

		// SE FOR MEIA NOITE O SISTEMA BUSCA OS ÚLTIMOS PEDIDOS DA SEMANA
		if (c.get(Calendar.HOUR_OF_DAY) == 0)
			c.add(Calendar.DAY_OF_YEAR, -31);
		else
			c.add(Calendar.MINUTE, -30);

		data = c.getTime();

		salvaRegistroLogExecucao(canal);

		sql = "select p.ph_pedorigem, p.ph_origem,  n.nh_emissao ph_datafat, p.ph_numnota, c.cl_cnpj, v.* from pedido_a v ";
		sql += " left join unisys.tpdhven0001 p on v.numero = p.ph_numero ";
		sql += " left join unisys.tcadcli0001 c on v.cliente = c.cl_codigo ";
		sql += " left join unisys.tnfhven0001 n on v.numero = n.nh_pedido ";
		sql += " where p.timeupd >= " + Oracle.strInsert(data);
		sql += " and not situacao is null ";
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
					try {
						it.qntFaturada = rsIt.getInt("qtd_liberado");
						if (rsIt.getInt("QTD_FATURADO") > 0)
							it.qntFaturada = rsIt.getInt("QTD_FATURADO");
					} catch (Exception e) {
						e.printStackTrace();
					}
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

				// SE O STATUS DO PEDIDO FOR DIFERENTE DO ATUAL ENTÃO ATUALIZA O
				// STATUS
				Pedido pedAux = pedDao.getPedido(ped.numero);
				if (pedAux.status.equals(ped.status) == false) {
					if (ped.status.equals(Pedido.FATURADO) && ped.valorFaturado == 0) {
						// NÃO FAZ NADA ** O PEDIDO FOI FATURADO MAS AINDA NÃO
						// FOI EMITIDO A NOTA FISCAO
					} else {
						pedDao.atualizaStatus(ped);
					}
				}
				qntPedidos++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		rs.close();

		// ATUALIZA OS PEDIDOS EXCLUÍDOS
		sql = "update ven_pedido P set status = 'REJEITADO' ";
		sql += " WHERE  CANAL = " + canal;
		sql += " AND    NUMERODISTRIBUIDOR IN ( ";
		sql += "    select NUMERO from geral.pedido@dbl_sp ";
		sql += "    where situacao = 6 ";
		sql += "    and   data >= " + Oracle.strInsert("01/01/2015");
		sql += " )";
		sql += " AND STATUS = 'EXPORTADO FATURAMENTO' ";
		conVendas.executar(sql);

		sql = "update ven_pedido p set status = 'CANCELADO' ";
		sql += " where canal = " + canal;
		sql += " and numeronota in ( ";
		sql += "     select nh_numero from unisys.tnfhven0001@dbl_sp ";
		sql += "     where  nh_cancelado =  'S' ";
		sql += "     and    nh_emissao   >= '01/01/2015'";
		sql += "     )";
		conVendas.executar(sql);

		System.out.println("RECEBIDOS: (" + qntPedidos + ") PEDIDOS");
		logger.error("EXECUTADO");
	}

	@Override
	public void recebeTitulos() throws Exception {

		// // SÓ EXECUTA QUANDO FOR MEIA NOITE
		// Calendar c = Calendar.getInstance();
		// if (c.get(Calendar.HOUR_OF_DAY) != 0)
		// return;

		Conexao con2 = Conector.getConexaoVK();

		// DELETE TODA A BASE DE CLIENTES E CARRAGA NOVAMENTE
		List<String> sqlClientes = new ArrayList<String>();
		sqlClientes.add("delete from ven_setorcliente where setor in (select setor from ven_canalsetor where canal = "
				+ canal + ")");
		sqlClientes.add("insert into ven_setorcliente " + " select distinct cs.setor, c.documento "
				+ " from cliente_a@dbl_SP c "
				+ " left join ven_canalsetor cs on c.vendedor = cs.setorexporta and canal = " + canal
				+ " where not cs.setor is null " + " and not c.documento in ( "
				+ " select f.cliente from ven_setorcliente f where f.setor = cs.setor and f.cliente = c.documento "
				+ ")");
		con2.executar(sqlClientes);

		con2.executar("delete from VEN_TITULO WHERE CANAL = " + canal);

		String sql = "";
		sql += " insert into ven_titulo (cliente,numero,parcela,vendedor,tipo,vencimento,valor,status,canal) ";
		sql += " SELECT MAX(C.CL_CNPJ) CNPJ, T.NUMERO, T.PARCELA, T.VENDEDOR, MAX(T.tipo), MAX(T.VENCIMENTO), SUM(T.VALOR) VALOR, T.STATUS, "
				+ canal;
		sql += " FROM UNISYS.TCADCLI0001@DBL_SP C, ";
		sql += " (SELECT * FROM TITULO_A@DBL_SP ";
		sql += " UNION ALL ";
		sql += " SELECT * FROM TITULO_B@DBL_SP ";
		sql += " UNION ALL ";
		sql += " SELECT * FROM TITULO_C@DBL_SP ";
		sql += " UNION ALL ";
		sql += " SELECT * FROM TITULO_D@DBL_SP ";
		sql += " ) T ";
		sql += " WHERE T.CLIENTE = C.CL_CODIGO ";
		sql += " AND   T.STATUS  <> 'B' ";
		sql += " GROUP BY T.NUMERO, T.PARCELA, T.VENDEDOR, T.STATUS ";
		con2.executar(sql);

		sql = "";
		sql += " UPDATE VEN_TITULO T SET VENDEDOR = (SELECT MAX(S.SETOR) FROM VEN_CANALSETOR S  ";
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

		// TODO Auto-generated method stub
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, -200);
		Date data = c.getTime();

		ClienteDao cliDao = new ClienteDao();
		SetorClienteDao setCliDao = new SetorClienteDao();

		String sql = "SELECT * FROM CLIENTE_A ";
		sql += " WHERE ESTADO = 'SP' ";
		// sql += " AND VENDEDOR = 24801 ";
		sql += " AND ATUALIZADO >= '" + new SimpleDateFormat("dd/MM/yyyy").format(data) + "'";

		System.out.println(sql);

		Resultado res = con.consultar(sql);
		while (res.next()) {
			try {
				Cliente cli = new Cliente();
				SetorCliente setCli = new SetorCliente();

				cli = new ClienteDao().getCliente(res.getString("documento"));
				if (cli == null)
					cli = new Cliente();

				cli.cnpj = res.getString("documento");
				cli.razao = res.getString("razao").replace("'", "");
				cli.fantasia = res.getString("fantasia").replace("'", "");
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
					if (setCli.setor != null && setCli.cliente != null)
						setCliDao.salvar(setCli);
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
		// "UPDATE VEN_PEDIDO P SET STATUS = 'PENDENTE EXPORTAÇÃO' WHERE STATUS
		// = 'BLOQUEIO COMERCIAL' AND CANAL = "
		// + canal);

		// CARREGA TODOS OS PEDIDO QUE ESTÃO LIBERADOS PARA EXPORTAÇÃO
		PedidoDao pedDao = new PedidoDao();
		List<Pedido> pedidos = pedDao.getPedidosPendenteExportacao(canal);

		for (Pedido ped : pedidos) {
			try {
				enviaPedido(ped);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		logger.error("EXECUTADO");
	}

	@Override
	public void recebeDevolucoes() throws Exception {

		Calendar c = Calendar.getInstance();
		// SE FOR MEIA NOITE O SISTEMA BUSCA OS ÚLTIMOS PEDIDOS DA SEMANA
		if (c.get(Calendar.HOUR_OF_DAY) == 0)
			c.add(Calendar.DAY_OF_YEAR, -60);
		else
			c.add(Calendar.DAY_OF_YEAR, -2);

		Conexao con2 = Conector.getConexaoVK();

		String sql = "";
		sql += " SELECT  NI_EMPRESA,NI_FILIAL,NI_NUMERO,NI_SERIE, CL_CNPJ, NI_EMISSAO,NI_VENDE,NI_PRODUTO,NI_QUANTIDA, NI_VUNTLIQ";
		sql += " FROM UNISYS.TNFHCOM0001@DBL_SP H, UNISYS.TNFICOM0001@DBL_SP I, UNISYS.TCADCLI0001@DBL_SP C  ";
		sql += " WHERE I.NI_FORNECE = C.CL_CODIGO";
		sql += " AND   NH_NUMERO = NI_NUMERO AND NH_SERIE = NI_SERIE AND NH_FORNECE = NI_FORNECE";
		sql += " AND   NH_TIPO = 'NDV'";
		sql += " AND   NH_CANCELADA = 'N'";
		sql += " AND   NI_EMISSAO >= " + Oracle.strInsert(c.getTime());

		Resultado res = con2.consultar(sql);

		int numeroDevolucao = 0;

		Pedido p = null;
		while (res.next()) {
			if (numeroDevolucao != res.getInt("NI_NUMERO")) {

				// GRAVA O PEDIDO NO BANCO
				if (p != null) {
					new PedidoDao().Salvar(p, true);
					p.status = "DEVOLUÇÃO";
					new PedidoDao().atualizaStatus(p);
				}

				p = new Pedido();
				p.canal = canal;
				p.numeroDistribuidor = res.getInt("NI_NUMERO");
				p.numeroNota = res.getInt("NI_NUMERO");
				p.data = res.getDate("NI_EMISSAO");
				p.dataFaturamento = res.getDate("NI_EMISSAO");
				p.dataCriacao = new Date();
				p.dataAtualizacao = new Date();
				p.dataRecebimento = new Date();
				p.dataEnvio = new Date();
				p.cnpj = res.getString("CL_CNPJ");
				CanalSetor canSet = new CanalSetorDao().getSetorExporta(res.getString("NI_VENDE"), canal);
				if (canSet != null) {
					p.setorVendedor = new CanalSetorDao().getSetorExporta(res.getString("NI_VENDE"), canal).getSetor();
				} else {
					p.setorVendedor = res.getString("NI_VENDE");
				}
				p.status = "FATURADO";
				p.itens = new ArrayList<>();

				numeroDevolucao = res.getInt("NI_NUMERO");
			}

			// ITENS DO PEDIDO
			PedidoItem pedidoItem = new PedidoItem();
			pedidoItem.produto = res.getString("NI_PRODUTO");
			pedidoItem.qntVenda = res.getInt("NI_QUANTIDA") * -1;
			pedidoItem.valorUnitario = res.getDouble("NI_VUNTLIQ");
			pedidoItem.qntFaturada = res.getInt("NI_QUANTIDA") * -1;
			pedidoItem.valorFaturado = res.getDouble("NI_VUNTLIQ") * res.getInt("NI_QUANTIDA") * -1;
			p.itens.add(pedidoItem);
		}

		return;
	}

	@Override
	public void recebeEstoque() throws Exception {
		String sql = "SELECT * FROM unisys.view_estoquevenda";

		Resultado res = con.consultar(sql);

		while (res.next()) {
			String produto = "0" + res.getString("produto").substring(1);

			CanalProduto canPro = new CanalProdutoDao().getCanalProduto(canal, produto);
			if (canPro != null) {
				canPro.estoque = res.getInt("estoquelib");
				canPro.estoqueData = new Date();
				new CanalProdutoDao().salvar(canPro);
			}
		}
		res.close();
	}
}
