package webCourseRegistrationREST;

public class Section {

	private int sectionId;
	private int courseId;
	private int instructorId;
	private String semester;
	private int year;
	private String schedule;
	private String room;
	private int capacity;
	private int enrolled;

	public Section() {
		sectionId = 0;
		courseId = 0;
		instructorId = 0;
		semester = "";
		year = 0;
		schedule = "";
		room = "";
		capacity = 0;
		enrolled = 0;
	}

	public Section(int sectionId, int courseId, int instructorId, String semester, int year, String schedule, String room, int capacity, int enrolled) {
		this.sectionId = sectionId;
		this.courseId = courseId;
		this.instructorId = instructorId;
		this.semester = semester;
		this.year = year;
		this.schedule = schedule;
		this.room = room;
		this.capacity = capacity;
		this.enrolled = enrolled;
	}

	public int getSectionId() {
		return sectionId;
	}

	public void setSectionId(int sectionId) {
		this.sectionId = sectionId;
	}

	public int getCourseId() {
		return courseId;
	}

	public void setCourseId(int courseId) {
		this.courseId = courseId;
	}

	public int getInstructorId() {
		return instructorId;
	}

	public void setInstructorId(int instructorId) {
		this.instructorId = instructorId;
	}

	public String getSemester() {
		return semester;
	}

	public void setSemester(String semester) {
		this.semester = semester;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public String getSchedule() {
		return schedule;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	public String getRoom() {
		return room;
	}

	public void setRoom(String room) {
		this.room = room;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public int getEnrolled() {
		return enrolled;
	}

	public void setEnrolled(int enrolled) {
		this.enrolled = enrolled;
	}

	public int seatsAvailable() {
		return capacity - enrolled;
	}

	public boolean isFull() {
		return enrolled >= capacity;
	}

	public void reserveSeat() {
		enrolled++;
	}
}
