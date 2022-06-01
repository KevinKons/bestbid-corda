package com.bestbid.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.bestbid.contracts.AccountsFundsContract;
import com.bestbid.schema.AccountsFundsStateSchemaV1;
import com.bestbid.schema.NftStateSchemaV1;
import com.bestbid.states.AccountsFundsState;
import com.bestbid.states.AccountsFundsStateTxType;
import com.bestbid.states.NftState;
import com.bestbid.utils.Generated;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MakeBidFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final String bidAmount;
        private final String bidder;
        private final String nftId;
        private final Party receiver;

        public Initiator(String bidAmount, String bidder, String nftId, Party receiver) {
            this.bidAmount = bidAmount;
            this.bidder = bidder;
            this.nftId = nftId;
            this.receiver = receiver;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
            Party me = getOurIdentity();

            StateAndRef<NftState> inputNftStateAndRef = getNftById();
            NftState nftInputState = inputNftStateAndRef.getState().getData();

            final TransactionBuilder builder = buildTransaction(notary, me, inputNftStateAndRef, nftInputState);
            if (nftInputState.getBidNumber() > 0) {
                addCurrentBidderToTransaction(nftInputState, builder);
            }

            builder.verify(getServiceHub());

            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            List<FlowSession> sessions = Collections.singletonList(initiateFlow(receiver));
            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

            return subFlow(new FinalityFlow(stx, sessions));
        }

        private void addCurrentBidderToTransaction(NftState nftInputState, TransactionBuilder builder) throws FlowException {
            StateAndRef<AccountsFundsState> currentBidderInputStateAndRef =
                    getAccountsStateByAddress(nftInputState.getCurrentBidder());
            AccountsFundsState currentBidderInputState = currentBidderInputStateAndRef.getState().getData();
            AccountsFundsState currentBidderOutputState = new AccountsFundsState(
                    AccountsFundsStateTxType.BID_OVERTAKEN,
                    currentBidderInputState.getFunds().add(nftInputState.getCurrentBid()),
                    currentBidderInputState.getEvmAddress(),
                    getOurIdentity(),
                    receiver
            );
            builder.addInputState(currentBidderInputStateAndRef)
                    .addOutputState(currentBidderOutputState);
        }

        private TransactionBuilder buildTransaction(Party notary, Party me, StateAndRef<NftState> inputNftStateAndRef,
                                                    NftState nftInputState) throws FlowException {
            NftState nftOutputState = new NftState(
                nftInputState.getBidNumber() + 1,
                false,
                nftInputState.getMinimumBid(),
                new BigDecimal(bidAmount),
                bidder,
                nftId,
                getOurIdentity(),
                receiver
            );
            StateAndRef<AccountsFundsState> newBidderInputStateAndRef = getAccountsStateByAddress(bidder);
            AccountsFundsState newBidderInputState = newBidderInputStateAndRef.getState().getData();
            AccountsFundsState newBidderOutputState = new AccountsFundsState(
                    AccountsFundsStateTxType.NEW_BID,
                    newBidderInputState.getFunds().subtract(new BigDecimal(bidAmount)),
                    newBidderInputState.getEvmAddress(),
                    getOurIdentity(),
                    receiver
            );

            List<PublicKey> signers = Arrays.asList(me.getOwningKey(), this.receiver.getOwningKey());
            return new TransactionBuilder(notary)
                    .addInputState(inputNftStateAndRef)
                    .addInputState(newBidderInputStateAndRef)
                    .addOutputState(nftOutputState)
                    .addOutputState(newBidderOutputState)
                    .addCommand(new AccountsFundsContract.Commands.MakeBid(new BigDecimal(bidAmount)), signers);
        }

        private StateAndRef<NftState> getNftById() throws FlowException {
            QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
            Field nftIdField = getNftIdField();
            CriteriaExpression criteria = Builder.equal(nftIdField, nftId);
            QueryCriteria nftQuery = new QueryCriteria.VaultCustomQueryCriteria(criteria);
            generalCriteria = generalCriteria.and(nftQuery);

            Vault.Page<NftState> nftStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(NftState.class, generalCriteria);

            try {
                return nftStateAndRefs.getStates().get(0);
            } catch (Exception e) {
                throw new FlowException("NFT with " + nftId + " doesn't exist");
            }
        }

        @Generated
        private Field getNftIdField() throws FlowException {
            try {
                return NftStateSchemaV1.PersistentNftState.class.getDeclaredField("nftId");
            } catch (NoSuchFieldException e) {
                throw new FlowException("value field not found");
            }
        }

        private StateAndRef<AccountsFundsState> getAccountsStateByAddress(String address) throws FlowException {
            QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
            Field addressField = getEvmAddressField();
            CriteriaExpression criteria = Builder.equal(addressField, address);
            QueryCriteria queryByAddress = new QueryCriteria.VaultCustomQueryCriteria(criteria);
            generalCriteria = generalCriteria.and(queryByAddress);

            Vault.Page<AccountsFundsState> accountsFundsStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(AccountsFundsState.class, generalCriteria);
            try{
                return accountsFundsStateAndRefs.getStates().get(0);
            } catch (Exception e) {
                throw new FlowException("User with address " + address + " not found");
            }
        }

        @Generated
        private Field getEvmAddressField() throws FlowException {
            try {
                return AccountsFundsStateSchemaV1.PersistentAccountsFundsState.class.getDeclaredField("evmAddress");
            } catch (NoSuchFieldException e) {
                throw new FlowException("value field not found");
            }
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
