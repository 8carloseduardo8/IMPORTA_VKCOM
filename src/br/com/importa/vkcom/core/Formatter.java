package br.com.importa.vkcom.core;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public interface Formatter {

    Locale LOCALE = new Locale("pt","BR");
    
    DecimalFormatSymbols SYMBOLS = new DecimalFormatSymbols(LOCALE);
    
    DecimalFormat DECIMAL_MOEDA = new DecimalFormat("###,###,###,###.00", SYMBOLS);
    
    DecimalFormat DECIMAL_PERCENT = new DecimalFormat("###,###,###,###.00 '%'", SYMBOLS);
    
}
