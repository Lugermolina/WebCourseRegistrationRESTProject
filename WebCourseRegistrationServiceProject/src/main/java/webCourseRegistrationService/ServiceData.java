package webCourseRegistrationService;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

// Reads the data shared with the REST project: the accounts, courses and departments from SQL Server,
// the students from Student.in in the REST project's data folder. The SOAP service only reads.
public class ServiceData {

	private static final String DATA_PATH = "C:\\VanierSegment4\\WebServices\\WebCourseRegistrationRESTProject I3\\data\\";

	// same connection string as the REST project (Lab 6 style)
	private static final String CONNECTION_STRING = "jdbc:sqlserver://localhost;"
			+ "databaseName=CourseRegistrationdb;"
			+ "integratedSecurity=true;"
			+ "connectTimeout=30;"
			+ "encrypt=true;"
			+ "trustServerCertificate=true;"
			+ "applicationIntent=ReadWrite;"
			+ "multiSubnetFailover=false;"
			+ "commandTimeout=30;";

	// same SHA-256 hash as User.hashPassword in the REST project, so both compare the same stored hash
	public static String hashPassword(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] bytes = digest.digest(password.getBytes("UTF-8"));
			String hash = "";
			for (int i = 0; i < bytes.length; i++) {
				hash += String.format("%02x", bytes[i]);
			}
			return hash;
		} catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
			return "";
		}
	}

	// Returns { role, studentid } of the account whose username and password match, or null.
	public static String[] findAccount(String username, String password) throws SQLException {
		Connection connection = DriverManager.getConnection(CONNECTION_STRING);

		String sql = "SELECT role, studentid FROM Users WHERE username = ? AND passwordhash = ?";
		PreparedStatement pstm = connection.prepareStatement(sql);
		pstm.setString(1, username);
		pstm.setString(2, hashPassword(password));
		ResultSet rs = pstm.executeQuery();

		boolean found = false;
		String[] account = new String[2];
		while (rs.next()) {
			account[0] = rs.getString("role");
			account[1] = "" + rs.getInt("studentid");
			found = true;
		}
		connection.close();

		if (!found) {
			return null;
		}
		return account;
	}

	// updated: the courses come from the Course table of SQL Server instead of Course.in
	public static ArrayList<Course> readCourses() throws SQLException {
		ArrayList<Course> courseList = new ArrayList<Course>();
		Connection connection = DriverManager.getConnection(CONNECTION_STRING);

		String sql = "SELECT courseid, coursecode, coursename, credits, departmentid FROM Course ORDER BY courseid";
		PreparedStatement pstm = connection.prepareStatement(sql);
		ResultSet rs = pstm.executeQuery();

		while (rs.next()) {
			courseList.add(new Course(rs.getInt("courseid"), rs.getString("coursecode"), rs.getString("coursename"),
					rs.getInt("credits"), rs.getInt("departmentid")));
		}
		connection.close();
		return courseList;
	}

	// updated: the name comes from the Department table of SQL Server instead of Department.in
	public static String departmentName(int departmentId) throws SQLException {
		String name = "";
		Connection connection = DriverManager.getConnection(CONNECTION_STRING);

		String sql = "SELECT departmentname FROM Department WHERE departmentid = ?";
		PreparedStatement pstm = connection.prepareStatement(sql);
		pstm.setInt(1, departmentId);
		ResultSet rs = pstm.executeQuery();

		if (rs.next()) {
			name = rs.getString("departmentname");
		}
		connection.close();
		return name;
	}

	// Returns the fields of one line of Student.in, or null. The balance is the last field (index 7).
	public static String[] findStudent(int studentId) throws FileNotFoundException {
		String[] student = null;
		String line = "";
		String[] fields;

		Scanner inFile = new Scanner(new FileReader(DATA_PATH + "Student.in"));
		while (inFile.hasNextLine()) {
			line = inFile.nextLine();
			fields = line.split("\t");
			if (Integer.parseInt(fields[0]) == studentId) {
				student = fields;
			}
		}
		inFile.close();
		return student;
	}
}
