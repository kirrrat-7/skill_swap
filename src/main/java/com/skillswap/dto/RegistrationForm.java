package com.skillswap.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationForm {
    @NotBlank(message = "Enter your full name.")
    @Size(max = 100, message = "Name must be 100 characters or fewer.")
    @Pattern(regexp = "[\\p{L}\\p{M} .,'’\\-]+", message = "Use letters, spaces, apostrophes, periods, or hyphens in your name.")
    private String fullName;

    @NotBlank(message = "Enter your email address.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 254, message = "Email must be 254 characters or fewer.")
    private String email;

    @NotBlank(message = "Create a password.")
    @Size(min = 12, max = 72, message = "Use 12 to 72 characters.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "Use at least one uppercase letter, one lowercase letter, and one number.")
    private String password;

    @NotBlank(message = "Confirm your password.")
    private String confirmPassword;

    @Size(max = 150, message = "College or university must be 150 characters or fewer.")
    private String college;

    @Size(max = 100, message = "Department must be 100 characters or fewer.")
    private String department;

    @Min(value = 1, message = "Semester must be between 1 and 12.")
    @Max(value = 12, message = "Semester must be between 1 and 12.")
    private Integer semester;

    @AssertTrue(message = "Accept the terms and privacy policy to create an account.")
    private boolean termsAccepted;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
    public boolean isTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(boolean termsAccepted) { this.termsAccepted = termsAccepted; }
}
