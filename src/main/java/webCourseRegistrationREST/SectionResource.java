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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("Section")
public class SectionResource {

	private String buildHTMLRow(Section s) {
		Course c = CourseRegistrationData.findCourse(s.getCourseId());
		Instructor ins = CourseRegistrationData.findInstructor(s.getInstructorId());
		return "<tr><td class=\"num\">" + s.getSectionId() + "</td><td>" + (c == null ? "" : c.getCourseCode())
				+ "</td>" + "<td>" + (c == null ? "" : c.getCourseName()) + "</td>"
				+ "<td>" + (ins == null ? "" : ins.fullName()) + "</td>"
				+ "<td>" + s.getSemester() + " " + s.getYear() + "</td><td>" + s.getSchedule() + "</td>"
				+ "<td>" + s.getRoom() + "</td><td class=\"num\">" + s.getEnrolled() + " / " + s.getCapacity()
				+ "</td>" + "<td class=\"num\">" + s.seatsAvailable() + "</td><td>" + (s.isFull() ? "FULL" : "OPEN")
				+ "</td></tr>";
	}

	private String buildHTMLTable(String heading, ArrayList<Section> list) {
		String result = "<h2>" + heading + "</h2>";
		result += "<table border=\"1\"><tr><th class=\"num\">Section</th><th>Code</th><th>Course</th><th>Instructor</th>"
				+ "<th>Term</th><th>Schedule</th><th>Room</th><th class=\"num\">Enrolled</th><th class=\"num\">Seats Left</th><th>Status</th></tr>";
		for (int i = 0; i < list.size(); i++) {
			result += buildHTMLRow(list.get(i));
		}
		result += "</table>";
		return result;
	}

	// new
	private int nextSectionId() {
		ArrayList<Section> sectionList = CourseRegistrationData.sectionList;
		int maxId = 0;
		for (int i = 0; i < sectionList.size(); i++) {
			if (sectionList.get(i).getSectionId() > maxId) {
				maxId = sectionList.get(i).getSectionId();
			}
		}
		return maxId + 1;
	}

	// new: true when two sections of the same term meet on a common day at overlapping hours.
	// A schedule is stored as "Mon/Wed 09:00-10:30": the days before the space, the hours after it.
	// Hours written as HH:mm compare correctly as text, so "09:00" < "10:30" needs no conversion.
	// Two periods overlap when each one starts before the other one ends.
	private boolean scheduleConflict(Section a, Section b) {

		if (!a.getSemester().equalsIgnoreCase(b.getSemester()) || a.getYear() != b.getYear()) {
			return false;
		}

		String[] partsA = a.getSchedule().split(" ");
		String[] partsB = b.getSchedule().split(" ");
		if (partsA.length < 2 || partsB.length < 2) {
			return false;
		}

		String[] daysA = partsA[0].split("/");
		String[] daysB = partsB[0].split("/");
		boolean commonDay = false;
		for (int i = 0; i < daysA.length; i++) {
			for (int j = 0; j < daysB.length; j++) {
				if (daysA[i].equalsIgnoreCase(daysB[j])) {
					commonDay = true;
				}
			}
		}
		if (!commonDay) {
			return false;
		}

		String[] hoursA = partsA[1].split("-");
		String[] hoursB = partsB[1].split("-");
		if (hoursA.length < 2 || hoursB.length < 2) {
			return false;
		}
		return hoursA[0].compareTo(hoursB[1]) < 0 && hoursB[0].compareTo(hoursA[1]) < 0;
	}

