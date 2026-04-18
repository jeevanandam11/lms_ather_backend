package com.example.lms_backend.repository;

import com.example.lms_backend.model.TopicHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicHistoryRepository extends JpaRepository<TopicHistory, Long> {
    List<TopicHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}
