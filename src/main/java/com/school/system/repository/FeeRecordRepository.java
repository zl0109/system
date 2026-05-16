package com.school.system.repository;

import com.school.system.entity.FeeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeeRecordRepository extends JpaRepository<FeeRecord, Integer> {
    // 根据任务ID和家长ID寻找那条唯一的缴费记录（用于支付时核销）
    FeeRecord findByTaskIdAndParentId(Integer taskId, Integer parentId);

    // 查询某个任务的所有缴费记录（后续管理台账用）
    List<FeeRecord> findByTaskId(Integer taskId);
}