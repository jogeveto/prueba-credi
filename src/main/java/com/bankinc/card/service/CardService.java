package com.bankinc.card.service;

import com.bankinc.card.exceptions.*;
import com.bankinc.card.model.Card;
import com.bankinc.card.repository.CardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class CardService {

    @Autowired
    private CardRepository cardRepository;

    private static final List<String> NAMES = Arrays.asList("Juan", "Maria", "Carlos", "Ana", "Luis", "Sofia", "Pedro");
    private static final List<String> SURNAMES = Arrays.asList("Gomez", "Perez", "Lopez", "Rodriguez", "Martinez", "Fernandez");


    public String generateCardNumber(String productId) {
        StringBuilder cardNumber = new StringBuilder(productId);
        Random random = new Random();

        while (cardNumber.length() < 16) {
            cardNumber.append(random.nextInt(10));
        }

        LocalDate expirationDate = LocalDate.now().plusYears(3);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");
        String expirationDateFormatted = expirationDate.format(formatter);

        String name = NAMES.get(random.nextInt(NAMES.size()));
        String surname = SURNAMES.get(random.nextInt(SURNAMES.size()));

        Card card = new Card();
        card.setCardId(cardNumber.toString());
        card.setHolderName(name + " " + surname);
        card.setExpirationDate(expirationDateFormatted);
        card.setActive(false);
        card.setBlocked(true);
        card.setBalance(BigDecimal.ZERO);
        
        try {
            cardRepository.save(card);
            return cardNumber.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating card number");
        }
    }

    public void activateCard(String cardId) {
        Card card = cardRepository.findByCardId(cardId)
            .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + cardId));
        
        if (card.isActive()) {
            throw new CardActivationException("Card is already active");
        }
        
        card.setActive(true);
        card.setBlocked(false);
        cardRepository.save(card);
    }

    public void blockCard(String cardId) {
        Card card = cardRepository.findByCardId(cardId)
            .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + cardId));
        
        if (card.isBlocked()) {
            throw new CardBlockedException("Card is already blocked");
        }
        
        card.setBlocked(true);
        cardRepository.save(card);
    }

    public void rechargeBalance(String cardId, BigDecimal balance) {
        Card card = cardRepository.findByCardIdAndIsActive(cardId, true)
            .orElseThrow(() -> new CardNotFoundException("Card not found or not active"));
        
        if (card.isBlocked()) {
            throw new CardBlockedException("Cannot recharge balance: card is blocked");
        }
        
        card.setBalance(card.getBalance().add(balance));
        cardRepository.save(card);
    }

    public BigDecimal getBalance(String cardId) {
        Card card = cardRepository.findByCardId(cardId)
            .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + cardId));
        return card.getBalance();
    }

    public Card getCard(String cardId) {
        return cardRepository.findByCardId(cardId)
            .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + cardId));
    }

    public Card updateCard(Card card) {
        if (card == null || card.getCardId() == null) {
            throw new InvalidCardNumberException("Invalid card data");
        }
        return cardRepository.save(card);
    }
}
