package com.smartstay.pms.security.repository;

import com.smartstay.pms.security.entity.SecurityQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityQuestionRepository extends JpaRepository<SecurityQuestion, Long> {
    List<SecurityQuestion> findByActiveTrue();
}
