package br.com.importa.vkcom.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
* <p><b>Classe   :</b> Config.java
* <p><b>Descrição:</b> <<Descreva o OBJETIVO da CLASSE>>
*
* <p><b>Projeto  :</b> flavios-intelrisk 
* <p><b>Pacote   :</b> br.com.flavios.constants
* <p><b>Empresa  :</b> Flávios Calçados e Esportes 
*
* @author     : fabiooliveira
* @version    : Revision: Date: 25/02/2014
*/
public class Config {
    
    public static final String CONFIG_FILE_NAME = "config.properties";
    public static final String EXTERNAL_CONFIG = ".\\config\\" + CONFIG_FILE_NAME;
    public static final String INTERNAL_CONFIG = CONFIG_FILE_NAME;
    public static final String RUN_PORT_KEY = "run_port";
    public static final int RUN_PORT_DEFAULT = 55555;
    public static Properties PROPS = null; 
    
    
    
    // bloco inicializado das configurações do LOG4J
    public static void loadProperties() throws FileNotFoundException, IOException {
        LogUtil.info(Config.class, "Carregando arquivo de propriedades");
        Properties props = new Properties();

        File file = new File(EXTERNAL_CONFIG);
        InputStream stream = null;

        if (file.exists() && file.isFile()) {
            stream = new FileInputStream(file);
            LogUtil.info(Config.class, "Utilizando arquivo externo: " + EXTERNAL_CONFIG);
        } else {
            stream = ClassLoader.getSystemResourceAsStream(INTERNAL_CONFIG);
            LogUtil.info(Config.class, "Utilizando arquivo interno: " + INTERNAL_CONFIG);
        }

        if (stream != null) {
            props.load(stream);
        } else {
            throw new IllegalArgumentException("Arquivo de configuração não encontrado. " + CONFIG_FILE_NAME + " não existe.");
        }
        PROPS = props;
    }

    public static int getRunPort() {
        if (PROPS != null && PROPS.containsKey(RUN_PORT_KEY)) {
            LogUtil.info(Config.class, "Aplicativo executando na porta " + PROPS.get(RUN_PORT_KEY));
            return LangUtils.toInteger(PROPS.get(RUN_PORT_KEY));
        } else {
            LogUtil.warn(Config.class, "Porta de execução inválida");
            LogUtil.warn(Config.class, "Utilizando porta padrão: " + RUN_PORT_DEFAULT);
            return RUN_PORT_DEFAULT;
        }
    }
    
    public static String getKeyValue(String key) {
        Object value = null;
        LogUtil.trace(Config.class, "Recuperando chave:", key);
        if (PROPS == null) {
            LogUtil.warn(Config.class, "Arquivo de propriedades não encontrado.");
        }
        else {
            value = PROPS.get(key);
            LogUtil.trace(Config.class, key, "=", value);
            if (value==null) {
                LogUtil.warn(Config.class, "A chave", key, "não foi encontrada.");    
            }
        }
        return LangUtils.toString(value);
    }
}
