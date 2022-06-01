package com.bestbid.flows;

import com.bestbid.flows.Nft.IssueNftFlow;
import com.bestbid.states.NftState;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.StartedMockNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.bestbid.flows.FlowHelpers.prepareMockNetworkParameters;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class IssueNftFlowTests {
    private final MockNetwork network;
    private final StartedMockNode initiator;
    private final StartedMockNode responder;
    private final Party partyInitiator;
    private final Party partyResponder;

    public IssueNftFlowTests() throws Exception {
        this.network = new MockNetwork(prepareMockNetworkParameters());
        this.initiator = network.createNode();
        this.responder = network.createNode();

        Arrays.asList(initiator, responder).forEach(it ->
                it.registerInitiatedFlow(EndAuctionFlow.Responder.class));

        this.partyInitiator = initiator.getInfo().getLegalIdentities().get(0);
        this.partyResponder = responder.getInfo().getLegalIdentities().get(0);
    }

    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown(){
        network.stopNodes();
    }

    @Test
    public void whenNftStateIsCreated_shouldReturnNftStateCreated() throws ExecutionException, InterruptedException, SignatureException {
        String minimumBid = "10000";
        final IssueNftFlow.Initiator issueNftFlow = new IssueNftFlow.Initiator(minimumBid, "1", partyResponder);
        final CordaFuture<SignedTransaction> futureIssueNft = initiator.startFlow(issueNftFlow);
        network.runNetwork();
        final SignedTransaction signedTransactionIssueFlow = futureIssueNft.get();
        signedTransactionIssueFlow.verifyRequiredSignatures();

        BigDecimal expectedMinimumBid = new BigDecimal(minimumBid);
        NftState expected =  new NftState(0, false, expectedMinimumBid, BigDecimal.ZERO,
                "", "1", partyInitiator, partyResponder);

        for (StartedMockNode node : ImmutableList.of(initiator, responder)) {
            final SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(signedTransactionIssueFlow.getId());
            assertNotNull(recordedTx);
            assertEquals(0, recordedTx.getTx().getInputs().size());
            final List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assertEquals(1, txOutputs.size());
            assertEquals(expected, txOutputs.get(0).getData());
        }
    }

    @Test(expected = ExecutionException.class)
    public void whenCreatingNftIfNftIsAlreadyCreated_shouldThrowException() throws ExecutionException, InterruptedException {
        String minimumBid = "10000";
        final IssueNftFlow.Initiator issueNftFlow = new IssueNftFlow.Initiator(minimumBid, "1", partyResponder);
        final IssueNftFlow.Initiator issueNftFlowSecond = new IssueNftFlow.Initiator(minimumBid, "1", partyResponder);

        final CordaFuture<SignedTransaction> futureIssueNft = initiator.startFlow(issueNftFlow);
        network.runNetwork();
        futureIssueNft.get();

        final CordaFuture<SignedTransaction> futureIssueNftSecond = initiator.startFlow(issueNftFlowSecond);
        network.runNetwork();
        futureIssueNftSecond.get();
    }

    @Test(expected = ExecutionException.class)
    public void whenCreatingNftIfMinimumBidIsZero_shouldThrowException() throws ExecutionException, InterruptedException {
        String minimumBid = "0";
        final IssueNftFlow.Initiator issueNftFlow = new IssueNftFlow.Initiator(minimumBid, "1", partyResponder);

        final CordaFuture<SignedTransaction> futureIssueNft = initiator.startFlow(issueNftFlow);
        network.runNetwork();
        futureIssueNft.get();
    }

}
