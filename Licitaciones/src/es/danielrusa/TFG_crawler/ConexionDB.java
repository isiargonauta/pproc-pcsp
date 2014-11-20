package es.danielrusa.TFG_crawler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.swing.JOptionPane;

public class ConexionDB {

	@SuppressWarnings("finally")
	public static Connection GetConnection() {
		Connection conexion = null;

		try {
			Class.forName("com.mysql.jdbc.Driver");

			String servidor = "jdbc:mysql://localhost:3306/licitaciones";
			String usuarioDB = "daniel";
			String passwordDB = "020202";
			// String usuarioDB = "root";
			// String passwordDB = "labje238!v2";
			conexion = DriverManager.getConnection(servidor, usuarioDB,
					passwordDB);
		} catch (ClassNotFoundException ex) {
			JOptionPane.showMessageDialog(null, ex,
					"Error1 en la Conexi�n con la BD " + ex.getMessage(),
					JOptionPane.ERROR_MESSAGE);
			conexion = null;
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(null, ex,
					"Error2 en la Conexi�n con la BD " + ex.getMessage(),
					JOptionPane.ERROR_MESSAGE);
			conexion = null;
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, ex,
					"Error3 en la Conexi�n con la BD " + ex.getMessage(),
					JOptionPane.ERROR_MESSAGE);
			conexion = null;
		} finally {
			return conexion;
		}
	}
}
