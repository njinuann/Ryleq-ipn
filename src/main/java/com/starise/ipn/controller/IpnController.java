package com.starise.ipn.controller;

import com.starise.ipn.Util.AXWorker;
import com.starise.ipn.entity.MpesaRequest;
//import com.starise.ipn.entity.TenantIdEntity;
import com.starise.ipn.entity.TenantIdEntity;
import com.starise.ipn.service.IpnService;
//import com.starise.ipn.service.MpesaTxnService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/api")
public class IpnController {

    private static final Logger logger = LoggerFactory.getLogger(IpnController.class);

    @Autowired
    private IpnService ipnService;

//    @Autowired
//    private MpesaTxnService mpesaTxnService;

    @GetMapping("/echo")
    public String getData() {
        return "{responseCode:00," +
                "responseMessage: echo Successful}";
    }
    @PostMapping("/processCB")
    public CompletableFuture<ResponseEntity<String>> processData(@RequestBody MpesaRequest data) {

        return ipnService.processMpesaTxnAsync(data).thenApply(tenantIdEntity -> {
            if (tenantIdEntity == null || tenantIdEntity.getId() == 0L) {
                return ResponseEntity.ok("{\n" +
                        "    \"ResultCode\": \"C2B00011\",\n" +
                        "    \"ResultDesc\": \"Rejected\"\n" +
                        "}");
            }
            return ResponseEntity.ok("{\n" +
                    "    \"ResultCode\": \"0\",\n" +
                    "    \"ResultDesc\": \"Accepted\"\n" +
                    "}");
        }).exceptionally(ex -> {
            logger.error("Error processing Mpesa transaction", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\n" +
                    "    \"ResultCode\": \"C2B99999\",\n" +
                    "    \"ResultDesc\": \"Error processing request\"\n" +
                    "}");
        });
    }


    @PostMapping("/validateCB")
    public ResponseEntity<String> validateDataById(@RequestBody MpesaRequest data) {
        System.out.println(data.toString());
        TenantIdEntity tenantIdEntity = ipnService.validateIdNo(data);
        if (tenantIdEntity == null || tenantIdEntity.getId() == 0L) {
            return ResponseEntity.ok("{    \n" +
                    "\"ResultCode\": \"C2B00011\",\n" +
                    "\"ResultDesc\": \"Rejected\",\n" +
                    "}");
        }
        return ResponseEntity.ok("{\n" +
                " \"ResultCode\": \"0\",\n" +
                " \"ResultDesc\": \"Accepted\",\n" +
                "}");

    }
}
