package com.bank.cli;

import com.bank.api.Card;
import com.bank.api.ICardService;

/**
 * Command class for managing card-related operations in the banking CLI.
 * Scope: card
 */
public class CardCommands {
    private final ICardService cardService;
    
    public CardCommands(ICardService cardService) {
        this.cardService = cardService;
    }

    /**
     * card:create <identificationNo> <password> <pinNumber>
     */
    public String create(String identificationNo, String password, String pinNumber) {
        var card = cardService.createCard(identificationNo, password, pinNumber);
        return card == null ? "Failed to create card" : format(card);
    }

    /**
     * card:get <identificationNo> <password>
     */
    public String get(String identificationNo, String password) {
        var card = cardService.getCard(identificationNo, password);
        return card == null ? "Failed to retrieve card" : format(card);
    }

    /**
     * card:pin <identificationNo> <password> <currentPin> <newPin>
     */
    public String pin(String identificationNo, String password, String currentPin, String newPin) {
        var card = cardService.updateCardPin(identificationNo, password, currentPin, newPin);
        return card == null ? "Failed to update PIN" : format(card);
    }

    /**
     * card:status <identificationNo> <password> <ACTIVATE|DEACTIVATE|FREEZE|UNFREEZE> <pinNumber>
     */
    public String status(String identificationNo, String password, String action, String pinNumber) {
        ICardService.UpdateAction parsedAction;
        try {
            parsedAction = ICardService.UpdateAction.valueOf(action.toUpperCase());
        } catch (Exception e) {
            return "Invalid action. Use ACTIVATE, DEACTIVATE, FREEZE, or UNFREEZE.";
        }
        var card = cardService.updateCardStatus(identificationNo, password, parsedAction, pinNumber);
        return card == null ? "Failed to update status" : format(card);
    }

    /**
     * card:limit <identificationNo> <password> <newLimit> <pinNumber>
     */
    public String limit(String identificationNo, String password, int newLimit, String pinNumber) {
        var card = cardService.updateCardTransactionLimit(identificationNo, password, newLimit, pinNumber);
        return card == null ? "Failed to update transaction limit" : format(card);
    }
    
    public String format(Card card) {
        return String.format(
                "Card Number: %s | Card Status: %s | Card Transaction Limit: %d",
                card.getCardNumber(),
                card.getStatus(),
                card.getTransactionLimit()
        );
    }
}
