package com.audit.climate.watchman.processing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ThresholdRepository extends JpaRepository<ThresholdEntry, UUID> {
    Optional<ThresholdEntry> findByModuleAndMetric(String module, String metric);
}
