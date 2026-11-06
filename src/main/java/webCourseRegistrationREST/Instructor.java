package webCourseRegistrationREST;

public class Instructor {

	private int instructorId;
	private String firstName;
	private String lastName;
	private String email;
	private int departmentId;

	public Instructor() {
		instructorId = 0;
		firstName = "";
		lastName = "";
		email = "";
		departmentId = 0;
	}

	public Instructor(int instructorId, String firstName, String lastName, String email, int departmentId) {
		this.instructorId = instructorId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.departmentId = departmentId;
	}

	public int getInstructorId() {
		return instructorId;
	}

	public void setInstructorId(int instructorId) {
		this.instructorId = instructorId;
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

	public int getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(int departmentId) {
		this.departmentId = departmentId;
	}

	public String fullName() {
		return firstName + " " + lastName;
	}
}
