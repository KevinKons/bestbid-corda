package com.bestbid.flows;

import com.bestbid.flows.AccountsFunds.AddFundsFlow;
import com.bestbid.flows.Nft.IssueNftFlow;
import com.bestbid.states.AccountsFundsState;
import com.bestbid.states.AccountsFundsStateTxType;
import com.bestbid.states.NftState;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.StartedMockNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.bestbid.flows.FlowHelpers.prepareMockNetworkParameters;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MakeBidFlowTests {

    private static final AccountsFundsStateTxType NEW_FUNDS = AccountsFundsStateTxType.NEW_FUNDS;
    private static final AccountsFundsStateTxType NEW_BID = AccountsFundsStateTxType.NEW_BID;
    private static final AccountsFundsStateTxType BID_OVERTAKEN = AccountsFundsStateTxType.BID_OVERTAKEN;

    private final MockNetwork network;
    private final StartedMockNode initiator;
    private final StartedMockNode responder;
    private final Party partyInitiator;
    private final Party partyResponder;

    public MakeBidFlowTests() throws Exception {
        this.network = new MockNetwork(prepareMockNetworkParameters());
        this.initiator = network.createNode();
        this.responder = network.createNode();

        Arrays.asList(initiator, responder).forEach(it ->
                it.registerInitiatedFlow(AddFundsFlow.Responder.class));

        this.partyInitiator = initiator.getInfo().getLegalIdentities().get(0);
        this.partyResponder = responder.getInfo().getLegalIdentities().get(0);
    }

    private final String accountOneAddress = "0x05DFG769DFG897SFD";
    private final String accountOneInitialFunds = "1000";
    private final BigDecimal accountOneInitialFundsBd = new BigDecimal(accountOneInitialFunds);
    private final String accountTwoAddress = "0x0F873KA2KJH4GGSK2";
    private final String accountTwoInitialFunds = "1000";
    private final BigDecimal accountTwoInitialFundsBd = new BigDecimal(accountTwoInitialFunds);
    private final String minimumNftBid = "50";
    private final BigDecimal minimumNftBidBd = new BigDecimal(minimumNftBid);
    private final String nftId = "1";

    @Before
    public void setup() {
        final AddFundsFlow.Initiator addFundsflowAccountOne = new AddFundsFlow.Initiator(NEW_FUNDS, accountOneInitialFunds,
                accountOneAddress, partyResponder);
        initiator.startFlow(addFundsflowAccountOne);

        final AddFundsFlow.Initiator addFundsflowAccountTwo = new AddFundsFlow.Initiator(NEW_FUNDS, accountTwoInitialFunds,
                accountTwoAddress, partyResponder);
        initiator.startFlow(addFundsflowAccountTwo);

        final IssueNftFlow.Initiator issueNftFlow = new IssueNftFlow.Initiator(minimumNftBid, nftId, partyResponder);
        initiator.startFlow(issueNftFlow);

        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void whenThereIsNoBidOnTheNft_shouldMakeFirstBid() throws Exception {
        final String bidAmount = "60";
        final BigDecimal bidAmountBd = new BigDecimal(bidAmount);
        final MakeBidFlow.Initiator flow = new MakeBidFlow.Initiator(bidAmount, accountOneAddress, nftId, partyResponder);
        final CordaFuture<SignedTransaction> future = initiator.startFlow(flow);

        network.runNetwork();
        final SignedTransaction tx = future.get();

        tx.verifyRequiredSignatures();

        AccountsFundsState expectedBidderOutputState = new AccountsFundsState(NEW_BID,
                accountOneInitialFundsBd.subtract(bidAmountBd), accountOneAddress, partyInitiator, partyResponder);
        NftState expctedNftOutputState = new NftState(1, false, minimumNftBidBd, bidAmountBd, accountOneAddress, nftId,
                partyInitiator, partyResponder);
        for (StartedMockNode node : ImmutableList.of(initiator, responder)) {
            final SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(tx.getId());
            assertNotNull(recordedTx);

            assertEquals(2, recordedTx.getTx().getInputs().size());
            assertEquals(2, recordedTx.getTx().getOutputStates().size());

            final List<ContractState> txOutputs = recordedTx.getTx().getOutputStates();
            NftState firstOutputState = (NftState) txOutputs.get(0);
            AccountsFundsState secondOutputState = (AccountsFundsState) txOutputs.get(1);

            assertEquals(expctedNftOutputState, firstOutputState);
            assertEquals(expectedBidderOutputState, secondOutputState);
        }
    }

    @Test
    public void whenThereIsAlreadyAnBidOnTheNft_shouldMakeSecondBid() throws Exception {
        //arrange
        final String firstBidAmount = "60";
        final MakeBidFlow.Initiator firstBidFlow = new MakeBidFlow.Initiator(firstBidAmount, accountOneAddress, nftId, partyResponder);
        initiator.startFlow(firstBidFlow);

        network.runNetwork();

        final String secondBidAmount = "70";
        final BigDecimal secondBidAmountBd = new BigDecimal(secondBidAmount);

        //act
        final MakeBidFlow.Initiator secondBidFlow = new MakeBidFlow.Initiator(secondBidAmount, accountTwoAddress, nftId,
                partyResponder);
        final CordaFuture<SignedTransaction> future = initiator.startFlow(secondBidFlow);

        network.runNetwork();
        final SignedTransaction tx = future.get();

        //assert
        tx.verifyRequiredSignatures();

        AccountsFundsState expectedLastBidderOutputState = new AccountsFundsState(BID_OVERTAKEN,
                accountOneInitialFundsBd, accountOneAddress, partyInitiator, partyResponder);
        AccountsFundsState expectedBidderOutputState = new AccountsFundsState(NEW_BID,
                accountTwoInitialFundsBd.subtract(secondBidAmountBd), accountTwoAddress, partyInitiator, partyResponder);
        NftState expctedNftOutputState = new NftState(2, false, minimumNftBidBd, secondBidAmountBd, accountTwoAddress, nftId,
                partyInitiator, partyResponder);
        for (StartedMockNode node : ImmutableList.of(initiator, responder)) {
            final SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(tx.getId());
            assertNotNull(recordedTx);

            assertEquals(3, recordedTx.getTx().getInputs().size());
            assertEquals(3, recordedTx.getTx().getOutputStates().size());

            final List<ContractState> txOutputs = recordedTx.getTx().getOutputStates();
            NftState firstOutputState = (NftState) txOutputs.get(0);
            AccountsFundsState secondOutputState = (AccountsFundsState) txOutputs.get(1);
            AccountsFundsState thirdOutputState = (AccountsFundsState) txOutputs.get(2);

            assertEquals(expctedNftOutputState, firstOutputState);
            assertEquals(expectedBidderOutputState, secondOutputState);
            assertEquals(expectedLastBidderOutputState, thirdOutputState);
        }
    }

    @Test(expected = ExecutionException.class)
    public void whenNftDoesntExist_shouldThrowException() throws Exception {
        final MakeBidFlow.Initiator flow = new MakeBidFlow.Initiator("60", accountOneAddress, "100", partyResponder);
        final CordaFuture<SignedTransaction> future = initiator.startFlow(flow);

        network.runNetwork();
        future.get();
    }

    @Test(expected = ExecutionException.class)
    public void whenAccountsFundsDoesntExist_shouldThrowException() throws Exception {
        final MakeBidFlow.Initiator flow = new MakeBidFlow.Initiator("60", "no address", nftId, partyResponder);
        final CordaFuture<SignedTransaction> future = initiator.startFlow(flow);

        network.runNetwork();
        future.get();
    }
}
