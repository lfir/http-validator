package cf.maybelambda.httpvalidator.springboot.controller;

import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import cf.maybelambda.httpvalidator.springboot.service.EmailNotificationService;
import cf.maybelambda.httpvalidator.springboot.service.EventListenerService;
import cf.maybelambda.httpvalidator.springboot.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin
public class AppInfoController {
    public static final String START_TIME_KEY = "start_time";
    public static final String TIME_ELAPSED_KEY = "time_elapsed";
    public static final String TASKS_TOTAL_KEY = "tasks_total";
    public static final String TASKS_OK_KEY = "tasks_ok";
    public static final String TASKS_FAILED_KEY = "tasks_failed";
    public static final String TASKS_ERRORS_KEY = "tasks_errors";
    public static final String NO_LASTRUN_DATA_ERROR_MSG = "No validation tasks have been completed yet";
    public static final String STATUS_ENDPOINT = "/api/status";
    public static final String LAST_RUN_ENDPOINT = "/api/validator/lastrun";
    public static final String ERROR_VALUE = "ERROR";
    static final String OK_VALUE = "OK";
    public static final String DATAFILE_STATUS_KEY = "datafile_status";
    public static final String CONFIG_STATUS_KEY = "config_status";
    @Autowired
    private XMLValidationTaskDao dao;
    @Autowired
    private EmailNotificationService mailServ;
    @Autowired
    private ValidationService valServ;
    @Autowired
    private EventListenerService eventServ;

    /**
     * If WebApplicationContext initialization was completed returns OK.
     * Body: datafile and config status (i.e. parsable / valid) and app's start time.
     *
     * @return The HTTP response object.
     */
    @GetMapping(STATUS_ENDPOINT)
    public ResponseEntity<Map<String, String>> informWebAppStatus() {
        Map<String, String> res = new HashMap<>();

        res.put(START_TIME_KEY, eventServ.getStartDateTime());

        res.put(DATAFILE_STATUS_KEY, this.dao.isDataFileStatusOk() ? OK_VALUE : ERROR_VALUE);

        res.put(
            CONFIG_STATUS_KEY,
            this.mailServ.isValidConfig() && this.valServ.isValidConfig() ? OK_VALUE : ERROR_VALUE
        );

        return ResponseEntity.ok(res);
    }

    @GetMapping(LAST_RUN_ENDPOINT)
    public ResponseEntity<Map<String, String>> informLastRunData() {
        ResponseEntity<Map<String, String>> res;
        Map<String, String> infoBody = this.valServ.getLastRunInfo();
        if (infoBody.isEmpty()) {
            infoBody.put(ERROR_VALUE.toLowerCase(), NO_LASTRUN_DATA_ERROR_MSG);
            res = ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(infoBody);
        } else {
            res = ResponseEntity.ok(infoBody);
        }

        return res;
    }
}
