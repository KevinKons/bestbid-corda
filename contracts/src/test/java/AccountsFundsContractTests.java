import com.bestbid.contracts.AccountsFundsContract;
import com.bestbid.contracts.NftContract;
import com.bestbid.states.AccountsFundsState;
import com.bestbid.states.AccountsFundsStateTxType;
import com.bestbid.states.NftState;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.transaction;

public class AccountsFundsContractTests {

    private final TestIdentity sender = new TestIdentity(new CordaX500Name("PartyA", "London", "GB"));
    private final TestIdentity receiver = new TestIdentity(new CordaX500Name("PartyC", "Sydney", "AU"));

    private final MockServices ledgerServices = new MockServices(
            Arrays.asList("com.bestbid.contracts", "com.bestbid.states"),
            sender,
            receiver
    );

    private final AccountsFundsStateTxType NEW_FUNDS = AccountsFundsStateTxType.NEW_FUNDS;
    private final AccountsFundsStateTxType NEW_BID = AccountsFundsStateTxType.NEW_BID;
    private final AccountsFundsStateTxType BID_OVERTAKEN = AccountsFundsStateTxType.BID_OVERTAKEN;

    private final String accountOneAddress = "0x0KLJH234978YFSLKJ4";
    private final String accountTwoAddress = "0x030FF76FFDV3T2T8FA";

    @Test
    public void givenCorrectInfo_whenFirstAddingFunds_shouldNotThrowErrors() {
        transaction(ledgerServices, tx -> {
            tx.output(AccountsFundsContract.ID, new AccountsFundsState(NEW_FUNDS, BigDecimal.TEN, accountOneAddress,
                    sender.getParty(), receiver.getParty()));
            tx.command(sender.getParty().getOwningKey(), new AccountsFundsContract.Commands.AddFunds(BigDecimal.TEN));
            tx.verifies();
            return null;
        });
    }

    @Test
    public void whenAddingFundsIfTwoOutputStatesAreGenerated_shouldThrowError() {
        transaction(ledgerServices, tx -> {
            tx.output(AccountsFundsContract.ID, new AccountsFundsState(NEW_FUNDS, BigDecimal.ZERO, accountOneAddress,
                    sender.getParty(), receiver.getParty()));
            tx.output(AccountsFundsContract.ID, new AccountsFundsState(NEW_FUNDS, BigDecimal.ZERO, accountOneAddress,
                    sender.getParty(), receiver.getParty()));
            tx.command(sender.getParty().getOwningKey(), new AccountsFundsContract.Commands.AddFunds(BigDecimal.ZERO));
            tx.failsWith("AddFunds commands transactions must generate only one output state.");
            return null;
        });
    }

    @Test
    public void whenAddingFundsIfTxTypeIsntNewFunds_shouldThrowError() {
        transaction(ledgerServices, tx -> {
            tx.output(AccountsFundsContract.ID, new AccountsFundsState(NEW_BID, BigDecimal.ZERO, accountOneAddress,
                    sender.getParty(), receiver.getParty()));
            tx.command(sender.getParty().getOwningKey(), new AccountsFundsContract.Commands.AddFunds(BigDecimal.ZERO));
            tx.failsWith("The tx type should be NEW_FUNDS");
            return null;
        });
    }

    @Test
    public void givenNoAccountsStateRegisteredWithAddress_whenTryingToAddFundsToAddress_shouldIssueAccountsFundsState() {
        transaction(ledgerServices, tx -> {
            tx.output(AccountsFundsContract.ID, new AccountsFundsState(NEW_FUNDS, BigDecimal.ZERO, accountOneAddress,
                    sender.getParty(), receiver.getParty()));
            tx.command(sender.getParty().getOwningKey(), new AccountsFundsContract.Commands.AddFunds(BigDecimal.ZERO));
            tx.failsWith("The amount shoud be greater than 0");
            return null;
        });
    }

    @Test
    public void givenCorrectInfo_whenFirstBidding_shouldNotThrowErrors() {
        transaction(ledgerServices, tx -> {
            tx.input(NftContract.ID, new NftState(0, false, BigDecimal.ONE, BigDecimal.ZERO, "", "1", sender.getParty(),
                    receiver.getParty()));
            tx.input(AccountsFundsContract.ID, new AccountsFundsState(NEW_FUNDS, BigDecimal.TEN, accountOneAddress,
                    sender.getParty(), receiver.getParty()));
            tx.output(NftContract.ID, new NftState(1, false, BigDecimal.ONE, BigDecimal.TEN, accountOneAddress, "1",
                    sender.getParty(), receiver.getParty()));
            tx.output(AccountsFundsContract.ID, new AccountsFundsState(NEW_BID, BigDecimal.ZERO, accountOneAddress,
                    sender.getParty(), receiver.getParty()));
            tx.command(sender.getParty().getOwningKey(), new AccountsFundsContract.Commands.MakeBid(BigDecimal.TEN));
            tx.verifies();
            return null;
        });
    }

    @Test
    public void givenCorrectInfo_whenSecondBidding_shouldNotThrowErrors() {
        transaction(ledgerServices, tx -> {
            tx.input(NftContract.ID, new NftState(1, false, BigDecimal.ONE, BigDecimal.ONE, accountOneAddress, "1",
                    sender.getParty(), receiver.getParty()));
            tx.input(AccountsFundsContract.ID, new AccountsFundsState(NEW_FUNDS, BigDecimal.TEN, accountTwoAddress,
                    sender.getParty(), receiver.getParty()));
            tx.input(AccountsFundsContract.ID, new AccountsFundsState(NEW_BID, BigDecimal.TEN, accountOneAddress,
                    sender.getParty(), receiver.getParty()));
            tx.output(NftContract.ID, new NftState(2, false, BigDecimal.ONE, BigDecimal.TEN, accountTwoAddress, "1",
                    sender.getParty(), receiver.getParty()));
            tx.output(AccountsFundsContract.ID, new AccountsFundsState(NEW_BID, BigDecimal.ZERO, accountTwoAddress,
                    sender.getParty(), receiver.getParty()));
            tx.output(AccountsFundsContract.ID, new AccountsFundsState(BID_OVERTAKEN, BigDecimal.ONE.add(BigDecimal.TEN),
                    accountOneAddress, sender.getParty(), receiver.getParty()));
            tx.command(sender.getParty().getOwningKey(), new AccountsFundsContract.Commands.MakeBid(BigDecimal.TEN));
            tx.verifies();
            return null;
        });
    }
}
