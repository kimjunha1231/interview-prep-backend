package com.junha.interview.repository;

import com.junha.interview.domain.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    java.util.Optional<InterviewSession> findByAccessKey(String accessKey);
}
