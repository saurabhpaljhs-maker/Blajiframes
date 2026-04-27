package com.balaji.repository;

import com.balaji.model.BeadingOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BeadingOptionRepository extends JpaRepository<BeadingOption, Long> {
}
