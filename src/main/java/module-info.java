module com.github.andirady.pomcli {
  exports com.github.andirady.pomcli;

  requires java.logging;
  requires java.net.http;
  requires java.xml;
  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.module.afterburner;
  requires info.picocli;

  opens com.github.andirady.pomcli to
      com.fasterxml.jackson.databind,
      info.picocli;
  opens com.github.andirady.pomcli.solrsearch to
      com.fasterxml.jackson.databind,
      info.picocli;
}
