package com.jesuspartal.specforge.infrastructure.repository;

import com.jesuspartal.specforge.domain.model.Spec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecRepository extends JpaRepository<Spec, Long> {
    List<Spec> findByOwnerLogin(String ownerLogin);
}