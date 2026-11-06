package webCourseRegistrationREST;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;

import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("Student")
public class StudentResource {

	private int nextStudentId() {
		ArrayList<Student> studentList = CourseRegistrationData.studentList;
		int maxId = 0;
		for (int i = 0; i < studentList.size(); i++) {
			if (studentList.get(i).getStudentId() > maxId) {
				maxId = studentList.get(i).getStudentId();
			}
		}
		return maxId + 1;
	}

	// new
	private int nextUserId() {
		ArrayList<User> userList = CourseRegistrationData.userList;
		int maxId = 0;
		for (int i = 0; i < userList.size(); i++) {
			if (userList.get(i).getUserId() > maxId) {
				maxId = userList.get(i).getUserId();
			}
		}
		return maxId + 1;
	}

	// new: rules shared by registration, Add and Update. Returns the reason of the refusal, or an empty text
	// when the fields are accepted. ignoreId is the student being updated, so the email is not compared
	// with its own record (0 on registration and Add).
	private String checkStudent(String firstName, String lastName, String email, String phone, String address,
			int ignoreId) {

		if (Validation.isEmpty(firstName) || Validation.isEmpty(lastName) || Validation.isEmpty(email)
				|| Validation.isEmpty(phone) || Validation.isEmpty(address)) {
			return "First name, last name, email, phone and address are all required.";
		}
		if (!Validation.isEmail(email)) {
			return "The email address " + email + " is not valid.";
		}
		// the email is also the login username, so it is searched in both tables
		ArrayList<Student> studentList = CourseRegistrationData.studentList;
		for (int i = 0; i < studentList.size(); i++) {
			Student st = studentList.get(i);
			if (st.getStudentId() != ignoreId && st.getEmail().equalsIgnoreCase(email)) {
				return "The email address " + email + " is already registered.";
			}
		}
		User sameName = CourseRegistrationData.findUserByName(email);
		if (sameName != null && (sameName.getStudentId() != ignoreId || ignoreId == 0)) {
			return "The email address " + email + " is already registered.";
		}
		if (Validation.formatPhone(phone).isEmpty()) {
			return "The phone " + phone + " is not valid. A phone number has 10 digits, e.g. 514-555-0199.";
		}
		if (!Validation.isAddress(address)) {
			return "The address needs a civic number and a street name.";
		}
		return "";
	}

	// new: shared by the public registration form and by the staff Add Student button, so both apply the
	// same rules. It checks every field, then stores the student in Student.in and the login account in
	// the Users table of SQL Server (username = the email, role STUDENT, password kept as a hash).
	private String registerStudent(String firstName, String lastName, String email, String phone, String address,
			String password) throws IOException {

		firstName = Validation.clean(firstName);
		lastName = Validation.clean(lastName);
		email = Validation.clean(email);
		phone = Validation.clean(phone);
		address = Validation.clean(address);

		String error = checkStudent(firstName, lastName, email, phone, address, 0);
		if (!error.isEmpty()) {
			return Page.message("Student Registration", "Registration REJECTED", error);
		}
		if (password == null || password.length() < 6) {
			return Page.message("Student Registration", "Registration REJECTED",
					"The password needs at least 6 characters.");
		}
		String formattedPhone = Validation.formatPhone(phone);

		Student st = new Student(nextStudentId(), firstName, lastName, email, formattedPhone, address,
				LocalDate.now().toString(), 0);
		CourseRegistrationData.studentList.add(st);
		CourseRegistrationData.saveStudents();

		User u = new User(nextUserId(), email, User.hashPassword(password), "STUDENT", st.getStudentId());
		CourseRegistrationData.userList.add(u);
		CourseRegistrationData.insertUser(u); // updated: saved in SQL Server

		String result = Page.open("Student Registration");
		result += "<h2>Registration CONFIRMED</h2>";
		result += "<table border=\"1\">";
		result += "<tr><td>Student Id</td><td>" + st.getStudentId() + "</td></tr>";
		result += "<tr><td>Name</td><td>" + st.fullName() + "</td></tr>";
		result += "<tr><td>Email</td><td>" + st.getEmail() + "</td></tr>";
		result += "<tr><td>Phone</td><td>" + st.getPhone() + "</td></tr>";
		result += "<tr><td>Address</td><td>" + st.getAddress() + "</td></tr>";
		result += "<tr><td>Registered On</td><td>" + st.getEnrollmentDate() + "</td></tr>";
		result += "<tr><td>Login username</td><td>" + u.getUsername() + "</td></tr>";
		result += "</table>";
		result += "<p><a href=\"" + Page.APP + "/login.html\">Login</a></p>";
		result += Page.close();

		return result;
	}

