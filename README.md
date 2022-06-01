<br />
<p align="center">
  <img src="https://bestbid.dev.pod-expand.becomeholonic.com/bestbid-logo.dd58f27.svg" alt="Corda" width="300">
</p>
<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="150">
</p>


## About The Project

This project is contains the two CorDapps - one that holds contracts and another that holds workflows -, and a API
that starts flows and return states from the CordApps. The contract CorDapp holds two states, one related to the
funds that a user has on the platform, and the other related to the NFTs that created at the platform. On the other 
hand the workflows CorDapp holds flows to issue NFT state, to add funds to a user, to make a bid on a NFT, and to
end a NFT auction.

This project was built using the [Java CorDapp template](https://github.com/corda/cordapp-template-java/). The CorDapp 
template is a stubbed-out CorDapp that you can use to bootstrap your own CorDapps.

## Running the project

### Pre-Requisites

We recommend using IntelliJ IDEA, in this way you won't need to install Gradle.

### Building the project

To build the project you will use the following command:

`gradlew deployNodes`

Be aware that this command may vary depending on your OS or OS configurations.

### Deploying the nodes

To deploy the nodes you need to execute the runnodes file inside the build/nodes directory

### Running the API

To run the api you will use the following command:

`gradlew runTemplateServer`

### Running test coverage

To run the test coverage you just need to build the project using the `gradlew clean build`
and them access the index.html file generated on the build/reports/jacoco/test/html directory
(this build directory is the one inside the CorDapps directory).

