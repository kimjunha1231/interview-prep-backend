package com.junha.interview.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "interview_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_key", nullable = false, unique = true, length = 36)
    private String accessKey;

    @Column(name = "email_sent", nullable = false)
    private boolean emailSent = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = true)
    private Member member;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Transient
    private List<Question> questions;

    @PrePersist
    public void prePersist() {
        if (this.accessKey == null) {
            this.accessKey = java.util.UUID.randomUUID().toString();
        }
    }

    public InterviewSession(Long id, Member member, LocalDateTime createdAt) {
        this.id = id;
        this.member = member;
        this.createdAt = createdAt;
    }
}
