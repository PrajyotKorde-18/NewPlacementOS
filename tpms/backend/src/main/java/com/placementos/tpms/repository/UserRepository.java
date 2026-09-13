package com.placementos.tpms.repository;

import com.placementos.tpms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(String role);

    boolean existsByEmail(String email);

    boolean existsByRole(String role);
}
