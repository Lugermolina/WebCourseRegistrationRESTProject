package webCourseRegistrationREST;

import java.io.IOException;
import java.util.ArrayList;

import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("Instructor")
public class InstructorResource {

	private int nextInstructorId() {
		ArrayList<Instructor> instructorList = CourseRegistrationData.instructorList;
		int maxId = 0;
		for (int i = 0; i < instructorList.size(); i++) {
			if (instructorList.get(i).getInstructorId() > maxId) {
				maxId = instructorList.get(i).getInstructorId();
			}
		}
		return maxId + 1;
	}

	private String buildHTMLTable(String heading, ArrayList<Instructor> list) {
		String result = "<h2>" + heading + "</h2>";
		result += "<table border=\"1\"><tr><th class=\"num\">Instructor Id</th><th>First Name</th>"
				+ "<th>Last Name</th><th>Email</th><th>Department</th></tr>";
		for (int i = 0; i < list.size(); i++) {
			Instructor ins = list.get(i);
			Department d = CourseRegistrationData.findDepartment(ins.getDepartmentId());
			result += "<tr><td class=\"num\">" + ins.getInstructorId() + "</td><td>" + ins.getFirstName()
					+ "</td><td>" + ins.getLastName() + "</td><td>" + ins.getEmail() + "</td><td>"
					+ (d == null ? "" : d.getDepartmentName()) + "</td></tr>";
		}
		result += "</table>";
		return result;
	}

	// the instructor form with its fields filled from the record, ready for Update or Delete
	private String buildHTMLForm(Instructor ins) {
		return Page.formOpen("Instructor")
				+ Page.field("Instructor Id:", "instructorIdParam", "" + ins.getInstructorId())
				+ Page.field("First name:", "firstNameParam", ins.getFirstName())
				+ Page.field("Last name:", "lastNameParam", ins.getLastName())
				+ Page.field("Email:", "emailParam", ins.getEmail())
				+ Page.field("Department Id:", "departmentIdParam", "" + ins.getDepartmentId())
				+ Page.formClose("Instructor", "Instructor");
	}

	private String buildHTMLResult(String heading, Instructor ins) {
		ArrayList<Instructor> result = new ArrayList<Instructor>();
		result.add(ins);
		return Page.open("Instructor REST Resource") + buildHTMLTable(heading, result) + buildHTMLForm(ins)
				+ Page.close();
	}

	// Rules shared by Add and Update. Returns the reason of the refusal, or an empty text when the fields
	// are accepted. ignoreId is the record being updated, so it is not compared with itself (0 on Add).
	private String checkInstructor(String firstName, String lastName, String email, int departmentId,
			int ignoreId) {

		if (Validation.isEmpty(firstName) || Validation.isEmpty(lastName) || Validation.isEmpty(email)) {
			return "First name, last name and email are all required.";
		}
		if (!Validation.isEmail(email)) {
			return "The email address " + email + " is not valid.";
		}

		ArrayList<Instructor> instructorList = CourseRegistrationData.instructorList;
		for (int i = 0; i < instructorList.size(); i++) {
			Instructor ins = instructorList.get(i);
			if (ins.getInstructorId() != ignoreId && ins.getEmail().equalsIgnoreCase(email)) {
				return "The email address " + email + " is already used by another instructor.";
			}
		}

		if (CourseRegistrationData.findDepartment(departmentId) == null) {
			return "The department was not found. Enter the id of an existing department.";
		}
		return "";
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLInstructorInfo() throws IOException {

		CourseRegistrationData.loadData();
		return Page.open("Instructor REST Resource")
				+ buildHTMLTable("Instructors", CourseRegistrationData.instructorList) + Page.close();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public ArrayList<Instructor> displayJSONInstructorInfo() throws IOException {

		CourseRegistrationData.loadData();
		return CourseRegistrationData.instructorList;
	}

	// Search button: reads the record from the Instructor table and returns the form filled with it
	@POST
	@Path("/find")
	@Produces(MediaType.TEXT_HTML)
	public String findHTMLInstructor(@CookieParam("authToken") String token,
			@FormParam("instructorIdParam") String instructorIdText) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		int instructorId = Validation.toInt(instructorIdText);
		if (instructorId < 0) {
			return Page.message("Instructor REST Resource", "Search REJECTED",
					"The instructor id must be a whole number.");
		}

		Instructor ins = CourseRegistrationData.findInstructor(instructorId);
		if (ins == null) {
			return Page.message("Instructor REST Resource", "Instructor " + instructorId + " was not found.", "");
		}
		return buildHTMLResult("Instructor Found", ins);
	}

	// Add button (MANAGER or above). The id is assigned by the system; the record is written to Instructor.in.
	@POST
	@Path("/add")
	@Produces(MediaType.TEXT_HTML)
	public String addHTMLInstructor(@CookieParam("authToken") String token,
			@FormParam("firstNameParam") String firstName, @FormParam("lastNameParam") String lastName,
			@FormParam("emailParam") String email, @FormParam("departmentIdParam") String departmentIdText)
			throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		firstName = Validation.clean(firstName);
		lastName = Validation.clean(lastName);
		email = Validation.clean(email);
		int departmentId = Validation.toInt(departmentIdText);

		String error = checkInstructor(firstName, lastName, email, departmentId, 0);
		if (!error.isEmpty()) {
			return Page.message("Instructor REST Resource", "Instructor REJECTED", error);
		}

		Instructor ins = new Instructor(nextInstructorId(), firstName, lastName, email, departmentId);
		CourseRegistrationData.instructorList.add(ins);
		CourseRegistrationData.saveInstructors();

		return buildHTMLResult("Instructor CONFIRMED", ins);
	}

