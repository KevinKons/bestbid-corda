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
import static org.junit.Assert.*;

public class EndAuctionFlowTests {
    private final MockNetwork network;
    private final StartedMockNode initiator;
    private final StartedMockNode responder;
    private final Party partyInitiator;
    private final Party partyResponder;

    public EndAuctionFlowTests() throws Exception {
        this.network = new MockNetwork(prepareMockNetworkParameters());
        this.initiator = network.createNode();
        this.responder = network.createNode();

        Arrays.asList(initiator, responder).forEach(it ->
                it.registerInitiatedFlow(EndAuctionFlow.Responder.class));

        this.partyInitiator = initiator.getInfo().getLegalIdentities().get(0);
        this.partyResponder = responder.getInfo().getLegalIdentities().get(0);
    }

    @Before
    public void setup() throws SignatureException, ExecutionException, InterruptedException {
        String minimumBid = "10000";
        final IssueNftFlow.Initiator issueNftFlow = new IssueNftFlow.Initiator(minimumBid, "1", partyResponder);
        final CordaFuture<SignedTransaction> futureIssueNft = initiator.startFlow(issueNftFlow);
        network.runNetwork();
        final SignedTransaction signedTransactionIssueFlow = futureIssueNft.get();
        signedTransactionIssueFlow.verifyRequiredSignatures();
    }

    @After
    public void tearDown(){
        network.stopNodes();
    }

    @Test
    public void whenEndAuctionToNftState_shouldRegisterAuctionEndedTrue() throws ExecutionException, InterruptedException, SignatureException {
        final EndAuctionFlow.Initiator endAuctionFlow = new EndAuctionFlow.Initiator("1", partyResponder);
        final CordaFuture<SignedTransaction> futureEndAuction = initiator.startFlow(endAuctionFlow);
        network.runNetwork();

        final SignedTransaction signedTransactionEndAuctionFlow = futureEndAuction.get();
        signedTransactionEndAuctionFlow.verifyRequiredSignatures();

        String minimumBid = "10000";
        BigDecimal expectedMinimumBid = new BigDecimal(minimumBid);
        NftState expected =  new NftState(0, true, expectedMinimumBid, BigDecimal.ZERO,
                "", "1", partyInitiator, partyResponder);

        for (StartedMockNode node : ImmutableList.of(initiator, responder)) {
            final SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(signedTransactionEndAuctionFlow.getId());
            assertNotNull(recordedTx);
            assertEquals(1, recordedTx.getTx().getInputs().size());
            final List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assertEquals(1, txOutputs.size());
            assertEquals(expected, txOutputs.get(0).getData());
        }
    }

    @Test(expected = ExecutionException.class)
    public void whenNftDoestExist_shouldThrowError() throws Exception {
        final EndAuctionFlow.Initiator endAuctionFlow = new EndAuctionFlow.Initiator("100", partyResponder);
        final CordaFuture<SignedTransaction> futureEndAuction = initiator.startFlow(endAuctionFlow);
        network.runNetwork();

        futureEndAuction.get();
    }

}
