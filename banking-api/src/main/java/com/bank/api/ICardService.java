package com.bank.api;

public interface ICardService {
    Card createCard(String identificationNo, String password, String pinNumber);
    Card getCard(String identificationNo, String password);
    Card updateCardPin(String identificationNo, String password, String currentPin, String newPin);
    Card updateCardStatus(String identificationNo, String password, UpdateAction action, String pinNumber);
    Card updateCardTransactionLimit(String identificationNo, String password, int newLimit, String pinNumber);

    enum UpdateAction { ACTIVATE, DEACTIVATE, FREEZE, UNFREEZE }
}
