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
    static final String STATUS_ENDPOINT = "/api/status";
    static final String ERROR_VALUE = "ERROR";
    static final String OK_VALUE = "OK";
    static final String START_TIME_KEY = "start_time";
    static final String DATAFILE_STATUS_KEY = "datafile_status";
    static final String CONFIG_STATUS_KEY = "config_status";
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

        res.put(START_TIME_KEY, HTTPValidatorWebApp.startTime);

        String dataFileStatus = OK_VALUE;
        try {
            this.dao.getDocData();
        } catch (XMLParseException e) {
            dataFileStatus = ERROR_VALUE;
        }
        res.put(DATAFILE_STATUS_KEY, dataFileStatus);

        res.put(
            CONFIG_STATUS_KEY,
            this.mailServ.isValidConfig() && this.valServ.isValidConfig() ? OK_VALUE : ERROR_VALUE
        );

        return ResponseEntity.ok(res);
    }
}
