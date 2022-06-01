package com.bestbid.webserver.dto;

import io.swagger.annotations.ApiModelProperty;

public class NftDTO {

    @ApiModelProperty(notes = "Bid Number", example = "1", required = true)
    private int bidNumber;
    @ApiModelProperty(notes = "Auction Ended", example = "false", required = true)
    private boolean auctionEnded;
    @ApiModelProperty(notes = "Minimum Bid", example = "10", required = true)
    private String minimumBid;
    @ApiModelProperty(notes = "Current Bid", example = "15")
    private String currentBid;
    @ApiModelProperty(notes = "Current Bidder Wallet", example = "0xD848B448F3E3276Fad558BE0719C889D647b0a88")
    private String currentBidder;
    @ApiModelProperty(notes = "NFT id", example = "1", required = true)
    private String nftId;

    public NftDTO() {
    }

    public NftDTO(int bidNumber, boolean auctionEnded, String minimumBid, String currentBid, String currentBidder, String nftId) {
        this.bidNumber = bidNumber;
        this.auctionEnded = auctionEnded;
        this.minimumBid = minimumBid;
        this.currentBid = currentBid;
        this.currentBidder = currentBidder;
        this.nftId = nftId;
    }

    public NftDTO(String minimumBid, String nftId) {
        this.minimumBid = minimumBid;
        this.nftId = nftId;
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
}
