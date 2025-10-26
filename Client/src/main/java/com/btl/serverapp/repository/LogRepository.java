package com.btl.serverapp.repository;

import com.btl.serverapp.entity.ViolationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<ViolationLog, Long> {
}
