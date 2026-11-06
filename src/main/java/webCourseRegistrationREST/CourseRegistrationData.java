package webCourseRegistrationREST;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

public class CourseRegistrationData {

	// updated: the tables now live in the data folder of the project, outside src.
	// The service writes to them, and a file changing inside src makes Eclipse republish the application.
   	private static final String DATA_PATH = "C:\\VanierSegment4\\WebServices\\WebCourseRegistrationRESTProject I3\\data\\";

	// new: connection to the SQL Server database that holds the login accounts (Users table),
	// the same connection string as Lab 6 with the database CourseRegistrationdb
	private static final String CONNECTION_STRING = "jdbc:sqlserver://localhost;"
			+ "databaseName=CourseRegistrationdb;"
			+ "integratedSecurity=true;"
			+ "connectTimeout=30;"
			+ "encrypt=true;"
			+ "trustServerCertificate=true;"
			+ "applicationIntent=ReadWrite;"
			+ "multiSubnetFailover=false;"
			+ "commandTimeout=30;";

	public static ArrayList<Department> departmentList;
	public static ArrayList<Instructor> instructorList;
	public static ArrayList<Course> courseList;
	public static ArrayList<Section> sectionList;
	public static ArrayList<Student> studentList;
	public static ArrayList<Enrollment> enrollmentList;
	public static ArrayList<Payment> paymentList;
	// updated: the login accounts, read from the Users table of SQL Server
	public static ArrayList<User> userList;

