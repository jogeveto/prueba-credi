package com.bankinc.card.service;

import com.bankinc.card.exceptions.*;
import com.bankinc.card.model.Card;
import com.bankinc.card.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    private Card mockCard;
    private static final String VALID_CARD_ID = "1234567890123456";
    private static final String VALID_PRODUCT_ID = "123456";
    private static final BigDecimal INITIAL_BALANCE = BigDecimal.ZERO;

    @BeforeEach
    void setUp() {
        mockCard = new Card();
        mockCard.setCardId(VALID_CARD_ID);
        mockCard.setActive(false);
        mockCard.setBlocked(true);
        mockCard.setBalance(INITIAL_BALANCE);
        mockCard.setHolderName("John Doe");
        mockCard.setExpirationDate(LocalDate.now().plusYears(3)
                .format(DateTimeFormatter.ofPattern("MM/yyyy")));
    }

    @Nested
    @DisplayName("Card Generation Tests")
    class CardGenerationTests {
        @Test
        @DisplayName("Should successfully generate a card number")
        void generateCardNumber_Success() {
            when(cardRepository.save(any(Card.class))).thenReturn(mockCard);

            String cardNumber = cardService.generateCardNumber(VALID_PRODUCT_ID);

            assertThat(cardNumber)
                    .isNotNull()
                    .hasSize(16)
                    .startsWith(VALID_PRODUCT_ID);
            verify(cardRepository).save(any(Card.class));
        }

        @Test
        @DisplayName("Should throw exception when card generation fails")
        void generateCardNumber_Failure() {
            when(cardRepository.save(any(Card.class))).thenThrow(new RuntimeException());

            assertThatThrownBy(() -> cardService.generateCardNumber(VALID_PRODUCT_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Error generating card number");
        }
    }

    @Nested
    @DisplayName("Card Activation Tests")
    class CardActivationTests {
        @Test
        @DisplayName("Should successfully activate a card")
        void activateCard_Success() {
            when(cardRepository.findByCardId(VALID_CARD_ID)).thenReturn(Optional.of(mockCard));
            when(cardRepository.save(any(Card.class))).thenReturn(mockCard);

            cardService.activateCard(VALID_CARD_ID);

            verify(cardRepository).save(argThat(card -> 
                card.isActive() && !card.isBlocked()
            ));
        }

        @Test
        @DisplayName("Should throw exception when activating non-existent card")
        void activateCard_NotFound() {
            when(cardRepository.findByCardId(VALID_CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.activateCard(VALID_CARD_ID))
                    .isInstanceOf(CardNotFoundException.class)
                    .hasMessage("Card not found with ID: " + VALID_CARD_ID);
        }

        @Test
        @DisplayName("Should throw exception when activating already active card")
        void activateCard_AlreadyActive() {
            mockCard.setActive(true);
            when(cardRepository.findByCardId(VALID_CARD_ID)).thenReturn(Optional.of(mockCard));

            assertThatThrownBy(() -> cardService.activateCard(VALID_CARD_ID))
                    .isInstanceOf(CardActivationException.class)
                    .hasMessage("Card is already active");
        }
    }

    @Nested
    @DisplayName("Card Blocking Tests")
    class CardBlockingTests {
        @Test
        @DisplayName("Should successfully block a card")
        void blockCard_Success() {
            mockCard.setBlocked(false);
            when(cardRepository.findByCardId(VALID_CARD_ID)).thenReturn(Optional.of(mockCard));
            when(cardRepository.save(any(Card.class))).thenReturn(mockCard);

            cardService.blockCard(VALID_CARD_ID);

            verify(cardRepository).save(argThat(Card::isBlocked));
        }

        @Test
        @DisplayName("Should throw exception when blocking already blocked card")
        void blockCard_AlreadyBlocked() {
            when(cardRepository.findByCardId(VALID_CARD_ID)).thenReturn(Optional.of(mockCard));

            assertThatThrownBy(() -> cardService.blockCard(VALID_CARD_ID))
                    .isInstanceOf(CardBlockedException.class)
                    .hasMessage("Card is already blocked");
        }
    }

    @Nested
    @DisplayName("Balance Management Tests")
    class BalanceManagementTests {
        @Test
        @DisplayName("Should successfully recharge balance")
        void rechargeBalance_Success() {
            mockCard.setActive(true);
            mockCard.setBlocked(false);
            BigDecimal rechargeAmount = new BigDecimal("100");
            when(cardRepository.findByCardIdAndIsActive(VALID_CARD_ID, true))
                    .thenReturn(Optional.of(mockCard));
            when(cardRepository.save(any(Card.class))).thenReturn(mockCard);

            cardService.rechargeBalance(VALID_CARD_ID, rechargeAmount);

            verify(cardRepository).save(argThat(card -> 
                card.getBalance().compareTo(INITIAL_BALANCE.add(rechargeAmount)) == 0
            ));
        }

        @Test
        @DisplayName("Should throw exception when recharging blocked card")
        void rechargeBalance_BlockedCard() {
            mockCard.setActive(true);
            when(cardRepository.findByCardIdAndIsActive(VALID_CARD_ID, true))
                    .thenReturn(Optional.of(mockCard));

            assertThatThrownBy(() -> cardService.rechargeBalance(VALID_CARD_ID, BigDecimal.TEN))
                    .isInstanceOf(CardBlockedException.class)
                    .hasMessage("Cannot recharge balance: card is blocked");
        }

        @Test
        @DisplayName("Should successfully get balance")
        void getBalance_Success() {
            when(cardRepository.findByCardId(VALID_CARD_ID)).thenReturn(Optional.of(mockCard));

            BigDecimal balance = cardService.getBalance(VALID_CARD_ID);

            assertThat(balance).isEqualTo(INITIAL_BALANCE);
        }
    }

    @Nested
    @DisplayName("Card Management Tests")
    class CardManagementTests {
        @Test
        @DisplayName("Should successfully get card")
        void getCard_Success() {
            when(cardRepository.findByCardId(VALID_CARD_ID)).thenReturn(Optional.of(mockCard));

            Card result = cardService.getCard(VALID_CARD_ID);

            assertThat(result).isEqualTo(mockCard);
        }

        @Test
        @DisplayName("Should successfully update card")
        void updateCard_Success() {
            when(cardRepository.save(any(Card.class))).thenReturn(mockCard);

            Card result = cardService.updateCard(mockCard);

            assertThat(result).isEqualTo(mockCard);
            verify(cardRepository).save(mockCard);
        }

        @Test
        @DisplayName("Should throw exception when updating invalid card")
        void updateCard_Invalid() {
            assertThatThrownBy(() -> cardService.updateCard(null))
                    .isInstanceOf(InvalidCardNumberException.class)
                    .hasMessage("Invalid card data");
        }
    }
}
