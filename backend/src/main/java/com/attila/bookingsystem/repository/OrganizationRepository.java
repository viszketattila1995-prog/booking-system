package com.attila.bookingsystem.repository;

import com.attila.bookingsystem.domain.Organization;
import com.attila.bookingsystem.domain.enums.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    List<Organization> findByStatus(OrganizationStatus status);
}
