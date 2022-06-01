package com.bestbid.flows.AccountsFunds;

import co.paralleluniverse.fibers.Suspendable;
import com.bestbid.contracts.AccountsFundsContract;
import com.bestbid.states.AccountsFundsState;
import com.bestbid.states.AccountsFundsStateTxType;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AddFundsFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final AccountsFundsStateTxType type;
        private final String funds;
        private final String evmAddress;
        private final Party receiver;

        public Initiator(AccountsFundsStateTxType type, String funds, String evmAddress, Party receiver) {
            this.type = type;
            this.funds = funds;
            this.evmAddress = evmAddress;
            this.receiver = receiver;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
            Party me = getOurIdentity();

            final TransactionBuilder builder = new TransactionBuilder(notary);

            Optional<StateAndRef<AccountsFundsState>> optStateAndRef = getServiceHub().getVaultService().
                    queryBy(AccountsFundsState.class).getStates().stream()
                    .filter(afs -> afs.getState().getData().getEvmAddress().equals(evmAddress)).findAny();

            BigDecimal changeAmount = new BigDecimal(funds);
            if (optStateAndRef.isPresent()) {
                BigDecimal oldFunds = optStateAndRef.get().getState().getData().getFunds();
                BigDecimal newFunds = oldFunds.add(changeAmount);
                builder.addOutputState(new AccountsFundsState(type, newFunds, evmAddress, me, receiver));
                builder.addInputState(optStateAndRef.get());
            } else {
                builder.addOutputState(new AccountsFundsState(type, changeAmount, evmAddress, me, receiver));
            }

            builder.addCommand(new AccountsFundsContract.Commands.AddFunds(changeAmount),
                    Arrays.asList(me.getOwningKey(), this.receiver.getOwningKey()));

            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            List<FlowSession> sessions = Collections.singletonList(initiateFlow(receiver));

            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

            return subFlow(new FinalityFlow(stx, sessions));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<Void>{
        private final FlowSession counterpartySession;

        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                @Suspendable
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                }
            });
            subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
            return null;
        }
    }
}
