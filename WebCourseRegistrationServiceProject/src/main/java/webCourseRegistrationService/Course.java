package webCourseRegistrationService;

// the course record returned by findCourse; same fields as the Course table and as the REST project's Course
public class Course {

	private int courseId;
	private String courseCode;
	private String courseName;
	private int credits;
	private int departmentId;

	public Course() {
		courseId = 0;
		courseCode = "";
		courseName = "";
		credits = 0;
		departmentId = 0;
	}

	public Course(int courseId, String courseCode, String courseName, int credits, int departmentId) {
		this.courseId = courseId;
		this.courseCode = courseCode;
		this.courseName = courseName;
		this.credits = credits;
		this.departmentId = departmentId;
	}

	public int getCourseId() {
		return courseId;
	}

	public void setCourseId(int courseId) {
		this.courseId = courseId;
	}

	public String getCourseCode() {
		return courseCode;
	}

	public void setCourseCode(String courseCode) {
		this.courseCode = courseCode;
	}

	public String getCourseName() {
		return courseName;
	}

	public void setCourseName(String courseName) {
		this.courseName = courseName;
	}

	public int getCredits() {
		return credits;
	}

	public void setCredits(int credits) {
		this.credits = credits;
	}

	public int getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(int departmentId) {
		this.departmentId = departmentId;
	}
}
