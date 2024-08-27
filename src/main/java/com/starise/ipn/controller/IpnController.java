package com.starise.ipn.controller;

import com.starise.ipn.entity.MpesaRequest;
//import com.starise.ipn.entity.TenantIdEntity;
import com.starise.ipn.entity.TenantIdEntity;
import com.starise.ipn.service.IpnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class IpnController {

    @Autowired
    private IpnService ipnService;

//    @GetMapping("/validateData/{id}")
//    public ResponseEntity<IpnEntity> validateData(@PathVariable Long id) {
//        IpnEntity entity = service.validateData(id);
//        if (entity != null) {
//            return ResponseEntity.ok(entity);
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }

    @PostMapping("/processCB")
    public ResponseEntity<String> processData(@RequestBody MpesaRequest data) throws Exception {
        TenantIdEntity tenantIdEntity = ipnService.postWithIdNo(data);

        if(tenantIdEntity==null || tenantIdEntity.getId()==0L){
            return ResponseEntity.ok("{    \n" +
                    "\"ResultCode\": \"C2B00011\",\n" +
                    "\"ResultDesc\": \"Rejected\",\n" +
                    "}");
        }

        return ResponseEntity.ok("{    \n" +
                "   \"ResultCode\": \"0\",\n" +
                "   \"ResultDesc\": \"Accepted\",\n" +
                "}");
    }

    @PostMapping("/validateCB")
    public ResponseEntity<String> validateDataById(@RequestBody MpesaRequest data) {
        System.out.println(data.toString());
        TenantIdEntity tenantIdEntity = ipnService.validateIdNo(data);
        if(tenantIdEntity==null || tenantIdEntity.getId()==0L){
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