	// Shared by the HTML and JSON reserve methods; applies all enrollment rules.
	// updated: two rules added (same course in another section, schedule conflict), and a confirmed
	// reservation is now written to Enrollment.out, Section.in and Student.in.
	private Enrollment reserveSeat(int studentId, int sectionId) throws IOException {

		Student st = CourseRegistrationData.findStudent(studentId);
		Section s = CourseRegistrationData.findSection(sectionId);

		if (st == null) {
			return new Enrollment(studentId, "", sectionId, "", "REJECTED",
					"Student " + studentId + " was not found.", 0, 0);
		}
		if (s == null) {
			return new Enrollment(studentId, st.fullName(), sectionId, "", "REJECTED",
					"Section " + sectionId + " was not found.", 0, st.getBalance());
		}

		Course c = CourseRegistrationData.findCourse(s.getCourseId());

		ArrayList<Enrollment> enrollmentList = CourseRegistrationData.enrollmentList;
		for (int i = 0; i < enrollmentList.size(); i++) {
			Enrollment e = enrollmentList.get(i);
			if (e.getStudentId() != studentId) {
				continue;
			}
			if (e.getSectionId() == sectionId) {
				return new Enrollment(studentId, st.fullName(), sectionId, c.getCourseCode(), "REJECTED",
						"Student is already enrolled in section " + sectionId + ".", 0, st.getBalance());
			}
			// new: the sections the student already holds are compared with the one requested
			Section held = CourseRegistrationData.findSection(e.getSectionId());
			if (held != null && held.getCourseId() == s.getCourseId()) {
				return new Enrollment(studentId, st.fullName(), sectionId, c.getCourseCode(), "REJECTED",
						"Student is already enrolled in " + c.getCourseCode() + " (section " + held.getSectionId()
								+ ").",
						0, st.getBalance());
			}
			if (held != null && scheduleConflict(held, s)) {
				return new Enrollment(studentId, st.fullName(), sectionId, c.getCourseCode(), "SCHEDULE CONFLICT",
						"Section " + sectionId + " (" + s.getSchedule() + ") overlaps section "
								+ held.getSectionId() + " " + e.getCourseCode() + " (" + held.getSchedule() + ").",
						0, st.getBalance());
			}
		}

		if (s.isFull()) {
			return new Enrollment(studentId, st.fullName(), sectionId, c.getCourseCode(), "CAPACITY EXCEEDED",
					"Section " + sectionId + " is full (" + s.getEnrolled() + " / " + s.getCapacity() + ").", 0,
					st.getBalance());
		}

		s.reserveSeat();
		st.chargeTuition(c.tuitionFee());
		st.setBalance(Math.round(st.getBalance() * 100) / 100.0);

		Enrollment confirmed = new Enrollment(studentId, st.fullName(), sectionId, c.getCourseCode(), "CONFIRMED",
				"Seat reserved in " + c.getCourseCode() + " " + c.getCourseName() + ". Seats left: "
						+ s.seatsAvailable() + ".",
				c.tuitionFee(), st.getBalance());
		confirmed.setDate(LocalDate.now().toString());
		enrollmentList.add(confirmed);

		CourseRegistrationData.saveEnrollments();
		CourseRegistrationData.saveSections();
		CourseRegistrationData.saveStudents();

		return confirmed;
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLSectionInfo() throws IOException {

		CourseRegistrationData.loadData();

		return Page.open("Section REST Resource")
				+ buildHTMLTable("Course Sections", CourseRegistrationData.sectionList) + Page.close();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public ArrayList<Section> displayJSONSectionInfo() throws IOException {

		CourseRegistrationData.loadData();
		return CourseRegistrationData.sectionList;
	}

	@GET
	@Path("/byCourse/{courseCodeParam}")
	@Produces(MediaType.APPLICATION_JSON)
	public ArrayList<Section> displayJSONSectionsByCourse(@PathParam("courseCodeParam") String courseCode)
			throws IOException {

		CourseRegistrationData.loadData();
		ArrayList<Section> sectionList = CourseRegistrationData.sectionList;
		ArrayList<Section> result = new ArrayList<Section>();
		Course c = CourseRegistrationData.findCourseByCode(courseCode);

		if (c == null) {
			return result;
		}

		for (int i = 0; i < sectionList.size(); i++) {
			if (sectionList.get(i).getCourseId() == c.getCourseId()) {
				result.add(sectionList.get(i));
			}
		}

		return result;
	}

	@GET
	@Path("/byCourse/{courseCodeParam}")
	@Produces(MediaType.TEXT_PLAIN)
	public String displayTextSectionsByCourse(@PathParam("courseCodeParam") String courseCode)
			throws IOException {

		CourseRegistrationData.loadData();
		Course c = CourseRegistrationData.findCourseByCode(courseCode);

		if (c == null) {
			return "Course " + courseCode + " was not found.\n";
		}

		String result = "Sections of " + c.getCourseCode() + " " + c.getCourseName() + "\n";
		ArrayList<Section> sectionList = CourseRegistrationData.sectionList;
		for (int i = 0; i < sectionList.size(); i++) {
			Section s = sectionList.get(i);
			if (s.getCourseId() == c.getCourseId()) {
				Instructor ins = CourseRegistrationData.findInstructor(s.getInstructorId());
				result += "Section " + s.getSectionId() + " | " + (ins == null ? "" : ins.fullName())
						+ " | " + s.getSemester() + " " + s.getYear() + " | " + s.getSchedule()
						+ " | Room " + s.getRoom() + " | Seats left: " + s.seatsAvailable()
						+ (s.isFull() ? " (FULL)" : "") + "\n";
			}
		}

		return result;
	}

	// new: same list as the two methods above, as an HTML table for the browser
	@GET
	@Path("/byCourse/{courseCodeParam}")
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLSectionsByCourse(@PathParam("courseCodeParam") String courseCode)
			throws IOException {

		ArrayList<Section> result = displayJSONSectionsByCourse(courseCode);
		return Page.open("Section REST Resource")
				+ buildHTMLTable("Sections of " + Validation.clean(courseCode).toUpperCase(), result) + Page.close();
	}

	// new: the section form with its fields filled from the record, ready for Update or Delete.
	// The stored schedule "Mon/Wed 09:00-10:30" is split back into its three fields: days, start and end.
	private String buildHTMLForm(Section s) {
		Course c = CourseRegistrationData.findCourse(s.getCourseId());
		String[] parts = s.getSchedule().split(" ");
		String[] hours = (parts.length < 2 ? "-" : parts[1]).split("-");
		return Page.formOpen("Section")
				+ Page.field("Section Id:", "sectionIdParam", "" + s.getSectionId())
				+ Page.field("Course code:", "courseCodeParam", c == null ? "" : c.getCourseCode())
				+ Page.field("Instructor Id:", "instructorIdParam", "" + s.getInstructorId())
				+ Page.field("Semester:", "semesterParam", s.getSemester())
				+ Page.field("Year:", "yearParam", "" + s.getYear())
				+ Page.field("Days:", "daysParam", parts[0])
				+ Page.field("Start time:", "startParam", hours.length < 2 ? "" : hours[0])
				+ Page.field("End time:", "endParam", hours.length < 2 ? "" : hours[1])
				+ Page.field("Room:", "roomParam", s.getRoom())
				+ Page.field("Capacity:", "capacityParam", "" + s.getCapacity())
				+ Page.formClose("Section", "Section");
	}

	// new
	private String buildHTMLResult(String heading, Section s) {
		ArrayList<Section> result = new ArrayList<Section>();
		result.add(s);
		return Page.open("Section REST Resource") + buildHTMLTable(heading, result) + buildHTMLForm(s)
				+ Page.close();
	}

	// new: rules shared by Add and Update. Returns the reason of the refusal, or an empty text when the
	// fields are accepted. candidate holds the values of the form; its id is the record being updated,
	// so it is not compared with itself.
	private String checkSection(Section candidate, Course c, Instructor ins, String days, String start,
			String end) {

		if (c == null) {
			return "The course was not found. Enter the code of an existing course.";
		}
		if (ins == null) {
			return "The instructor was not found. Enter the id of an existing instructor.";
		}
		if (!candidate.getSemester().equals("Fall") && !candidate.getSemester().equals("Winter")) {
			return "The semester must be Fall or Winter.";
		}
		if (candidate.getYear() < 2026 || candidate.getYear() > 2030) {
			return "The year must be between 2026 and 2030.";
		}
		// days are written as Mon/Wed: three letter day names separated by a slash
		if (!days.matches("(Mon|Tue|Wed|Thu|Fri)(/(Mon|Tue|Wed|Thu|Fri))*")) {
			return "The days must be written like Mon/Wed, using Mon, Tue, Wed, Thu and Fri.";
		}
		// the two hours must look like 09:00 and the class must end after it starts
		if (!start.matches("\\d{2}:\\d{2}") || !end.matches("\\d{2}:\\d{2}") || start.compareTo(end) >= 0) {
			return "The hours must be written as HH:mm and the end time must be after the start time.";
		}
		if (Validation.isEmpty(candidate.getRoom())) {
			return "The room is required.";
		}
		if (candidate.getCapacity() < 1 || candidate.getCapacity() > 200) {
			return "The capacity must be a whole number between 1 and 200.";
		}

		// an instructor, like a room, cannot hold two sections at the same hours
		ArrayList<Section> sectionList = CourseRegistrationData.sectionList;
		for (int i = 0; i < sectionList.size(); i++) {
			Section other = sectionList.get(i);
			if (other.getSectionId() == candidate.getSectionId() || !scheduleConflict(other, candidate)) {
				continue;
			}
			if (other.getInstructorId() == candidate.getInstructorId()) {
				return ins.fullName() + " already teaches section " + other.getSectionId() + " ("
						+ other.getSchedule() + ").";
			}
			if (other.getRoom().equalsIgnoreCase(candidate.getRoom())) {
				return "Room " + candidate.getRoom() + " is already used by section " + other.getSectionId() + " ("
						+ other.getSchedule() + ").";
			}
		}
		return "";
	}

	// new: Search button of the section form: reads the record from the Section table and returns the
	// form filled with it
	@POST
	@Path("/find")
	@Produces(MediaType.TEXT_HTML)
	public String findHTMLSection(@CookieParam("authToken") String token,
			@FormParam("sectionIdParam") String sectionIdText) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		int sectionId = Validation.toInt(sectionIdText);
		if (sectionId < 0) {
			return Page.message("Section REST Resource", "Search REJECTED", "The section id must be a whole number.");
		}

		Section s = CourseRegistrationData.findSection(sectionId);
		if (s == null) {
			return Page.message("Section REST Resource", "Section " + sectionId + " was not found.", "");
		}
		return buildHTMLResult("Section Found", s);
	}

	// new: Add button of the section form (MANAGER or above). The section id is assigned by the system
	// and the new record is written to Section.in.
	@POST
	@Path("/add")
	@Produces(MediaType.TEXT_HTML)
	public String addHTMLSection(@CookieParam("authToken") String token,
			@FormParam("courseCodeParam") String courseCode, @FormParam("instructorIdParam") String instructorIdText,
			@FormParam("semesterParam") String semester, @FormParam("yearParam") String yearText,
			@FormParam("daysParam") String days, @FormParam("startParam") String start,
			@FormParam("endParam") String end, @FormParam("roomParam") String room,
			@FormParam("capacityParam") String capacityText) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		days = Validation.clean(days);
		start = Validation.clean(start);
		end = Validation.clean(end);
		Course c = CourseRegistrationData.findCourseByCode(Validation.clean(courseCode));
		Instructor ins = CourseRegistrationData.findInstructor(Validation.toInt(instructorIdText));

		Section s = new Section(nextSectionId(), c == null ? 0 : c.getCourseId(),
				ins == null ? 0 : ins.getInstructorId(), Validation.clean(semester), Validation.toInt(yearText),
				days + " " + start + "-" + end, Validation.clean(room), Validation.toInt(capacityText), 0);

		String error = checkSection(s, c, ins, days, start, end);
		if (!error.isEmpty()) {
			return Page.message("Section REST Resource", "Section REJECTED", error);
		}

		CourseRegistrationData.sectionList.add(s);
		CourseRegistrationData.saveSections();

		return buildHTMLResult("Section CONFIRMED", s);
	}

