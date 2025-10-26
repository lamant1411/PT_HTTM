package com.btl.serverml.repository;

import com.btl.serverml.entity.ManagedModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelRepository extends JpaRepository<ManagedModel, Long> {
}
