package com.example.wmbservice.repository;

import com.example.wmbservice.model.Criticality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CriticalityRepository extends JpaRepository<Criticality, Long> {
    Optional<Criticality> findByNameIgnoreCase(String name);
}
