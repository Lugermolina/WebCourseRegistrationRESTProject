package webCourseRegistrationREST;

public class Student {

	private int studentId;
	private String firstName;
	private String lastName;
	private String email;
	private String phone;
	private String address;
	private String enrollmentDate;
	private double balance;

	public Student() {
		studentId = 0;
		firstName = "";
		lastName = "";
		email = "";
		phone = "";
		address = "";
		enrollmentDate = "";
		balance = 0;
	}

	public Student(int studentId, String firstName, String lastName, String email, String phone, String address, String enrollmentDate, double balance) {
		this.studentId = studentId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.phone = phone;
		this.address = address;
		this.enrollmentDate = enrollmentDate;
		this.balance = balance;
	}

	public int getStudentId() {
		return studentId;
	}

	public void setStudentId(int studentId) {
		this.studentId = studentId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getEnrollmentDate() {
		return enrollmentDate;
	}

	public void setEnrollmentDate(String enrollmentDate) {
		this.enrollmentDate = enrollmentDate;
	}

	public double getBalance() {
		return balance;
	}

	public void setBalance(double balance) {
		this.balance = balance;
	}

	public String fullName() {
		return firstName + " " + lastName;
	}

	public void chargeTuition(double amount) {
		balance += amount;
	}

	public void applyPayment(double amount) {
		balance -= amount;
	}
}
