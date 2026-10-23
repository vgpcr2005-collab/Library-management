package edu.alliance.library.model;

public final class Student extends User {
    private final long studentRecordId;
    private final String studentId;
    private final String department;
    private final String phone;

    public Student(long userId, String email, String fullName, long studentRecordId,
                   String studentId, String department, String phone) {
        super(userId, email, fullName, "STUDENT");
        if (studentRecordId < 0) throw new IllegalArgumentException("Invalid student record");
        this.studentRecordId = studentRecordId;
        this.studentId = required(studentId);
        this.department = required(department);
        this.phone = required(phone);
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Student fields are required");
        return value.trim();
    }

    public long getStudentRecordId() { return studentRecordId; }
    public String getStudentId() { return studentId; }
    public String getDepartment() { return department; }
    public String getPhone() { return phone; }
}
