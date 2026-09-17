Feature: Gradle adapter evidence

  Scenario: records a value and attachment
    * def message = 'compatibility-proof'
    * match message == 'compatibility-proof'
    * eval karate.embed(message, 'text/plain', 'proof.txt')
