package com.bestbid.states;

import com.bestbid.contracts.AccountsFundsContract;
import com.bestbid.schema.AccountsFundsStateSchemaV1;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


@BelongsToContract(AccountsFundsContract.class)
public class AccountsFundsState implements ContractState, QueryableState {

    private final AccountsFundsStateTxType type;
    private final BigDecimal funds;
    private final String evmAddress;
    private final Party sender;
    private final Party receiver;
    private final UniqueIdentifier linearId;

    public AccountsFundsState(AccountsFundsStateTxType type, BigDecimal funds, String evmAddress, Party sender, Party receiver) {
        this.type = type;
        this.funds = funds;
        this.evmAddress = evmAddress;
        this.sender = sender;
        this.receiver = receiver;
        this.linearId = new UniqueIdentifier();
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(sender,receiver);
    }

    @Override
    public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof AccountsFundsStateSchemaV1) {
            return new AccountsFundsStateSchemaV1.PersistentAccountsFundsState(
                    this.type.getName(),
                    this.evmAddress,
                    this.funds.toString(),
                    this.sender.getName().toString(),
                    this.receiver.getName().toString(),
                    this.linearId.getId());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return Collections.singletonList(new AccountsFundsStateSchemaV1());
    }

    public BigDecimal getFunds() {

        return funds;
    }

    public AccountsFundsStateTxType getType() {
        return type;
    }

    public String getEvmAddress() {
        return evmAddress;
    }

    public Party getSender() {
        return sender;
    }

    public Party getReceiver() {
        return receiver;
    }

    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public String toString() {
        return String.format("AccountsFundsState(evmAddress=%s, funds=%s, linearId=%s, sender=%s, receiver=%s)",
                evmAddress, funds.toString(), linearId.toString(), sender.getName(), receiver.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccountsFundsState that = (AccountsFundsState) o;

        if (type != that.type) return false;
        if (!Objects.equals(funds, that.funds)) return false;
        if (!Objects.equals(evmAddress, that.evmAddress)) return false;
        if (!Objects.equals(sender, that.sender)) return false;
        return Objects.equals(receiver, that.receiver);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (funds != null ? funds.hashCode() : 0);
        result = 31 * result + (evmAddress != null ? evmAddress.hashCode() : 0);
        result = 31 * result + (sender != null ? sender.hashCode() : 0);
        result = 31 * result + (receiver != null ? receiver.hashCode() : 0);
        return result;
    }
}