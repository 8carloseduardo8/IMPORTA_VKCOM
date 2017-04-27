package br.com.importa.vkcom.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class LangUtils {

    /**
     *
     * Nome: toInteger
     *
     * Propósito: Tratar valores BigDecimal para serem passados ao PDC com o
     * numero de casas decimais correto
     *
     * @param param BigDecimal com o valor desejado
     * @param casas Numero de casas decimais a ser passado para o PDC
     * @return Integer contendo o valor a passar para o PDC
     */
    public static Integer toInteger(BigDecimal param, int casas) {
        try {
            if (isEmptyOrNull(param)) {
                return 0;
            } else {
                BigDecimal potencia = BigDecimal.TEN.pow(casas);
                BigDecimal retorno = param.multiply(potencia);
                return retorno.intValue();
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * <p>
     * Nome: toInteger
     * <p>
     * Propósito:
     *
     * @param param
     * @return Integer.
     */
    public static Integer toInteger(Object param) {
        Integer retorno = 0;
        try {
            if (isEmptyOrNull(param)) {
                retorno = 0;
            } else {
                String strNumero = param.toString();

                retorno = Integer.valueOf(strNumero);
            }
        } catch (Exception e) {
            retorno = 0;
        }
        return retorno;
    }
   
   public static BigDecimal toBigDecimal(Object param) {
        BigDecimal retorno = BigDecimal.ZERO;
        try {
            if (!isEmptyOrNull(param)) {
                String strNumero = param.toString();
                strNumero =strNumero.replace(",", ".");
                retorno = new BigDecimal(strNumero);
            }
        } catch (Exception e) {
            retorno = BigDecimal.ZERO;
        }
        return retorno;
    }
   
    public static Short toShort(Object param) {
        Short retorno = 0;
        try {
            if (isEmptyOrNull(param)) {
                retorno = 0;
            } else {
                String strNumero = param.toString();

                retorno = Short.valueOf(strNumero);
            }
        } catch (Exception e) {
            retorno = 0;
        }
        return retorno;
    }

    public static String toString(Object param) {
        String retorno = "";
        try {
            if (!isEmptyOrNull(param)) {
                retorno = param.toString();
            }
        } catch (Exception e) {
            retorno = "";
        }
        return retorno;
    }
    
    public static long toLong(Object param) {
        try {
            if (isEmptyOrNull(param)) {
                return Long.valueOf(0);
            } else {
                if (isNumerico(param.toString())) {
                    return Long.parseLong(param.toString());
                } else {
                    return Long.valueOf(0);
                }
            }
        } catch (Exception e) {
            return Long.valueOf(0);
        }
    }

    public static long toLong(String param) {
        return toLong((Object)param);
    }

    @Deprecated
    public static Long toLong(BigDecimal param, int casas) {

        try {
            if (isEmptyOrNull(param)) {
                return Long.valueOf(0);
            } else {
                BigDecimal potencia = BigDecimal.TEN.pow(casas);
                BigDecimal retorno = param.multiply(potencia);
                return retorno.longValue();
            }
        } catch (Exception e) {
            return Long.valueOf(0);
        }
    }

    @Deprecated
    public static Long toLong(Double param, int casas) {
        return toLong(new BigDecimal(param), casas);
    }

    public static long toLong(Long parametroLong) {
        if (parametroLong == null) {
            return Long.valueOf(0);
        } else {
            return parametroLong;
        }
    }

    public static long toLong(BigInteger param) {
        try {
            if (isEmptyOrNull(param)) {
                return Long.valueOf(0);
            } else {
                return param.longValue();
            }
        } catch (Exception e) {
            return Long.valueOf(0);
        }
    }

    /**
     *
     * Nome: isEmptyOrNull Objetivo: Verificar se o Objeto � nulo ou vazio.
     *
     * @param object Oject a ser comparada.
     * @return True se null ou Empty
     * @see Referências externas.
     */
    public static boolean isEmptyOrNull(Object o) {
        if (o == null || o.toString().trim().equals("")) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
    
    public static boolean isNull(Object o) {
        if (o == null) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public static boolean isZeroOrNull(Number number) {
        boolean is = isEmptyOrNull((Object) number);
        if (number instanceof Double || number instanceof Float || number instanceof BigDecimal) {
            is = is || number.doubleValue() == 0D;
        } else {
            is = is || number.longValue() == 0;
        }
        return is;
    }

    public static boolean isZeroOrNull(String number) {
        number = removerPontosVirgulaHifenBarra(number);
        Long valor = toLong(number);
        return isZeroOrNull(valor);
    }

    public static boolean isNotZeroOrNull(String number) {
        return !isZeroOrNull(number);
    }

    public static boolean isNotZeroOrNull(Number number) {
        return !isZeroOrNull(number);
    }

    /**
     * Método:isNumerico Verifica se uma string é numérica
     *
     * @param texto
     * @return
     */
    public static boolean isNumerico(String texto) {
        texto = removerPontosVirgulaHifenBarra(texto.trim());
        // loop
        for (int i = 0; i < texto.length(); i++) {
            // decisao
            if (!Character.isDigit(texto.charAt(i))) {
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    /**
     * Método:removerPontosVirgulaHifenBarra Remove os caracteres especiais de
     * mascaras aplicadas em números
     *
     * @param valor
     * @return
     */
    public static String removerPontosVirgulaHifenBarra(String valor) {
        valor = valor.replace(".", "");
        valor = valor.replace(",", "");
        valor = valor.replace("-", "");
        valor = valor.replace("/", "");
        return valor;
    }
    
    public static String concat(Object ...a) {
        return concat(false,a);
    }
    
    public static String concatSpaces(Object ...a) {
        return concat(true,a);
    }
    
    public static String concat(boolean spaces, Object ...a) {
        StringBuilder sb = new StringBuilder();
        for (Object i : a) {
            if (spaces) {
                sb.append(" ");
            }
            sb.append(i!=null?i.toString():i);
        }
        return sb.toString();
    }
    
    public static boolean  diff(Object obj1, Object obj2) {
        boolean diff = true;
        boolean nDiff = false;
        // se os 2 forem nulos == MUDOU
        if (obj1 == null && obj2 == null) {
            return nDiff;
        }

        if (!(obj1 != null && obj2 != null)) {
            return diff;
        }
        else {
            if (!obj1.getClass().equals(obj2.getClass())) {
                throw  new IllegalArgumentException("O tipo de dados dos parametrros não podem ser diferente");
            }
            if (obj1 instanceof BigDecimal) {
                obj1 = Formatter.DECIMAL_MOEDA.format(obj1);
                obj2 = Formatter.DECIMAL_MOEDA.format(obj2);
            }
            return obj1.equals(obj2)?nDiff:diff;
        }
    }
    
    public static boolean less(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("valor null inválido para comparação");
        }
        return a.compareTo(b) < 0;
    }
    
    public static boolean lessOrEqual(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("valor null inválido para comparação");
        }
        return a.compareTo(b) <= 0;
    }
    
    public static boolean more(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("valor null inválido para comparação");
        }
        return a.compareTo(b) > 0;
    }
    
    public static boolean moreOrEqual(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("valor null inválido para comparação");
        }
        return a.compareTo(b) >= 0;
    }
    
    /**
     * Retorna o tamanho de uma lista, caso a lista seja NULL retorna 0(zero)
     * @param l
     * @return 
     */
    public static Integer size(List l) {
        return l!=null?l.size():0;
    }
    
    /**
     * Remover metodo de teste
     * @deprecated
     */
    @Deprecated
    public static void testDiff() {
        System.out.println(false + "=" + diff(null, null));
        System.out.println(true + "=" + diff(1, null));
        System.out.println(true + "=" + diff(null, 2));
        System.out.println(true + "=" + diff(1, 2));
        System.out.println(false + "=" + diff(1, 1));
        
        System.out.println(false + "=" + diff("", ""));
        System.out.println(true + "=" + diff("", "1"));
        System.out.println(true + "=" + diff("1", ""));
        System.out.println(true + "=" + diff("123", "12"));
        System.out.println(false + "=" + diff("12", "12"));
        System.out.println(false + "=" + diff(new BigDecimal("12"), new BigDecimal("12.000")));
        System.out.println(false + "=" + diff("12", 12));
    }
    
//    public static void main(String []args) {
//        testDiff();
//    }

}
