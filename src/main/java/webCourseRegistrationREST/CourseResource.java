package webCourseRegistrationREST;

import java.io.IOException;
import java.util.ArrayList;

import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("Course")
public class CourseResource {

	private String departmentName(Course c) {
		Department d = CourseRegistrationData.findDepartment(c.getDepartmentId());
		return (d == null) ? "" : d.getDepartmentName();
	}

	// true when the course has at least one section in the given semester (e.g. "Fall")
	private boolean offeredIn(Course c, String semester) {
		ArrayList<Section> sectionList = CourseRegistrationData.sectionList;
		for (int i = 0; i < sectionList.size(); i++) {
			Section s = sectionList.get(i);
			if (s.getCourseId() == c.getCourseId() && s.getSemester().equalsIgnoreCase(semester)) {
				return true;
			}
		}
		return false;
	}

	// true when the keyword appears in the course code, course name, or the name of an instructor teaching it
	private boolean matchesKeyword(Course c, String keyword) {
		String k = keyword.toLowerCase();
		if (c.getCourseCode().toLowerCase().contains(k) || c.getCourseName().toLowerCase().contains(k)) {
			return true;
		}
		ArrayList<Section> sectionList = CourseRegistrationData.sectionList;
		for (int i = 0; i < sectionList.size(); i++) {
			Section s = sectionList.get(i);
			if (s.getCourseId() == c.getCourseId()) {
				Instructor ins = CourseRegistrationData.findInstructor(s.getInstructorId());
				if (ins != null && ins.fullName().toLowerCase().contains(k)) {
					return true;
				}
			}
		}
		return false;
	}

	// new: the search itself, shared by the JSON and the HTML versions of /search
	private ArrayList<Course> searchByKeyword(String keyword) {
		ArrayList<Course> courseList = CourseRegistrationData.courseList;
		ArrayList<Course> result = new ArrayList<Course>();

		if (keyword == null || keyword.trim().isEmpty()) {
			return result;
		}

		for (int i = 0; i < courseList.size(); i++) {
			if (matchesKeyword(courseList.get(i), keyword.trim())) {
				result.add(courseList.get(i));
			}
		}

		return result;
	}

	// new
	private int nextCourseId() {
		ArrayList<Course> courseList = CourseRegistrationData.courseList;
		int maxId = 0;
		for (int i = 0; i < courseList.size(); i++) {
			if (courseList.get(i).getCourseId() > maxId) {
				maxId = courseList.get(i).getCourseId();
			}
		}
		return maxId + 1;
	}

