package app.uptate.service;

import app.cargoflow.ecommerce.model.Provider;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.javalite.activejdbc.Base;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusChecker.class);
    private static ObjectMapper mapper = null;
    private HashMap<String, String> ordersStatus;
    private HashMap<String, String> ordersRegisters;
    private static final String databaseUrl = System.getenv("TANAIS_SERVICE_DB_URL");
    private static final String databasePass = System.getenv("TANAIS_SERVICE_DB_PWD");
    private static final String databaseLogin = System.getenv("TANAIS_SERVICE_DB_USER");
    private static final String tanaisProviderId = System.getenv("TANAIS_PROVIDER_ID");
    private static List<Map> documentNum;
    private static Provider tanaisProvider;

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        LOGGER.info("+ Start tanais update service...");
        System.out.println("+ Start tanais update service...");
        try {
            if (Strings.isNullOrEmpty(databaseUrl)) {
                throw new IllegalStateException("! Database source isn't defined");
            }
            Base.open("org.postgresql.Driver", databaseUrl,
                    databaseLogin,
                    databasePass
            );

            tanaisProvider = Provider.getByCode(tanaisProviderId);
            if (tanaisProvider == null){
                LOGGER.error("Please configure tanais provider id (loaded configuration is empty, id: {})", tanaisProviderId);
                return;
            }

            StatusChecker checker = new StatusChecker();
            documentNum = checker.getDocNumber();
            documentNum.forEach(map -> {
                checker.connectToApi((String)map.get("master_document_no"));
            });

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            if (Base.hasConnection()) {
                Base.close();
            }
        }
        System.out.println("! Update done: " + (System.currentTimeMillis() - startTime) / 1000 + " s");
        LOGGER.info("! Update done: {} s", (System.currentTimeMillis() - startTime) / 1000);
    }

    // ???????????????? ???????????? ???????? ?????????????? ?????????????????? ???? ?????????????? cargoflow_shipment
    List<Map> getDocNumber () {
        final String query = "SELECT master_document_no FROM cargoflow_shipment";
        List<Map> documentNum = Base.findAll(query);
        return documentNum;
    }

    // ???????????????? get ?????????????? ?? api
    void connectToApi(String docNumber) {
        System.out.println("docNumber " + docNumber);
        HttpClient client = HttpClientBuilder.create().build();
        try {
            HttpGet httpGet = new HttpGet(tanaisProvider.getCallbackUrl() + "/client/awb-status/" + docNumber + "/");
            httpGet.addHeader("Authorization", "Basic " + Base64.getEncoder()
                    .encodeToString(
                            (tanaisProvider.getPlatformId() + ":" + tanaisProvider.getPlatformSecret())
                                    .getBytes(StandardCharsets.UTF_8))
            );

            HttpResponse response = client.execute(httpGet);
            String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            // ???????????????? ???????? ???????????????????????? ????????????
            handleResponse(result, docNumber);
            return;
        } catch (Exception e) {
            LOGGER.error("! Tanais Update service call failure {}", e.getMessage());
        }
    }

    // ???????????????????? ?? ?????????????? order ???????? custom_status ???? ???????????? ????????????
    void updateOrderStatus(HashMap<String, String> ordersStatus) {
        ordersStatus.forEach((num, status) -> {
            final String query = "UPDATE public.order SET custom_status = '" + status + "' WHERE tracking_number = '" + num + "'";
            Base.exec(query);
        });
    }

    // ???????????????????? ?? ?????????????? order ???????? register ???? ???????????? ????????????
    void updateOrderRegister(HashMap<String, String> ordersRegisters) {
        ordersRegisters.forEach((num, register) -> {
            if (!register.isEmpty()) {
                final String query = "UPDATE public.order SET register = '" + register + "' WHERE tracking_number = '" + num + "'";
                Base.exec(query);
            }
        });
    }

    // ???????????????????? ?????????????? shipment ?? ?????????????? cargoflow_shipment
    void updateShipmentStatus(Shipment shipment, String docNumber) {
        final String query = "UPDATE cargoflow_shipment SET custom_status = '" + shipment.getAwbStatus() + "' WHERE master_document_no = '" + docNumber + "'";
        Base.exec(query);
    }

    private void handleResponse(String message, String docNumber) {
        try {
            JsonRpcResponse response = getJsonMapper().readValue(message, JsonRpcResponse.class);
            if (!response.success()) {
                System.out.println("response.getError() " + response.getErrors());
                LOGGER.error("Tanais Update service call failure: {}", response.getErrors());
            } else {
                Shipment shipment = getJsonMapper().readValue(message, Shipment.class);
                // ???????????????????? ????????????????????
                Base.openTransaction();
                updateShipmentStatus(shipment, docNumber);
                ordersStatus = new HashMap<>();
                shipment.getOrders().forEach(order -> {
                    ordersStatus.put(order.getNum(), order.getStatus());
                    ordersRegisters.put(order.getNum(), order.getRegister());
                });
                updateOrderRegister(ordersRegisters);
                updateOrderStatus(ordersStatus);
                // ???????????? ????????????????????
                Base.commitTransaction();
            }
        } catch (Exception e) {
            System.out.println("Parsing result error: " + e.getMessage());
            LOGGER.error("Parsing result error: {}", e.getMessage());
        }
    }

    private ObjectMapper getJsonMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        return mapper;
    }
}