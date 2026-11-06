package webCourseRegistrationService;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.ArrayList;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.jws.soap.SOAPBinding.Style;

@WebService  // indicate that my service is SOAP
@SOAPBinding(style = Style.RPC)
public class CourseRegistrationService {

	// login module: checks the username and the password against the Users table of SQL Server,
	// the same accounts as the REST login
	@WebMethod
	public String login(@WebParam(name = "Username") String username, @WebParam(name = "Password") String password)
	{
		try
		{
			String[] account = ServiceData.findAccount(username, password);
			if (account == null)
			{
				return "Login REJECTED: wrong username or password.";
			}
			return "Login CONFIRMED for " + username + ". Access level: " + account[0] + ".";
		}
		catch (SQLException e)
		{
			return "Connection fails! " + e.getMessage();
		}
	}

	// courses whose code or name contains the keyword, one line per course (open to everyone, as in REST)
	@WebMethod
	public String searchCourses(@WebParam(name = "Keyword") String keyword)
	{
		String c_Records = "";
		int count = 0;

		try
		{
			ArrayList<Course> courseList = ServiceData.readCourses();

			for (int i = 0; i < courseList.size(); i++)
			{
				Course course = courseList.get(i);
				if (course.getCourseCode().toLowerCase().contains(keyword.toLowerCase())
						|| course.getCourseName().toLowerCase().contains(keyword.toLowerCase()))
				{
					c_Records += course.getCourseCode() + " " + course.getCourseName()
							+ ", " + course.getCredits() + " credits, "
							+ ServiceData.departmentName(course.getDepartmentId()) + "\n";
					count++;
				}
			}
		}
		catch (SQLException e) // updated: the courses now come from SQL Server
		{
			return "Connection fails! " + e.getMessage();
		}

		return c_Records + "Courses found: " + count;
	}

	// one course as an object, like searchBillingInfo in Lab 5; an unknown code returns an empty answer
	@WebMethod
	public Course findCourse(@WebParam(name = "CourseCode") String courseCode)
	{
		Course result = null;

		try
		{
			ArrayList<Course> courseList = ServiceData.readCourses();

			for (int i = 0; i < courseList.size(); i++)
			{
				if (courseList.get(i).getCourseCode().equalsIgnoreCase(courseCode))
				{
					result = courseList.get(i);
				}
			}
		}
		catch (SQLException e) // updated: the courses now come from SQL Server
		{
			System.out.println("Course table not read: " + e.getMessage());
			return null;
		}

		return result;
	}

	// tuition balance of a student account. SOAP keeps no cookie, so the credentials come with each call.
	@WebMethod
	public String getBalance(@WebParam(name = "Username") String username, @WebParam(name = "Password") String password)
	{
		try
		{
			String[] account = ServiceData.findAccount(username, password);
			if (account == null)
			{
				return "Balance REJECTED: wrong username or password.";
			}
			if (!account[0].equals("STUDENT"))
			{
				return "Balance REJECTED: " + username + " is a " + account[0] + " account; only students have a balance.";
			}

			String[] student = ServiceData.findStudent(Integer.parseInt(account[1]));
			if (student == null)
			{
				return "Balance REJECTED: student " + account[1] + " was not found.";
			}

			double balance = Double.parseDouble(student[7]);
			String status = "PAYMENT DUE";
			if (balance == 0)
			{
				status = "PAID IN FULL";
			}
			return "Balance of " + student[1] + " " + student[2] + ": " + String.format("%.2f", balance) + "$ ("
					+ status + ")";
		}
		catch (SQLException e)
		{
			return "Connection fails! " + e.getMessage();
		}
		catch (FileNotFoundException e)
		{
			return "Student.in file not found";
		}
	}
}
