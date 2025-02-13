package com.bankinc.card.repository;

import com.bankinc.card.model.Card;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {
    Optional<Card> findByCardId(String cardId);
    Optional<Card> findByCardIdAndIsActive(String cardId, boolean isActive);
}
