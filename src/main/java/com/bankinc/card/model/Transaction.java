package com.bankinc.card.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;
    private LocalDateTime timestamp;
    private boolean anulated;
    @ManyToOne
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    // Constructor vacío
    public Transaction() {
    }

    // Constructor con todos los campos
    public Transaction(String transactionId, Card card, BigDecimal price, 
                      LocalDateTime timestamp, boolean anulated) {
        this.card = card;
        this.price = price;
        this.timestamp = timestamp;
        this.anulated = anulated;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setTransactionId(UUID id) {
        this.id = id;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isAnulated() {
        return anulated;
    }

    public void setAnulated(boolean anulated) {
        this.anulated = anulated;
    }
}
