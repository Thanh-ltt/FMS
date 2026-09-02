package com.FMS.repositories;

import com.FMS.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import com.FMS.enums.Role;
@Repository
public interface UserRepository extends JpaRepository<User,String> {
        Optional<User> findByUsername(String username);
        boolean existsByUsername(String username);
        boolean existsByUsernameAndIdNot(String username, String id);
        boolean existsByEmployeeCode(String employeeCode);
        boolean existsByEmployeeCodeAndIdNot(String employeeCode, String id);
        List<User> findByRoleIn(List<Role> roles);
        boolean existsByRole(Role role);
}
