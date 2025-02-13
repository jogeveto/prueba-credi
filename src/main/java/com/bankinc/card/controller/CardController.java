package com.bankinc.card.controller;

import com.bankinc.card.exceptions.*;
import com.bankinc.card.service.CardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/card")
@Tag(name = "CardController", description = "En esta API manejamos todo lo relacionado a la tarjeta de credito")
public class CardController {

    @Autowired
    private CardService cardService;

    @Operation(summary = "Creamos una tarjeta de credito con los 6 digitos del producto, se asignan nombres y apellidos al azar")
    @GetMapping("/{productId}/number")
    public ResponseEntity<String> generateCardNumber(@PathVariable String productId) {
        if (productId.length() != 6 || !productId.matches("-?\\d{6}")) {
            throw new InvalidCardNumberException("Product ID must be a 6-digit number");
        }
        String cardNumber = cardService.generateCardNumber(productId);
        return new ResponseEntity<>(cardNumber, HttpStatus.OK);
    }

    @Operation(summary = "Activamos la tarjeta de credito")
    @PostMapping("/enroll")
    public ResponseEntity<String> activateCard(@RequestBody Map<String, String> payload) {
        if (!payload.containsKey("cardId")) {
            throw new InvalidCardNumberException("Card ID is required");
        }
        cardService.activateCard(payload.get("cardId"));
        return new ResponseEntity<>("Card activated successfully", HttpStatus.OK);
    }

    @Operation(summary = "Bolqueamos la tarjeta de credito")
    @DeleteMapping("/{cardId}")
    public ResponseEntity<String> blockCard(@PathVariable String cardId) {
        cardService.blockCard(cardId);
        return new ResponseEntity<>("Card blocked successfully", HttpStatus.OK);
    }

    @Operation(summary = "Recargamos el cupo de la tajeta de credito")
    @PostMapping("/balance")
    public ResponseEntity<String> rechargeBalance(@RequestBody Map<String, Object> payload) {
        if (!payload.containsKey("cardId") || !payload.containsKey("balance")) {
            throw new InvalidTransactionException("Card ID and balance are required");
        }
        
        String cardId = payload.get("cardId").toString();
        BigDecimal balance;
        try {
            balance = new BigDecimal(payload.get("balance").toString());
            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidBalanceException("Balance must be greater than zero");
            }
        } catch (NumberFormatException e) {
            throw new InvalidBalanceException("Invalid balance format");
        }
        
        cardService.rechargeBalance(cardId, balance);
        return new ResponseEntity<>("Balance recharged successfully", HttpStatus.OK);
    }

    @Operation(summary = "Obtenemos el cupo de la tarjeta de credito")
    @GetMapping("/balance/{cardId}")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String cardId) {
        BigDecimal balance = cardService.getBalance(cardId);
        return new ResponseEntity<>(balance, HttpStatus.OK);
    }
}

