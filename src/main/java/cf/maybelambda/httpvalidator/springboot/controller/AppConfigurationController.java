package cf.maybelambda.httpvalidator.springboot.controller;

import cf.maybelambda.httpvalidator.springboot.HTTPValidatorWebApp;
import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import cf.maybelambda.httpvalidator.springboot.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.ERROR_VALUE;

@RestController
@CrossOrigin
public class AppConfigurationController {
    public static final String UPD_RUN_SCHEDULE_ENDPOINT = "/api/validator/runschedule";
    public static final String UPD_DATA_FILE_ENDPOINT = "/api/validator/datafile";
    public static final String CRON_EXPRESSION_KEY = "cron_expression";
    public static final String INVALID_CRON_EXPRESSION_ERROR_MSG = "Invalid Cron Expression";
    public static final String INVALID_DATA_FILE_ERROR_MSG = "Invalid Data File";
    public static final String UPD_DATA_FILE_ERROR_MSG = "Error updating Data File";
    @Autowired
    private ValidationService valServ;
    @Autowired
    private XMLValidationTaskDao dao;

    @PutMapping(UPD_RUN_SCHEDULE_ENDPOINT)
    public ResponseEntity<Map<String, String>> updateValidatorRunSchedule(@RequestBody Map<String, String> body) {
        ResponseEntity<Map<String, String>> res;
        String expr = body.get(CRON_EXPRESSION_KEY);

        if (this.valServ.isValidCronExpression(expr)) {
            HTTPValidatorWebApp.restartAppContextWithNewRunSchedule(expr);
            res = ResponseEntity.ok(null);
        } else {
            Map<String, String> resBody = new HashMap<>();
            resBody.put(ERROR_VALUE.toLowerCase(), INVALID_CRON_EXPRESSION_ERROR_MSG);
            res = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resBody);
        }

        return res;
    }

    @PutMapping(UPD_DATA_FILE_ENDPOINT)
    public ResponseEntity<Map<String, String>> updateValidatorDataFile(@RequestBody MultipartFile file) {
        ResponseEntity<Map<String, String>> res;
        res = ResponseEntity.ok(null);
        Map<String, String> resBody = new HashMap<>();

        try {
            this.dao.updateDataFile(file);
        } catch (IOException e) {
            resBody.put(ERROR_VALUE.toLowerCase(), UPD_DATA_FILE_ERROR_MSG);
            res = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resBody);
        } catch (Exception e) {
            resBody.put(ERROR_VALUE.toLowerCase(), INVALID_DATA_FILE_ERROR_MSG);
            res = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resBody);
        }

        return res;
    }
}
