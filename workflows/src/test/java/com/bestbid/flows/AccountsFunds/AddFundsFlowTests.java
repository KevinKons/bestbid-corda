package com.bestbid.flows.AccountsFunds;

import com.bestbid.states.AccountsFundsState;
import com.bestbid.states.AccountsFundsStateTxType;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.bestbid.flows.FlowHelpers.prepareMockNetworkParameters;
import static org.junit.Assert.*;

public class AddFundsFlowTests {

    private final MockNetwork network;
    private final StartedMockNode initiator;
    private final StartedMockNode responder;
    private final Party partyInitiator;
    private final Party partyResponder;

    public AddFundsFlowTests() throws Exception {
        this.network = new MockNetwork(prepareMockNetworkParameters());
        this.initiator = network.createNode();
        this.responder = network.createNode();

        Arrays.asList(initiator, responder).forEach(it ->
                it.registerInitiatedFlow(AddFundsFlow.Responder.class));

        this.partyInitiator = initiator.getInfo().getLegalIdentities().get(0);
        this.partyResponder = responder.getInfo().getLegalIdentities().get(0);
    }

    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    private static final AccountsFundsStateTxType NEW_FUNDS = AccountsFundsStateTxType.NEW_FUNDS;
    private static final AccountsFundsStateTxType NEW_BID = AccountsFundsStateTxType.NEW_BID;
    private static final AccountsFundsStateTxType BID_OVERTAKEN = AccountsFundsStateTxType.BID_OVERTAKEN;

    @Test
    public void whenAddingFundsToUnregisteredAddress_shouldRegisterTheAddressWithTheFunds() throws Exception {
        String funds = "10000";
        BigDecimal bigDecimalFunds = new BigDecimal(funds);
        String address = "0x05DFG769DFG897SFD";
        final AddFundsFlow.Initiator flow = new AddFundsFlow.Initiator(NEW_FUNDS, funds, address, partyResponder);
        final CordaFuture<SignedTransaction> future = initiator.startFlow(flow);
        network.runNetwork();

        final SignedTransaction tx = future.get();
        tx.verifyRequiredSignatures();

        AccountsFundsState expected = new AccountsFundsState(NEW_FUNDS, bigDecimalFunds, address, partyInitiator, partyResponder);
        for (StartedMockNode node : ImmutableList.of(initiator, responder)) {
            final SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(tx.getId());
            assertNotNull(recordedTx);
            assertTrue(recordedTx.getTx().getInputs().isEmpty());
            final List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assertEquals(1, txOutputs.size());
            assertEquals(expected, txOutputs.get(0).getData());
        }
    }

    @Test
    public void givenAnAlreadyRegisteredAddres_whenAddingFundsToThatAddress_shouldAddTheFundsToIt() throws Exception {
        //arrange
        String funds = "10000";
        BigDecimal bigDecimalFunds = new BigDecimal(funds);
        String address = "0x05DFG769DFG897SFD";
        final AddFundsFlow.Initiator flow = new AddFundsFlow.Initiator(NEW_FUNDS, funds, address, partyResponder);
        final AddFundsFlow.Initiator flow2 = new AddFundsFlow.Initiator(NEW_FUNDS, funds, address, partyResponder);

        //act
        final CordaFuture<SignedTransaction> future = initiator.startFlow(flow);
        network.runNetwork();
        future.get();

        final CordaFuture<SignedTransaction> future2 = initiator.startFlow(flow2);
        network.runNetwork();

        final SignedTransaction tx = future2.get();

        //assert
        tx.verifyRequiredSignatures();

        BigDecimal expectedFunds = bigDecimalFunds.add(bigDecimalFunds);
        AccountsFundsState expected = new AccountsFundsState(NEW_FUNDS, expectedFunds, address, partyInitiator, partyResponder);
        for (StartedMockNode node : ImmutableList.of(initiator, responder)) {
            final SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(tx.getId());
            assertNotNull(recordedTx);
            assertEquals(1, recordedTx.getTx().getInputs().size());
            final List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assertEquals(1, txOutputs.size());
            assertEquals(expected, txOutputs.get(0).getData());
        }
    }

    @Test(expected = ExecutionException.class)
    public void whenAddingFundsIfAmountIsZero_shouldThrowException() throws Exception {
        String funds = "0";
        String address = "0x05DFG769DFG897SFD";
        final AddFundsFlow.Initiator flow = new AddFundsFlow.Initiator(NEW_FUNDS, funds, address, partyResponder);
        final CordaFuture<SignedTransaction> future = initiator.startFlow(flow);

        network.runNetwork();
        future.get();
    }


}
