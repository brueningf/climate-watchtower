package com.audit.climate.watchman.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RawEventRepository extends JpaRepository<RawEvent, UUID> {
}
