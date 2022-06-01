package com.bestbid.states;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public enum AccountsFundsStateTxType {
    NEW_FUNDS("NEW_FUNDS"),
    NEW_BID("NEW_BID"),
    BID_OVERTAKEN("BID_OVERTAKEN");

    private String name;

    AccountsFundsStateTxType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