	// Update button (MANAGER or above): the record with that id receives the values of the form
	@POST
	@Path("/update")
	@Produces(MediaType.TEXT_HTML)
	public String updateHTMLInstructor(@CookieParam("authToken") String token,
			@FormParam("instructorIdParam") String instructorIdText, @FormParam("firstNameParam") String firstName,
			@FormParam("lastNameParam") String lastName, @FormParam("emailParam") String email,
			@FormParam("departmentIdParam") String departmentIdText) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		Instructor ins = CourseRegistrationData.findInstructor(Validation.toInt(instructorIdText));
		if (ins == null) {
			return Page.message("Instructor REST Resource", "Update REJECTED",
					"Instructor " + Validation.clean(instructorIdText) + " was not found. Search it first.");
		}

		firstName = Validation.clean(firstName);
		lastName = Validation.clean(lastName);
		email = Validation.clean(email);
		int departmentId = Validation.toInt(departmentIdText);

		String error = checkInstructor(firstName, lastName, email, departmentId, ins.getInstructorId());
		if (!error.isEmpty()) {
			return Page.message("Instructor REST Resource", "Update REJECTED", error);
		}

		ins.setFirstName(firstName);
		ins.setLastName(lastName);
		ins.setEmail(email);
		ins.setDepartmentId(departmentId);
		CourseRegistrationData.saveInstructors();

		return buildHTMLResult("Instructor UPDATED", ins);
	}

	// Delete button (MANAGER or above). An instructor who still teaches a section is kept.
	@POST
	@Path("/delete")
	@Produces(MediaType.TEXT_HTML)
	public String deleteHTMLInstructor(@CookieParam("authToken") String token,
			@FormParam("instructorIdParam") String instructorIdText) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		Instructor ins = CourseRegistrationData.findInstructor(Validation.toInt(instructorIdText));
		if (ins == null) {
			return Page.message("Instructor REST Resource", "Delete REJECTED",
					"Instructor " + Validation.clean(instructorIdText) + " was not found.");
		}

		ArrayList<Section> sectionList = CourseRegistrationData.sectionList;
		for (int i = 0; i < sectionList.size(); i++) {
			if (sectionList.get(i).getInstructorId() == ins.getInstructorId()) {
				return Page.message("Instructor REST Resource", "Delete REJECTED", ins.fullName()
						+ " still teaches section " + sectionList.get(i).getSectionId() + ".");
			}
		}

		CourseRegistrationData.instructorList.remove(ins);
		CourseRegistrationData.saveInstructors();

		return Page.message("Instructor REST Resource", "Instructor DELETED",
				"Instructor " + ins.getInstructorId() + " " + ins.fullName() + " was removed.");
	}
}
