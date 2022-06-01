package com.bestbid.schema;

import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import org.hibernate.annotations.Type;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.UUID;

public class AccountsFundsStateSchemaV1 extends MappedSchema {
    public AccountsFundsStateSchemaV1() {
        super(AccountsFundsStateSchema.class, 1, Arrays.asList(PersistentAccountsFundsState.class));
    }

    @Nullable
    @Override
    public String getMigrationResource() {
        return "accounts-funds-state.changelog-master";
    }

    @Entity
    @Table(name = "accounts_funds_states")
    public static class PersistentAccountsFundsState extends PersistentState {
        @Column(name = "type") private final String type;
        @Column(name = "evm_address") private final String evmAddress;
        @Column(name = "funds") private final String funds;
        @Column(name = "linear_id") @Type(type = "uuid-char") private final UUID linearId;
        @Column(name = "sender") private final String sender;
        @Column(name = "receiver") private final String receiver;

        public PersistentAccountsFundsState(String type, String evmAddress, String funds, String sender,
                                            String receiver, UUID linearId) {
            this.type = type;
            this.evmAddress = evmAddress;
            this.funds = funds;
            this.sender = sender;
            this.receiver = receiver;
            this.linearId = linearId;
        }

        // Default constructor required by hibernate.
        public PersistentAccountsFundsState() {
            this.type = null;
            this.evmAddress = null;
            this.funds = null;
            this.sender = null;
            this.receiver = null;
            this.linearId = null;
        }

        public String getType() {
            return type;
        }

        public String getEvmAddress() {
            return evmAddress;
        }

        public String getFunds() {
            return funds;
        }

        public UUID getLinearId() {
            return linearId;
        }

        public String getSender() {
            return sender;
        }

        public String getReceiver() {
            return receiver;
        }
    }
}