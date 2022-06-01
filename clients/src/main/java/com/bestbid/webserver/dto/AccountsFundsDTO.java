package com.bestbid.webserver.dto;

import com.bestbid.states.AccountsFundsStateTxType;
import io.swagger.annotations.ApiModelProperty;

public class AccountsFundsDTO {
    @ApiModelProperty(notes = "Accounts Funds State tx Type", example = "NEW_FUNDS", required = true)
    private AccountsFundsStateTxType type;
    @ApiModelProperty(notes = "Amount of money", example = "100", required = true)
    private String amount;
    @ApiModelProperty(notes = "Wallet Address", example = "0xD848B448F3E3276Fad558BE0719C889D647b0a88", required = true)
    private String address;

    public AccountsFundsDTO(AccountsFundsStateTxType type, String amount, String address) {
        this.type = type;
        this.amount = amount;
        this.address = address;
    }

    public AccountsFundsStateTxType getType() {
        return type;
    }

    public void setType(AccountsFundsStateTxType type) {
        this.type = type;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
