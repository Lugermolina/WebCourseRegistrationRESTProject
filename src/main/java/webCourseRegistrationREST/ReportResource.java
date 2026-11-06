package webCourseRegistrationREST;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

// Output reports. In every table, text columns are aligned left and number columns right (class "num"),
// with the heading aligned the same way as its column.
@Path("Report")
public class ReportResource {

	private String reportHeading(String title) {
		return "<h2>" + title + "</h2><p>Printed on " + LocalDate.now().toString() + "</p>";
	}

	// Summary report: one line per department instead of one line per section, with the grand total at the end.
	@GET
	@Path("/enrollmentSummary")
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLEnrollmentSummary(@CookieParam("authToken") String token) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		// data structure (HashMap): key = department id, value = the running totals of that department,
		// kept in an array: [0] sections, [1] capacity, [2] seats taken, [3] tuition value.
		// Each section adds its numbers to the entry of its department, so one pass over the sections is enough.
		HashMap<Integer, double[]> totals = new HashMap<Integer, double[]>();
		ArrayList<Department> departmentList = CourseRegistrationData.departmentList;
		for (int i = 0; i < departmentList.size(); i++) {
			totals.put(departmentList.get(i).getDepartmentId(), new double[4]);
		}

		// pattern (Iterator): the list hands out an Iterator object that walks it one element at a time.
		// hasNext() answers "is there another section?", next() returns it and moves forward.
		// The loop does not use an index and does not need to know how the list is stored.
		Iterator<Section> it = CourseRegistrationData.sectionList.iterator();
		while (it.hasNext()) {
			Section s = it.next();
			Course c = CourseRegistrationData.findCourse(s.getCourseId());
			if (c == null || !totals.containsKey(c.getDepartmentId())) {
				continue;
			}
			double[] t = totals.get(c.getDepartmentId());
			t[0] += 1;
			t[1] += s.getCapacity();
			t[2] += s.getEnrolled();
			t[3] += s.getEnrolled() * c.tuitionFee();
		}

		String result = Page.open("Enrollment Summary Report");
		result += reportHeading("Enrollment Summary by Department");
		result += "<table border=\"1\"><tr><th>Department</th><th class=\"num\">Sections</th>"
				+ "<th class=\"num\">Capacity</th><th class=\"num\">Seats Taken</th><th class=\"num\">Fill Rate</th>"
				+ "<th class=\"num\">Tuition Value</th></tr>";

		double[] grand = new double[4];
		for (Map.Entry<Integer, double[]> entry : totals.entrySet()) {
			Department d = CourseRegistrationData.findDepartment(entry.getKey());
			double[] t = entry.getValue();
			result += "<tr><td>" + d.getDepartmentName() + "</td><td class=\"num\">" + (int) t[0] + "</td>"
					+ "<td class=\"num\">" + (int) t[1] + "</td><td class=\"num\">" + (int) t[2] + "</td>"
					+ "<td class=\"num\">" + fillRate(t[2], t[1]) + "</td><td class=\"num\">" + Page.money(t[3])
					+ "</td></tr>";
			for (int i = 0; i < 4; i++) {
				grand[i] += t[i];
			}
		}

		result += "<tr class=\"total\"><td>Total</td><td class=\"num\">" + (int) grand[0] + "</td>"
				+ "<td class=\"num\">" + (int) grand[1] + "</td><td class=\"num\">" + (int) grand[2] + "</td>"
				+ "<td class=\"num\">" + fillRate(grand[2], grand[1]) + "</td><td class=\"num\">"
				+ Page.money(grand[3]) + "</td></tr>";
		result += "</table>";
		result += "<p>Tuition value = seats taken multiplied by the tuition fee of the course.</p>";
		result += Page.close();

