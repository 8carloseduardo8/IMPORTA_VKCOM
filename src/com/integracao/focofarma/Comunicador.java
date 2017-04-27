package com.integracao.focofarma;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import vendas.dao.PedidoDao;

import com.integrador.Integrador;
import com.integrador.ServletIntegradorNormal;
import com.integrador.ServletIntegradorNormal.Finaliza;
import com.util.Email;
import com.util.Str;

import br.com.smp.vk.venda.model.Pedido;
import br.com.smp.vk.venda.model.PedidoItem;

public class Comunicador extends Integrador {

	private final int canal = 7;

	public Comunicador(Finaliza f, ServletIntegradorNormal integrador) {
		super(f, integrador, "FOCOFARMA");
	}

	public Comunicador() {
	}

	@Override
	public void recebePedidos() throws Exception {
	}

	@Override
	public void recebeTitulos() throws Exception {
	}

	@Override
	public void recebeClientes() throws Exception {
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
				new Email("cmc@cifarma.com.br", "@cifa123").enviaSemAnexo(
						"PEDIDO DE VENDA CIFARMA (" + ped.numero + ")", montaEmail(ped),
						"mayara.foco@hotmail.com,centralpedido.cifarma@gmail.com");
				System.out.println("PEDIDO ENVIADO COM SUCESSO");
				ped.status = Pedido.EXPORTADO_FATURAMENTO;
				new PedidoDao().atualizaStatus(ped);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		salvaRegistroLogExecucao(canal);

	}

	// /////////////////////////////////////////////////////////////////////////////////////////////////
	public static String montaEmail(Pedido pedido) throws Exception {

		String s = "Segue pedido de Venda: ";
		s += "\n";
		s += "\n";
		s += "<style type=\"text/css\">";
		s += "table.bordasimples {border-collapse: collapse;}";
		s += "table.bordasimples tr td {border:1px solid black; padding: 2px}";
		s += "</style>";
		s += "";
		s += "<table style=\"width:600px;\">";
		s += "	<tr>";
		s += "		<td style=\"width:150px\" ><b>NUM PEDIDO</b></td>";
		s += "		<td>" + pedido.numero + "</td>";
		s += "	</tr>";
		s += "	<tr>";
		s += "		<td><b>DATA</b></td>";
		s += "		<td>" + new SimpleDateFormat("dd/MM/yyyy").format(pedido.data) + "</td>";
		s += "	</tr>";
		s += "	<tr>";
		s += "		<td><b>VENDEDOR</b></td>";
		s += "		<td>" + pedido.vendedorNome + "</td>";
		s += "	</tr>";

		s += "	<tr>";
		s += "		<td><b>PRAZO</b></td>";
		s += "		<td>" + pedido.prazoNome + "</td>";
		s += "	</tr>";

		s += "	<tr>";
		s += "		<td><b>CNPJ</b></td>";
		s += "		<td>" + pedido.cnpj + "</td>";
		s += "	</tr>";
		s += "	<tr>";
		s += "		<td><b>RAZÃO</b></td>";
		s += "		<td>" + pedido.clienteRazao + "</td>";
		s += "	</tr>";
		s += "	<tr>";
		s += "		<td><b>FANTASIA</b></td>";
		s += "		<td>" + pedido.clienteFantasia + "</td>";
		s += "	</tr>";
		s += "	<tr>";
		s += "		<td><b>T. PEDIDO</b></td>";
		s += "		<td>" + "R$ " + new DecimalFormat("#,##0.00").format(pedido.valorLiquido) + "</td>";
		s += "	</tr>";
		s += "	<tr>";
		s += "		<td><b>T. DESCONTOS</b></td>";
		s += "		<td>" + "R$ " + new DecimalFormat("#,##0.00").format(pedido.valorBruto - pedido.valorLiquido)
				+ "</td>";
		s += "	</tr>";
		s += "	<tr>";
		s += "		<td><b>OBS INTERNA</b></td>";
		s += "		<td>" + pedido.obs + "</td>";
		s += "	</tr>";
		s += "	<tr>";
		s += "		<td><b>OBS NOTA FISCAL</b></td>";
		s += "		<td>" + "" + "</td>";
		s += "	</tr>";
		s += "</table>";
		s += "<table class=\"bordasimples\" style=\"margin-top:20px;width:800px\" >";
		s += "	<tr style=\"background-color:green;color:white\" >";
		s += "		<td>CODIGO</td>";
		s += "		<td style=\"width:400px\" >DESCRICAO</td>";
		s += "		<td style=\"text-align:right\" >QTD</td>";
		s += "		<td style=\"text-align:right\" >PRECO LIQ</td>";
		s += "		<td style=\"text-align:right\" >TOTAL LIQ</td>";
		s += "		<td style=\"text-align:right\" >% DESC</td>";
		// s += " <td style=\"text-align:right\">COMISSAO</td>";
		s += "	</tr>";

		for (PedidoItem it : pedido.itens) {
			s += "	<tr>";
			s += "		<td>" + it.produto + "</td>";
			s += "		<td style=\"width:400px\" >" + it.produtoDescricao + "</td>";
			s += "		<td style=\"text-align:right\" >" + (it.qntVenda + it.qntBonificacao) + "</td>";
			s += "		<td style=\"text-align:right\" >" + "R$ " + new DecimalFormat("#,##0.00")
					.format((it.qntVenda * it.valorUnitario) / (it.qntVenda + it.qntBonificacao)) + "</td>";
			s += "		<td style=\"text-align:right\" >" + "R$ "
					+ new DecimalFormat("#,##0.00").format(it.valorUnitario * it.qntVenda) + "</td>";
			s += "		<td style=\"text-align:right\" >" + Str.arredondar2(
					((it.valorBruto - ((it.qntVenda * it.valorUnitario) / (it.qntVenda + it.qntBonificacao)))
							/ it.valorBruto) * 100)
					+ "%" + "</td>";
			// s += " <td style=\"text-align:right\" >" + it.perc_comissao_rep
			// + "%" + "</td>";
			s += "	</tr>";
		}

		s += "</table>";

		System.out.println(s);
		return s;
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
