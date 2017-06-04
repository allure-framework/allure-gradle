@tag
Feature: Test one

  Scenario Outline: Outline
    Given a is <a>
    And b is <b>
    When I add a to b
    Then result is <result>
    Examples:
      | a | b | result |
      | 2 | 2 | 4      |
