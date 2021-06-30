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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusChecker.class);
    private static ObjectMapper mapper = null;
    private HashMap<String, String> orderNumber = new HashMap<>();
    private static String databaseUrl = "jdbc:postgresql://localhost:5432/e.commerce";
    private static final String docNumber = "750-89819321";

    public static void main(String[] args) {
        try {
            if (Strings.isNullOrEmpty(databaseUrl)) {
                throw new IllegalStateException("! Database source isn't defined");
            }
            Base.open("org.postgresql.Driver", databaseUrl,
                    "postgres",
                    "l980327a"
            );
            StatusChecker checker = new StatusChecker();
            checker.connectToApi(docNumber);
            checker.updateOrderStatus();

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            if (Base.hasConnection()) {
                Base.close();
            }
        }
    }

    void getDocNumber () {
        final String query = "SELECT master_document_no FROM cargoflow_shipment";
        List<Map> statusList = Base.findAll(query);
//        System.out.println(statusList.toString());
// список номеров накладных
        statusList.forEach(map -> {
            System.out.println( map.get("master_document_no"));
        });
    }

    // отправка get запроса к api
    void connectToApi(String docNumber) {
        DefaultHttpClient Client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("https://b2c.tanais.tech/api/v1/client/awb-status/" + docNumber + "/");
        String encoding = null;
        try {
            encoding = DatatypeConverter.printBase64Binary("sblogistica.ru:zQMFQ6b".getBytes("UTF-8"));
            httpGet.setHeader("Authorization", "Basic " + encoding);
            HttpResponse response = Client.execute(httpGet);
            String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            System.out.println("result " + result);

            Shipment shipment = getJsonMapper().readValue(result, Shipment.class);
            updateShipmentStatus(shipment);
            shipment.getOrders().forEach(order -> {
                orderNumber.put(order.getNum(), order.getStatus());
            });
        } catch (IOException e) {
            LOGGER.error("! Tanais service call failure {}", e.getMessage());
        }
    }

    // обновление в таблице order поля custom_status по номеру заказа
    void updateOrderStatus() {
        // поиск в таблице order по трек номеру
//        final String query = "SELECT * FROM public.order WHERE tracking_number = ?";
//        List<Map> orderList = Base.findAll(query, orderNumber.get(0));

        orderNumber.forEach((num, status) -> {
            final String query = "UPDATE public.order SET custom_status = '" + status + "' WHERE tracking_number = '" + num + "'";
            Base.exec(query);
        });
    }

    // обновление статуса shipment в таблице cargoflow_shipment
    void updateShipmentStatus(Shipment shipment) {
        final String query = "UPDATE cargoflow_shipment SET custom_status = '" + shipment.getAwbStatus() + "' WHERE master_document_no = '" + docNumber + "'";
        Base.exec(query);
    }

    private ObjectMapper getJsonMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        return mapper;
    }
}