	private String buildHTMLTable(String heading, ArrayList<Course> list) {
		String result = "<h2>" + heading + "</h2>";
		result += "<table border=\"1\"><tr><th class=\"num\">Course Id</th><th>Code</th><th>Name</th>"
				+ "<th>Department</th><th class=\"num\">Credits</th><th class=\"num\">Tuition Fee</th></tr>";
		for (int i = 0; i < list.size(); i++) {
			Course c = list.get(i);
			result += "<tr><td class=\"num\">" + c.getCourseId() + "</td><td>" + c.getCourseCode() + "</td>"
					+ "<td>" + c.getCourseName() + "</td><td>" + departmentName(c) + "</td>"
					+ "<td class=\"num\">" + c.getCredits() + "</td><td class=\"num\">" + Page.money(c.tuitionFee())
					+ "</td></tr>";
		}
		result += "</table>";
		result += "<h3>Courses found: " + list.size() + "</h3>";
		return result;
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLCourseInfo() throws IOException {

		CourseRegistrationData.loadData();

		String result = Page.open("Course Catalog REST Resource");
		result += buildHTMLTable("Course Catalog", CourseRegistrationData.courseList);
		result += Page.close();

		return result;
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String displayTextCourseInfo() throws IOException {

		CourseRegistrationData.loadData();
		ArrayList<Course> courseList = CourseRegistrationData.courseList;

		String result = "Course Catalog\n";
		for (int i = 0; i < courseList.size(); i++) {
			Course c = courseList.get(i);
			result += "course_id: " + c.getCourseId() + ", course_code: " + c.getCourseCode()
					+ ", course_name: " + c.getCourseName() + ", department: " + departmentName(c)
					+ ", credits: " + c.getCredits()
					+ ", tuition_fee: " + String.format("%.2f", c.tuitionFee()) + "$\n";
		}

		return result;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public ArrayList<Course> displayJSONCourseInfo() throws IOException {

		CourseRegistrationData.loadData();
		return CourseRegistrationData.courseList;
	}

	// Business process 1: display list of courses by category (department)
	@GET
	@Path("/byDepartment/{departmentParam}")
	@Produces(MediaType.APPLICATION_JSON)
	public ArrayList<Course> displayJSONCoursesByDepartment(@PathParam("departmentParam") String departmentName)
			throws IOException {

		CourseRegistrationData.loadData();
		ArrayList<Course> courseList = CourseRegistrationData.courseList;
		ArrayList<Course> result = new ArrayList<Course>();

		for (int i = 0; i < courseList.size(); i++) {
			if (departmentName(courseList.get(i)).equalsIgnoreCase(departmentName)) {
				result.add(courseList.get(i));
			}
		}

		return result;
	}

	// Business process 1: display list of courses by category (department and/or semester)
	@GET
	@Path("/byCategory")
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLCoursesByCategory(@QueryParam("departmentParam") String departmentName,
			@QueryParam("semesterParam") String semester) throws IOException {

		CourseRegistrationData.loadData();
		ArrayList<Course> courseList = CourseRegistrationData.courseList;
		ArrayList<Course> result = new ArrayList<Course>();

		for (int i = 0; i < courseList.size(); i++) {
			Course c = courseList.get(i);
			boolean departmentOk = (departmentName == null || departmentName.isEmpty()
					|| departmentName(c).equalsIgnoreCase(departmentName));
			boolean semesterOk = (semester == null || semester.isEmpty() || offeredIn(c, semester));
			if (departmentOk && semesterOk) {
				result.add(c);
			}
		}

		String heading = "Courses - Department: "
				+ (departmentName == null || departmentName.isEmpty() ? "All" : Validation.clean(departmentName))
				+ ", Semester: " + (semester == null || semester.isEmpty() ? "All" : Validation.clean(semester));

		return Page.open("Course Catalog REST Resource") + buildHTMLTable(heading, result) + Page.close();
	}

	// Business process 2: search for a given course by code
	@GET
	@Path("/searchCourse/{courseCodeParam}")
	@Produces(MediaType.APPLICATION_JSON)
	public Course searchJSONCourseInfo(@PathParam("courseCodeParam") String courseCode) throws IOException {

		CourseRegistrationData.loadData();
		return CourseRegistrationData.findCourseByCode(courseCode);
	}

	// Business process 2: search for a course by code, title, or instructor
	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public ArrayList<Course> searchJSONCoursesByKeyword(@QueryParam("keywordParam") String keyword)
			throws IOException {

		CourseRegistrationData.loadData();
		return searchByKeyword(keyword);
	}

	// new: same search as searchJSONCoursesByKeyword, as an HTML table. A browser asks for text/html,
	// so the search form now receives a page; Postman and the Java client still receive JSON.
	@GET
	@Path("/search")
	@Produces(MediaType.TEXT_HTML)
	public String searchHTMLCoursesByKeyword(@QueryParam("keywordParam") String keyword) throws IOException {

		CourseRegistrationData.loadData();

		return Page.open("Course Catalog REST Resource")
				+ buildHTMLTable("Search results for: " + Validation.clean(keyword), searchByKeyword(keyword))
				+ Page.close();
	}

	// new: the course form with its fields filled from the record, ready for Update or Delete
	private String buildHTMLForm(Course c) {
		return Page.formOpen("Course")
				+ Page.field("Course Id:", "courseIdParam", "" + c.getCourseId())
				+ Page.field("Course code:", "courseCodeParam", c.getCourseCode())
				+ Page.field("Course name:", "courseNameParam", c.getCourseName())
				+ Page.field("Credits:", "creditsParam", "" + c.getCredits())
				+ Page.field("Department Id:", "departmentIdParam", "" + c.getDepartmentId())
				+ Page.formClose("Course", "Course");
	}

	// new
	private String buildHTMLResult(String heading, Course c) {
		ArrayList<Course> result = new ArrayList<Course>();
		result.add(c);
		return Page.open("Course Catalog REST Resource") + buildHTMLTable(heading, result)
				+ "<p><a href=\"" + Page.APP + "/rest/Section/byCourse/" + c.getCourseCode()
				+ "\">Sections of this course</a></p>" + buildHTMLForm(c) + Page.close();
	}

	// new: rules shared by Add and Update. Returns the reason of the refusal, or an empty text when the
	// fields are accepted. ignoreId is the record being updated, so it is not compared with itself (0 on Add).
	private String checkCourse(String courseCode, String courseName, int credits, int departmentId, int ignoreId) {

		if (Validation.isEmpty(courseCode) || Validation.isEmpty(courseName)) {
			return "Course code and course name are both required.";
		}
		// a course code is two letters followed by three digits, like SE301
		if (!courseCode.matches("[A-Z]{2}\\d{3}")) {
			return "The course code " + courseCode + " is not valid. Use two letters and three digits, e.g. SE301.";
		}
		Course sameCode = CourseRegistrationData.findCourseByCode(courseCode);
		if (sameCode != null && sameCode.getCourseId() != ignoreId) {
			return "The course code " + courseCode + " already exists.";
		}
		if (credits < 1 || credits > 6) {
			return "The credits must be a whole number between 1 and 6.";
		}
		if (CourseRegistrationData.findDepartment(departmentId) == null) {
			return "The department was not found. Enter the id of an existing department.";
		}
		return "";
	}

	// new: Search button of the course form: reads the record from the Course table and returns the form
	// filled with it
	@POST
	@Path("/find")
	@Produces(MediaType.TEXT_HTML)
	public String findHTMLCourse(@CookieParam("authToken") String token,
			@FormParam("courseIdParam") String courseIdText) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		int courseId = Validation.toInt(courseIdText);
		if (courseId < 0) {
			return Page.message("Course Catalog REST Resource", "Search REJECTED",
					"The course id must be a whole number.");
		}

		Course c = CourseRegistrationData.findCourse(courseId);
		if (c == null) {
			return Page.message("Course Catalog REST Resource", "Course " + courseId + " was not found.", "");
		}
		return buildHTMLResult("Course Found", c);
	}

	// new: Add button of the course form (MANAGER or above). The course id is assigned by the system
	// and the new record is written to the Course table of SQL Server.
	@POST
	@Path("/add")
	@Produces(MediaType.TEXT_HTML)
	public String addHTMLCourse(@CookieParam("authToken") String token,
			@FormParam("courseCodeParam") String courseCode, @FormParam("courseNameParam") String courseName,
			@FormParam("creditsParam") String creditsText, @FormParam("departmentIdParam") String departmentIdText)
			throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		courseCode = Validation.clean(courseCode).toUpperCase();
		courseName = Validation.clean(courseName);
		int credits = Validation.toInt(creditsText);
		int departmentId = Validation.toInt(departmentIdText);

		String error = checkCourse(courseCode, courseName, credits, departmentId, 0);
		if (!error.isEmpty()) {
			return Page.message("Course Catalog REST Resource", "Course REJECTED", error);
		}

		Course c = new Course(nextCourseId(), courseCode, courseName, credits, departmentId);
		CourseRegistrationData.courseList.add(c);
		CourseRegistrationData.insertCourse(c); // updated: saved in SQL Server

		return buildHTMLResult("Course CONFIRMED", c);
	}

	// new: Update button of the course form (MANAGER or above): the record with that id receives the
	// values of the form
	@POST
	@Path("/update")
	@Produces(MediaType.TEXT_HTML)
	public String updateHTMLCourse(@CookieParam("authToken") String token,
			@FormParam("courseIdParam") String courseIdText, @FormParam("courseCodeParam") String courseCode,
			@FormParam("courseNameParam") String courseName, @FormParam("creditsParam") String creditsText,
			@FormParam("departmentIdParam") String departmentIdText) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		Course c = CourseRegistrationData.findCourse(Validation.toInt(courseIdText));
		if (c == null) {
			return Page.message("Course Catalog REST Resource", "Update REJECTED",
					"Course " + Validation.clean(courseIdText) + " was not found. Search it first.");
		}

		courseCode = Validation.clean(courseCode).toUpperCase();
		courseName = Validation.clean(courseName);
		int credits = Validation.toInt(creditsText);
		int departmentId = Validation.toInt(departmentIdText);

		String error = checkCourse(courseCode, courseName, credits, departmentId, c.getCourseId());
		if (!error.isEmpty()) {
			return Page.message("Course Catalog REST Resource", "Update REJECTED", error);
		}

		// Enrollment.out keeps the course code of each reservation, so the code stays once students hold seats
		if (!courseCode.equals(c.getCourseCode())) {
			ArrayList<Enrollment> enrollmentList = CourseRegistrationData.enrollmentList;
			for (int i = 0; i < enrollmentList.size(); i++) {
				if (enrollmentList.get(i).getCourseCode().equals(c.getCourseCode())) {
					return Page.message("Course Catalog REST Resource", "Update REJECTED", "The code of "
							+ c.getCourseCode() + " cannot change: students are already enrolled in it.");
				}
			}
		}

		c.setCourseCode(courseCode);
		c.setCourseName(courseName);
		c.setCredits(credits);
		c.setDepartmentId(departmentId);
		CourseRegistrationData.updateCourse(c); // updated: saved in SQL Server

		return buildHTMLResult("Course UPDATED", c);
	}

