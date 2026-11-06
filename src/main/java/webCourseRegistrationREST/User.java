package webCourseRegistrationREST;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class User {

	// access levels, lowest to highest: each level can do everything the levels below it can
	public static final int STUDENT = 0;
	public static final int CLERK = 1;
	public static final int MANAGER = 2;
	public static final int ADMIN = 3;

	private int userId;
	private String username;
	private String passwordHash;
	private String role;
	private int studentId;

	public User() {
		userId = 0;
		username = "";
		passwordHash = "";
		role = "";
		studentId = 0;
	}

	public User(int userId, String username, String passwordHash, String role, int studentId) {
		this.userId = userId;
		this.username = username;
		this.passwordHash = passwordHash;
		this.role = role;
		this.studentId = studentId;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public int getStudentId() {
		return studentId;
	}

	public void setStudentId(int studentId) {
		this.studentId = studentId;
	}

	// turns the role text stored in the Users table into a number, so two roles can be compared with < and >=
	public int accessLevel() {
		if (role.equals("ADMIN")) {
			return ADMIN;
		}
		if (role.equals("MANAGER")) {
			return MANAGER;
		}
		if (role.equals("CLERK")) {
			return CLERK;
		}
		return STUDENT;
	}

	// a student account always works on its own record, whatever id was typed;
	// a staff account works on the student id it was given
	public int studentIdFor(int requestedId) {
		if (accessLevel() == STUDENT) {
			return studentId;
		}
		return requestedId;
	}

	// hashes a password with SHA-256, so the Users table never holds the real password.
	// The digest is 32 bytes; each byte is written as two hexadecimal characters.
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

	public boolean passwordMatches(String password) {
		return passwordHash.equals(hashPassword(password));
	}
}
