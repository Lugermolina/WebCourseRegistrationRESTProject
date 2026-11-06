package webCourseRegistrationREST;

public class Payment {

	private String receiptNo;
	private int studentId;
	private String studentName;
	private double amountPaid;
	private String paymentMethod;
	private String status;
	private String message;
	private double newBalance;
	// new: day of the transaction, stored in Payment.out
	private String date;

	public Payment() {
		receiptNo = "";
		studentId = 0;
		studentName = "";
		amountPaid = 0;
		paymentMethod = "";
		status = "";
		message = "";
		newBalance = 0;
		date = "";
	}

	public Payment(String receiptNo, int studentId, String studentName, double amountPaid, String paymentMethod,
			String status, String message, double newBalance) {
		this.receiptNo = receiptNo;
		this.studentId = studentId;
		this.studentName = studentName;
		this.amountPaid = amountPaid;
		this.paymentMethod = paymentMethod;
		this.status = status;
		this.message = message;
		this.newBalance = newBalance;
		date = "";
	}

	public String getReceiptNo() {
		return receiptNo;
	}

	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}

	public int getStudentId() {
		return studentId;
	}

	public void setStudentId(int studentId) {
		this.studentId = studentId;
	}

	public String getStudentName() {
		return studentName;
	}

	public void setStudentName(String studentName) {
		this.studentName = studentName;
	}

	public double getAmountPaid() {
		return amountPaid;
	}

	public void setAmountPaid(double amountPaid) {
		this.amountPaid = amountPaid;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public double getNewBalance() {
		return newBalance;
	}

	public void setNewBalance(double newBalance) {
		this.newBalance = newBalance;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}
}
