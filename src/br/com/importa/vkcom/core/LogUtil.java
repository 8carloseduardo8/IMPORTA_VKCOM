package br.com.importa.vkcom.core;

import java.io.File;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class LogUtil {

    
    private static final String EXTERNAL_CONFIG = ".\\logs\\config\\log4j.xml";

    // bloco inicializado das configurações do LOG4J
    static {
        File xml = new File(EXTERNAL_CONFIG);
        if (xml.exists() && xml.isFile()) {
            DOMConfigurator.configure(EXTERNAL_CONFIG);
        }
        else {
            DOMConfigurator.configure(ClassLoader.getSystemResource("log4j.xml"));
        }
    }
    
    /**
     * Construtor padrão Privado classe utilitária acesso via métodos estáticos
     */
    private LogUtil() {
        
    }
    
    private static Logger getLogger(Class clazz) {
         return Logger.getLogger(clazz);
    }
    
    /**
     * Implementação para execução do método <b>trace</b> do framework de getLogging
     *
     * @param clazz
     * @param mensagem
     */
    public static void trace(Class clazz, Object... mensagem) {
        if (getLogger(clazz).isTraceEnabled()) {
            getLogger(clazz).trace(LangUtils.concatSpaces(mensagem));
        }
    }

    /**
     * Implementação para execução do método <b>trace</b> do framework de getLogging
     * @param clazz
     * @param t
     */
    public static void trace(Class clazz, Throwable t) {
        if (getLogger(clazz).isTraceEnabled()) {
            getLogger(clazz).trace(t.getMessage(), t);
        }
    }
    
    /**
     * Implementação para execução do método <b>trace</b> do framework de getLogging
     * @param clazz
     * @param content 
     */
    public static void trace(Class clazz, Log content) {
        if (getLogger(clazz).isTraceEnabled()) {
            getLogger(clazz).trace(LangUtils.concatSpaces(content.getLog()));
        }
    }

    /**
     * Implementação para execução do método <b>debug</b> do framework de getLogging
     * @param clazz
     * @param mensagem
     */
    public static void debug(Class clazz, Object... mensagem) {
        if (getLogger(clazz).isDebugEnabled()) {
            getLogger(clazz).debug(LangUtils.concatSpaces(mensagem));
        }
    }
    
    /**
     * Implementação para execução do método <b>debug</b> do framework de getLogging
     * @param clazz
     * @param content 
     */
    public static void debug(Class clazz, Log content) {
        if (getLogger(clazz).isDebugEnabled()) {
            getLogger(clazz).debug(LangUtils.concatSpaces(content.getLog()));
        }
    }
    
    /**
     * Implementação para execução do método <b>debug</b> do framework de getLogging
     *
     * @param clazz
     * @param t
     */
    public static void debug(Class clazz, Throwable t) {
        if (getLogger(clazz).isDebugEnabled()) {
            getLogger(clazz).debug(t.getMessage(), t);
        }
    }
    
    /**
     * Implementação para execução do método <b>debug</b> do framework de getLogging
     * @param clazz
     * @param t
     * @param mensagem 
     */
    public static void debug(Class clazz, Throwable t, Object ...mensagem) {
        if (getLogger(clazz).isDebugEnabled()) {
            getLogger(clazz).debug(LangUtils.concatSpaces(mensagem), t);
        }
    }

    /**
     * Implementação para execução do método <b>info</b> do framework de getLogging
     *
     * @param clazz
     * @param mensagem
     */
    public static void info(Class clazz, Object... mensagem) {
        if (getLogger(clazz).isInfoEnabled()) {
            getLogger(clazz).info(LangUtils.concatSpaces(mensagem));
        }
    }

    /**
     * Implementação para execução do método <b>info</b> do framework de getLogging
     *
     * @param clazz
     * @param t
     */
    public static void info(Class clazz, Throwable t) {
        if (getLogger(clazz).isInfoEnabled()) {
            getLogger(clazz).info(t.getMessage(), t);
        }
    }

    /**
     * Implementação para execução do método <b>warn</b> do framework de getLogging
     *
     * @param clazz
     * @param mensagem
     */
    public static void warn(Class clazz, Object... mensagem) {
        getLogger(clazz).warn(LangUtils.concatSpaces(mensagem));
    }

    /**
     * Implementação para execução do método <b>warn</b> do framework de getLogging
     *
     * @param clazz
     * @param t
     */
    public static void warn(Class clazz, Throwable t) {
        getLogger(clazz).warn(t.getMessage(), t);
    }

    /**
     * Implementação para execução do método <b>error</b> do framework de getLogging
     *
     * @param clazz
     * @param mensagem
     */
    public static void error(Class clazz, Object... mensagem) {
        getLogger(clazz).error(LangUtils.concatSpaces(mensagem));
    }

    /**
     * Implementação para execução do método <b>error</b> do framework de getLogging
     *
     * @param clazz
     * @param t
     */
    public static void error(Class clazz, Throwable t) {
        getLogger(clazz).error(t.getMessage(), t);
    }
    
    public static void error(Class clazz, Throwable t, Object ...mensagem) {
        getLogger(clazz).error(LangUtils.concatSpaces(mensagem), t);
    }

    /**
     * Implementação para execução do método <b>fatal</b> do framework de getLogging
     *
     * @param clazz
     * @param mensagem
     */
    public static void fatal(Class clazz, Object... mensagem) {
        getLogger(clazz).fatal(LangUtils.concatSpaces(mensagem));
    }

    /**
     * Implementação para execução do método <b>fatal</b> do framework de getLogging
     *
     * @param clazz
     * @param t
     */
    public static void fatal(Class clazz, Throwable t) {
        getLogger(clazz).fatal(t.getMessage(), t);
    }
    
    /**
     * Implementação para execução do método <b>fatal</b> do framework de getLogging
     * @param clazz
     * @param t
     * @param mensagem 
     */
    public static void fatal(Class clazz, Throwable t, Object ...mensagem) {
        getLogger(clazz).fatal(LangUtils.concatSpaces(mensagem), t);
    }

}
