package com.FMS.entity;

import com.FMS.enums.Role;
import com.FMS.enums.Gender;
import com.FMS.model.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.time.LocalDate;

@Entity
@Table(name = "users")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "INVALID_USERNAME")
    @Size(min = 6, max = 50, message = "INVALID_USERNAME")
    String username;

    @NotBlank(message = "INVALID_PASSWORD")
    @Size(min = 8, message = "INVALID_PASSWORD")
    String password;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "INVALID_KEY")
    Role role;

    String employeeCode;
    String fullName;
    String phone;
    String email;
    String address;
    String idNumber;

    LocalDate dob;

    @Enumerated(EnumType.STRING)
    Gender gender;

    String position;
    LocalDate hireDate;

    @Column(columnDefinition = "TEXT")
    String avatarUrl;

    @Builder.Default
    Boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    Boolean mustChangePassword = false;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return active == null || active; }
}
