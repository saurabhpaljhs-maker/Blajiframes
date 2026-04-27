package com.balaji.repository;

import com.balaji.model.FrameSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FrameSizeRepository extends JpaRepository<FrameSize, Long> {
    Optional<FrameSize> findBySize(String size);
}