	// new: profile page, shared by the path version, the Search button and the Update button.
	// It ends with the student form filled from the record, ready for Update or Delete.
	private String buildHTMLProfile(String heading, Student st) {

		String result = Page.open("Student REST Resource");
		result += "<h2>" + heading + "</h2>";
		result += "<table border=\"1\">";
		result += "<tr><td>Student Id</td><td>" + st.getStudentId() + "</td></tr>";
		result += "<tr><td>Name</td><td>" + st.fullName() + "</td></tr>";
		result += "<tr><td>Email</td><td>" + st.getEmail() + "</td></tr>";
		result += "<tr><td>Phone</td><td>" + st.getPhone() + "</td></tr>";
		result += "<tr><td>Address</td><td>" + st.getAddress() + "</td></tr>";
		result += "<tr><td>Registered On</td><td>" + st.getEnrollmentDate() + "</td></tr>";
		result += "<tr><td>Balance</td><td>" + Page.money(st.getBalance()) + "</td></tr>";
		result += "</table>";

		result += "<h3>Seats Reserved</h3>";
		result += "<table border=\"1\"><tr><th>Date</th><th class=\"num\">Section</th><th>Course</th><th class=\"num\">Tuition Charged</th></tr>";
		ArrayList<Enrollment> enrollmentList = CourseRegistrationData.enrollmentList;
		for (int i = 0; i < enrollmentList.size(); i++) {
			Enrollment e = enrollmentList.get(i);
			if (e.getStudentId() == st.getStudentId()) {
				result += "<tr><td>" + e.getDate() + "</td><td class=\"num\">" + e.getSectionId() + "</td><td>"
						+ e.getCourseCode() + "</td><td class=\"num\">" + Page.money(e.getTuitionCharged())
						+ "</td></tr>";
			}
		}
		result += "</table>";
		result += "<p><a href=\"" + Page.APP + "/rest/Payment/balance/" + st.getStudentId()
				+ "\">Tuition balance</a></p>";

		result += Page.formOpen("Student")
				+ Page.field("Student Id:", "studentIdParam", "" + st.getStudentId())
				+ Page.field("First name:", "firstNameParam", st.getFirstName())
				+ Page.field("Last name:", "lastNameParam", st.getLastName())
				+ Page.field("Email (login username):", "emailParam", st.getEmail())
				+ Page.field("Phone:", "phoneParam", st.getPhone())
				+ Page.field("Address:", "addressParam", st.getAddress())
				+ "<label class=\"field\">Temporary password:</label>"
				+ "<input type=\"password\" name=\"passwordParam\"><br>"
				+ Page.formClose("Student", "Student");
		result += Page.close();

		return result;
	}

