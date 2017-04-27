package br.com.importa.vkcom.core;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class RuntimeCalc {

    private String operacao;
    private long dia;
    private long hora;
    private long minuto;
    private long segundo;
    private long milisegundo;
    private long timeInicial;
    private long timeFinal;
    
    private static final long SEGUNDO = 1000;
    private static final long MINUTO = SEGUNDO*60;
    private static final long HORA = MINUTO*60;
    private static final long DIA = HORA*24;

    
    /**
     * Construtor padrão privado
     */
    @SuppressWarnings("unused")
	private RuntimeCalc() {
        
    }
    
    /**
     * Construtor parametrizado para receber a descrição da operação
     * @param operacao 
     */
    public RuntimeCalc(String operacao) {
        setTimeInicial(System.currentTimeMillis());
        this.operacao = operacao;
    }
    
    /**
     * Inicia o cálculo do tempo de execução
     * @param isLog
     */
    public void start(boolean isLog) {
        if (isLog) {
            LogUtil.info(RuntimeCalc.class, getOperacao(), " - iniciando execução");
        }
        setTimeInicial(System.currentTimeMillis());
    }
    
    public void start() {
        start(false);
    }
    
    
    /**
     * Finaliza o cálculo do tempo de execução
     * @param isLog
     */
    public void stop(boolean isLog) {
        setTimeFinal(System.currentTimeMillis());
        Long tempo = getTimeFinal() - getTimeInicial();
        dia = tempo/DIA;
        hora = tempo/HORA%24;
        minuto = tempo/MINUTO%60;
        segundo = tempo/SEGUNDO%60;
        milisegundo = (tempo % 1000);
        
        if (isLog) {
            LogUtil.info(RuntimeCalc.class, getOperacao(), " - execução finalizada");
            LogUtil.info(RuntimeCalc.class, toString());
        }
    }
    
    public void stop() {
        stop(false);
    }
    

    /**
     * Retorna a String com as informações do tempo de execução em detalhe.
     * @return 
     */
    @Override
    public String toString() {
        NumberFormat f2 = new DecimalFormat("00");
        NumberFormat f3 = new DecimalFormat("000");
        return LangUtils.concat(
                getOperacao()
                , " tempo de execução: "
                , f2.format(getDia()) ," dia(s) "
                , f2.format(getHora()), ":", f2.format(getMinuto()), ":", f2.format(getSegundo()), " ", f3.format(getMilisegundo()));
    }
    
    /**
     * @return the operacao
     */
    public String getOperacao() {
        return operacao;
    }

    /**
     * @return the dia
     */
    public long getDia() {
        return dia;
    }

    /**
     * @return the hora
     */
    public long getHora() {
        return hora;
    }

    /**
     * @return the minuto
     */
    public long getMinuto() {
        return minuto;
    }

    /**
     * @return the segundo
     */
    public long getSegundo() {
        return segundo;
    }

    /**
     * @return the milisegundo
     */
    public long getMilisegundo() {
        return milisegundo;
    }

    /**
     * @return the timeInicial
     */
    public long getTimeInicial() {
        return timeInicial;
    }

    /**
     * @param timeInicial the timeInicial to set
     */
    public void setTimeInicial(long timeInicial) {
        this.timeInicial = timeInicial;
    }

    /**
     * @return the timeFinal
     */
    public long getTimeFinal() {
        return timeFinal;
    }

    /**
     * @param timeFinal the timeFinal to set
     */
    public void setTimeFinal(long timeFinal) {
        this.timeFinal = timeFinal;
    }

    
}
