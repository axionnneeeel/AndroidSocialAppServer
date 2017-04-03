package androidsocialappserver;

import com.mysql.jdbc.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;


public class MySqlConnectionModel {

    Connection connection;
    ResultSet resultSet;

    public MySqlConnectionModel(){

    }

    public boolean connect(String username, String password) throws SQLException, ClassNotFoundException{
        Class.forName("com.mysql.jdbc.Driver");
        connection = (Connection) DriverManager.getConnection("jdbc:mysql://localhost:3306/appdb", username, password);
        return connection != null;
    }

    public Connection getConection(){
        return connection;
    }

}