	// new: Update button of the section form (MANAGER or above): the record with that id receives the
	// values of the form. The number of seats taken is not a field of the form and stays as it is.
	@POST
	@Path("/update")
	@Produces(MediaType.TEXT_HTML)
	public String updateHTMLSection(@CookieParam("authToken") String token,
			@FormParam("sectionIdParam") String sectionIdText, @FormParam("courseCodeParam") String courseCode,
			@FormParam("instructorIdParam") String instructorIdText, @FormParam("semesterParam") String semester,
			@FormParam("yearParam") String yearText, @FormParam("daysParam") String days,
			@FormParam("startParam") String start, @FormParam("endParam") String end,
			@FormParam("roomParam") String room, @FormParam("capacityParam") String capacityText)
			throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		Section s = CourseRegistrationData.findSection(Validation.toInt(sectionIdText));
		if (s == null) {
			return Page.message("Section REST Resource", "Update REJECTED",
					"Section " + Validation.clean(sectionIdText) + " was not found. Search it first.");
		}

		days = Validation.clean(days);
		start = Validation.clean(start);
		end = Validation.clean(end);
		Course c = CourseRegistrationData.findCourseByCode(Validation.clean(courseCode));
		Instructor ins = CourseRegistrationData.findInstructor(Validation.toInt(instructorIdText));

