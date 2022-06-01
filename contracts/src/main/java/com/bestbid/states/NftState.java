package com.bestbid.states;

import com.bestbid.contracts.NftContract;
import com.bestbid.schema.NftStateSchemaV1;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@BelongsToContract(NftContract.class)
public class NftState implements ContractState, QueryableState {

    private final int bidNumber;
    private final boolean auctionEnded;
    private final BigDecimal minimumBid;
    private final BigDecimal currentBid;
    private final String currentBidder;
    private final String nftId;
    private final Party sender;
    private final Party receiver;
    private final UniqueIdentifier linearId;

    public NftState(int bidNumber, boolean auctionEnded, BigDecimal minimumBid, BigDecimal currentBid, String currentBidder,
                    String nftId, Party sender, Party receiver) {
        this.bidNumber = bidNumber;
        this.auctionEnded = auctionEnded;
        this.minimumBid = minimumBid;
        this.currentBid = currentBid;
        this.currentBidder = currentBidder;
        this.nftId = nftId;
        this.sender = sender;
        this.receiver = receiver;
        this.linearId = new UniqueIdentifier();
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(sender,receiver);
    }

    @NotNull
    @Override
    public PersistentState generateMappedObject(@NotNull MappedSchema schema) {
        if (schema instanceof NftStateSchemaV1) {
            return new NftStateSchemaV1.PersistentNftState(
                    this.bidNumber,
                    this.auctionEnded,
                    this.minimumBid.toString(),
                    this.currentBid.toString(),
                    this.currentBidder,
                    this.nftId,
                    this.linearId.getId(),
                    this.sender.getName().toString(),
                    this.receiver.getName().toString());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @NotNull
    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return Collections.singletonList(new NftStateSchemaV1());
    }

    public int getBidNumber() {
        return bidNumber;
    }

    public boolean isAuctionEnded() {
        return auctionEnded;
    }

    public BigDecimal getMinimumBid() {
        return minimumBid;
    }

    public BigDecimal getCurrentBid() {
        return currentBid;
    }

    public String getCurrentBidder() {
        return currentBidder;
    }

    public String getNftId() {
        return nftId;
    }

    public Party getSender() {
        return sender;
    }

    public Party getReceiver() {
        return receiver;
    }

    public UniqueIdentifier getLinearId() {
        return linearId;
    }
  
  @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NftState nftState = (NftState) o;

        if (bidNumber != nftState.bidNumber) return false;
        if (auctionEnded != nftState.auctionEnded) return false;
        if (!minimumBid.equals(nftState.minimumBid)) return false;
        if (currentBid != null ? !currentBid.equals(nftState.currentBid) : nftState.currentBid != null) return false;
        if (currentBidder != null ? !currentBidder.equals(nftState.currentBidder) : nftState.currentBidder != null)
            return false;
        if (!nftId.equals(nftState.nftId)) return false;
        if (!sender.equals(nftState.sender)) return false;
        return receiver.equals(nftState.receiver);
    }

    @Override
    public int hashCode() {
        int result = bidNumber;
        result = 31 * result + (auctionEnded ? 1 : 0);
        result = 31 * result + minimumBid.hashCode();
        result = 31 * result + (currentBid != null ? currentBid.hashCode() : 0);
        result = 31 * result + (currentBidder != null ? currentBidder.hashCode() : 0);
        result = 31 * result + nftId.hashCode();
        result = 31 * result + sender.hashCode();
        result = 31 * result + receiver.hashCode();
        return result;
    }
}
