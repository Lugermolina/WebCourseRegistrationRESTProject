package webCourseRegistrationREST;

public class Enrollment {

	private int studentId;
	private String studentName;
	private int sectionId;
	private String courseCode;
	private String status;
	private String message;
	private double tuitionCharged;
	private double newBalance;
	// new: day of the transaction, stored in Enrollment.out
	private String date;

	public Enrollment() {
		studentId = 0;
		studentName = "";
		sectionId = 0;
		courseCode = "";
		status = "";
		message = "";
		tuitionCharged = 0;
		newBalance = 0;
		date = "";
	}

	public Enrollment(int studentId, String studentName, int sectionId, String courseCode, String status,
			String message, double tuitionCharged, double newBalance) {
		this.studentId = studentId;
		this.studentName = studentName;
		this.sectionId = sectionId;
		this.courseCode = courseCode;
		this.status = status;
		this.message = message;
		this.tuitionCharged = tuitionCharged;
		this.newBalance = newBalance;
		date = "";
	}

	public int getStudentId() {
		return studentId;
	}

	public void setStudentId(int studentId) {
		this.studentId = studentId;
	}

	public String getStudentName() {
		return studentName;
	}

	public void setStudentName(String studentName) {
		this.studentName = studentName;
	}

	public int getSectionId() {
		return sectionId;
	}

	public void setSectionId(int sectionId) {
		this.sectionId = sectionId;
	}

	public String getCourseCode() {
		return courseCode;
	}

	public void setCourseCode(String courseCode) {
		this.courseCode = courseCode;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public double getTuitionCharged() {
		return tuitionCharged;
	}

	public void setTuitionCharged(double tuitionCharged) {
		this.tuitionCharged = tuitionCharged;
	}

	public double getNewBalance() {
		return newBalance;
	}

	public void setNewBalance(double newBalance) {
		this.newBalance = newBalance;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}
}