	// pattern (Singleton idea): one set of lists for the whole application. The first request loads the files;
	// every later request finds courseList already filled and returns at once.
	// updated: the two .out tables are loaded as well, so reservations and payments survive a restart.
	// updated: the accounts now come from SQL Server (loadUsers) instead of User.in.
	// updated: the departments and courses come from SQL Server too (loadDepartments, loadCourses).
	public static void loadData() throws FileNotFoundException {

		if (courseList != null) {
			return;
		}

		departmentList = new ArrayList<Department>();
		instructorList = new ArrayList<Instructor>();
		courseList = new ArrayList<Course>();
		sectionList = new ArrayList<Section>();
		studentList = new ArrayList<Student>();
		enrollmentList = new ArrayList<Enrollment>();
		paymentList = new ArrayList<Payment>();
		userList = new ArrayList<User>();

		String line = "";
		String[] fields;

		loadDepartments(); // updated: read from SQL Server instead of Department.in

		Scanner inFile = new Scanner(new FileReader(DATA_PATH + "Instructor.in"));
		while (inFile.hasNextLine()) {
			line = inFile.nextLine();
			fields = line.split("\t");
			instructorList.add(new Instructor(Integer.parseInt(fields[0]), fields[1], fields[2], fields[3],
					Integer.parseInt(fields[4])));
		}
		inFile.close();

		loadCourses(); // updated: read from SQL Server instead of Course.in

		inFile = new Scanner(new FileReader(DATA_PATH + "Section.in"));
		while (inFile.hasNextLine()) {
			line = inFile.nextLine();
			fields = line.split("\t");
			sectionList.add(new Section(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]),
					Integer.parseInt(fields[2]), fields[3], Integer.parseInt(fields[4]), fields[5], fields[6],
					Integer.parseInt(fields[7]), Integer.parseInt(fields[8])));
		}
		inFile.close();

		inFile = new Scanner(new FileReader(DATA_PATH + "Student.in"));
		while (inFile.hasNextLine()) {
			line = inFile.nextLine();
			fields = line.split("\t");
			studentList.add(new Student(Integer.parseInt(fields[0]), fields[1], fields[2], fields[3], fields[4],
					fields[5], fields[6], Double.parseDouble(fields[7])));
		}
		inFile.close();

		// updated: login accounts, read from SQL Server
		loadUsers();

		// new: the .out tables exist only after the first reservation or payment, so they are read when present.
		// Fields of Enrollment.out: studentId, studentName, sectionId, courseCode, tuitionCharged, newBalance, date
		if (new File(DATA_PATH + "Enrollment.out").exists()) {
			inFile = new Scanner(new FileReader(DATA_PATH + "Enrollment.out"));
			while (inFile.hasNextLine()) {
				line = inFile.nextLine();
				fields = line.split("\t");
				Enrollment e = new Enrollment(Integer.parseInt(fields[0]), fields[1], Integer.parseInt(fields[2]),
						fields[3], "CONFIRMED", "", Double.parseDouble(fields[4]), Double.parseDouble(fields[5]));
				e.setDate(fields[6]);
				enrollmentList.add(e);
			}
			inFile.close();
		}

		// Fields of Payment.out: receiptNo, studentId, studentName, amountPaid, paymentMethod, newBalance, date
		if (new File(DATA_PATH + "Payment.out").exists()) {
			inFile = new Scanner(new FileReader(DATA_PATH + "Payment.out"));
			while (inFile.hasNextLine()) {
				line = inFile.nextLine();
				fields = line.split("\t");
				Payment p = new Payment(fields[0], Integer.parseInt(fields[1]), fields[2],
						Double.parseDouble(fields[3]), fields[4], "PAID", "", Double.parseDouble(fields[5]));
				p.setDate(fields[6]);
				paymentList.add(p);
			}
			inFile.close();
		}
	}

	// new: amounts are written with two decimals and a period (Locale.US), whatever the regional settings
	// of the computer, so Double.parseDouble can always read them back
	private static String amount(double value) {
		return String.format(Locale.US, "%.2f", value);
	}

	// new: the save methods below write a whole list back to its table, one record per line, fields separated
	// by a tab, in the same order loadData reads them. Each one is called right after its list changes.
	// new: reads the Department table into departmentList (it replaces Department.in)
	private static void loadDepartments() {
		try {
			Connection connection = DriverManager.getConnection(CONNECTION_STRING);

			String sql = "SELECT departmentid, departmentname, departmenthead FROM Department ORDER BY departmentid";
			PreparedStatement pstm = connection.prepareStatement(sql);
			ResultSet rs = pstm.executeQuery();

			while (rs.next()) {
				departmentList.add(new Department(rs.getInt("departmentid"), rs.getString("departmentname"),
						rs.getString("departmenthead")));
			}
			connection.close();
		} catch (SQLException e) {
			System.out.println("Department table not read: " + e.getMessage());
		}
	}

	// new: the three methods below replace saveDepartments; each change touches one row of the Department table
	public static void insertDepartment(Department d) {
		try {
			Connection connection = DriverManager.getConnection(CONNECTION_STRING);

			String sql = "INSERT INTO Department (departmentid, departmentname, departmenthead) VALUES (?, ?, ?)";
			PreparedStatement pstm = connection.prepareStatement(sql);
			pstm.setInt(1, d.getDepartmentId());
			pstm.setString(2, d.getDepartmentName());
			pstm.setString(3, d.getDepartmentHead());
			pstm.executeUpdate();

			connection.close();
		} catch (SQLException e) {
			System.out.println("Department not inserted: " + e.getMessage());
		}
	}

	// new
	public static void updateDepartment(Department d) {
		try {
			Connection connection = DriverManager.getConnection(CONNECTION_STRING);

			String sql = "UPDATE Department SET departmentname = ?, departmenthead = ? WHERE departmentid = ?";
			PreparedStatement pstm = connection.prepareStatement(sql);
			pstm.setString(1, d.getDepartmentName());
			pstm.setString(2, d.getDepartmentHead());
			pstm.setInt(3, d.getDepartmentId());
			pstm.executeUpdate();

			connection.close();
		} catch (SQLException e) {
			System.out.println("Department not updated: " + e.getMessage());
		}
	}

	// new
	public static void deleteDepartment(int departmentId) {
		try {
			Connection connection = DriverManager.getConnection(CONNECTION_STRING);

			String sql = "DELETE FROM Department WHERE departmentid = ?";
			PreparedStatement pstm = connection.prepareStatement(sql);
			pstm.setInt(1, departmentId);
			pstm.executeUpdate();

			connection.close();
		} catch (SQLException e) {
			System.out.println("Department not deleted: " + e.getMessage());
		}
	}

	// new: reads the Course table into courseList (it replaces Course.in)
	private static void loadCourses() {
		try {
			Connection connection = DriverManager.getConnection(CONNECTION_STRING);

			String sql = "SELECT courseid, coursecode, coursename, credits, departmentid FROM Course ORDER BY courseid";
			PreparedStatement pstm = connection.prepareStatement(sql);
			ResultSet rs = pstm.executeQuery();

			while (rs.next()) {
				courseList.add(new Course(rs.getInt("courseid"), rs.getString("coursecode"), rs.getString("coursename"),
						rs.getInt("credits"), rs.getInt("departmentid")));
			}
			connection.close();
		} catch (SQLException e) {
			System.out.println("Course table not read: " + e.getMessage());
		}
	}

	// new: the three methods below replace saveCourses; each change touches one row of the Course table
	public static void insertCourse(Course c) {
		try {
			Connection connection = DriverManager.getConnection(CONNECTION_STRING);

			String sql = "INSERT INTO Course (courseid, coursecode, coursename, credits, departmentid) VALUES (?, ?, ?, ?, ?)";
			PreparedStatement pstm = connection.prepareStatement(sql);
			pstm.setInt(1, c.getCourseId());
			pstm.setString(2, c.getCourseCode());
			pstm.setString(3, c.getCourseName());
			pstm.setInt(4, c.getCredits());
			pstm.setInt(5, c.getDepartmentId());
			pstm.executeUpdate();

			connection.close();
		} catch (SQLException e) {
			System.out.println("Course not inserted: " + e.getMessage());
		}
	}

	// new
	public static void updateCourse(Course c) {
		try {
			Connection connection = DriverManager.getConnection(CONNECTION_STRING);

			String sql = "UPDATE Course SET coursecode = ?, coursename = ?, credits = ?, departmentid = ? WHERE courseid = ?";
			PreparedStatement pstm = connection.prepareStatement(sql);
			pstm.setString(1, c.getCourseCode());
			pstm.setString(2, c.getCourseName());
			pstm.setInt(3, c.getCredits());
			pstm.setInt(4, c.getDepartmentId());
			pstm.setInt(5, c.getCourseId());
			pstm.executeUpdate();

			connection.close();
		} catch (SQLException e) {
			System.out.println("Course not updated: " + e.getMessage());
		}
	}

	// new
	public static void deleteCourse(int courseId) {
		try {
			Connection connection = DriverManager.getConnection(CONNECTION_STRING);

			String sql = "DELETE FROM Course WHERE courseid = ?";
			PreparedStatement pstm = connection.prepareStatement(sql);
			pstm.setInt(1, courseId);
			pstm.executeUpdate();

			connection.close();
		} catch (SQLException e) {
			System.out.println("Course not deleted: " + e.getMessage());
		}
	}

	public static void saveInstructors() throws IOException {
		PrintWriter outFile = new PrintWriter(new FileWriter(DATA_PATH + "Instructor.in"));
		for (int i = 0; i < instructorList.size(); i++) {
			Instructor ins = instructorList.get(i);
			outFile.println(ins.getInstructorId() + "\t" + ins.getFirstName() + "\t" + ins.getLastName() + "\t"
					+ ins.getEmail() + "\t" + ins.getDepartmentId());
		}
		outFile.close();
	}

	public static void saveSections() throws IOException {
		PrintWriter outFile = new PrintWriter(new FileWriter(DATA_PATH + "Section.in"));
		for (int i = 0; i < sectionList.size(); i++) {
			Section s = sectionList.get(i);
			outFile.println(s.getSectionId() + "\t" + s.getCourseId() + "\t" + s.getInstructorId() + "\t"
					+ s.getSemester() + "\t" + s.getYear() + "\t" + s.getSchedule() + "\t" + s.getRoom() + "\t"
					+ s.getCapacity() + "\t" + s.getEnrolled());
		}
		outFile.close();
	}

	public static void saveStudents() throws IOException {
		PrintWriter outFile = new PrintWriter(new FileWriter(DATA_PATH + "Student.in"));
		for (int i = 0; i < studentList.size(); i++) {
			Student st = studentList.get(i);
			outFile.println(st.getStudentId() + "\t" + st.getFirstName() + "\t" + st.getLastName() + "\t"
					+ st.getEmail() + "\t" + st.getPhone() + "\t" + st.getAddress() + "\t"
					+ st.getEnrollmentDate() + "\t" + amount(st.getBalance()));
		}
		outFile.close();
	}

	// new: reads the Users table into userList. Columns: userid, username, passwordhash, role,
	// studentid (0 for staff). A database error leaves the list empty and is written to the GlassFish log.
	private static void loadUsers() {
		try {
			Connection connection = DriverManager.getConnection(CONNECTION_STRING);

			String sql = "SELECT userid, username, passwordhash, role, studentid FROM Users ORDER BY userid";
			PreparedStatement pstm = connection.prepareStatement(sql);
			ResultSet rs = pstm.executeQuery();

			while (rs.next()) {
				userList.add(new User(rs.getInt("userid"), rs.getString("username"), rs.getString("passwordhash"),
						rs.getString("role"), rs.getInt("studentid")));
			}
			connection.close();
		} catch (SQLException e) {
			System.out.println("Users table not read: " + e.getMessage());
		}
	}

	// new: the three methods below replace saveUsers. Each change touches one row of the Users table,
	// instead of rewriting the whole User.in file. They are called right after userList changes.
	public static void insertUser(User u) {
		try {
			Connection connection = DriverManager.getConnection(CONNECTION_STRING);

			String sql = "INSERT INTO Users (userid, username, passwordhash, role, studentid) VALUES (?, ?, ?, ?, ?)";
			PreparedStatement pstm = connection.prepareStatement(sql);
			pstm.setInt(1, u.getUserId());
			pstm.setString(2, u.getUsername());
			pstm.setString(3, u.getPasswordHash());
			pstm.setString(4, u.getRole());
			pstm.setInt(5, u.getStudentId());
			pstm.executeUpdate();

			connection.close();
		} catch (SQLException e) {
			System.out.println("Account not inserted: " + e.getMessage());
		}
	}

	// new: the username changes with a student's email; the role and the password hash change from the
	// account form
	public static void updateUser(User u) {
		try {
			Connection connection = DriverManager.getConnection(CONNECTION_STRING);

			String sql = "UPDATE Users SET username = ?, passwordhash = ?, role = ? WHERE userid = ?";
			PreparedStatement pstm = connection.prepareStatement(sql);
			pstm.setString(1, u.getUsername());
			pstm.setString(2, u.getPasswordHash());
			pstm.setString(3, u.getRole());
			pstm.setInt(4, u.getUserId());
			pstm.executeUpdate();

			connection.close();
		} catch (SQLException e) {
			System.out.println("Account not updated: " + e.getMessage());
		}
	}

	// new
	public static void deleteUser(int userId) {
		try {
			Connection connection = DriverManager.getConnection(CONNECTION_STRING);

			String sql = "DELETE FROM Users WHERE userid = ?";
			PreparedStatement pstm = connection.prepareStatement(sql);
			pstm.setInt(1, userId);
			pstm.executeUpdate();

			connection.close();
		} catch (SQLException e) {
			System.out.println("Account not deleted: " + e.getMessage());
		}
	}

	public static void saveEnrollments() throws IOException {
		PrintWriter outFile = new PrintWriter(new FileWriter(DATA_PATH + "Enrollment.out"));
		for (int i = 0; i < enrollmentList.size(); i++) {
			Enrollment e = enrollmentList.get(i);
			outFile.println(e.getStudentId() + "\t" + e.getStudentName() + "\t" + e.getSectionId() + "\t"
					+ e.getCourseCode() + "\t" + amount(e.getTuitionCharged()) + "\t" + amount(e.getNewBalance())
					+ "\t" + e.getDate());
		}
		outFile.close();
	}

	public static void savePayments() throws IOException {
		PrintWriter outFile = new PrintWriter(new FileWriter(DATA_PATH + "Payment.out"));
		for (int i = 0; i < paymentList.size(); i++) {
			Payment p = paymentList.get(i);
			outFile.println(p.getReceiptNo() + "\t" + p.getStudentId() + "\t" + p.getStudentName() + "\t"
					+ amount(p.getAmountPaid()) + "\t" + p.getPaymentMethod() + "\t" + amount(p.getNewBalance())
					+ "\t" + p.getDate());
		}
		outFile.close();
	}

	public static Department findDepartment(int departmentId) {
		for (int i = 0; i < departmentList.size(); i++) {
			if (departmentList.get(i).getDepartmentId() == departmentId) {
				return departmentList.get(i);
			}
		}
		return null;
	}

	public static Instructor findInstructor(int instructorId) {
		for (int i = 0; i < instructorList.size(); i++) {
			if (instructorList.get(i).getInstructorId() == instructorId) {
				return instructorList.get(i);
			}
		}
		return null;
	}

	public static Course findCourse(int courseId) {
		for (int i = 0; i < courseList.size(); i++) {
			if (courseList.get(i).getCourseId() == courseId) {
				return courseList.get(i);
			}
		}
		return null;
	}

	public static Course findCourseByCode(String courseCode) {
		for (int i = 0; i < courseList.size(); i++) {
			if (courseList.get(i).getCourseCode().equalsIgnoreCase(courseCode)) {
				return courseList.get(i);
			}
		}
		return null;
	}

	public static Section findSection(int sectionId) {
		for (int i = 0; i < sectionList.size(); i++) {
			if (sectionList.get(i).getSectionId() == sectionId) {
				return sectionList.get(i);
			}
		}
		return null;
	}

	public static Student findStudent(int studentId) {
		for (int i = 0; i < studentList.size(); i++) {
			if (studentList.get(i).getStudentId() == studentId) {
				return studentList.get(i);
			}
		}
		return null;
	}

	// new
	public static User findUser(int userId) {
		for (int i = 0; i < userList.size(); i++) {
			if (userList.get(i).getUserId() == userId) {
				return userList.get(i);
			}
		}
		return null;
	}

	// new: used at login and to refuse a username that is already taken
	public static User findUserByName(String username) {
		for (int i = 0; i < userList.size(); i++) {
			if (userList.get(i).getUsername().equalsIgnoreCase(username)) {
				return userList.get(i);
			}
		}
		return null;
	}
}
