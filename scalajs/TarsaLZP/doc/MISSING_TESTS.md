### Missing tests ideas

- example data
  - share it between various types of tests

- CompressionActor
  - encoding and decoding
  - sample files and measurements needed
  
- MainActionHandler
  - use TestProbe instead of real compression actor
  - popups are probably not testable using mocked DOM under node.js
  
- end-to-end tests
  - required for testing file loading and saving
  - write using Selenium
    - Selenium can (?) handle popups for file loading, saving and for alerts
    - with them production code can have full test coverage
