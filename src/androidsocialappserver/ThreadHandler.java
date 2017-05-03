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

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

class ThreadHandler extends Thread {
    private final Socket socket;
    private Connection dbConnection;
    private final int clientNumber;
    private String loggedUser;
    private Integer loggedUserId;

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
                
                switch (optionValue) {
                    case 1:
                        {
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
                            else {
                                result.next();
                                this.loggedUser = username;
                                this.loggedUserId = result.getInt("idUser");
                                output.write(1);
                            }       
                            output.flush();
                            break;
                        }
                    case 2:
                        {
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
                            }       output.flush();
                            break;
                        }
                    case 3:
                        {
                            System.out.println("Am primit comanda 3.(CERERE DATE USER)");
                            String sql = "SELECT * from users WHERE User = ? ";
                            PreparedStatement statement = dbConnection.prepareStatement(sql);
                            statement.setString(1,this.loggedUser);
                            ResultSet result = statement.executeQuery();
                            result.next();
                            output.writeUTF(result.getString("User"));
                            output.writeUTF(result.getString("Email"));
                            if(result.getString("Firstname") != null)
                                output.writeUTF(result.getString("Firstname"));
                            else output.writeUTF("");
                            if(result.getString("Lastname") != null)
                                output.writeUTF(result.getString("Lastname"));
                            else output.writeUTF("");
                            if(result.getString("Avatar") != null){
                                String avatarPath = result.getString("Avatar");
                                byte[] imageInByte;
                                BufferedImage originalImage = ImageIO.read(new File(avatarPath));
                                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                                    ImageIO.write(originalImage, "jpg", baos);
                                    baos.flush();
                                    imageInByte = baos.toByteArray();
                                }
                                
                                int avatarLength = imageInByte.length;
                                output.writeInt(avatarLength);
                                output.write(imageInByte, 0, avatarLength);
                                output.flush();
                            }
                            else {
                                output.writeInt(0);
                                output.flush();
                            }
                            break;
                        }
                    case 4:
                        {
                            System.out.println("Am primit comanda 4.(MODIFICA DATE USER)");
                            String firstName = input.readUTF();
                            String lastName = input.readUTF();
                            String email = input.readUTF();
                            int avatarSize = input.readInt();
                            String photoName = null;
                            if(avatarSize != 0){
                                byte[] avatar = new byte[avatarSize];
                                input.readFully(avatar, 0, avatarSize);


                                InputStream in = new ByteArrayInputStream(avatar);
                                BufferedImage bImageFromConvert = ImageIO.read(in);

                                photoName = "E:/SocialAppAvatars/"+this.loggedUser+"_"+System.currentTimeMillis()+".jpg";
                                ImageIO.write(bImageFromConvert, "jpg", new File(
                                                photoName));
                            }
                            
                            if(avatarSize != 0){
                                String sql = "UPDATE users SET Firstname=?,Lastname=?,Email=?,Avatar=? WHERE User = ? ";
                                PreparedStatement statement = dbConnection.prepareStatement(sql);
                                statement.setString(1,firstName);
                                statement.setString(2,lastName);
                                statement.setString(3,email);
                                statement.setString(4, photoName);
                                statement.setString(5,this.loggedUser);
                                statement.executeUpdate();
                            }else{
                                String sql = "UPDATE users SET Firstname=?,Lastname=?,Email=? WHERE User = ? ";
                                PreparedStatement statement = dbConnection.prepareStatement(sql);
                                statement.setString(1,firstName);
                                statement.setString(2,lastName);
                                statement.setString(3,email);
                                statement.setString(4,this.loggedUser);
                                statement.executeUpdate();
                            }
                            break;
                        }
                    case 5:
                        {
                            System.out.println("Am primit comanda 5.(CERERE FRIEND LIST)");
                            String sql = "Select User,Avatar from users where idUser In (SELECT idFriend FROM users u natural join friends f where u.idUser = ?) ;";
                            PreparedStatement statement = dbConnection.prepareStatement(sql);
                            statement.setInt(1,this.loggedUserId);
                            ResultSet result = statement.executeQuery();
                            
                            result.last();
                            output.writeInt(result.getRow());
                            
                            result.beforeFirst();
                            while(result.next()){
                                String friendName = result.getString("User");
                                output.writeUTF(friendName);
                                
                                if(result.getString("Avatar") != null){
                                    String avatarPath = result.getString("Avatar");
                                    byte[] imageInByte;
                                    BufferedImage originalImage = ImageIO.read(new File(avatarPath));
                                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                                        ImageIO.write(originalImage, "jpg", baos);
                                        baos.flush();
                                        imageInByte = baos.toByteArray();
                                    }

                                    int avatarLength = imageInByte.length;
                                    output.writeInt(avatarLength);
                                    output.write(imageInByte, 0, avatarLength);
                                    output.flush();
                                }
                                else {
                                    output.writeInt(0);
                                    output.flush();
                                }
                            }
                            break;
                        }
                    default:
                        break;
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
