package com.junha.interview.repository;

import com.junha.interview.domain.EmailSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailSubscriptionRepository extends JpaRepository<EmailSubscription, Long> {

    Optional<EmailSubscription> findByEmailHash(String emailHash);

    List<EmailSubscription> findAllByActiveTrue();
}