	// updated: the list holds personal information, so it now requires a staff login (CLERK or above)
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLStudentInfo(@CookieParam("authToken") String token) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.CLERK) {
			return Page.denied(current, "CLERK");
		}

		ArrayList<Student> studentList = CourseRegistrationData.studentList;

		String result = Page.open("Student REST Resource");
		result += "<h2>Registered Students</h2>";
		result += "<table border=\"1\"><tr><th class=\"num\">Id</th><th>First Name</th><th>Last Name</th><th>Email</th>"
				+ "<th>Phone</th><th>Address</th><th>Registered On</th><th class=\"num\">Balance</th></tr>";
		for (int i = 0; i < studentList.size(); i++) {
			Student st = studentList.get(i);
			result += "<tr><td class=\"num\">" + st.getStudentId() + "</td><td>" + st.getFirstName() + "</td>"
					+ "<td>" + st.getLastName() + "</td><td>" + st.getEmail() + "</td>"
					+ "<td>" + st.getPhone() + "</td><td>" + st.getAddress() + "</td>"
					+ "<td>" + st.getEnrollmentDate() + "</td>"
					+ "<td class=\"num\">" + Page.money(st.getBalance()) + "</td></tr>";
		}
		result += "</table>";
		result += "<h3>Students registered: " + studentList.size() + "</h3>";
		result += Page.close();

		return result;
	}

	// updated: without a staff login the JSON list comes back empty
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public ArrayList<Student> displayJSONStudentInfo(@CookieParam("authToken") String token) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.CLERK) {
			return new ArrayList<Student>();
		}
		return CourseRegistrationData.studentList;
	}

	// updated: requires a login; a student account receives its own profile only
	@GET
	@Path("/profile/{studentIdParam}")
	@Produces(MediaType.APPLICATION_JSON)
	public Student displayJSONStudentProfile(@CookieParam("authToken") String token,
			@PathParam("studentIdParam") int studentId) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null) {
			return null;
		}
		return CourseRegistrationData.findStudent(current.studentIdFor(studentId));
	}

	// updated: requires a login; a student account receives its own profile only
	@GET
	@Path("/profile/{studentIdParam}")
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLStudentProfile(@CookieParam("authToken") String token,
			@PathParam("studentIdParam") int studentId) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null) {
			return Page.denied(current, "STUDENT");
		}

		studentId = current.studentIdFor(studentId);
		Student st = CourseRegistrationData.findStudent(studentId);
		if (st == null) {
			return Page.message("Student REST Resource", "Student " + studentId + " was not found.", "");
		}
		return buildHTMLProfile("Student Profile", st);
	}

	// new: Search button of the student form: reads the record from the Student table and returns the
	// profile with the form filled. A student account always receives its own record.
	@POST
	@Path("/find")
	@Produces(MediaType.TEXT_HTML)
	public String findHTMLStudent(@CookieParam("authToken") String token,
			@FormParam("studentIdParam") String studentIdText) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null) {
			return Page.denied(current, "STUDENT");
		}

		int studentId = current.studentIdFor(Validation.toInt(studentIdText));
		if (studentId < 0) {
			return Page.message("Student REST Resource", "Search REJECTED",
					"The student id must be a whole number.");
		}

		Student st = CourseRegistrationData.findStudent(studentId);
		if (st == null) {
			return Page.message("Student REST Resource", "Student " + studentId + " was not found.", "");
		}
		return buildHTMLProfile("Student Profile", st);
	}

	// Business process 3: student registration
	// updated: POST with form parameters instead of GET, and a password that opens the login account.
	// Open to everyone: it is how a new student gets an account.
	@POST
	@Path("/register")
	@Produces(MediaType.TEXT_HTML)
	public String registerHTMLStudent(@FormParam("firstNameParam") String firstName,
			@FormParam("lastNameParam") String lastName, @FormParam("emailParam") String email,
			@FormParam("phoneParam") String phone, @FormParam("addressParam") String address,
			@FormParam("passwordParam") String password) throws IOException {

		CourseRegistrationData.loadData();
		return registerStudent(firstName, lastName, email, phone, address, password);
	}

	// new: Add button of the student form, for the staff (CLERK or above)
	@POST
	@Path("/add")
	@Produces(MediaType.TEXT_HTML)
	public String addHTMLStudent(@CookieParam("authToken") String token,
			@FormParam("firstNameParam") String firstName, @FormParam("lastNameParam") String lastName,
			@FormParam("emailParam") String email, @FormParam("phoneParam") String phone,
			@FormParam("addressParam") String address, @FormParam("passwordParam") String password)
			throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.CLERK) {
			return Page.denied(current, "CLERK");
		}
		return registerStudent(firstName, lastName, email, phone, address, password);
	}

	// new: Update button of the student form. The staff (CLERK or above) may update any student; a student
	// account updates its own record only. The id, the registration date and the balance do not change.
	// When the email changes, the login username in the Users table changes with it.
	@POST
	@Path("/update")
	@Produces(MediaType.TEXT_HTML)
	public String updateHTMLStudent(@CookieParam("authToken") String token,
			@FormParam("studentIdParam") String studentIdText, @FormParam("firstNameParam") String firstName,
			@FormParam("lastNameParam") String lastName, @FormParam("emailParam") String email,
			@FormParam("phoneParam") String phone, @FormParam("addressParam") String address) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null) {
			return Page.denied(current, "STUDENT");
		}

		Student st = CourseRegistrationData.findStudent(current.studentIdFor(Validation.toInt(studentIdText)));
		if (st == null) {
			return Page.message("Student REST Resource", "Update REJECTED",
					"Student " + Validation.clean(studentIdText) + " was not found. Search it first.");
		}

		firstName = Validation.clean(firstName);
		lastName = Validation.clean(lastName);
		email = Validation.clean(email);
		phone = Validation.clean(phone);
		address = Validation.clean(address);

		String error = checkStudent(firstName, lastName, email, phone, address, st.getStudentId());
		if (!error.isEmpty()) {
			return Page.message("Student REST Resource", "Update REJECTED", error);
		}

		User account = CourseRegistrationData.findUserByName(st.getEmail());
		if (account != null) {
			account.setUsername(email);
			CourseRegistrationData.updateUser(account); // updated: saved in SQL Server
		}

		st.setFirstName(firstName);
		st.setLastName(lastName);
		st.setEmail(email);
		st.setPhone(Validation.formatPhone(phone));
		st.setAddress(address);
		CourseRegistrationData.saveStudents();

		return buildHTMLProfile("Student UPDATED", st);
	}

	// new: Delete button of the student form (MANAGER or above). A student who holds seats or owes tuition
	// is kept. The login account of the student is removed from the Users table together with the record.
	@POST
	@Path("/delete")
	@Produces(MediaType.TEXT_HTML)
	public String deleteHTMLStudent(@CookieParam("authToken") String token,
			@FormParam("studentIdParam") String studentIdText) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		Student st = CourseRegistrationData.findStudent(Validation.toInt(studentIdText));
		if (st == null) {
			return Page.message("Student REST Resource", "Delete REJECTED",
					"Student " + Validation.clean(studentIdText) + " was not found.");
		}
		if (st.getBalance() != 0) {
			return Page.message("Student REST Resource", "Delete REJECTED",
					st.fullName() + " has a balance of " + Page.money(st.getBalance()) + ".");
		}
		ArrayList<Enrollment> enrollmentList = CourseRegistrationData.enrollmentList;
		for (int i = 0; i < enrollmentList.size(); i++) {
			if (enrollmentList.get(i).getStudentId() == st.getStudentId()) {
				return Page.message("Student REST Resource", "Delete REJECTED", st.fullName()
						+ " holds a seat in section " + enrollmentList.get(i).getSectionId() + ".");
			}
		}

		User account = CourseRegistrationData.findUserByName(st.getEmail());
		if (account != null) {
			CourseRegistrationData.userList.remove(account);
			CourseRegistrationData.deleteUser(account.getUserId()); // updated: removed from SQL Server
			SessionManager.getInstance().closeUser(account.getUserId());
		}

		CourseRegistrationData.studentList.remove(st);
		CourseRegistrationData.saveStudents();

		return Page.message("Student REST Resource", "Student DELETED",
				"Student " + st.getStudentId() + " " + st.fullName() + " and the login account were removed.");
	}
}
