package com.bestbid.contracts;

import com.bestbid.states.AccountsFundsState;
import com.bestbid.states.AccountsFundsStateTxType;
import com.bestbid.states.NftState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class AccountsFundsContract implements Contract {

    public static final String ID = "com.bestbid.contracts.AccountsFundsContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final CommandData commandData = tx.getCommands().get(0).getValue();

        if (commandData instanceof AccountsFundsContract.Commands.AddFunds) {
            verifyAddFunds(tx);
        } else if (commandData instanceof AccountsFundsContract.Commands.MakeBid) {
            verifyMakeBid(tx);
        } else {
            throw new IllegalArgumentException("Command not found");
        }
    }

    private void verifyAddFunds(LedgerTransaction tx) {
        AccountsFundsState output = tx.outputsOfType(AccountsFundsState.class).get(0);
        Commands.AddFunds command = (Commands.AddFunds) tx.getCommand(0).component1();
        BigDecimal amountAdded = command.getAmount();
        requireThat(req -> {
            req.using("AddFunds commands transactions must generate only one output state.",
                    tx.getOutputStates().size() == 1);

            req.using("The tx type should be NEW_FUNDS",
                    output.getType() == AccountsFundsStateTxType.NEW_FUNDS);

            req.using("The amount shoud be greater than 0",
                    amountAdded.compareTo(new BigDecimal("0")) > 0);
            return null;
        });
    }

    private void verifyMakeBid(LedgerTransaction tx) {
        ContractState firstInputState = tx.getInputStates().get(0);
        requireThat(req -> {
            req.using("First input state must be of type NftState",
                    firstInputState instanceof NftState);
            return null;
        });

        NftState nftInputState = (NftState) firstInputState;
        if (nftInputState.getBidNumber() == 0) {
            verifyFirstBidOnNft(tx, nftInputState);
        } else {
            verifyBidsThatArentTheFirstOne(tx, nftInputState);
        }
    }

    private void verifyFirstBidOnNft(LedgerTransaction tx, NftState nftInputState) {
        requireThat(req -> {
            req.using("On first bid only 2 input states should be consumed",
                    tx.getInputStates().size() == 2);
            return null;
        });

        ContractState secondInputState = tx.getInputStates().get(1);
        requireThat(req -> {
            req.using("Second input state must be of type AccountsFundsState",
                    secondInputState instanceof AccountsFundsState);
            return null;
        });

        AccountsFundsState accountsFundsInputState = (AccountsFundsState) secondInputState;
        BigDecimal amountBidded = ((Commands.MakeBid) tx.getCommand(0).component1()).getAmount();

        requireThat(req -> {
            req.using("Amount bidded must be equal or higher than minimum bid",
                    amountBidded.compareTo(nftInputState.getMinimumBid()) >= 0);

            req.using("Bidder need to have enough funds",
                    accountsFundsInputState.getFunds().compareTo(amountBidded) >= 0);

            req.using("When a first bid on an NFT is made two and only two output states must be generated",
                    tx.getOutputStates().size() == 2);
            return null;
        });

        ContractState firstOutputState = tx.getOutputStates().get(0);
        ContractState secondOutputState = tx.getOutputStates().get(1);

        requireThat(req -> {
            req.using("first output state must be of type NftState",
                    firstOutputState instanceof NftState);

            req.using("second output state must be of type AccountsFundsState",
                    secondOutputState instanceof AccountsFundsState);
            return null;
        });

        NftState nftOutputState = (NftState) firstOutputState;
        AccountsFundsState accountsFundsOutputState = (AccountsFundsState) secondOutputState;

        requireThat(req -> {
            req.using("Outputted AccountsFundsState must have funds equal to old funds minus amount bidded",
                    accountsFundsOutputState.getFunds().compareTo(accountsFundsInputState.getFunds().subtract(amountBidded)) == 0);

            req.using("Outputted AccountsFundsState must have type as NEW_BID",
                    accountsFundsOutputState.getType() == AccountsFundsStateTxType.NEW_BID);

            req.using("Outputted NftState must have bid number equal to 1",
                    nftOutputState.getBidNumber() == 1);

            req.using("Outputted NftState must have current bid equal to amount bidded",
                    nftOutputState.getCurrentBid().compareTo(amountBidded) == 0);
            return null;
        });
    }

    private void verifyBidsThatArentTheFirstOne(LedgerTransaction tx, NftState nftInputState) {
        requireThat(req -> {
            req.using("After the first bid all tx must have 3 and only 3 input states.",
                    tx.getInputStates().size() == 3);
            return null;
        });

        ContractState secondInputState = tx.getInputStates().get(1);
        ContractState thirdInputState = tx.getInputStates().get(2);
        requireThat(req -> {
            req.using("Second input state must be of type AccountsFundsState",
                    secondInputState instanceof AccountsFundsState);

            req.using("Third input state must be of type AccountsFundsState",
                    thirdInputState instanceof AccountsFundsState);
            return null;
        });

        AccountsFundsState newBidderAccountsFundsInputState = (AccountsFundsState) secondInputState;
        AccountsFundsState oldBidderAccountsFundsInputState = (AccountsFundsState) thirdInputState;
        BigDecimal amountBidded = ((Commands.MakeBid) tx.getCommand(0).component1()).getAmount();

        requireThat(req -> {
            req.using("New bidder cannot be the same as current bidder",
                    !nftInputState.getCurrentBidder().equals(newBidderAccountsFundsInputState.getEvmAddress()));

            req.using("Amount bidded must be higher than current bid",
                    amountBidded.compareTo(nftInputState.getCurrentBid()) > 0);

            req.using("Bidder need to have enough funds",
                    newBidderAccountsFundsInputState.getFunds().compareTo(amountBidded) >= 0);
            return null;
        });

        ContractState firstOutputState = tx.getOutputStates().get(0);
        ContractState secondOutputState = tx.getOutputStates().get(1);
        ContractState thirdOutputState = tx.getOutputStates().get(2);

        requireThat(req -> {
            req.using("first output state must be of type NftState",
                    firstOutputState instanceof NftState);

            req.using("second output state must be of type AccountsFundsState",
                    secondOutputState instanceof AccountsFundsState);

            req.using("third output state must be of type AccountsFundsState",
                    thirdOutputState instanceof AccountsFundsState);
            return null;
        });

        NftState nftOutputState = (NftState) firstOutputState;
        AccountsFundsState newBidderAccountsFundsOutputState = (AccountsFundsState) secondOutputState;
        AccountsFundsState oldBidderAccountsFundsOutputState = (AccountsFundsState) thirdOutputState;

        requireThat(req -> {
            req.using("Outputted AccountsFundsState from bidder must have funds equal to old funds minus amount bidded",
                    newBidderAccountsFundsOutputState.getFunds().compareTo(newBidderAccountsFundsInputState.getFunds().subtract(amountBidded)) == 0);

            req.using("Outputted AccountsFundsState from old bidder must have funds equal to his current funds plus the amount that he bidded",
                    oldBidderAccountsFundsOutputState.getFunds().compareTo(oldBidderAccountsFundsInputState.getFunds().add(nftInputState.getCurrentBid())) == 0);

            req.using("Outputted AccountsFundsState from bidder must have type as NEW_BID",
                    newBidderAccountsFundsOutputState.getType() == AccountsFundsStateTxType.NEW_BID);

            req.using("Outputted AccountsFundsState from old bidder must have type as BID_OVERTAKEN",
                    oldBidderAccountsFundsOutputState.getType() == AccountsFundsStateTxType.BID_OVERTAKEN);

            req.using("Outputted NftState has wrong bid number",
                    nftOutputState.getBidNumber() == (nftInputState.getBidNumber() + 1));

            req.using("Outputted NftState must have current bid equal to amount bidded",
                    nftOutputState.getCurrentBid().compareTo(amountBidded) == 0);
            return null;
        });
    }


    public interface Commands extends CommandData {
        class AddFunds implements Commands {
            private final BigDecimal amount;

            public AddFunds(BigDecimal amount) {
                this.amount = amount;
            }

            public BigDecimal getAmount() {
                return amount;
            }
        }

        class MakeBid implements Commands {
            private final BigDecimal amount;

            public MakeBid(BigDecimal amount) {
                this.amount = amount;
            }

            public BigDecimal getAmount() {
                return amount;
            }
        }
    }
}
