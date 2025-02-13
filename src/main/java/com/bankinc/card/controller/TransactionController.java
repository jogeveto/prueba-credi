package com.bankinc.card.controller;

import com.bankinc.card.exceptions.InvalidTransactionException;
import com.bankinc.card.exceptions.TransactionNotFoundException;
import com.bankinc.card.model.Transaction;
import com.bankinc.card.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/transaction")
@Tag(name = "TransactionController", description = "En esta API manejamos todo lo relacionado a los movimientos de la tarjeta de credito")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Operation(summary = "Realizamos un debito al cupo de la tarjeta de credito")
    @PostMapping("/purchase")
    public ResponseEntity<String> purchase(@RequestBody Map<String, Object> payload) {
        if (!payload.containsKey("cardId") || !payload.containsKey("price")) {
            throw new InvalidTransactionException("cardId and price are required");
        }
        
        String cardId = payload.get("cardId").toString();
        BigDecimal price = new BigDecimal(payload.get("price").toString());
        String response = transactionService.purchase(cardId, price).toString();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Verificamos un movimiento de la tarjeta de credito")
    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new InvalidTransactionException("Transaction ID is required");
        }
        
        Transaction response = transactionService.getTransaction(transactionId);
        if (response == null) {
            throw new TransactionNotFoundException("Transaction not found with ID: " + transactionId);
        }
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Anulamos un movieminto de la tarjeta de credito si es menor a 24 horas de haberse realizado")
    @PostMapping("/anulation")
    public ResponseEntity<Boolean> anulateTransaction(@RequestBody Map<String, Object> payload) {
        if (!payload.containsKey("cardId") || !payload.containsKey("transactionId")) {
            throw new InvalidTransactionException("cardId and transactionId are required");
        }
        
        String cardId = payload.get("cardId").toString();
        String transactionId = payload.get("transactionId").toString();
        Boolean response = transactionService.anulateTransaction(cardId, UUID.fromString(transactionId));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
