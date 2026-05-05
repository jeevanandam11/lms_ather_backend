package com.example.lms_backend.repository;

import com.example.lms_backend.model.MockInterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MockInterviewSessionRepo extends JpaRepository<MockInterviewSession, Long> {
    List<MockInterviewSession> findByUserIdOrderByCreatedAtDesc(Long userId);
}