		return result;
	}

	private String fillRate(double taken, double capacity) {
		if (capacity == 0) {
			return "0%";
		}
		return Math.round(taken * 100 / capacity) + "%";
	}

	// Exception report: only the records that need attention, instead of the whole tables.
	@GET
	@Path("/exceptions")
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLExceptions(@CookieParam("authToken") String token) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.MANAGER) {
			return Page.denied(current, "MANAGER");
		}

		String result = Page.open("Exception Report");
		result += reportHeading("Exception Report");

		result += "<h3>Sections full or with 2 seats or less</h3>";
		result += "<table border=\"1\"><tr><th class=\"num\">Section</th><th>Course</th><th>Term</th>"
				+ "<th class=\"num\">Enrolled</th><th class=\"num\">Seats Left</th><th>Status</th></tr>";
		int count = 0;
		Iterator<Section> it = CourseRegistrationData.sectionList.iterator();
		while (it.hasNext()) {
			Section s = it.next();
			if (s.seatsAvailable() <= 2) {
				Course c = CourseRegistrationData.findCourse(s.getCourseId());
				result += "<tr><td class=\"num\">" + s.getSectionId() + "</td><td>"
						+ (c == null ? "" : c.getCourseCode() + " " + c.getCourseName()) + "</td><td>"
						+ s.getSemester() + " " + s.getYear() + "</td><td class=\"num\">" + s.getEnrolled() + " / "
						+ s.getCapacity() + "</td><td class=\"num\">" + s.seatsAvailable() + "</td><td>"
						+ (s.isFull() ? "FULL" : "ALMOST FULL") + "</td></tr>";
				count++;
			}
		}
		result += "</table><p>Sections listed: " + count + "</p>";

		result += "<h3>Sections with no student enrolled</h3>";
		result += "<table border=\"1\"><tr><th class=\"num\">Section</th><th>Course</th><th>Term</th>"
				+ "<th>Schedule</th><th class=\"num\">Capacity</th></tr>";
		count = 0;
		it = CourseRegistrationData.sectionList.iterator();
		while (it.hasNext()) {
			Section s = it.next();
			if (s.getEnrolled() == 0) {
				Course c = CourseRegistrationData.findCourse(s.getCourseId());
				result += "<tr><td class=\"num\">" + s.getSectionId() + "</td><td>"
						+ (c == null ? "" : c.getCourseCode() + " " + c.getCourseName()) + "</td><td>"
						+ s.getSemester() + " " + s.getYear() + "</td><td>" + s.getSchedule()
						+ "</td><td class=\"num\">" + s.getCapacity() + "</td></tr>";
				count++;
			}
		}
		result += "</table><p>Sections listed: " + count + "</p>";

		// the students who owe tuition are copied to a list of their own, so sorting it leaves studentList untouched
		ArrayList<Student> owing = new ArrayList<Student>();
		ArrayList<Student> studentList = CourseRegistrationData.studentList;
		for (int i = 0; i < studentList.size(); i++) {
			if (studentList.get(i).getBalance() > 0) {
				owing.add(studentList.get(i));
			}
		}

		// lambda expression: (a, b) -> ... is a small function with no name, given to sort() as the rule
		// for comparing two students. Comparing b with a (instead of a with b) puts the largest balance first.
		owing.sort((a, b) -> Double.compare(b.getBalance(), a.getBalance()));

		result += "<h3>Students with a balance due, largest first</h3>";
		result += "<table border=\"1\"><tr><th class=\"num\">Id</th><th>Student</th><th>Email</th>"
				+ "<th>Phone</th><th class=\"num\">Balance Due</th></tr>";
		double totalDue = 0;
		for (int i = 0; i < owing.size(); i++) {
			Student st = owing.get(i);
			totalDue += st.getBalance();
			result += "<tr><td class=\"num\">" + st.getStudentId() + "</td><td>" + st.fullName() + "</td><td>"
					+ st.getEmail() + "</td><td>" + st.getPhone() + "</td><td class=\"num\">"
					+ Page.money(st.getBalance()) + "</td></tr>";
		}
		result += "<tr class=\"total\"><td></td><td>Total (" + owing.size() + " students)</td><td></td><td></td>"
				+ "<td class=\"num\">" + Page.money(totalDue) + "</td></tr>";
		result += "</table>";
		result += Page.close();

		return result;
	}

	// Detail report: the lines of Enrollment.out (CLERK or above)
	@GET
	@Path("/enrollments")
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLEnrollments(@CookieParam("authToken") String token) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.CLERK) {
			return Page.denied(current, "CLERK");
		}

		String result = Page.open("Enrollment Report");
		result += reportHeading("Enrollments (Enrollment.out)");
		result += "<table border=\"1\"><tr><th>Date</th><th class=\"num\">Student Id</th><th>Student</th>"
				+ "<th class=\"num\">Section</th><th>Course</th><th class=\"num\">Tuition Charged</th></tr>";
		double total = 0;
		ArrayList<Enrollment> enrollmentList = CourseRegistrationData.enrollmentList;
		for (int i = 0; i < enrollmentList.size(); i++) {
			Enrollment e = enrollmentList.get(i);
			total += e.getTuitionCharged();
			result += "<tr><td>" + e.getDate() + "</td><td class=\"num\">" + e.getStudentId() + "</td><td>"
					+ e.getStudentName() + "</td><td class=\"num\">" + e.getSectionId() + "</td><td>"
					+ e.getCourseCode() + "</td><td class=\"num\">" + Page.money(e.getTuitionCharged())
					+ "</td></tr>";
		}
		result += "<tr class=\"total\"><td></td><td></td><td>Total (" + enrollmentList.size()
				+ " enrollments)</td><td></td><td></td><td class=\"num\">" + Page.money(total) + "</td></tr>";
		result += "</table>";
		result += Page.close();

		return result;
	}

	// Detail report: the lines of Payment.out (CLERK or above)
	@GET
	@Path("/payments")
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLPayments(@CookieParam("authToken") String token) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.CLERK) {
			return Page.denied(current, "CLERK");
		}

		String result = Page.open("Payment Report");
		result += reportHeading("Payments (Payment.out)");
		result += "<table border=\"1\"><tr><th>Date</th><th>Receipt No</th><th class=\"num\">Student Id</th>"
				+ "<th>Student</th><th>Method</th><th class=\"num\">Amount Paid</th></tr>";
		double total = 0;
		ArrayList<Payment> paymentList = CourseRegistrationData.paymentList;
		for (int i = 0; i < paymentList.size(); i++) {
			Payment p = paymentList.get(i);
			total += p.getAmountPaid();
			result += "<tr><td>" + p.getDate() + "</td><td>" + p.getReceiptNo() + "</td><td class=\"num\">"
					+ p.getStudentId() + "</td><td>" + p.getStudentName() + "</td><td>" + p.getPaymentMethod()
					+ "</td><td class=\"num\">" + Page.money(p.getAmountPaid()) + "</td></tr>";
		}
		result += "<tr class=\"total\"><td></td><td></td><td></td><td>Total (" + paymentList.size()
				+ " payments)</td><td></td><td class=\"num\">" + Page.money(total) + "</td></tr>";
		result += "</table>";
		result += Page.close();

		return result;
	}
}