	// new: Delete button of the course form (MANAGER or above). A course that still has sections is kept.
	@POST
	@Path("/delete")
	@Produces(MediaType.TEXT_HTML)
	public String deleteHTMLCourse(@CookieParam("authToken") String token,
			@FormParam("courseIdParam") String courseIdText) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		Course c = CourseRegistrationData.findCourse(Validation.toInt(courseIdText));
		if (c == null) {
			return Page.message("Course Catalog REST Resource", "Delete REJECTED",
					"Course " + Validation.clean(courseIdText) + " was not found.");
		}

		ArrayList<Section> sectionList = CourseRegistrationData.sectionList;
		for (int i = 0; i < sectionList.size(); i++) {
			if (sectionList.get(i).getCourseId() == c.getCourseId()) {
				return Page.message("Course Catalog REST Resource", "Delete REJECTED", c.getCourseCode()
						+ " still has sections, for example section " + sectionList.get(i).getSectionId()
						+ ". Delete its sections first.");
			}
		}

		CourseRegistrationData.courseList.remove(c);
		CourseRegistrationData.deleteCourse(c.getCourseId()); // updated: removed from SQL Server

		return Page.message("Course Catalog REST Resource", "Course DELETED",
				"Course " + c.getCourseId() + " " + c.getCourseCode() + " " + c.getCourseName() + " was removed.");
	}

	// new: PUT version of the Update button, as in Lab 6 (@PUT with @QueryParam), for Postman.
	// It calls updateHTMLCourse, so the same access level and validation rules apply.
	@PUT
	@Path("/updateCourse")
	@Produces(MediaType.TEXT_HTML)
	public String updateCourseParameter(@CookieParam("authToken") String token,
			@QueryParam("courseIdParam") String courseIdText, @QueryParam("courseCodeParam") String courseCode,
			@QueryParam("courseNameParam") String courseName, @QueryParam("creditsParam") String creditsText,
			@QueryParam("departmentIdParam") String departmentIdText) throws IOException {

		return updateHTMLCourse(token, courseIdText, courseCode, courseName, creditsText, departmentIdText);
	}

	// new: DELETE version of the Delete button, as in Lab 6 (@DELETE with @QueryParam), for Postman.
	// It calls deleteHTMLCourse, so a course that still has sections is kept.
	@DELETE
	@Path("/deleteCourse")
	@Produces(MediaType.TEXT_HTML)
	public String deleteCourseParameter(@CookieParam("authToken") String token,
			@QueryParam("courseIdParam") String courseIdText) throws IOException {

		return deleteHTMLCourse(token, courseIdText);
	}
}
