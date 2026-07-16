package com.attila.bookingsystem.repository;

import com.attila.bookingsystem.domain.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppRoleRepository extends JpaRepository<AppRole, Integer> {

    Optional<AppRole> findByName(String name);
}
