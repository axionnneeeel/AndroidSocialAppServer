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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
            int optionValue = 0;

            while (moreData) {
                
                optionValue = input.read();
                if(optionValue == -1){
                    if(!this.loggedUser.isEmpty()){
                        String sql = "UPDATE users SET Loged=0 WHERE User =  ?";
                        PreparedStatement makeOffline = dbConnection.prepareStatement(sql);
                        makeOffline.setString(1,this.loggedUser);
                        makeOffline.executeUpdate();
                        Controller.onlineUsers.remove(this.loggedUser);
                    }
                        
                    break;
                }
                
                
                switch (optionValue) {
                    case 1:
                        {
                            System.out.println("Am primit comanda 1.(LOGIN)");
                            String username = input.readUTF();
                            String password = input.readUTF();
                            output.write(1);
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
                                sql = "UPDATE users SET Loged=1 WHERE User =  ?";
                                Controller.onlineUsers.put(this.loggedUser, socket);
                                
                                PreparedStatement makeOnline = dbConnection.prepareStatement(sql);
                                makeOnline.setString(1,this.loggedUser);
                                makeOnline.executeUpdate();
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
                            String sql = "Select distinct idFriend,User,Avatar,Loged from users join friends on users.idUser = friends.idFriend where users.idUser In (SELECT idFriend FROM users u natural join friends f where u.idUser = ?) and users.idUser = friends.idFriend ;";
                            PreparedStatement statement = dbConnection.prepareStatement(sql);
                            statement.setInt(1,this.loggedUserId);
                            ResultSet result = statement.executeQuery();
                            
                            result.last();
                            output.write(5);
                            output.writeInt(result.getRow());
                            
                            result.beforeFirst();
                            while(result.next()){
                                Integer friendId = result.getInt("idFriend");
                                output.writeInt(friendId);
                                
                                String friendName = result.getString("User");
                                output.writeUTF(friendName);
                                
                                Integer friendStatus = result.getInt("Loged");
                                output.writeInt(friendStatus);
                                
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
                    case 6:
                        {
                            System.out.println("Am primit comanda 6.(DELETE USER)");
                            Integer userToBeDeleted = input.readInt();
                            
                            String sql = "DELETE from friends WHERE idUser = ? AND idFriend = ? ";
                            PreparedStatement statement = dbConnection.prepareStatement(sql);
                            statement.setInt(1,this.loggedUserId);
                            statement.setInt(2,userToBeDeleted);
                            statement.executeUpdate();
                            
                            sql = "DELETE from friends WHERE idUser = ? AND idFriend = ? ";
                            PreparedStatement statement2 = dbConnection.prepareStatement(sql);
                            statement2.setInt(1,userToBeDeleted);
                            statement2.setInt(2,this.loggedUserId);
                            statement2.executeUpdate();
                            break;
                        }
                    case 7:
                        {
                            System.out.println("Am primit comanda 7.(ADD FRIEND LIST)");
                            String userToBeAdded = input.readUTF();
                            
                            String sql = "SELECT * from users WHERE User = ? ";
                            PreparedStatement statement = dbConnection.prepareStatement(sql);
                            statement.setString(1,userToBeAdded);
                            ResultSet result = statement.executeQuery();
                            if (!result.isBeforeFirst() ) {
                                output.writeInt(-1);
                                System.out.println("Nu am gasit userul dorit in lista de utilizatori.");
                            }
                            else {
                                result.next();
                                sql = "INSERT INTO friends(idUser,idFriend) VALUES (?,?) ";
                                PreparedStatement statementRegister = dbConnection.prepareStatement(sql);
                                statementRegister.setInt(1,this.loggedUserId);
                                statementRegister.setInt(2,result.getInt("idUser"));
                                statementRegister.executeUpdate();
                                
                                sql = "INSERT INTO friends(idFriend,idUser) VALUES (?,?) ";
                                PreparedStatement statementRegister2 = dbConnection.prepareStatement(sql);
                                statementRegister2.setInt(1,this.loggedUserId);
                                statementRegister2.setInt(2,result.getInt("idUser"));
                                statementRegister2.executeUpdate();
                                
                                
                                output.writeInt(1);
                            }
                            output.flush();
                            break;
                        }
                    case 8:
                        {
                            System.out.println("Am primit comanda 8.(CHAT)");
                            
                            String userToChat = input.readUTF();
                            
                            String bdSearch = this.loggedUser+"_"+userToChat;
                            String bdSearch2 = userToChat+"_"+this.loggedUser;
                            
                            String sql = "SELECT * from conversations WHERE usersname = ? or usersname = ?";
                            PreparedStatement statement = dbConnection.prepareStatement(sql);
                            statement.setString(1,bdSearch);
                            statement.setString(2, bdSearch2);
                            ResultSet result = statement.executeQuery();
                            if (!result.isBeforeFirst() ) {
                                output.writeInt(-1);
                                
                                sql = "INSERT INTO conversations(usersname,conversationPath) VALUES (?,?) ";
                                PreparedStatement statementAdd = dbConnection.prepareStatement(sql);
                                statementAdd.setString(1,this.loggedUser+"_"+userToChat);
                                BufferedWriter writer = null;
                                
                                File logFile = new File("E:\\SocialAppAvatars\\"+this.loggedUser+"_"+userToChat+".txt");

                                writer = new BufferedWriter(new FileWriter(logFile));
                                statementAdd.setString(2,logFile.getAbsolutePath());
                                statementAdd.executeUpdate();
                                writer.close();
                            }
                            else {
                                result.next();
                                String conversationPath = result.getString("conversationPath");
                                List<String> myConversation = new ArrayList<>();
                                try (BufferedReader br = new BufferedReader(new FileReader(conversationPath))) {
                                    String line;
                                    while ((line = br.readLine()) != null) {
                                       myConversation.add(line);
                                    }
                                }
                                
                                output.writeInt(myConversation.size());
                                for(String eachMessage : myConversation)
                                    output.writeUTF(eachMessage);
                            }       
                            output.flush();
                            break;
                        }
                    case 9:
                        {
                            System.out.println("Am primit comanda 9.(TRIMITERE MESAJ CHAT)");
                            
                            String userToChat = input.readUTF();
                            String message = input.readUTF();
                            
                            String bdSearch = this.loggedUser+"_"+userToChat;
                            String bdSearch2 = userToChat+"_"+this.loggedUser;
                            
                            String sql = "SELECT * from conversations WHERE usersname = ? or usersname = ?";
                            PreparedStatement statement = dbConnection.prepareStatement(sql);
                            statement.setString(1,bdSearch);
                            statement.setString(2, bdSearch2);
                            ResultSet result = statement.executeQuery();
                            
                            result.next();
                            String conversationPath = result.getString("conversationPath");
                            
                            BufferedWriter writer = null;
                            writer = new BufferedWriter(new FileWriter(conversationPath, true));
                            writer.write("\n");
                            writer.write(this.loggedUser+" "+message);
                            writer.close();
                            
                            /*for (Map.Entry<String, Socket> entry : Controller.onlineUsers.entrySet()) { 
                                if(entry.getKey().equals(userToChat)){
                                    System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
                                    DataOutputStream outputClient = new DataOutputStream(entry.getValue().getOutputStream());
                                    outputClient.writeUTF(message);
                                    outputClient.flush();
                                    
                                }
                            }*/
                            
                            break;
                        }
                    case 10:
                        {
                            System.out.println("Am primit comanda 10.(ACTUALIZARE CHAT)");
                            
                            String userToChat = input.readUTF();
                            Integer numberOfMessages = input.readInt();
                            
                            String bdSearch = this.loggedUser+"_"+userToChat;
                            String bdSearch2 = userToChat+"_"+this.loggedUser;
                            
                            String sql = "SELECT * from conversations WHERE usersname = ? or usersname = ?";
                            PreparedStatement statement = dbConnection.prepareStatement(sql);
                            statement.setString(1,bdSearch);
                            statement.setString(2, bdSearch2);
                            ResultSet result = statement.executeQuery();
                            
                            result.next();
                            String conversationPath = result.getString("conversationPath");
                            List<String> myConversation = new ArrayList<>();
                            try (BufferedReader br = new BufferedReader(new FileReader(conversationPath))) {
                                String line;
                                while ((line = br.readLine()) != null) {
                                   myConversation.add(line);
                                }
                            }

                            
                            output.writeInt(myConversation.size() - numberOfMessages);
                            if(myConversation.size() - numberOfMessages > 0){
                                for(int i=numberOfMessages;i<myConversation.size();i++)
                                    output.writeUTF(myConversation.get(i));
                            }
                               
                            output.flush();
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
