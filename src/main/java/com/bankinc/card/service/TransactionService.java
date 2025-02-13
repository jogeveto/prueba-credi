package com.bankinc.card.service;

import com.bankinc.card.exceptions.*;
import com.bankinc.card.model.Card;
import com.bankinc.card.model.Transaction;
import com.bankinc.card.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CardService cardService;

    @Transactional
    public UUID purchase(String cardId, BigDecimal price) {
        // Validar que el precio sea positivo
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transaction amount must be greater than zero");
        }

        // Obtener y validar la existencia de la tarjeta
        Card card = cardService.getCard(cardId);
        if (card == null) {
            throw new CardNotFoundException("Card not found");
        }

        // Validar que la tarjeta esté activa
        if (!card.isActive()) {
            throw new CardNotActiveException("Card is not activated");
        }

        // Validar que la tarjeta no esté bloqueada
        if (card.isBlocked()) {
            throw new CardBlockedException("Card is blocked");
        }

        // Validar la fecha de vencimiento
        LocalDateTime now = LocalDateTime.now();
        if (isCardExpired(card.getExpirationDate(), now)) {
            throw new CardExpiredException("Card is expired");
        }

        // Validar el saldo disponible
        if (card.getBalance().compareTo(price) < 0) {
            throw new InsufficientFundsException("Insufficient funds: available balance is " 
                + card.getBalance());
        }

        try {
            // Crear la transacción
            Transaction transaction = new Transaction();
            transaction.setCard(card);
            transaction.setPrice(price);
            transaction.setTimestamp(now);
            transaction.setAnulated(false);

            // Actualizar el balance de la tarjeta
            card.setBalance(card.getBalance().subtract(price));
            cardService.updateCard(card);

            // Guardar la transacción
            transactionRepository.save(transaction);
            
            return transaction.getId();

        } catch (Exception e) {
            throw new TransactionProcessingException("Error processing transaction: " + e.getMessage());
        }
    }

    // Método auxiliar para validar la fecha de vencimiento
    private boolean isCardExpired(String expirationDate, LocalDateTime currentDate) {
        // Asumiendo que expirationDate está en formato "MM/yy"
        try {
            String[] parts = expirationDate.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = 2000 + Integer.parseInt(parts[1]); // Convertir yy a yyyy

            LocalDateTime cardExpiration = LocalDateTime.of(year, month, 1, 0, 0)
                .plusMonths(1).minusSeconds(1); // Último día del mes a las 23:59:59

            return currentDate.isAfter(cardExpiration);
        } catch (Exception e) {
            throw new InvalidCardDataException("Invalid expiration date format");
        }
    }


    public Transaction getTransaction(String transactionId) {
        try {
            UUID uuid = UUID.fromString(transactionId);
            Transaction transaction = transactionRepository.findById(uuid)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + transactionId));
            return transaction;
        } catch (IllegalArgumentException e) {
            throw new InvalidTransactionException("Invalid transaction ID format");
        }
    }

    @Transactional
public boolean anulateTransaction(String cardId, UUID transactionId) {
    try {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new TransactionProcessingException("Transaction not found"));

        if (!transaction.getCard().getCardId().equals(cardId)) {
            throw new InvalidTransactionException("Transaction does not belong to this card");
        }

        if (transaction.getTimestamp().isBefore(LocalDateTime.now().minusHours(24))) {
            throw new InvalidTransactionException("Transaction cannot be anulated after 24 hours");
        }

        if (transaction.isAnulated()) {
            throw new InvalidTransactionException("Transaction already anulated");
        }

        transaction.setAnulated(true);
        
        Card card = transaction.getCard();
        card.setBalance(card.getBalance().add(transaction.getPrice()));
        cardService.updateCard(card);
        
        transactionRepository.save(transaction);
        
        return true;

    } catch (Exception e) {
        throw new TransactionProcessingException("Error processing transaction anulation: " + e.getMessage());
    }
}

}
