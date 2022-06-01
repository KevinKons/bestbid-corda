package com.bestbid.contracts;

import com.bestbid.states.NftState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class NftContract implements Contract {

    public static final String ID = "com.bestbid.contracts.NftContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final CommandData commandData = tx.getCommands().get(0).getValue();

        if (commandData instanceof Commands.Issue) {
            verifyIssue(tx);
        } else if (commandData instanceof AccountsFundsContract.Commands.MakeBid) {
            verifyMakeBid(tx);
        } else if (commandData instanceof Commands.EndAuction) {
            verifyEndAuction(tx);
        } else {
            throw new IllegalArgumentException("Command not found");
        }
    }

    private void verifyEndAuction(LedgerTransaction tx) {
        requireThat(req -> {
              req.using("There must be only one NftState inputted.",
                    tx.getInputStates().size() == 1);

              req.using("Input state must be of type NftState.",
                    tx.inputsOfType(NftState.class).size() == 1);

              NftState inputState = (NftState) tx.getInputStates().get(0);

              req.using("Auction ended must be false on the input State.",
                      !inputState.isAuctionEnded());

              req.using("There must be only one NftState outputted.",
                    tx.getOutputStates().size() == 1);

              req.using("Output state must be of type NftState.",
                    tx.outputsOfType(NftState.class).size() == 1);

              NftState outputState = (NftState) tx.getOutputStates().get(0);

              req.using("Auction ended must be true on the output State.",
                     outputState.isAuctionEnded());

            return null;
        });
    }

    private void verifyIssue(LedgerTransaction tx) {
        ContractState output = tx.getOutputStates().get(0);
        requireThat(req -> {
            req.using("No inputs should be consumed when issuing a NftState.",
                    tx.getInputStates().size() == 0);

            NftState outputState = tx.outputsOfType(NftState.class).get(0);
            req.using("Minimum Bid must be more than zero.",
                    outputState.getMinimumBid().compareTo(BigDecimal.ZERO) > 0);

            req.using("Only one output state must be generated when issuing a new NftState",
                    tx.getOutputStates().size() == 1);

            req.using("When issuing a new NftState the type of the outputted state must be NftState",
                    output instanceof NftState);
            return null;
        });

        NftState outputtedNftState = (NftState) output;
        requireThat(req -> {
            req.using("When issuing a NftState the bid number must be 0",
                    outputtedNftState.getBidNumber() == 0);

            req.using("When issuing a NftState the current bidder must be empty",
                    outputtedNftState.getCurrentBidder().equals(""));

            req.using("When issuing a NftId must not be null",
                    outputtedNftState.getNftId() != null);
           return null;
        });
    }

    private void verifyMakeBid(LedgerTransaction tx) {
        ContractState firstInputState = tx.getInputStates().get(0);
        requireThat(req -> {
            req.using("First input state must be of type NftState.",
                    firstInputState instanceof NftState);

            req.using("There must be only one NftState outputted.",
                    tx.outputsOfType(NftState.class).size() == 1);

            NftState inputState = (NftState) tx.getInputStates().get(0);

            req.using("You can't make a bid on a ended auction.",
                    !inputState.isAuctionEnded());

            NftState outputState = (NftState) tx.getOutputStates().get(0);

            req.using("Auction ended must be false on the output State.",
                    !outputState.isAuctionEnded());
            return null;
        });
    }

    public interface Commands extends CommandData {
        class Issue implements Commands {}
        class EndAuction implements Commands {}
    }
}
