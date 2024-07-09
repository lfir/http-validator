## HTTP Validator

[![CI Badge](https://github.com/lfir/http-validator/actions/workflows/ci.yml/badge.svg)](https://github.com/lfir/http-validator/actions/workflows/ci.yml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/1ef9731e22064eccad14c374565e12bb)](https://app.codacy.com/gh/lfir/http-validator/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/1ef9731e22064eccad14c374565e12bb)](https://app.codacy.com/gh/lfir/http-validator/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_coverage)

### Main features

- Executes HTTP requests periodically and performs some basic checks on the response data 
("validation tasks").

- Reads task information from an XML file that can be updated independently of the application code,
directly or via Web API (app restart not needed).
- Sends email notifications about invalid results (using [Sendgrid](https://sendgrid.com)'s service).
- Run schedule can also be set through the API and a separate configuration file. 
In the second case restarting the application manually after a change is also required.

### Notes

- Header values in the XML can contain unescaped spaces (i.e. for User-Agent).
- When run schedule is updated from the API WebApplicationContext is restarted automatically to begin 
using it (PID remains the same, beans & variables are re-initialized).

Next steps:

- API docs
- Sending POST requests
