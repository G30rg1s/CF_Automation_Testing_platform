package gr.aueb.cf.automation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    @Column(name = "email", nullable = false, length = 254, columnDefinition = "VARCHAR(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must contain exactly 10 digits")
    @Pattern(regexp = "^69.*$", message = "Phone number must start with 69")
    @Column(name = "phone", nullable = false, length = 16)
    private String phone;

    @NotBlank(message = "Father's name is required")
    @Size(max = 100, message = "Father's name must not exceed 100 characters")
    @Column(name = "father_name", nullable = false, length = 100)
    private String fatherName;

    @NotBlank(message = "Mother's name is required")
    @Size(max = 100, message = "Mother's name must not exceed 100 characters")
    @Column(name = "mother_name", nullable = false, length = 100)
    private String motherName;

    @NotNull(message = "Age is required")
    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 120, message = "Age must not exceed 120")
    @Column(name = "age", nullable = false)
    private Integer age;

    @NotBlank(message = "Password hash is required")
    @Size(max = 100, message = "Password hash must not exceed 100 characters")
    @ToString.Exclude
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
}