package cf.maybelambda.httpvalidator.springboot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class AppInfoController {
	protected static final String STATUS_ENDPOINT = "/api/status";

	/**
	 * If WebApplicationContext initialization was completed returns OK.
	 * @return The HTTP response object.
	 */
	@GetMapping(STATUS_ENDPOINT)
	public HttpStatus informWebAppStatus() {
		return HttpStatus.OK;
	}
}
