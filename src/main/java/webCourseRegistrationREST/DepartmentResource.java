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

@Path("Department")
public class DepartmentResource {

	private int nextDepartmentId() {
		ArrayList<Department> departmentList = CourseRegistrationData.departmentList;
		int maxId = 0;
		for (int i = 0; i < departmentList.size(); i++) {
			if (departmentList.get(i).getDepartmentId() > maxId) {
				maxId = departmentList.get(i).getDepartmentId();
			}
		}
		return maxId + 1;
	}

	private String buildHTMLTable(String heading, ArrayList<Department> list) {
		String result = "<h2>" + heading + "</h2>";
		result += "<table border=\"1\"><tr><th class=\"num\">Department Id</th><th>Name</th>"
				+ "<th>Department Head</th></tr>";
		for (int i = 0; i < list.size(); i++) {
			Department d = list.get(i);
			result += "<tr><td class=\"num\">" + d.getDepartmentId() + "</td><td>" + d.getDepartmentName()
					+ "</td><td>" + d.getDepartmentHead() + "</td></tr>";
		}
		result += "</table>";
		return result;
	}

	// the department form with its fields filled from the record, ready for Update or Delete
	private String buildHTMLForm(Department d) {
		return Page.formOpen("Department")
				+ Page.field("Department Id:", "departmentIdParam", "" + d.getDepartmentId())
				+ Page.field("Department name:", "departmentNameParam", d.getDepartmentName())
				+ Page.field("Department head:", "departmentHeadParam", d.getDepartmentHead())
				+ Page.formClose("Department", "Department");
	}

	private String buildHTMLResult(String heading, Department d) {
		ArrayList<Department> result = new ArrayList<Department>();
		result.add(d);
		return Page.open("Department REST Resource") + buildHTMLTable(heading, result) + buildHTMLForm(d)
				+ Page.close();
	}

	// Rules shared by Add and Update. Returns the reason of the refusal, or an empty text when the fields
	// are accepted. ignoreId is the record being updated, so it is not compared with itself (0 on Add).
	private String checkDepartment(String departmentName, String departmentHead, int ignoreId) {

		if (Validation.isEmpty(departmentName) || Validation.isEmpty(departmentHead)) {
			return "Department name and department head are both required.";
		}

		ArrayList<Department> departmentList = CourseRegistrationData.departmentList;
		for (int i = 0; i < departmentList.size(); i++) {
			Department d = departmentList.get(i);
			if (d.getDepartmentId() != ignoreId && d.getDepartmentName().equalsIgnoreCase(departmentName)) {
				return "The department " + departmentName + " already exists.";
			}
		}
		return "";
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLDepartmentInfo() throws IOException {

		CourseRegistrationData.loadData();
		return Page.open("Department REST Resource")
				+ buildHTMLTable("Departments", CourseRegistrationData.departmentList) + Page.close();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public ArrayList<Department> displayJSONDepartmentInfo() throws IOException {

		CourseRegistrationData.loadData();
		return CourseRegistrationData.departmentList;
	}

	// Search button: reads the record from the Department table and returns the form filled with it
	@POST
	@Path("/find")
	@Produces(MediaType.TEXT_HTML)
	public String findHTMLDepartment(@CookieParam("authToken") String token,
			@FormParam("departmentIdParam") String departmentIdText) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		int departmentId = Validation.toInt(departmentIdText);
		if (departmentId < 0) {
			return Page.message("Department REST Resource", "Search REJECTED",
					"The department id must be a whole number.");
		}

		Department d = CourseRegistrationData.findDepartment(departmentId);
		if (d == null) {
			return Page.message("Department REST Resource", "Department " + departmentId + " was not found.", "");
		}
		return buildHTMLResult("Department Found", d);
	}

	// Add button (MANAGER or above). The id is assigned by the system; the record is written to the Department table of SQL Server.
	@POST
	@Path("/add")
	@Produces(MediaType.TEXT_HTML)
	public String addHTMLDepartment(@CookieParam("authToken") String token,
			@FormParam("departmentNameParam") String departmentName,
			@FormParam("departmentHeadParam") String departmentHead) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		departmentName = Validation.clean(departmentName);
		departmentHead = Validation.clean(departmentHead);

		String error = checkDepartment(departmentName, departmentHead, 0);
		if (!error.isEmpty()) {
			return Page.message("Department REST Resource", "Department REJECTED", error);
		}

		Department d = new Department(nextDepartmentId(), departmentName, departmentHead);
		CourseRegistrationData.departmentList.add(d);
		CourseRegistrationData.insertDepartment(d); // updated: saved in SQL Server

		return buildHTMLResult("Department CONFIRMED", d);
	}

	// Update button (MANAGER or above): the record with that id receives the values of the form
	@POST
	@Path("/update")
	@Produces(MediaType.TEXT_HTML)
	public String updateHTMLDepartment(@CookieParam("authToken") String token,
			@FormParam("departmentIdParam") String departmentIdText,
			@FormParam("departmentNameParam") String departmentName,
			@FormParam("departmentHeadParam") String departmentHead) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		Department d = CourseRegistrationData.findDepartment(Validation.toInt(departmentIdText));
		if (d == null) {
			return Page.message("Department REST Resource", "Update REJECTED",
					"Department " + Validation.clean(departmentIdText) + " was not found. Search it first.");
		}

		departmentName = Validation.clean(departmentName);
		departmentHead = Validation.clean(departmentHead);

		String error = checkDepartment(departmentName, departmentHead, d.getDepartmentId());
		if (!error.isEmpty()) {
			return Page.message("Department REST Resource", "Update REJECTED", error);
		}

		d.setDepartmentName(departmentName);
		d.setDepartmentHead(departmentHead);
		CourseRegistrationData.updateDepartment(d); // updated: saved in SQL Server

		return buildHTMLResult("Department UPDATED", d);
	}

	// Delete button (MANAGER or above). A department that still has courses or instructors is kept,
	// otherwise those records would point to a department that no longer exists.
	@POST
	@Path("/delete")
	@Produces(MediaType.TEXT_HTML)
	public String deleteHTMLDepartment(@CookieParam("authToken") String token,
			@FormParam("departmentIdParam") String departmentIdText) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		Department d = CourseRegistrationData.findDepartment(Validation.toInt(departmentIdText));
		if (d == null) {
			return Page.message("Department REST Resource", "Delete REJECTED",
					"Department " + Validation.clean(departmentIdText) + " was not found.");
		}

		ArrayList<Course> courseList = CourseRegistrationData.courseList;
		for (int i = 0; i < courseList.size(); i++) {
			if (courseList.get(i).getDepartmentId() == d.getDepartmentId()) {
				return Page.message("Department REST Resource", "Delete REJECTED", d.getDepartmentName()
						+ " still has courses, for example " + courseList.get(i).getCourseCode() + ".");
			}
		}
		ArrayList<Instructor> instructorList = CourseRegistrationData.instructorList;
		for (int i = 0; i < instructorList.size(); i++) {
			if (instructorList.get(i).getDepartmentId() == d.getDepartmentId()) {
				return Page.message("Department REST Resource", "Delete REJECTED", d.getDepartmentName()
						+ " still has instructors, for example " + instructorList.get(i).fullName() + ".");
			}
		}

		CourseRegistrationData.departmentList.remove(d);
		CourseRegistrationData.deleteDepartment(d.getDepartmentId()); // updated: removed from SQL Server

		return Page.message("Department REST Resource", "Department DELETED",
				"Department " + d.getDepartmentId() + " " + d.getDepartmentName() + " was removed.");
	}
}
