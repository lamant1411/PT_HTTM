package com.btl.serverapp.repository;

import com.btl.serverapp.entity.ViolationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogRepository extends JpaRepository<ViolationLog, Long> {
}