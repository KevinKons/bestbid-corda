package com.bestbid.flows;

import com.google.common.collect.ImmutableList;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.node.MockNetworkNotarySpec;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.TestCordapp;

public interface FlowHelpers {

    static MockNetworkParameters prepareMockNetworkParameters() throws Exception {
        return new MockNetworkParameters()
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB"))))
                .withCordappsForAllNodes(ImmutableList.of(
                        TestCordapp.findCordapp("com.bestbid.contracts"),
                        TestCordapp.findCordapp("com.bestbid.flows"))
                );
    }
}
