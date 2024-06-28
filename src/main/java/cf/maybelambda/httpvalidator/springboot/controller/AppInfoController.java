package cf.maybelambda.httpvalidator.springboot.controller;

import cf.maybelambda.httpvalidator.springboot.HTTPValidatorWebApp;
import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import cf.maybelambda.httpvalidator.springboot.service.EmailNotificationService;
import cf.maybelambda.httpvalidator.springboot.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.management.modelmbean.XMLParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin
public class AppInfoController {
    protected static final String STATUS_ENDPOINT = "/api/status";
    @Autowired
    XMLValidationTaskDao dao;
    @Autowired
    EmailNotificationService mailServ;
    @Autowired
    ValidationService valServ;

    /**
     * If WebApplicationContext initialization was completed returns OK.
     * Body: datafile and config status (i.e. parsable / valid) and app's start time.
     *
     * @return The HTTP response object.
     */
    @GetMapping(STATUS_ENDPOINT)
    public ResponseEntity<Map<String, String>> informWebAppStatus() {
        Map<String, String> res = new HashMap<>();
        String errVal = "ERROR";
        String okVal = "OK";

        res.put("start_time", HTTPValidatorWebApp.startTime);

        String dataFileStatus = okVal;
        try {
            this.dao.getDocData();
        } catch (XMLParseException e) {
            dataFileStatus = errVal;
        }
        res.put("datafile_status", dataFileStatus);

        res.put(
         "config_status",
            this.mailServ.isValidConfig() && this.valServ.isValidConfig() ? okVal : errVal
        );

        return ResponseEntity.ok(res);
    }
}
