package com.integracao.cifarma;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.integrador.Integrador;
import com.integrador.ServletIntegradorNormal;
import com.integrador.ServletIntegradorNormal.Finaliza;
import com.util.Email;

import br.com.smp.vk.venda.model.CanalProduto;
import br.com.smp.vk.venda.model.CanalSetor;
import br.com.smp.vk.venda.model.Cliente;
import br.com.smp.vk.venda.model.Funcionario;
import br.com.smp.vk.venda.model.Pedido;
import br.com.smp.vk.venda.model.PedidoItem;
import br.com.smp.vk.venda.model.Prazo;
import br.com.smp.vk.venda.model.Produto;
import br.com.smp.vk.venda.model.Setor;
import br.com.smp.vk.venda.model.SetorCliente;
import conect.Conector;
import conect.Conexao;
import conect.Oracle;
import conect.Resultado;
import vendas.dao.CanalProdutoDao;
import vendas.dao.CanalSetorDao;
import vendas.dao.ClienteDao;
import vendas.dao.FuncionarioDao;
import vendas.dao.ParametrosDao;
import vendas.dao.PedidoDao;
import vendas.dao.PrazoDao;
import vendas.dao.ProdutoDao;
import vendas.dao.SetorClienteDao;
import vendas.dao.SetorDao;

public class Comunicador extends Integrador {

	private final int canal = 1;
	private final String empresa = "0002";
	private final String filial = "0003";

	Conexao con;

