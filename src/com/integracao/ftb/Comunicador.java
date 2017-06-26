package com.integracao.ftb;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import com.integrador.Integrador;
import com.integrador.ServletIntegradorNormal;
import com.integrador.ServletIntegradorNormal.Finaliza;

import br.com.core.util.LogUtil;
import conect.Conector;
import conect.Conexao;
import conect.Oracle;
import conect.Resultado;

public class Comunicador extends Integrador {

	// 10.5.101.241

	private static final FTPClient ftp = new FTPClient();
	private static final String host = "10.5.100.110";
	private static final String porta = "21";
	private static final String usuario = "ftb";
	private static final String senha = "@ftb123";

	private static final String integracao = "FTB";
	private static final String pastaPedido = "/ftb/PEDIDO";
	private static final String pastaRetorno = "/ftb/RETORNO";

	private static final int canal = 5;

	public static void main(String args[]) {
		// new Comunicador(null).start();
		try {
			new Comunicador(null, null).enviaPedidos();
			new Comunicador(null, null).recebePedidos();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Comunicador(Finaliza f, ServletIntegradorNormal integrador) {
		super(f, integrador, "FTB");
		conectar();
	}

	public void conectar() {
		try {
			ftp.connect(host);

			// verifica se conectou com sucesso!
			if (FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
				ftp.login(usuario, senha);
			} else {
				// erro ao se conectar
				ftp.disconnect();
				LogUtil.error(Comunicador.class, integracao + ": Conexão recusada");
			}
			LogUtil.info(Comunicador.class, integracao + ": Conectando...");
		} catch (Exception e) {
			LogUtil.error(Comunicador.class, "Ocorreu um erro: " + e.getMessage());
		}
	}

	@Override
	public void enviaPedidos() throws Exception {
		// FUNÇÃO DE RECEBER PEDIDO

		conectar();
		LogUtil.info(Comunicador.class, integracao + ": Alterando pasta: " + pastaPedido);
		ftp.changeWorkingDirectory("/ftb");
		ftp.changeWorkingDirectory(pastaPedido);
		FTPFile[] files = ftp.listFiles();
		LogUtil.info(Comunicador.class, integracao + ": Total de arquivos: " + files.length);
		new File("C:\\VK-FARMA\\INTEGRACAO\\" + integracao).mkdirs();

		for (FTPFile f : files) {
			try {
				LogUtil.info(Comunicador.class, integracao + ": Lendo arquivo: " + f.getName());
				String arqDown = "C:\\VK-FARMA\\INTEGRACAO\\" + integracao + "\\" + f.getName();
				if (f.getName().toUpperCase().endsWith("PED")) {
					FileOutputStream fos = new FileOutputStream(arqDown);
					boolean download = ftp.retrieveFile(f.getName(), fos);
					if (download) {
						// CARREGA O ARQUIVO
						String versao = "", cnpj = "", numeroPedidoCliente = "", dataPedido = "", horaPedido = "";
						int codigoCliente = 0;
						String obs = "";
						List<PedidoItem> itens = new ArrayList<>();

						BufferedReader br = new BufferedReader(new FileReader(arqDown));
						String s;

						while ((s = br.readLine()) != null) {
							if (s.startsWith("01")) {
								// CABEÇALHO
								versao = s.substring(2, 2 + 6);
								cnpj = s.substring(8, 8 + 14);
								codigoCliente = Integer.parseInt(s.substring(22, 22 + 6));
								numeroPedidoCliente = s.substring(28, 28 + 9);
								dataPedido = s.substring(37, 37 + 8);
								horaPedido = s.substring(45, 45 + 4);

							} else if (s.startsWith("02")) {
								// MENSAGEM
								obs = s.substring(2, 2 + 40);
							} else if (s.startsWith("03")) {
								// ITENS DO PEDIDO
								PedidoItem it = new PedidoItem();
								it.tipo = s.substring(2, 2 + 1);
								it.codigo = s.substring(3, 3 + 13);
								it.quantidade = Integer.parseInt(s.substring(16, 16 + 7));
								itens.add(it);
							} else if (s.startsWith("09")) {
								// TOTALIZADOR
							}
						}
						br.close();

						Conexao con = Conector.getConexaoMinasGerais();

						String sql;
						Resultado res;
						int idPedido = 0;
						sql = "select unisys.pedidovenda.nextval numero from dual";
						res = con.consultar(sql);
						if (res.next())
							idPedido = res.getInt("numero");
						res.close();

						if (idPedido == 0)
							throw new Exception("ERRO AO CARREGAR NÚMERO DO PEDIDO");

						String vendedor = "2090211";
						String prazo = "9";
						String prazoDescricao = "60|";

						// PEDIDO CARREGADO, AGORA ENTRA O PROCESSO DE GRAVAR NA
						// GERA.PEDIDO (MG)

						List<String> listaSQL = new ArrayList<>();

						sql = "INSERT INTO GERAL.PEDIDO (EMPRESA,FILIAL,NUMERO,INTEGRACAO) VALUES (";
						sql += Oracle.strInsert("0002") + ",";
						sql += Oracle.strInsert("0001") + ",";
						sql += Oracle.strInsert(idPedido) + ",";
						sql += Oracle.strInsert(integracao) + ")";
						listaSQL.add(sql);

						sql = "UPDATE GERAL.PEDIDO SET ";
						sql += "VENDEDOR = " + Oracle.strInsert(vendedor) + ",";
						sql += "CLIENTE = " + Oracle.strInsert(codigoCliente) + ",";
						sql += "NEG = " + Oracle.strInsert("2011") + ",";
						sql += "CONDICAO = " + Oracle.strInsert(prazo) + ",";
						sql += "PRAZO = " + Oracle.strInsert(prazoDescricao) + ",";
						sql += "TIPO = " + Oracle.strInsert("1") + ",";
						sql += "OBSERVACAO = " + Oracle.strInsert(obs.trim()) + ",";
						sql += "FATURAMENTO = " + Oracle.strInsert(0) + ",";
						// sql += "DATA = " + Oracle.strInsert(new
						// SimpleDateFormat("YYYYMMDD").parse(dataPedido)) +
						// ",";
						sql += "DATA = " + Oracle.strInsert(new Date()) + ",";
						sql += "ENTREGA = " + Oracle.strInsert(new Date()) + ",";
						sql += "SITUACAO = " + Oracle.strInsert(1) + ",";
						sql += "ATUALIZADO = " + Oracle.strInsert(new Date()) + ",";
						sql += "TIPO_ORIGEM = " + Oracle.strInsert(1) + ",";
						sql += "MOTIVO_VISITA = " + Oracle.strInsert(1) + ",";
						sql += "NUMERO_CLIENTE = " + Oracle.strInsert(numeroPedidoCliente) + ",";
						sql += "PEDIDO_ORIGEM = " + Oracle.strInsert(f.getName().substring(0, 8));
						sql += "WHERE NUMERO = " + Oracle.strInsert(idPedido);
						listaSQL.add(sql);

						int seq = 0;
						for (PedidoItem it : itens) {
							// BUSCA O CÓDIGO DO PRODUTO
							Produto produto = Produto.getProdutoEan(it.codigo);
							// BUSCA O VALOR BRUTO DO PRODUTO

							sql = "INSERT INTO GERAL.ITEM (EMPRESA,FILIAL,PEDIDO,SEQUENCIA,PRODUTO) VALUES (";
							sql += Oracle.strInsert("0002") + ",";
							sql += Oracle.strInsert("0001") + ",";
							sql += Oracle.strInsert(idPedido) + ",";
							sql += Oracle.strInsert(++seq) + ",";
							sql += Oracle.strInsert(produto.codigoExporta) + ")";
							listaSQL.add(sql);

							sql = "update item set";
							sql += " bonificacao = 0";
							sql += ", quantidade = " + Oracle.strInsert(it.quantidade);
							sql += ", preco = " + produto.valorBruto;
							sql += ", precoFinal = " + (produto.valorBruto * (1 - (produto.desconto / 100)));
							sql += ", desconto_1 = " + (produto.desconto);
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
							sql += ", tpvenda = '101'";
							sql += " where empresa = " + Oracle.strInsert("0002");
							sql += " and   filial  = " + Oracle.strInsert("0001");
							sql += " and   pedido  = " + idPedido;
							sql += " and   produto  = " + produto.codigoExporta;
							listaSQL.add(sql);
						}

						con.executar(listaSQL);
						ftp.deleteFile(f.getName());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void recebePedidos() throws Exception {
		// FUNÇÃO DE ENVIAR RETORNO
		int qntPedidos = 0;

		Conexao con = Conector.getConexaoMinasGerais();

		String sql = "";
		sql += " select p.ph_pedorigem, p.ph_origem,  n.nh_emissao ph_datafat, p.ph_numnota, c.cl_cnpj, f.numero_cliente, f.pedido_origem, v.* from pedido_a v ";
		sql += " left join unisys.tpdhven0001 p on v.numero = p.ph_numero ";
		sql += " left join unisys.tcadcli0001 c on v.cliente = c.cl_codigo ";
		sql += " left join unisys.tnfhven0001 n on v.numero = n.nh_pedido ";
		sql += " left join geral.pedido f on v.numero = f.numero ";
		sql += " where v.numero in (";
		sql += "    select numero from geral.pedido f ";
		sql += "    where f.integracao = " + Oracle.strInsert(integracao);
		sql += "    and f.tipo_retorno is null";
		sql += " )";
		sql += " and not v.situacao is null ";
		sql += " order by v.data desc ";
		Resultado rs = con.consultar(sql);

		while (rs.next()) {
			try {
				String situacao = rs.getString("situacao");

				if (situacao.equals("FATURADO") || situacao.equals("REJEITADO") || situacao.equals("FECHADO")
						|| situacao.equals("EXCLUIDO") || situacao.equals("SEPARANDO")) {

					boolean rejeitado = (situacao.equals("REJEITADO") || situacao.equals("EXCLUIDO")) == true;

					File arquivo = new File("C:\\VK-FARMA\\INTEGRACAO\\" + integracao + "\\"
							+ new DecimalFormat("00000000").format(rs.getInt("pedido_origem")) + ".RET");

					BufferedWriter bw = new BufferedWriter(new FileWriter(arquivo));

					// GRAVA O PEDIDO DE RETORNO
					String linha;
					linha = String.format("%2s", "01");
					linha += String.format("%6s", "1.01  ");
					linha += rs.getString("cl_cnpj");
					linha += new DecimalFormat("000000").format(rs.getInt("numero_cliente"));
					linha += new DecimalFormat("0000000000").format(rs.getInt("numero"));
					bw.write(linha);
					bw.newLine();

					int numeroPedido = rs.getInt("numero");

					if (situacao.equals("EXCLUIDO")) {
						sql = "select i.*, 0 qtd_liberado from item i where i.PEDIDO = " + numeroPedido;
					} else {
						// CARREGANDO OS ITENS DO PEDIDO
						sql = "select * from gerente_item i where i.PEDIDO = " + numeroPedido;
					}
					Resultado rsIt = con.consultar(sql);

					int qtItens = 0, qtUnidades = 0;

					while (rsIt.next()) {

						sql = "select pr_unean from unisys.tcadpro0001 where pr_codigo = "
								+ Oracle.strInsert(rsIt.getString("produto"));
						Resultado rsProd = con.consultar(sql);

						String ean = "";
						if (rsProd.next()) {
							ean = rsProd.getString("pr_unean");
						} else {
							throw new Exception("CODIGO EAN NÃO ENCONTRADO - " + rsIt.getString("produto"));
						}
						rsProd.close();

						int qtd_liberado = rsIt.getInt("qtd_liberado");
						if (rejeitado == true)
							qtd_liberado = 0;

						linha = String.format("%2s", "02");
						linha += String.format("%1s", "2");
						linha += ean;
						linha += new DecimalFormat("0000000").format(qtd_liberado);

						bw.write(linha);
						bw.newLine();

						qtItens++;
						qtUnidades += qtd_liberado;

					}
					rsIt.close();

					linha = "09";
					linha += new DecimalFormat("0000").format(qtItens);
					linha += new DecimalFormat("00000000").format(qtUnidades);
					bw.write(linha);
					bw.newLine();

					bw.flush();
					bw.close();

					enviaArquivo(pastaRetorno, arquivo.getAbsolutePath());

					// ATUALIZA O PEDIDO PARA NÃO SER ENVIADO NOVAMENTE
					sql = "UPDATE GERAL.PEDIDO SET TIPO_RETORNO = 'A' WHERE NUMERO = " + Oracle.strInsert(numeroPedido);
					con.executar(sql);

					qntPedidos++;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		rs.close();

		System.out.println("RECEBIDOS: (" + qntPedidos + ") PEDIDOS");
	}

	private class PedidoItem {
		String tipo;
		String codigo;
		int quantidade;
	}

	private static class Produto {
		String ean;
		String codigo;
		String codigoExporta;
		double valorBruto;
		double desconto;

		public static Produto getProdutoEan(String ean) throws Exception {

			Produto p = new Produto();

			String sql = "select cp.produtoexporta, p.codigo, p.ean,";
			sql += " max(cpp.valorbruto) valorbruto from ven_condicaoproduto cpp, ven_condicao c, ven_canalproduto cp, ven_produto p";
			sql += " where cpp.condicao = c.codigo";
			sql += " and   cp.canal = c.canal";
			sql += " and   cpp.produto = cp.produto";
			sql += " and   cp.produto = p.codigo";
			sql += " and   c.canal = " + canal;
			sql += " and   p.ean = " + Oracle.strInsert(ean);
			sql += " group by cp.produtoexporta, p.codigo, p.ean";
			Resultado res = Conector.getConexaoVK().consultar(sql);

			if (res.next()) {
				p.ean = res.getString("ean");
				p.codigo = res.getString("codigo");
				p.codigoExporta = res.getString("produtoExporta");
				p.valorBruto = res.getDouble("valorbruto");
			}
			res.close();

			// BUSCA O DESCONTO
			sql = "select desconto from geral.produto_INTEGRACAO_desconto";
			sql += " where integracao = " + Oracle.strInsert(integracao);
			sql += " and   produto = " + Oracle.strInsert(p.codigoExporta);
			res = Conector.getConexaoMinasGerais().consultar(sql);
			if (res.next()) {
				p.desconto = res.getDouble("desconto");
			}
			res.close();

			if (p.codigoExporta == null || p.codigoExporta.equals("")) {
				throw new Exception("PRODUTO EAN(" + ean + ") NÃO ENCONTRADO!");
			} else if (p.desconto <= 0) {
				// throw new Exception("PRODUTO NÃO CADASTRADO NA TABELA
				// PRODUTO_INTEGRACAO_DESCONTO");
			}

			return p;
		}
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

		// ajusta o tipo do arquivo a ser enviado
		// if (arquivo.endsWith(".txt")) {
		// System.out.println("ENVIANDO ARQUIVO TEXTO");
		// ftp.setFileType(FTPClient.ASCII_FILE_TYPE);
		// } else if (arquivo.endsWith(".jpg")) {
		// ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
		// } else {
		// ftp.setFileType(FTPClient.ASCII_FILE_TYPE);
		// }
		LogUtil.info(Comunicador.class, integracao + ": Pasta " + pasta);
		LogUtil.info(Comunicador.class, integracao + ": Enviando arquivo " + nomeArquivo + "...");

		// faz o envio do arquivo
		// ftp.mkd(pasta);
		ftp.changeWorkingDirectory(pasta);
		// String dirs[] = pasta.split("/");
		// for (String dir : dirs) {
		// System.out.println("mkdir: " + dir);
		// ftp.mkd(dir);
		// ftp.changeWorkingDirectory(dir);
		// }

		ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
		ftp.storeFile(nomeArquivo, is);
		is.close();
		LogUtil.info(Comunicador.class, integracao + ": Arquivo " + nomeArquivo + " enviado com sucesso!");
	}

	@Override
	public void recebeTitulos() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void recebeClientes() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void recebeDevolucoes() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void recebeEstoque() throws Exception {
		// TODO Auto-generated method stub

	}

}
