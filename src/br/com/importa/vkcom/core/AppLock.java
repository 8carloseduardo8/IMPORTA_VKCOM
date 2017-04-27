package br.com.importa.vkcom.core;

import java.io.IOException;
import java.net.ServerSocket;
import javax.swing.JOptionPane;

public class AppLock {

    int port;
    ServerSocket serverSocket;

// ------------------------------------------------------------------     
    public AppLock(int lockport) {
        port = lockport;
    }

//  ------------------------------------------------------------------    
    public void lock() throws IOException {
        serverSocket = new ServerSocket(port);
    }

//  ------------------------------------------------------------------  
    public void unlock() {
        try {
            serverSocket.close();
        } catch (IOException ioex) {
        }
    }

//  ------------------------------------------------------------------  
    public static boolean isLocked(int lockport) {
        boolean retorno = false;
        try {
            ServerSocket ss = new ServerSocket(lockport);
            ss.close();
        } catch (IOException ioex) {
            retorno = true;
        }
        return retorno;
    }

    @Deprecated
    public static void main(String args[]) {
        //import AppLock;  

        // port é qq número acima de 1024, de preferencia leia em um arq. de properties  
        int port = 44444;

        if (AppLock.isLocked(port)) {
            JOptionPane.showMessageDialog(null, "Aplicação já aberta", "ERRO", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } else {
            // Aplicação livre, vamos trava-la  
            AppLock appLock = new AppLock(port);
            try {
                appLock.lock();
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
        }
    }

}
