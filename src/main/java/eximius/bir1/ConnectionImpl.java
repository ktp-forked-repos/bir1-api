package eximius.bir1;

import static eximius.bir1.Utils.isBlank;
import static eximius.bir1.Utils.isNull;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.time.LocalDateTime;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.AddressingFeature;

import org.tempuri.UslugaBIRzewnPubl;

import cis.bir.publ._2014._07.IUslugaBIRzewnPubl;
import eximius.bir1.data.DataHandler;
import eximius.bir1.report.ReportHandler;
import eximius.bir1.value.ValueHandler;

final class ConnectionImpl implements Connection {
    private static final String HEADER_SID = "sid";
    private static final int TIMEOUT_IN_MINUTES = 59;

    private final IUslugaBIRzewnPubl port;

    private final String apiKey;

    private LocalDateTime lastRequest;

    private String sessionKey;

    private final DataHandler dataHandler;

    private final ReportHandler reportHandler;

    private final ValueHandler valueHandler;

    ConnectionImpl(final String apiKey) {
        this.apiKey = apiKey;
        final UslugaBIRzewnPubl service = new UslugaBIRzewnPubl();
        this.port = service.getE3(new AddressingFeature());
        dataHandler = new DataHandlerImpl(this, port);
        reportHandler = new ReportHandlerImpl(this, port);
        valueHandler = new ValueHandlerImpl(this, port);
    }

    @Override
    public final DataHandler findData() {
        return dataHandler;
    }

    @Override
    public final ReportHandler getReport() {
        return reportHandler;
    }

    @Override
    public final ValueHandler getValue() {
        return valueHandler;
    }

    final synchronized void reinitiate() {
        if (needsReinitiation()) {
            lastRequest = LocalDateTime.now();
            sessionKey = port.zaloguj(apiKey);
            if (isBlank(sessionKey)) {
                throw new RuntimeException("Invalid API KEY: " + apiKey);
            }
            setLoginHeader(sessionKey);
        }
    }

    private final boolean needsReinitiation() {
        return isNull(lastRequest) || lastRequest.plusMinutes(TIMEOUT_IN_MINUTES).isBefore(LocalDateTime.now());
    }

    private final void setLoginHeader(final String sid) {
        final BindingProvider provider = (BindingProvider) port;
        provider.getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, singletonMap(HEADER_SID, asList(sid)));
    }

    @Override
    public final void close() throws IOException {
        port.wyloguj(sessionKey);
    }
}