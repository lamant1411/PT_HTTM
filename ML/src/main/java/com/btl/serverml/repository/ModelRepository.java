package com.btl.serverml.repository;

import com.btl.serverml.entity.ManagedModel;
import com.btl.serverml.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRepository extends JpaRepository<ManagedModel, Long> {

    // Tìm model đang được lock cho một Problem
    Optional<ManagedModel> findByProblemAndIsLocked(Problem problem, boolean isLocked);

    // Tìm tất cả model của một Problem
    List<ManagedModel> findByProblem(Problem problem);

    // Query này sẽ "Unlock" tất cả các model khác của cùng 1 Problem
    @Modifying
    @Query("UPDATE ManagedModel m SET m.isLocked = false WHERE m.problem = :problem AND m.id <> :modelId")
    void unlockOtherModels(Problem problem, Long modelId);
}