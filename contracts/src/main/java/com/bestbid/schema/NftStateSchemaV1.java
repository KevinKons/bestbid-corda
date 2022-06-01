package com.bestbid.schema;

import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import org.hibernate.annotations.Type;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.UUID;

public class NftStateSchemaV1 extends MappedSchema {
    public NftStateSchemaV1() {
        super(PersistentNftState.class, 1, Arrays.asList(PersistentNftState.class));
    }

    @Nullable
    @Override
    public String getMigrationResource() {
        return "nft-state.changelog-master";
    }

    @Entity
    @Table(name = "nft_states")
    public static class PersistentNftState extends PersistentState {
        @Column(name = "bid_number") private final int bidNumber;
        @Column(name = "auction_ended") private final boolean auctionEnded;
        @Column(name = "minimum_bid") private final String minimumBid;
        @Column(name = "current_bid") private final String currentBid;
        @Column(name = "current_bidder") private final String currentBidder;
        @Column(name = "nft_id", unique = true) private final String nftId;
        @Column(name = "sender") private final String sender;
        @Column(name = "receiver") private final String receiver;
        @Column(name = "linear_id") @Type(type = "uuid-char") private final UUID linearId;

        public PersistentNftState(int bidNumber, boolean auctionEnded, String minimumBid, String currentBid, String currentBidder,
                                  String nftId, UUID linearId, String sender, String receiver) {
            this.bidNumber = bidNumber;
            this.auctionEnded = auctionEnded;
            this.minimumBid = minimumBid;
            this.currentBid = currentBid;
            this.currentBidder = currentBidder;
            this.nftId = nftId;
            this.linearId = linearId;
            this.sender = sender;
            this.receiver = receiver;
        }

        // Default constructor required by hibernate.
        public PersistentNftState() {
            this.bidNumber = 0;
            this.auctionEnded = false;
            this.minimumBid = null;
            this.currentBid = null;
            this.currentBidder = null;
            this.nftId = null;
            this.linearId = null;
            this.sender = null;
            this.receiver = null;
        }

        public int getBidNumber() {
            return bidNumber;
        }

        public boolean isAuctionEnded() {
            return auctionEnded;
        }

        public String getMinimumBid() {
            return minimumBid;
        }

        public String getCurrentBid() {
            return currentBid;
        }

        public String getCurrentBidder() {
            return currentBidder;
        }

        public String getNftId() {
            return nftId;
        }

        public String getSender() {
            return sender;
        }

        public String getReceiver() {
            return receiver;
        }

        public UUID getLinearId() {
            return linearId;
        }
    }
}