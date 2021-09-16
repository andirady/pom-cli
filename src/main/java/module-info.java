module com.github.andirady.mq {
    exports com.github.andirady.mq;
    requires java.logging;
    requires java.net.http;
    requires java.xml;
    requires com.fasterxml.jackson.databind;
    requires info.picocli;
    
    opens com.github.andirady.mq to com.fasterxml.jackson.databind, info.picocli;
}
