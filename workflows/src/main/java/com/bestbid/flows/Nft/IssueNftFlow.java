package com.bestbid.flows.Nft;

import co.paralleluniverse.fibers.Suspendable;
import com.bestbid.contracts.NftContract;
import com.bestbid.schema.NftStateSchemaV1;
import com.bestbid.states.NftState;
import com.bestbid.utils.Generated;
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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IssueNftFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final String minimumBid;
        private final String nftId;
        private final Party receiver;

        public Initiator(String minimumBid, String nftId, Party receiver) {
            this.minimumBid = minimumBid;
            this.nftId = nftId;
            this.receiver = receiver;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
            verifyNftStateIsAlreadyCreated();

            Party me = getOurIdentity();

            List<PublicKey> signers = Arrays.asList(me.getOwningKey(), this.receiver.getOwningKey());
            final TransactionBuilder builder = new TransactionBuilder(notary)
                    .addOutputState(new NftState(0, false, new BigDecimal(minimumBid), new BigDecimal("0"), "", nftId, me, receiver))
                    .addCommand(new NftContract.Commands.Issue(), signers);
            builder.verify(getServiceHub());

            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            List<FlowSession> sessions = Collections.singletonList(initiateFlow(receiver));
            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

            return subFlow(new FinalityFlow(stx, sessions));
        }

        private void verifyNftStateIsAlreadyCreated() throws FlowException {
            QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
            Field nftIdField = getNftIdField();
            CriteriaExpression criteria = Builder.equal(nftIdField, nftId);
            QueryCriteria nftQuery = new QueryCriteria.VaultCustomQueryCriteria(criteria);
            generalCriteria = generalCriteria.and(nftQuery);

            Vault.Page<NftState> nftStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(NftState.class, generalCriteria);

            if(nftStateAndRefs.getStates().size() == 1) {
                throw new FlowException("Nft with id " + nftId + " is already created");
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
