## HTTP Validator
[![](https://github.com/lfir/http-validator/actions/workflows/ci.yml/badge.svg)](https://github.com/lfir/http-validator/actions/workflows/ci.yml)

### Main features
- Executes HTTP requests periodically and performs some basic checks on the response data ("validation tasks").
- Reads task information from an XML file that can be updated independently of the application code 
(restart not needed either).
- Sends email notifications about invalid results (using [Sendgrid](https://sendgrid.com)'s service).

### Notes
Run schedule can also be set through a separate configuration file (restart required).

Next steps:
- Parallel task execution
- Offer some  basic processing statistics through the web API
