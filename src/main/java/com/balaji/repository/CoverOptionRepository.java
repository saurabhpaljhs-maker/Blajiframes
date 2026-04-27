package com.balaji.repository;

import com.balaji.model.CoverOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoverOptionRepository extends JpaRepository<CoverOption, Long> {
}
