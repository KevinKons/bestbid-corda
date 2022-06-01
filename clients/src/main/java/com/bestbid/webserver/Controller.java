package com.bestbid.webserver;

import com.bestbid.flows.AccountsFunds.AddFundsFlow;
import com.bestbid.flows.EndAuctionFlow;
import com.bestbid.flows.MakeBidFlow;
import com.bestbid.flows.Nft.IssueNftFlow;
import com.bestbid.schema.NftStateSchemaV1;
import com.bestbid.states.AccountsFundsState;
import com.bestbid.states.AccountsFundsStateTxType;
import com.bestbid.states.NftState;
import com.bestbid.webserver.dto.AccountsFundsDTO;
import com.bestbid.webserver.dto.NftDTO;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/")
public class Controller {

    @Value("${parties.partyC.name}")
    private String partyCName;

    private final CordaRPCOps proxy;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }

    @GetMapping(value = "/health", produces = "text/plain")
    private String healthCheckEndPoint() {
        return "There is a endpoint here.";
    }

    @GetMapping(value = "/funds", produces = APPLICATION_JSON_VALUE)
    public List<AccountsFundsDTO> getAccountsFunds() {
        List<StateAndRef<AccountsFundsState>> stateAndRefAccountsFundsState = proxy.vaultQuery(AccountsFundsState.class).getStates();

        return stateAndRefAccountsFundsState.stream().map(ref -> new AccountsFundsDTO(
                ref.getState().component1().getType(),
                ref.getState().component1().getFunds().toString(),
                ref.getState().component1().getEvmAddress())
        ).collect(Collectors.toList());
    }

    @GetMapping(value = "/funds/{address}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAccountsFund(@PathVariable String address) {
        List<StateAndRef<AccountsFundsState>> stateAndRefAccountsFundsState = proxy.vaultQuery(AccountsFundsState.class).getStates();

        List<AccountsFundsDTO> accountsFundsList = stateAndRefAccountsFundsState.stream().map(ref -> new AccountsFundsDTO(
                ref.getState().component1().getType(),
                ref.getState().component1().getFunds().toString(),
                ref.getState().component1().getEvmAddress())
        ).collect(Collectors.toList());

        Optional<AccountsFundsDTO> optAddFundsDTO = accountsFundsList.stream()
                .filter(af -> af.getAddress().equalsIgnoreCase(address)).findFirst();
        if(optAddFundsDTO.isPresent()) {
            return new ResponseEntity<>(optAddFundsDTO.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>("Address not registered", HttpStatus.NOT_FOUND);
    }

    @PostMapping(value = "/addFundsEventHappend", consumes = APPLICATION_JSON_VALUE, produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> issueAddFundsEvent(@RequestBody AccountsFundsDTO accountsFundsDTO) throws IllegalArgumentException {

        CordaX500Name partyX500Name = CordaX500Name.parse(partyCName);
        Party otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name);

        try {
            SignedTransaction result = proxy.startTrackedFlowDynamic(
                    AddFundsFlow.Initiator.class,
                    AccountsFundsStateTxType.NEW_FUNDS,
                    accountsFundsDTO.getAmount(),
                    accountsFundsDTO.getAddress(),
                    otherParty
            )
                    .getReturnValue().get();

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction id " + result.getId() + " committed to ledger.\n " + result.getTx().getOutput(0));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @GetMapping(value = "/nftState/{nftId}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getNftStatesByNftId(@PathVariable String nftId) {
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
        Field nftIdField;
        try {
            nftIdField = NftStateSchemaV1.PersistentNftState.class.getDeclaredField("nftId");
        } catch (NoSuchFieldException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
        CriteriaExpression criteria = Builder.equal(nftIdField, nftId);
        QueryCriteria nftQuery = new QueryCriteria.VaultCustomQueryCriteria(criteria);
        generalCriteria = generalCriteria.and(nftQuery);

        List<StateAndRef<NftState>> nftStateAndRefs = proxy
                .vaultQueryByCriteria(generalCriteria, NftState.class).getStates();

        if (nftStateAndRefs.size() == 0) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("There is no NFT registered with this ID");
        }

        NftState nftState = nftStateAndRefs.get(0).component1().component1();
        NftDTO nftDTO = new NftDTO(nftState.getBidNumber(), nftState.isAuctionEnded(), nftState.getMinimumBid().toString(),
                nftState.getCurrentBid().toString(), nftState.getCurrentBidder(), nftState.getNftId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(nftDTO);
    }


    @GetMapping(value = "/nftState", produces = APPLICATION_JSON_VALUE)
    public List<NftDTO> getNftStates() {
        List<StateAndRef<NftState>> stateAndRefAccountsFundsState = proxy.vaultQuery(NftState.class).getStates();

        return stateAndRefAccountsFundsState.stream()
                .map(ref -> {
                    NftState nftState = ref.getState().getData();
                    return new NftDTO(
                        nftState.getBidNumber(),
                        nftState.isAuctionEnded(),
                        nftState.getMinimumBid().toString(),
                        nftState.getCurrentBid().toString(),
                        nftState.getCurrentBidder(),
                        nftState.getNftId()
                    );
                })
                .collect(Collectors.toList());
    }

    @PostMapping(value = "/nftState", consumes = APPLICATION_JSON_VALUE, produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> issueNftState(@RequestBody NftDTO nftDTO) throws IllegalArgumentException {

        CordaX500Name partyX500Name = CordaX500Name.parse(partyCName);
        Party otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name);

        try {
            SignedTransaction result = proxy.startTrackedFlowDynamic(
                    IssueNftFlow.Initiator.class,
                    nftDTO.getMinimumBid(),
                    nftDTO.getNftId(),
                    otherParty
            )
                    .getReturnValue().get();

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction id " + result.getId() + " committed to ledger.\n " + result.getTx().getOutput(0));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @PostMapping(value = "/bid", consumes = APPLICATION_JSON_VALUE, produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> makeBid(@RequestBody NftDTO nftDTO) throws IllegalArgumentException {

        CordaX500Name partyX500Name = CordaX500Name.parse(partyCName);
        Party otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name);

        try {
            SignedTransaction result = proxy.startTrackedFlowDynamic(
                    MakeBidFlow.Initiator.class,
                    nftDTO.getCurrentBid(),
                    nftDTO.getCurrentBidder(),
                    nftDTO.getNftId(),
                    otherParty
            )
                    .getReturnValue().get();

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction id " + result.getId() + " committed to ledger.\n " + result.getTx().getOutput(0));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @PostMapping(value = "/endAuction", consumes = APPLICATION_JSON_VALUE, produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> endAuction(@RequestBody NftDTO nftDTO) throws IllegalArgumentException {

        CordaX500Name partyX500Name = CordaX500Name.parse(partyCName);
        Party otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name);

        try {
            SignedTransaction result = proxy.startTrackedFlowDynamic(
                    EndAuctionFlow.Initiator.class,
                    nftDTO.getNftId(),
                    otherParty
            )
                    .getReturnValue().get();

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction id " + result.getId() + " committed to ledger.\n " + result.getTx().getOutput(0));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }
}
