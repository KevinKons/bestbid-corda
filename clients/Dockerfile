#FROM corda/corda-zulu-java1.8-4.5

#EXPOSE 10050

#WORKDIR /app

#COPY build/libs/*.jar /app/api-client-corda.jar

#ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-Djava.security.egd=file:/dev/./urandom","-jar","/app/api-client-corda.jar"]


FROM azul/zulu-openjdk:8u192

COPY clients/build/libs/clients-0.1.jar /clients.jar
ENTRYPOINT ["java","-jar","/clients.jar"]