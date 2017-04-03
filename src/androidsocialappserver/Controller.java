/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package androidsocialappserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Razvan
 */
public class Controller {
    private final MySqlConnectionModel dbModel = new MySqlConnectionModel();
    
    public void solve(){
        
        try {
            dbModel.connect("razvan", "razvan123");
        } catch (SQLException | ClassNotFoundException ex) {
            Logger.getLogger(AndroidSocialAppServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        int nreq = 1;
        try
        {
            ServerSocket sock = new ServerSocket (8080);
            for (;;)
            {
                Socket newsock = sock.accept();
                System.out.println("New client connected, creating separate thread..");
                Thread t = new ThreadHandler(newsock,nreq,dbModel.getConection());
                t.start();
            }
        }
        catch (IOException e)
        {
            System.out.println("IO error " + e);
        }
        System.out.println("End!");
        
    }
}
