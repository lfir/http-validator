## HTTP Validator

[![CI Badge](https://github.com/lfir/http-validator/actions/workflows/ci.yml/badge.svg)](https://github.com/lfir/http-validator/actions/workflows/ci.yml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/1ef9731e22064eccad14c374565e12bb)](https://app.codacy.com/gh/lfir/http-validator/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/1ef9731e22064eccad14c374565e12bb)](https://app.codacy.com/gh/lfir/http-validator/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_coverage)

### Main features

- Executes HTTP requests periodically and performs some basic checks on the response data 
("validation tasks").
- Reads task information from a separate XML file.
- Sends email notifications about invalid results (using [Mailgun](https://mailgun.com)'s service).

For more details check the following site:

### [Usage guide & API reference](https://lfir.github.io/http-validator/asciidoc/api-guide.html)

### Next steps

- Making Validation Tasks more flexible
  - Ignore whitespace differences outside of values in content types like JSON
  - Check response headers & other (non-text based) content types
  - Notification settings specific to each task