		// the new values are checked on a separate object first, so a refused update leaves the record untouched
		Section candidate = new Section(s.getSectionId(), c == null ? 0 : c.getCourseId(),
				ins == null ? 0 : ins.getInstructorId(), Validation.clean(semester), Validation.toInt(yearText),
				days + " " + start + "-" + end, Validation.clean(room), Validation.toInt(capacityText),
				s.getEnrolled());

		String error = checkSection(candidate, c, ins, days, start, end);
		if (!error.isEmpty()) {
			return Page.message("Section REST Resource", "Update REJECTED", error);
		}
		if (candidate.getCapacity() < s.getEnrolled()) {
			return Page.message("Section REST Resource", "Update REJECTED", "The capacity cannot go below the "
					+ s.getEnrolled() + " seats already taken.");
		}
		if (s.getEnrolled() > 0 && candidate.getCourseId() != s.getCourseId()) {
			return Page.message("Section REST Resource", "Update REJECTED",
					"The course of a section cannot change once students are enrolled in it.");
		}

		s.setCourseId(candidate.getCourseId());
		s.setInstructorId(candidate.getInstructorId());
		s.setSemester(candidate.getSemester());
		s.setYear(candidate.getYear());
		s.setSchedule(candidate.getSchedule());
		s.setRoom(candidate.getRoom());
		s.setCapacity(candidate.getCapacity());
		CourseRegistrationData.saveSections();

