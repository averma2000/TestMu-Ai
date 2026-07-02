# TestMu AI Framework

Self-healing Selenium + TestNG automation framework with AI-powered test generation, failure analysis, and flaky test classification.

## Features

| Feature | Description |
|---------|-------------|
| **Selenium + TestNG** | Page Object Model, WebDriverManager, UI & REST API tests |
| **LLM Test Generation** | Generates Login, Dashboard & API tests from module specs |
| **Failure Explainer** | LLM analyzes failures with screenshots, DOM, stack traces |
| **Self-Healing Agent** | Auto-fixes locators, waits, and API expectations; retries tests |
| **Flaky Classifier** | Tracks pass/fail history across runs; scores and reports flaky tests |
| **Learning Loop** | Prior failures inform analysis and pre-run healing |
| **ExtentReports** | HTML reports with embedded screenshots |

## Prerequisites

- Java 17+
- Maven 3.9+
- Chrome browser (for UI tests)
- OpenRouter / OpenAI / Gemini API key (for AI features)

## Quick Start

```bash
# 1. Clone and configure
cp .env.example .env
# Add OPENROUTER_API_KEY=sk-or-v1-... to .env

# 2. Run all tests
mvn test -Dheadless=true

# 3. Generate fresh LLM test cases (optional — overwrites generated/ tests)
mvn exec:java -Dexec.classpathScope=test
```

## Project Structure

```
src/main/java/com/testmu/
├── core/           # DriverManager, BaseUITest, listeners
├── pages/          # LoginPage, DashboardPage (self-healing locators)
├── api/            # RestApiClient
├── agent/
│   ├── generation/ # LLM test case generator
│   ├── flaky/      # Flaky test classifier
│   ├── healing/    # Self-healing engine
│   └── learning/   # Cross-run learning advisor
└── reporting/      # ExtentReports

src/test/java/com/testmu/tests/
├── login/          # Hand-written login tests
├── dashboard/      # Hand-written dashboard tests
├── api/            # Hand-written API tests
└── generated/      # LLM-generated tests (regenerate via exec:java)
```

## AI Configuration

Set in `.env` (gitignored):

```env
OPENROUTER_API_KEY=sk-or-v1-your-key
```

Or in `config.properties`:

```properties
ai.provider=openrouter
ai.model=openai/gpt-4o-mini
ai.agent.enabled=true
ai.self.healing.enabled=true
ai.flaky.classifier.enabled=true
```

## Reports

| Report | Location |
|--------|----------|
| Extent (latest) | `reports/extent/TestMu_Automation_Report_latest.html` |
| Failure insights | `reports/failure-insights/index.html` |
| Flaky classifier | `reports/flaky/flaky-report.html` |
| Agent memory | `agent-memory/` |

## Test Groups

- `pass` — happy path
- `edge` — boundary cases
- `fail` — intentional failures (triggers AI agent)
- `generated` — LLM-generated tests

Run specific groups:

```bash
mvn test -Dgroups=login
mvn test "-Dtest=LoginTest"
```

## Agentic Workflow

```
Test fails
  → Failure Explainer (LLM + screenshot + DOM)
  → Self-Healing Engine (locator / wait / API fix)
  → Retry test (TestNG HealingRetryAnalyzer)
  → Save to agent-memory (learning history)
  → Flaky Classifier updates stability score
  → Next run: LearningAdvisor applies pre-healing
```

## License

MIT
