package br.com.importa.vkcom.core;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public abstract class DateUtil {

    public static final Locale pt_BR = new Locale("pt", "BR");
    public static final String DEFAULT_PATTERN = "dd/MM/yyyy";
    public static final DateFormatSymbols SYMBOL = new DateFormatSymbols(pt_BR);
        
    
    private DateUtil() {
    }
    
    
    /**
     * Converte uma String em um objeto data, de acordo com o pattern
     *
     * @param data
     * @param pattern
     * @return
     */
    public static Date getData(String data, String pattern) {
        Date getData = null;
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, SYMBOL);
        try {
            getData = sdf.parse(data);
        } catch (ParseException ex) {
           throw new FlaviosDateException("Erro ao converter data. Data: '" + data + "' Pattern: '"+ pattern+"'",  ex);
        }
        return getData;
    }

    /**
     * Converte uma string em Date utilizando o pattern DEFAULT
     *
     * @param data
     * @return
     */
    public static Date getData(String data) {
        return getData(data, DEFAULT_PATTERN);
    }

    /**
     * Método que formata um texto em data no padrão dd/MM/aaaa.
     *
     * @param data O texto da data.
     * @return Um objeto Date ou null caso não consiga fazer o parser.
     */
//    public static Date getData(String data) {
//        return formataData(data, "dd/MM/yyyy");
//    }

    /**
     * Método que formata uma data em texto no padrão dd/MM/aaaa.
     *
     * @param data O objeto Date.
     * @return Uma String formatada ou null caso a data não seja válida.
     */
    public static String getData(Date data) {
        return formataData(data, "dd/MM/yyyy");
    }

    /**
     * Método que formata um texto em data no padrão dd/MM/aaaa HH:mm:ss.
     *
     * @param data O texto da data.
     * @return Um objeto Date ou null caso não consiga fazer o parser.
     */
    public static Date getDataHora(String data) {
        return formataData(data, "dd/MM/yyyy HH:mm:ss");
    }

    /**
     * Método que formata uma data em texto no padrão dd/MM/aaaa HH:mm:ss.
     *
     * @param data O objeto Date.
     * @return Uma String formatada ou null caso a data não seja válida.
     */
    public static String getDataHora(Date data) {
        return formataData(data, "dd/MM/yyyy HH:mm:ss");
    }

    /**
     * Método que formata a data.
     *
     * @param data A data do tipo Date.
     * @param formato O formato desejado.
     * @return A data formatada ou null se tiver erro.
     */
    public static String formataData(Date data, String formato) {
        try {
            return new SimpleDateFormat(formato).format(data);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Método que formata a data.
     *
     * @param data A data em formato string.
     * @param formato O formato desejado.
     * @return A data como objeto ou null se tiver erro.
     */
    public static Date formataData(String data, String formato) {
        try {
            return new SimpleDateFormat(formato).parse(data);
        } catch (Exception ex) {
            return null;
        }
    }
    
    /**
     * Formata data no padrão pt_BR
     * <code>dd/MM/yyyy</code>.
     *
     * @param date
     * @return Data formatada dd/MM/yyyy.
     */
    public static String dataFormatter(Date date) {
        if (date != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            return dateFormat.format(date);
        }
        return null;
    }

    /**
     * Método que retorna a diferença em dias entre duas datas.
     *
     * @param dataInicial É a data inicial que será comparada.
     * @param dataFinal É a data final que será comparada.
     * @return Diferença em dias entre as duas datas.
     */
    public static long getDiasDiferenca(Date dataInicial, Date dataFinal) {
        Calendar di = Calendar.getInstance();
        Calendar df = Calendar.getInstance();
        di.setTime(dataInicial);
        df.setTime(dataFinal);
        
        long diferenca = df.getTimeInMillis() - di.getTimeInMillis();
        // Quantidade de milissegundos em um dia
        int tempoDia = 1000 * 60 * 60 * 24;
        long diasDiferenca = diferenca / tempoDia;
        return diasDiferenca;
    }
    
    public static long getAtraso(Date dataInicial, Date dataFinal) {
        Calendar di = Calendar.getInstance();
        Calendar df = Calendar.getInstance();
        di.setTime(dataInicial);
        df.setTime(dataFinal);
        
        if (di.before(df)) {
            return 0;
        }

        long diferenca = df.getTimeInMillis() - di.getTimeInMillis();
        // Quantidade de milissegundos em um dia
        int tempoDia = 1000 * 60 * 60 * 24;
        long diasDiferenca = diferenca / tempoDia;
        return diasDiferenca;
    }
    
    /**
     * Verifica se uma data é anterior a outra data
     * @param dataInicial
     * @param dataFinal
     * @return 
     */
    public static boolean before(Date dataInicial, Date dataFinal) {
        Calendar di = Calendar.getInstance();
        Calendar df = Calendar.getInstance();
        di.setTime(dataInicial);
        df.setTime(dataFinal);
        return di.before(df);
    }
    
    /**
     * Verifica se uma data é posterior a outra data
     * @param dataInicial
     * @param dataFinal
     * @return 
     */
    public static boolean after(Date dataInicial, Date dataFinal) {
        Calendar di = Calendar.getInstance();
        Calendar df = Calendar.getInstance();
        di.setTime(dataInicial);
        df.setTime(dataFinal);
        return di.after(df);
    }

    /**
     * Retorna a data adicionando o dia, mês ou ano.
     *
     * @param date É a data que será modificada.
     * @param constanteCalendar Constante da classe Calendar que representa o
     * dia, mês ou ano.
     * @param diaMesAno Quantidade em dias, mês ou ano.
     * @return Uma data.
     */
    public static Date adicionaDiaMesAno(Date date, final int constanteCalendar, int diaMesAno) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(constanteCalendar, diaMesAno);
        return calendar.getTime();

    }
}