		return buildHTMLResult("Section UPDATED", s);
	}

	// new: Delete button of the section form (MANAGER or above). A section with students enrolled is kept.
	@POST
	@Path("/delete")
	@Produces(MediaType.TEXT_HTML)
	public String deleteHTMLSection(@CookieParam("authToken") String token,
			@FormParam("sectionIdParam") String sectionIdText) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		Section s = CourseRegistrationData.findSection(Validation.toInt(sectionIdText));
		if (s == null) {
			return Page.message("Section REST Resource", "Delete REJECTED",
					"Section " + Validation.clean(sectionIdText) + " was not found.");
		}
		if (s.getEnrolled() > 0) {
			return Page.message("Section REST Resource", "Delete REJECTED", "Section " + s.getSectionId()
					+ " has " + s.getEnrolled() + " students enrolled.");
		}

		CourseRegistrationData.sectionList.remove(s);
		CourseRegistrationData.saveSections();

		return Page.message("Section REST Resource", "Section DELETED",
				"Section " + s.getSectionId() + " (" + s.getSchedule() + ") was removed.");
	}

	// new: transaction form of the reservation. The student is searched by id, the details are read from
	// the Student table and shown, and the page carries the reservation form already filled with that id.
	@GET
	@Path("/reserveForm")
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLReserveForm(@CookieParam("authToken") String token,
			@QueryParam("studentIdParam") String studentIdText) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null) {
			return Page.denied(current, "STUDENT");
		}

		int studentId = current.studentIdFor(Validation.toInt(studentIdText));
		if (studentId < 0) {
			return Page.message("Reserve a Seat", "Search REJECTED", "The student id must be a whole number.");
		}
		Student st = CourseRegistrationData.findStudent(studentId);
		if (st == null) {
			return Page.message("Reserve a Seat", "Student " + studentId + " was not found.", "");
		}

		String result = Page.open("Reserve a Seat");
		result += "<h2>Reserve a Seat in a Course Section</h2>";
		result += "<table border=\"1\">";
		result += "<tr><td>Student Id</td><td>" + st.getStudentId() + "</td></tr>";
		result += "<tr><td>Name</td><td>" + st.fullName() + "</td></tr>";
		result += "<tr><td>Email</td><td>" + st.getEmail() + "</td></tr>";
		result += "<tr><td>Balance</td><td>" + Page.money(st.getBalance()) + "</td></tr>";
		result += "</table>";

		result += "<form method=\"post\" action=\"" + Page.APP + "/rest/Section/reserve\">";
		result += "<input type=\"hidden\" name=\"studentIdParam\" value=\"" + st.getStudentId() + "\">";
		result += "<p><label>Section: <select name=\"sectionIdParam\">";
		ArrayList<Section> sectionList = CourseRegistrationData.sectionList;
		for (int i = 0; i < sectionList.size(); i++) {
			Section s = sectionList.get(i);
			Course c = CourseRegistrationData.findCourse(s.getCourseId());
			result += "<option value=\"" + s.getSectionId() + "\">" + s.getSectionId() + " | "
					+ (c == null ? "" : c.getCourseCode() + " " + c.getCourseName()) + " | " + s.getSemester() + " "
					+ s.getYear() + " | " + s.getSchedule() + " | "
					+ (s.isFull() ? "FULL" : "seats left: " + s.seatsAvailable()) + "</option>";
		}
		result += "</select></label></p>";
		result += "<input type=\"submit\" value=\"Reserve Seat\">";
		result += "</form>";
		result += Page.close();

		return result;
	}

	// Business process 4: reserve a seat in a course section
	// updated: POST with form parameters instead of GET, and a login is required.
	// The two ids arrive as text and are converted here.
	@POST
	@Path("/reserve")
	@Produces(MediaType.TEXT_HTML)
	public String reserveHTMLSeat(@CookieParam("authToken") String token,
			@FormParam("studentIdParam") String studentIdText, @FormParam("sectionIdParam") String sectionIdText)
			throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null) {
			return Page.denied(current, "STUDENT");
		}

		int studentId = current.studentIdFor(Validation.toInt(studentIdText));
		int sectionId = Validation.toInt(sectionIdText);
		if (studentId < 0 || sectionId < 0) {
			return Page.message("Enrollment REST Resource", "Enrollment REJECTED",
					"The student id and the section id must be whole numbers.");
		}

		Enrollment e = reserveSeat(studentId, sectionId);

		String result = Page.open("Enrollment REST Resource");
		result += "<h2>Enrollment " + e.getStatus() + "</h2>";
		result += "<table border=\"1\">";
		result += "<tr><td>Student</td><td>" + e.getStudentId() + " " + e.getStudentName() + "</td></tr>";
		result += "<tr><td>Section</td><td>" + e.getSectionId() + " " + e.getCourseCode() + "</td></tr>";
		result += "<tr><td>Message</td><td>" + e.getMessage() + "</td></tr>";
		result += "<tr><td>Tuition Charged</td><td>" + Page.money(e.getTuitionCharged()) + "</td></tr>";
		result += "<tr><td>Balance</td><td>" + Page.money(e.getNewBalance()) + "</td></tr>";
		result += "</table>";
		result += "<p><a href=\"" + Page.APP + "/rest/Section/reserveForm?studentIdParam=" + e.getStudentId()
				+ "\">Reserve another seat</a> | <a href=\"" + Page.APP + "/rest/Payment/payForm?studentIdParam="
				+ e.getStudentId() + "\">Pay tuition</a></p>";
		result += Page.close();

		return result;
	}

	// Business process 4: same rules as reserveHTMLSeat, for JSON clients
	// updated: POST with form parameters, login required
	@POST
	@Path("/reserve")
	@Produces(MediaType.APPLICATION_JSON)
	public Enrollment reserveJSONSeat(@CookieParam("authToken") String token,
			@FormParam("studentIdParam") String studentIdText, @FormParam("sectionIdParam") String sectionIdText)
			throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null) {
			return new Enrollment(0, "", 0, "", "REJECTED", "Login required.", 0, 0);
		}

		int studentId = current.studentIdFor(Validation.toInt(studentIdText));
		int sectionId = Validation.toInt(sectionIdText);
		if (studentId < 0 || sectionId < 0) {
			return new Enrollment(0, "", 0, "", "REJECTED",
					"The student id and the section id must be whole numbers.", 0, 0);
		}

		return reserveSeat(studentId, sectionId);
	}
}
