package com.placementos.tpms.repository;

import com.placementos.tpms.entity.RoleProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleProfileRepository extends JpaRepository<RoleProfile, Long> {

    Optional<RoleProfile> findByDriveId(Long driveId);
}
