/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package androidsocialappserver;

/**
 *
 * @author Razvan
 */

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

class ThreadHandler extends Thread {
    private final Socket socket;
    private Connection dbConnection;
    private final int clientNumber;

    ThreadHandler(Socket s, int v,Connection DB) {
        socket = s;
        clientNumber = v;
        dbConnection = DB;
    }


    @Override
    public void run() {
        try {
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            DataInputStream input = new DataInputStream(socket.getInputStream());
            
            boolean moreData = true;
            int optionValue;

            while (moreData) {
                
                optionValue = input.read();
                
                if(optionValue == 1){
                    System.out.println("Am primit comanda 1.(LOGIN)");
                    String username = input.readUTF();
                    String password = input.readUTF();
                    
                    System.out.println("Am primit datele: User: "+username+" Password: "+password+". Verific in BD.");
                    
                    String sql = "SELECT * from users WHERE User = ? and Password = ?";
                    PreparedStatement statement = dbConnection.prepareStatement(sql);
                    statement.setString(1,username);
                    statement.setString(2, password);
                    ResultSet result = statement.executeQuery();
                    
                    if (!result.isBeforeFirst() ) {    
                        output.write(0);
                        System.out.println("Nu am putut loga userul, datele nu se potrivesc");
                    } 
                    else output.write(1);
                    output.flush();
                    
                }
                else if(optionValue == 2){
                    System.out.println("Am primit comanda 2.(REGISTER)");
                    String username = input.readUTF();
                    String password = input.readUTF();
                    String email = input.readUTF();
                    
                    System.out.println("Am primit datele: User: "+username+" Password: "+password+" Email: "+password+". Verific in BD.");
                    
                    String sql = "SELECT * from users WHERE User = ? ";
                    PreparedStatement statement = dbConnection.prepareStatement(sql);
                    statement.setString(1,username);
                    ResultSet result = statement.executeQuery();
                    
                    if (result.isBeforeFirst() ) {    
                        output.write(0);
                        System.out.println("Nu am gasit userul in DB. Incerc inregistrarea.");
                    } 
                    else {
                        //output.write(1);
                        sql = "INSERT INTO users(User,Password,Email) VALUES (?,?,?) ";
                        PreparedStatement statementRegister = dbConnection.prepareStatement(sql);
                        statementRegister.setString(1,username);
                        statementRegister.setString(2,password);
                        statementRegister.setString(3,email);
                        int rows = statementRegister.executeUpdate();
                        if(rows < 1){
                            output.write(0);
                            System.out.println("Userul deja exista in DB.");
                        }
                        else{
                            output.write(1);
                            System.out.println("Userul "+username+" a fost inregistrat cu succes.");
                        }
                    }
                    output.flush();
                    
                }
            }
            
            socket.close();
            System.out.println("Disconnected from client number: " + clientNumber);
        } catch (IOException e) {
            System.out.println("IO error " + e);
        } catch (SQLException ex) {
            Logger.getLogger(ThreadHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
