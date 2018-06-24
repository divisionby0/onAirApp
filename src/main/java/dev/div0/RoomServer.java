package dev.div0;

import com.google.gson.JsonArray;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.internal.server.config.JsonRpcConfiguration;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistry;
import org.kurento.room.AutodiscoveryKurentoClientProvider;
import org.kurento.room.api.KurentoClientProvider;
import org.kurento.room.kms.FixedOneKmsManager;
import org.kurento.room.rpc.JsonRpcNotificationService;
import org.kurento.room.rpc.JsonRpcUserControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.kurento.commons.PropertiesManager.getPropertyJson;

@Import(JsonRpcConfiguration.class)
@SpringBootApplication
public class RoomServer implements JsonRpcConfigurer {
    public static final String KMSS_URIS_PROPERTY = "kms.uris";
    public static final String KMSS_URIS_DEFAULT = "[ \"ws://localhost:8888/kurento\" ]";

    private static final Logger log = LoggerFactory.getLogger(RoomServer.class);

    @Bean
    @ConditionalOnMissingBean
    public KurentoClientProvider kmsManager() {

        JsonArray kmsUris = getPropertyJson(KMSS_URIS_PROPERTY, KMSS_URIS_DEFAULT, JsonArray.class);
        List<String> kmsWsUris = JsonUtils.toStringList(kmsUris);

        if (kmsWsUris.isEmpty()) {
            throw new IllegalArgumentException(KMSS_URIS_PROPERTY
                    + " should contain at least one kms url");
        }

        String firstKmsWsUri = kmsWsUris.get(0);

        if (firstKmsWsUri.equals("autodiscovery")) {
            log.info("Using autodiscovery rules to locate KMS on every pipeline");
            return new AutodiscoveryKurentoClientProvider();
        } else {
            log.info("Configuring Kurento Room Server to use first of the following kmss: " + kmsWsUris);
            return new FixedOneKmsManager(firstKmsWsUri);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcNotificationServiceCustom notificationService() {

        return new JsonRpcNotificationServiceCustom();
    }

    @Bean
    @ConditionalOnMissingBean
    public NotificationRoomManagerCustom roomManager() {
        return new NotificationRoomManagerCustom(notificationService(), kmsManager());
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcUserControlCustom userControl() {

        return new JsonRpcUserControlCustom(roomManager());
    }

    @Bean
    @ConditionalOnMissingBean
    public RoomJsonRpcHandlerCustom roomHandler() {
        return new RoomJsonRpcHandlerCustom(userControl(), notificationService());
    }

    @Override
    public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
        registry.addHandler(roomHandler().withPingWatchdog(true), "/room");
    }

    public static void main(String[] args) throws Exception {
        start(args);
    }

    public static ConfigurableApplicationContext start(String[] args) {
        log.info("Using /dev/urandom for secure random generation");
        System.setProperty("java.security.egd", "file:/dev/./urandom");
        return SpringApplication.run(RoomServer.class, args);
    }
}
