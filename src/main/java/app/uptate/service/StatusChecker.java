package app.uptate.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.javalite.activejdbc.Base;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusChecker.class);
    private static ObjectMapper mapper = null;
    private final HashMap<String, String> orderNumber = new HashMap<>();
    private static String databaseUrl = System.getenv("TANAIS_SERVICE_DB_URL");
    private static String databasePass = System.getenv("TANAIS_SERVICE_DB_PWD");
    private static String databaseLogin = System.getenv("TANAIS_SERVICE_DB_USER");
    private static final String docNumber = "750-89819321";
    private static List<Map> documentNum;

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
            StatusChecker checker = new StatusChecker();
            documentNum = checker.getDocNumber();
//            documentNum.forEach(map -> {
//                checker.connectToApi(map.get("master_document_no").toString());
//                checker.updateOrderStatus();
//            });
            checker.connectToApi(docNumber);
//            checker.updateOrderStatus();

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

    // получаем список всех номеров накладных из таблицы cargoflow_shipment
    List<Map> getDocNumber () {
        final String query = "SELECT master_document_no FROM cargoflow_shipment";
        List<Map> documentNum = Base.findAll(query);
        return documentNum;
    }

    // отправка get запроса к api
    void connectToApi(String docNumber) {
        System.out.println("docNumber " + docNumber);
        DefaultHttpClient Client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("https://b2c.tanais.tech/api/v1/client/awb-status/" + docNumber + "/");
        String encoding = null;
        try {
            encoding = DatatypeConverter.printBase64Binary("sblogistica.ru:zQMFQ6b".getBytes("UTF-8"));
            httpGet.setHeader("Authorization", "Basic " + encoding);
            HttpResponse response = Client.execute(httpGet);
            String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            // проверка если возвращается ошибка
            handleResponse(result);
        } catch (IOException e) {
            LOGGER.error("! Tanais Update service call failure {}", e.getMessage());
        }
    }

    // обновление в таблице order поля custom_status по номеру заказа
    void updateOrderStatus() {
        orderNumber.forEach((num, status) -> {
            final String query = "UPDATE public.order SET custom_status = '" + status + "' WHERE tracking_number = '" + num + "'";
            Base.exec(query);
        });
        Base.commitTransaction();
    }

    // обновление статуса shipment в таблице cargoflow_shipment
    void updateShipmentStatus(Shipment shipment) {
        // начинается транзакция
        Base.openTransaction();
        final String query = "UPDATE cargoflow_shipment SET custom_status = '" + shipment.getAwbStatus() + "' WHERE master_document_no = '" + docNumber + "'";
        Base.exec(query);
    }

    private void handleResponse(String message) {
        try {
            JsonRpcResponse response = getJsonMapper().readValue(message, JsonRpcResponse.class);
            if (!response.success()) {
                System.out.println("response.getError() " + response.getErrors());
                LOGGER.error("Tanais Update service call failure: {}", response.getErrors());
            } else {
                Shipment shipment = getJsonMapper().readValue(message, Shipment.class);
                updateShipmentStatus(shipment);
                shipment.getOrders().forEach(order -> {
                    orderNumber.put(order.getNum(), order.getStatus());
                });
                updateOrderStatus();
            }
        } catch (Exception e) {
            System.out.println("Parsing result error: " + e.getMessage());
            LOGGER.error("Parsing result error: {}", e.getMessage());
        }
//        return Collections.emptyList();
    }

    private ObjectMapper getJsonMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        return mapper;
    }
}