	public static void main(String argv[]) {
		try {
			// new Comunicador().recebeVendedores();

			// new Comunicador().enviaPedidos();
			// new Comunicador().recebeClientes();
			new Comunicador().recebePedidos();

			// new Comunicador().enviaManifesto();

			// new Comunicador().recebeTitulos();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Comunicador(Finaliza f, ServletIntegradorNormal integrador) {
		super(f, integrador, "CIFARMA-GO");
		con = Conector.getConexaoCifarmaGO();
	}

	public Comunicador() {
		con = Conector.getConexaoCifarmaGO();
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
				// GRAVA NA FILIAL O CODIGO PRODUTO EXPORTA

				CanalProduto canProd = new CanalProdutoDao().getCanalProduto(canal, it.produto);

				if (canProd == null) {
					throw new Exception("NÃO FOI POSSÍVEL CARREGAR O CÓDIGO DE EXPORTAÇAÕ DO PRODUTO!");
				}

				sequencia += 1;
				sql = "insert into item (empresa,filial,pedido,sequencia,produto) values (";
				sql += Oracle.strInsert(empresa) + ",";
				sql += Oracle.strInsert(filial) + ",";
				sql += Oracle.strInsert(idPedido) + ",";
				sql += Oracle.strInsert(sequencia) + ",";
				sql += Oracle.strInsert(canProd.produtoExporta);
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
				sql += ", comissao = " + Oracle.strInsert(0);
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
				sql += " and   produto  = " + canProd.produtoExporta;
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

		System.out.println("PEDIDO ENVIADO COM SUCESSO");

		// ATUALIZA STATUS DO PEDIDO PARA EXPORTADO
		pedido.status = Pedido.EXPORTADO_FATURAMENTO;
		// new PedidoDao().Salvar(pedido);
		new PedidoDao().atualizaNumeroDistribuidor(pedido);
		new PedidoDao().atualizaStatus(pedido);

	}

	@Override
	public void recebePedidos() throws Exception {

		try {
			recebeVendedores();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			enviaManifesto();
		} catch (Exception e) {
			e.printStackTrace();
		}

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
			c.add(Calendar.DAY_OF_YEAR, -10);
		else
			c.add(Calendar.MINUTE, -30);

		// c.add(Calendar.DAY_OF_YEAR, -100);

		data = c.getTime();

		sql = "select p.ph_pedorigem, p.ph_origem,  n.nh_emissao ph_datafat, p.ph_numnota, c.cl_cnpj, v.* from pedido_a v ";
		sql += " left join unisys.tpdhven0001 p on v.numero = p.ph_numero ";
		sql += " left join unisys.tcadcli0001 c on v.cliente = c.cl_codigo ";
		sql += " left join unisys.tnfhven0001 n on v.numero = n.nh_pedido ";
		sql += " where p.timeupd >= " + Oracle.strInsert(data);
		// sql += " and ph_numero = 187835 ";
		// sql += " and ph_pedorigem > 0 ";
		// sql += " and situacao = 'REJEITADO'";
		sql += " and not situacao is null ";
		sql += " order by v.data desc";
		rs = con.consultar(sql);

		salvaRegistroLogExecucao(canal);

		while (rs.next()) {
			try {
				Pedido ped = new Pedido();
				ped.status = rs.getString("situacao");

				if (rs.getString("situacao").equals("REJEITADO") && rs.getInt("ph_pedorigem") > 0) {
					ped.status = "TRANSFERIDO";
				}

				ped.numero = rs.getInt("pedido_origem");
				ped.numeroDistribuidor = rs.getInt("numero");

				ped.data = rs.getDate("data");
				ped.cnpj = rs.getString("cl_cnpj");

				// TENTA LOCALIZAR O NÚMERO DA NOTA E A DATA DA NOTA
				ped.dataFaturamento = rs.getDate("ph_datafat");
				ped.numeroNota = rs.getInt("ph_numnota");

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
				ped.origem = rs.getString("ph_origem");

				ped.valorBruto = 0;
				ped.valorLiquido = 0;
				ped.valorFaturado = 0;
				ped.coicidencia = 0;

				// CARREGA O MANIFESTO
				if (ped.numeroNota > 0) {
					sql = " select nh_numero, nh_manifesto, nh_datamanifesto, nh_prventrega,nh_qtdevol,";
					sql += " (select fo_razao from unisys.tcadfor0001 where fo_codigo = nh_transp) transportadora";
					sql += " from unisys.tnfhven0001 n where n.nh_numero = " + ped.numeroNota;
					Resultado rsNota = con.consultar(sql);
					if (rsNota.next()) {
						ped.manifesto = rsNota.getInt("nh_manifesto");
						ped.manifestoData = rsNota.getDate("nh_datamanifesto");
						ped.previsaoEntrega = rsNota.getDate("nh_prventrega");
						ped.transportadora = rsNota.getString("transportadora");
						ped.qntVolumes = rsNota.getInt("nh_qtdevol");
					}
					rsNota.close();
				}

				// CARREGANDO OS ITENS DO PEDIDO
				sql = "select * from gerente_item i where i.PEDIDO = " + ped.numeroDistribuidor;
				Resultado rsIt = con.consultar(sql);

				ArrayList<PedidoItem> lista = new ArrayList<PedidoItem>();

				while (rsIt.next()) {

					CanalProduto canProd = null;

					PedidoItem it = new PedidoItem();
					it.produto = rsIt.getString("produto");
					Produto produto = geProdutoSubString(rsIt.getString("produto"));
					if (produto != null) {
						it.produto = produto.codigo;
						it.produtoDescricao = produto.descricao;
					}

					// CARREGA O VALOR BRUTO E O CÓDIGO DO PRODUTO DA UNIDROGAS
					canProd = getCanalProdutoUnidrogas(it.produto);

					if (canProd == null)
						canProd = new CanalProdutoDao().getCanalProdutoExporta(canal, it.produto);
					if (canProd == null)
						canProd = new CanalProdutoDao().getCanalProduto(canal, it.produto);
					if (canProd == null)
						canProd = new CanalProdutoDao().getCanalProdutoExporta(canal, it.produto);

					it.pedido = ped.numero;

					it.qntVenda = rsIt.getInt("pi_quantida_2");
					it.valorUnitario = (rsIt.getDouble("valor_pedido") / it.qntVenda);
					try {
						it.qntFaturada = rsIt.getInt("qtd_liberado");
						if (rsIt.getInt("QTD_FATURADO") > 0)
							it.qntFaturada = rsIt.getInt("QTD_FATURADO");
					} catch (Exception e) {
						e.printStackTrace();
					}

					if (canProd != null) {
						it.valorBruto = canProd.valorBruto;
						it.produto = canProd.produto;
					} else {
						it.valorBruto = it.valorUnitario;
					}
					it.valorFaturado = rsIt.getDouble("valor_faturado");

					// VERIFICA SE A QUANTIDADE ESTÁ DIGITADA NA TABELA DE
					// FALTAS
					// CARREGA A QUANTIDADE NA TABELA TPDIFALTAS
					// CARREGANDO OS ITENS DO PEDIDO
					sql = "select * from unisys.tpdifaltas0001 where pi_numero = '" + rsIt.getString("PEDIDO")
							+ "' and pi_produto = '" + it.produto + "'";
					Resultado rsFaltas = con.consultar(sql);
					if (rsFaltas.next()) {
						it.qntVenda = rsFaltas.getInt("pi_quantida");
						it.qntFaturada = rsFaltas.getInt("pi_atendido");
					}
					rsFaltas.close();

					// ////////////////////////////////////////////////////////////////
					ped.valorBruto += it.valorBruto;
					ped.valorFaturado += it.valorFaturado;
					ped.valorLiquido += rsIt.getDouble("valor_pedido") * it.qntVenda;

					lista.add(it);
				}
				rsIt.close();

				ped.itens = lista;

				// não salva o pedido que tem o status transferido
				// if (ped.status.equals("TRANSFERIDO") == false) {
				PedidoDao pedDao = new PedidoDao();
				pedDao.Salvar(ped, true);

				// SE O STATUS DO PEDIDO FOR DIFERENTE DO ATUAL ENTÃO
				// ATUALIZA O
				// STATUS
				Pedido pedAux = pedDao.getPedido(ped.numero);
				if (pedAux.status.equals(ped.status) == false) {
					if (ped.status.equals(Pedido.FATURADO) && ped.valorFaturado == 0) {
						// NÃO FAZ NADA ** O PEDIDO FOI FATURADO MAS AINDA
						// NÃO
						// FOI EMITIDO A NOTA FISCAO
					} else {
						pedDao.atualizaStatus(ped);
					}
				}
				// }

				qntPedidos++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		rs.close();

		// ATUALIZA OS PEDIDOS EXCLUÍDOS
		sql = "update ven_pedido P set status = 'REJEITADO' ";
		sql += " WHERE  CANAL = " + canal;
		sql += " AND    NUMERODISTRIBUIDOR IN ( ";
		sql += "    select NUMERO from geral.pedido@dbl_go ";
		sql += "    where situacao = 6 ";
		sql += "    and   data >= " + Oracle.strInsert("01/01/2015");
		sql += " )";
		sql += " AND STATUS = 'EXPORTADO FATURAMENTO' ";
		conVendas.executar(sql);

		sql = "update ven_pedido p set status = 'CANCELADO' ";
		sql += " where canal = " + canal;
		sql += " and numeronota in ( ";
		sql += "     select nh_numero from unisys.tnfhven0001@dbl_go ";
		sql += "     where  nh_cancelado =  'S' ";
		sql += "     and    nh_emissao   >= '01/01/2015'";
		sql += "     )";
		conVendas.executar(sql);

		System.out.println("RECEBIDOS: (" + qntPedidos + ") PEDIDOS");
	}

	@Override
	public void recebeTitulos() throws Exception {

		if (true)
			return;

		// // SÓ EXECUTA QUANDO FOR MEIA NOITE
		// Calendar c = Calendar.getInstance();
		// if (c.get(Calendar.HOUR_OF_DAY) != 0)
		// return;

		Conexao con2 = Conector.getConexaoVK();
		try {

			// DELETE TODA A BASE DE CLIENTES E CARRAGA NOVAMENTE
			List<String> sqlClientes = new ArrayList<String>();
			sqlClientes
					.add("delete from ven_setorcliente where setor in (select setor from ven_canalsetor where canal = "
							+ canal + ")");
			sqlClientes.add("insert into ven_setorcliente " + " select distinct cs.setor, c.documento "
					+ " from cliente_a@dbl_cifgo c "
					+ " left join ven_canalsetor cs on c.vendedor = cs.setorexporta and canal = " + canal
					+ " where not cs.setor is null " + " and not c.documento in ( "
					+ " select f.cliente from ven_setorcliente f where f.setor = cs.setor and f.cliente = c.documento "
					+ ")");
			con2.executar(sqlClientes);
		} catch (Exception e) {
			e.printStackTrace();
		}

		con2.executar("delete from VEN_TITULO WHERE CANAL = " + canal);

		String sql = "";
		sql += " insert into ven_titulo (cliente,numero,parcela,vendedor,tipo,vencimento,valor,status,canal) ";
		sql += " SELECT MAX(C.CL_CNPJ) CNPJ, T.NUMERO, T.PARCELA, T.VENDEDOR, MAX(T.tipo), MAX(T.VENCIMENTO), SUM(T.VALOR) VALOR, T.STATUS, "
				+ canal;
		sql += " FROM UNISYS.TCADCLI0001@dbl_cifgo C, ";
		sql += " (SELECT * FROM TITULO_A@dbl_cifgo ";
		sql += " UNION ALL ";
		sql += " SELECT * FROM TITULO_B@dbl_cifgo ";
		sql += " UNION ALL ";
		sql += " SELECT * FROM TITULO_C@dbl_cifgo ";
		sql += " UNION ALL ";
		sql += " SELECT * FROM TITULO_D@dbl_cifgo ";
		sql += " ) T ";
		sql += " WHERE T.CLIENTE = C.CL_CODIGO ";
		sql += " AND   T.STATUS  <> 'B' ";
		sql += " GROUP BY T.NUMERO, T.PARCELA, T.VENDEDOR, T.STATUS ";
		con2.executar(sql);

		sql = "";
		sql += "UPDATE VEN_TITULO T SET VENDEDOR = (SELECT S.SETOR FROM VEN_CANALSETOR S  ";
		sql += "WHERE S.CANAL = " + canal + " AND S.SETOREXPORTA = T.VENDEDOR AND NOT S.SETOR IS NULL ";
		sql += ") ";
		sql += "WHERE T.VENDEDOR in ( ";
		sql += "SELECT SETOREXPORTA FROM VEN_CANALSETOR F WHERE F.CANAL = " + canal;
		sql += ") ";
		con2.executar(sql);
	}

	@Override
	public void recebeClientes() throws Exception {

		// SE FOR MEIA NOITE O SISTEMA BUSCA OS ÚLTIMOS PEDIDOS DA SEMANA
		Calendar c = Calendar.getInstance();
		if (c.get(Calendar.HOUR_OF_DAY) == 0)
			c.add(Calendar.DAY_OF_YEAR, -60);
		else
			c.add(Calendar.DAY_OF_YEAR, -1);

		ClienteDao cliDao = new ClienteDao();
		SetorClienteDao setCliDao = new SetorClienteDao();

		String sql = "SELECT * FROM CLIENTE_A ";
		sql += " WHERE ATUALIZADO >= '" + new SimpleDateFormat("dd/MM/yyyy").format(c.getTime()) + "'";
		// sql += " AND VENDEDOR IN ('202731')";

		Resultado res = con.consultar(sql);
		while (res.next()) {
			try {
				Cliente cli = new Cliente();
				SetorCliente setCli = new SetorCliente();

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
				if (res.getString("email_exp") != null) {
					cli.emailManifesto = res.getString("email_exp");
				}

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

	}

	private void recebeVendedores() throws Exception {

		String sql = " SELECT v.vn_codigo, V.VN_GERENTE, VN_SUPERV, VN_FANTASIA, VN_TIPOVN,VN_EMAIL ";
		sql += " FROM UNISYS.TCADVND0001 V";
		sql += " WHERE VN_ATIVO = 'S'";
		sql += " AND TIMEUPD >= SYSDATE - 30";
		sql += " AND DDELETE IS NULL";

		Resultado res = con.consultar(sql);
		while (res.next()) {
			try {

				String cargo = res.getString("VN_TIPOVN");

				if (cargo.equals("V"))
					cargo = Setor.cargo_vendedor;
				else if (cargo.equals("S"))
					cargo = Setor.cargo_supervisor;
				else if (cargo.equals("G"))
					cargo = Setor.cargo_gerente_regional;

				Setor setor = new SetorDao().getSetor(res.getString("VN_CODIGO"));
				if (setor == null) {
					setor = new Setor();
					Funcionario f = new Funcionario();
					f.codigo = 0;
					f.cargo = cargo;
					f.descricao = res.getString("VN_FANTASIA");
					f.email = res.getString("VN_EMAIL");
					new FuncionarioDao().salvar(f);

					setor.funcionario = f.codigo;
					setor.dataInserido = new Date();
				}

				// VERIFICA SE O FUNCIONÁRIO ESTÁ GRAVADO
				Funcionario f = new FuncionarioDao().getFuncionario(setor.funcionario);
				if (f == null) {
					f = new Funcionario();
					f.codigo = setor.funcionario;
					f.cargo = cargo;
					f.descricao = res.getString("VN_FANTASIA");
					new FuncionarioDao().salvar(f);
				}

				setor.codigo = res.getString("vn_codigo");
				setor.cargo = cargo;
				setor.dataAlterado = new Date();
				setor.descricao = res.getString("vn_fantasia");
				setor.senha = "a";
				setor.situacao = "A";
				setor.funcionario = f.codigo;
				f.email = res.getString("VN_EMAIL");

				setor.supervisor = res.getString("vn_superv");
				setor.gerente_regional = res.getString("vn_gerente");
				setor.gerente_nacional = res.getString("vn_gerente");

				if (setor.cargo.equals(Setor.cargo_vendedor))
					setor.representante = setor.codigo;
				if (setor.cargo.equals(Setor.cargo_supervisor))
					setor.supervisor = setor.codigo;
				if (setor.cargo.equals(Setor.cargo_gerente_regional))
					setor.gerente_regional = setor.codigo;

				new SetorDao().salvar(setor);
				new FuncionarioDao().salvar(f);

				// if (setor.cargo.equals(Setor.cargo_gerente_regional)) {
				// setor.cargo = Setor.cargo_gerente_nacional;
				// new SetorDao().salvar(setor);
				// }
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		res.close();
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
		System.out.println("ENVIANDO PEDIDOS PARA DISTRIBUIDORA: " + pedidos.size());
		for (Pedido ped : pedidos) {
			try {
				enviaPedido(ped);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void recebeDevolucoes() throws Exception {

		Calendar c = Calendar.getInstance();
		// SE FOR MEIA NOITE O SISTEMA BUSCA OS ÚLTIMOS PEDIDOS DA SEMANA
		if (c.get(Calendar.HOUR_OF_DAY) == 0)
			c.add(Calendar.DAY_OF_YEAR, -30);
		else
			c.add(Calendar.DAY_OF_YEAR, -1);

		Conexao con2 = Conector.getConexaoVK();

		String sql = "";
		sql += " SELECT  NI_EMPRESA,NI_FILIAL,NI_NUMERO,NI_SERIE, CL_CNPJ, NI_EMISSAO,NI_VENDE,NI_PRODUTO,NI_QUANTIDA, NI_VUNTLIQ";
		sql += " FROM UNISYS.TNFHCOM0001@dbl_cifgo H, UNISYS.TNFICOM0001@dbl_cifgo I, UNISYS.TCADCLI0001@dbl_cifgo C  ";
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

			CanalProduto canProd = new CanalProdutoDao().getCanalProdutoExporta(canal, res.getString("NI_PRODUTO"));

			if (canProd != null) {
				pedidoItem.produto = canProd.produto;
			} else {
				pedidoItem.produto = res.getString("NI_PRODUTO");
			}
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
		// TODO Auto-generated method stub

	}

	private void enviaManifesto() {

		try {

			Conexao conVK = Conector.getConexaoVK();

			String sql = "select numero from ven_pedido p ";
			sql += " where manifestodata >= sysdate - 30 ";
			sql += " and   canal = " + canal;
			sql += " and   not exists (";
			sql += " select 1 from ven_pedidomanifesto m where m.pedido = p.numero and m.canal = p.canal";
			sql += " )";

			Resultado res = conVK.consultar(sql);

			while (res.next()) {
				Pedido p = new PedidoDao().getPedido(res.getInt("numero"));
				enviaManifesto(p);
			}
			res.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void enviaManifesto(Pedido ped) {

		try {
			if (ped.manifesto == 0)
				return;

			Conexao conVK = Conector.getConexaoVK();

			// VERIFICA SE O MANIFESTO JÁ FOI ENVIADO
			String sql = "SELECT * FROM ven_pedidomanifesto ";
			sql += " WHERE PEDIDO = " + Oracle.strInsert(ped.numero);
			sql += " AND   CANAL = " + ped.canal;
			Resultado res = conVK.consultar(sql);
			if (res.next()) {
				res.close();
				return;
			}
			res.close();

			// VERIFICA O PRAZO DE ENVIO DO MANIFESTO (UM DIA APOS SUA EMISSÃO)
			sql = "SELECT MANIFESTODATA FROM VEN_PEDIDO WHERE NUMERO = " + ped.numero;
			res = conVK.consultar(sql);
			if (res.next()) {
				Calendar c = Calendar.getInstance();
				c.add(Calendar.DAY_OF_YEAR, 1);
				if (res.getDate("MANIFESTODATA").getTime() >= c.getTime().getTime()) {
					res.close();
					return;
				}
			}
			res.close();

			String html = "";
			html += "<head>";
			html += "<style>";
			html += " table, th, td {border: 1px solid black;border-collapse: collapse;} ";
			html += "</style>";
			html += "</head>";
			html += "<div class=WordSection1>";
			html += "<table style=\"height: 65px;  font-size: 110%; padding: 10px 10px 10px 10px; border=\"1\" width=\"650\">";
			html += "<tbody>";
			html += "<tr>";
			html += "<td colspan=\"2\" style=\"width: 50px; height: 50px; text-align: center; padding: 10px 10px 10px 10px;\">";
			html += "<img style=\"padding-bottom: 20px;\" src=\"http://cmc.cifarma.com.br/logomanifesto.png\" /> <br /> <strong>ACOMPANHAMENTO DE FATURAMENTO</strong> <br /> <br /></td>";
			html += "</tr>";
			html += "<tr>";
			html += "<td colspan=\"1\" style=\"padding: 10px 10px 10px 10px;\"><strong>Data saida:</strong> <br /> "
					+ new SimpleDateFormat("dd/MM/yyyy").format(ped.manifestoData) + "</td>";
			html += "<td colspan=\"1\" style=\"padding: 10px 10px 10px 10px;\"><strong>Previsão Entrega:</strong> <br /> "
					+ new SimpleDateFormat("dd/MM/yyyy").format(ped.previsaoEntrega) + "</td>";
			html += "</tr>";
			html += "<tr>";
			html += "<td colspan=\"2\" style=\"padding: 10px 10px 10px 10px;\"><strong>Cliente:</strong>";
			html += "<br /> " + ped.clienteRazao;
			html += "</tr>";
			html += "<tr>";
			html += "<td colspan=\"1\" style=\"padding: 10px 10px 10px 10px;\"><strong>Cidade:</strong>";
			html += "<br /> " + ped.clienteCidade;
			html += "<td colspan=\"1\" style=\"padding: 10px 10px 10px 10px;\"><strong>UF:</strong>";
			html += "<br /> " + ped.clienteUF;
			html += "</tr>";
			html += "<tr>";
			html += "<td colspan=\"2\" style=\"padding: 10px 10px 10px 10px;\"><strong>N&ordm; Nota Fiscal:</strong> <br /> "
					+ ped.numeroNota;
			html += "</tr>";
			html += "<tr>";
			html += "<td colspan=\"1\" style=\"padding: 10px 10px 10px 10px;\"><strong>N&ordm; Pedido:</strong> <br /> "
					+ ped.numeroDistribuidor;
			html += "<td colspan=\"1\" style=\"padding: 10px 10px 10px 10px;\"><strong>N&ordm; Cliente:</strong> <br /> "
					+ ped.numeroCliente;
			html += "</tr>";
			html += "<tr>";
			html += "<td colspan=\"2\" style=\"padding: 10px 10px 10px 10px;\"><strong>Valor Total:</strong> <br /> "
					+ new DecimalFormat("#,##0.00").format(ped.valorFaturado);
			html += "</tr>";
			html += "<tr>";
			html += "<td colspan=\"2\" style=\"padding: 10px 10px 10px 10px;\"><strong>Volumes:</strong>";
			html += "<br /> " + ped.qntVolumes;
			html += "</tr>";
			html += "<tr>";
			html += "<td colspan=\"2\" style=\"padding: 10px 10px 10px 10px;\"><strong>Transportadora:</strong>";
			html += "<br /> " + ped.transportadora + "</td>";
			html += "</tr>";
			html += "</tbody>";
			html += "</table>";
			html += "</div>";

			Map<String, String> map = new ParametrosDao().getTodosParametros();
			String email = null;
			if (map.containsKey("EMAIL_MANIFESTO")) {
				email = map.get("EMAIL_MANIFESTO");
				Cliente cli = new ClienteDao().getCliente(ped.cnpj);
				if (cli.emailManifesto != null) {
					email += "," + cli.emailManifesto;
				}
				Setor setorVendedor = new SetorDao().getSetor(ped.setorVendedor);
				Setor setorSupervisor = new SetorDao().getSetor(ped.setorSupervisor);
				Setor setorGerenteR = new SetorDao().getSetor(ped.setorGerenteR);

				Funcionario funVendedor = null;
				Funcionario funSupervisor = null;
				Funcionario funGerenteR = null;
				if (setorVendedor != null)
					funVendedor = new FuncionarioDao().getFuncionario(setorVendedor.funcionario);
				if (setorSupervisor != null)
					funSupervisor = new FuncionarioDao().getFuncionario(setorSupervisor.funcionario);
				if (setorGerenteR != null)
					funGerenteR = new FuncionarioDao().getFuncionario(setorGerenteR.funcionario);

				// email do vendedor
				if (funVendedor != null && funVendedor.email != null && funVendedor.email.equals("") == false) {
					email += "," + funVendedor.email;
				}
				// email do supervisor
				if (funSupervisor != null && funSupervisor.email != null && funSupervisor.email.equals("") == false) {
					email += "," + funSupervisor.email;
				}
				// email do gerente Regional
				if (funGerenteR != null && funGerenteR.email != null && funGerenteR.email.equals("") == false) {
					email += "," + funGerenteR.email;
				}
			}

			if (email != null) {
				new Email("cifarma.manifesto@gmail.com", "cmcadmin").enviaSemAnexo(
						"Manifesto Cifarma - Número Nota: " + ped.numeroNota + " / " + ped.clienteRazao, html, email);

				sql = "INSERT INTO VEN_PEDIDOMANIFESTO (CANAL,PEDIDO,EMAIL,DATA) VALUES (" + ped.canal + ","
						+ ped.numero + ",'" + email + "',sysdate)";
				conVK.executar(sql);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static CanalProduto getCanalProdutoUnidrogas(String produto) throws Exception {
		Conexao conAux = Conector.getConexaoVK();
		String sql = "select * from ven_canalproduto where canal = 2 and produto like '%" + produto.substring(2) + "'";
		System.out.println(sql);
		Resultado res = conAux.consultar(sql);
		CanalProduto canProd = null;
		if (res.next()) {
			canProd = CanalProdutoDao.parse(res);
		}
		res.close();

		return canProd;
	}

	public static Produto geProdutoSubString(String produto) throws Exception {
		Conexao conAux = Conector.getConexaoVK();
		String sql = "select * from ven_produto where codigo like '%" + produto.substring(2) + "'";
		System.out.println(sql);
		Resultado res = conAux.consultar(sql);
		Produto prod = null;
		if (res.next()) {
			prod = ProdutoDao.parse(res);
		}
		res.close();

		return prod;
	}

}
