package com.bankinc.card.service;

import com.bankinc.card.exceptions.*;
import com.bankinc.card.model.Card;
import com.bankinc.card.model.Transaction;
import com.bankinc.card.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardService cardService;

    @InjectMocks
    private TransactionService transactionService;

    private Card mockCard;
    private Transaction mockTransaction;
    private static final String VALID_CARD_ID = "1234567890123456";
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("1000");
    private static final BigDecimal VALID_PRICE = new BigDecimal("100");

    @BeforeEach
    void setUp() {
        mockCard = new Card();
        mockCard.setCardId(VALID_CARD_ID);
        mockCard.setActive(true);
        mockCard.setBlocked(false);
        mockCard.setBalance(INITIAL_BALANCE);
        mockCard.setExpirationDate("12/2025");

        mockTransaction = new Transaction();
        mockTransaction.setId(UUID.randomUUID());
        mockTransaction.setCard(mockCard);
        mockTransaction.setPrice(VALID_PRICE);
        mockTransaction.setTimestamp(LocalDateTime.now());
        mockTransaction.setAnulated(false);
    }

    @Nested
    @DisplayName("Purchase Tests")
    class PurchaseTests {
        @Test
        @DisplayName("Should successfully process a purchase")
        void purchase_Success() {
            // Arrange
            when(cardService.getCard(VALID_CARD_ID)).thenReturn(mockCard);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction savedTransaction = invocation.getArgument(0);
                savedTransaction.setId(UUID.randomUUID()); // Aseguramos que tenga un ID
                return savedTransaction;
            });

            // Act
            UUID result = transactionService.purchase(VALID_CARD_ID, VALID_PRICE);

            // Assert
            assertThat(result).isNotNull();
            verify(cardService).updateCard(
                    argThat(card -> card.getBalance().compareTo(INITIAL_BALANCE.subtract(VALID_PRICE)) == 0));
            verify(transactionRepository).save(any(Transaction.class));
        }
        // ... otros tests ...
    }

    @Nested
    @DisplayName("Transaction Anulation Tests")
    class TransactionAnulationTests {
        @Test
        @DisplayName("Should throw exception when trying to anulate after 24 hours")
        void anulateTransaction_After24Hours() {
            // Arrange
            UUID transactionId = mockTransaction.getId();
            mockTransaction.setTimestamp(LocalDateTime.now().minusDays(2));

            when(transactionRepository.findById(transactionId))
                    .thenReturn(Optional.of(mockTransaction));

            // Act & Assert
            assertThatThrownBy(() -> transactionService.anulateTransaction(VALID_CARD_ID, transactionId))
                    .isInstanceOf(TransactionProcessingException.class) // Cambiado a TransactionProcessingException
                    .hasMessageContaining("Transaction cannot be anulated after 24 hours");
        }

        @Nested
        @DisplayName("Transaction Retrieval Tests")
        class TransactionRetrievalTests {
            @Test
            @DisplayName("Should successfully retrieve a transaction")
            void getTransaction_Success() {
                String transactionId = mockTransaction.getId().toString();
                when(transactionRepository.findById(any(UUID.class)))
                        .thenReturn(Optional.of(mockTransaction));

                Transaction result = transactionService.getTransaction(transactionId);

                assertThat(result)
                        .isNotNull()
                        .isEqualTo(mockTransaction);
            }

            @Test
            @DisplayName("Should throw exception when transaction is not found")
            void getTransaction_NotFound() {
                String transactionId = UUID.randomUUID().toString();
                when(transactionRepository.findById(any(UUID.class)))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> transactionService.getTransaction(transactionId))
                        .isInstanceOf(TransactionNotFoundException.class);
            }
        }
    }
}
