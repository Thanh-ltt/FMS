package com.FMS.repositories;

import com.FMS.entity.TripProof;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TripProofRepository extends JpaRepository<TripProof, String> {
    Optional<TripProof> findByTripId(String tripId);
    boolean existsByTripId(String tripId);
}
